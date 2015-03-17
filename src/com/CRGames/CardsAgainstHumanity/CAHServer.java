package com.CRGames.CardsAgainstHumanity;

import com.CRGames.GameNetworking.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.*;
import java.net.*;
import java.util.*;

public class CAHServer extends Server
{
    public final int MINIMUM_PLAYERS_REQUIRED = 4;

    private Game game;
    private ServerConsole serverConsole;
    private boolean beginningCountdownStarted;
    private HashMap<Integer, CAHConnectedClient> CAHclients;

    private Timer gameBeginningCountdown;

    public CAHServer(ServerConsole _serverConsole) throws IOException
    {
        super(3359, true, 6);

        beginningCountdownStarted = false;
        serverConsole = _serverConsole;
        CAHclients = new HashMap();
    }

    @Override
    public synchronized void start() {
        super.start();
        try {
            serverConsole.showMessage("Server running at IP address: " + InetAddress.getLocalHost().getHostAddress());
        } catch (Exception e)
        {
            System.out.println("Unknown host exception.");
        }
    }

    public void sendToClient(CAHConnectedClient c, GameMessage message)
    {
        this.sendToClient(c.getID(), message);
    }

    @Override
    protected void handleReceivedMessage(GameMessage gameMessage)
    {
        CAHConnectedClient client = getCAHClient(gameMessage.getIDofSender());
        switch (((CAHGameMessage)gameMessage).getType())
        {
            case NAME_ASSIGNMENT:
                NameAssignmentMessage nameAssignmentMessage = (NameAssignmentMessage)gameMessage;
                client.setName(nameAssignmentMessage.getName());
                serverConsole.showMessage("Name received name: "+nameAssignmentMessage.getName());
                break;
            case CHAT_MESSAGE:
                ChatMessage chatMessage = (ChatMessage)gameMessage;
                serverConsole.showMessage(chatMessage.getMessage());
                sendToAllClients(gameMessage);
                break;
            case CARD_CHOICE:
                CardChoiceMessage cardChoiceMessage = (CardChoiceMessage)gameMessage;
                JSONArray allChosenCards = cardChoiceMessage.getCards();
                serverConsole.showMessage("Card choice from: "+ client.getName() + " role: " + client.getRole() +" message:: "+cardChoiceMessage.getCards().toString());

                if (cardChoiceMessage != null)
                {
                    if (client.getRole().equals("Q"))
                    {
                        if (game.cardsPlayedContains(allChosenCards))
                        {
                            for (CAHConnectedClient c: getCAHClients())
                            {
                                if (c.handContains(allChosenCards))
                                {
                                    serverConsole.showMessage("Awarding 1 point to " + c.getName());
                                    sendToAllClients(new ServerMessage(this, "Server: 1 Point awarded to: " + c.getName() + "."));
                                    sendToClient(c, new CAHGameMessage(this, CAHGameMessage.MessageType.AWARD_POINT_MESSAGE));
                                    c.awardPoint();

                                    if (c.getPointTotal() >= 5)
                                    {
                                        serverConsole.showMessage(c.getName() + " has 5 points. Ending game.");
                                        sendToAllClients(new ServerMessage(this, "Server: The winner is " + c.getName() + " with " + c.getPointTotal() + " points."));
                                        sendToAllClients(new ServerMessage(this, "Server: New game beginning in 20 seconds."));

                                        serverConsole.showMessage("Creating timer to start new game in " + (20000/1000) + " seconds.");
                                        //Timer timer = new Timer();
                                        //timer.schedule(new EndOfGameTask(game, this, timer), 20000);
                                        game.setGameState(Game.GameState.STARTING_NEW_GAME);
                                        return;
                                    }
                                }
                                c.removeSelectedCardsFromHand();
                            }

                            serverConsole.showMessage("Sending winning card message.");
                            sendToAllClients(new WinningCardMessage(this, (JSONObject)allChosenCards.get(0))); //TODO: send more
                            game.clearWhiteCardsPlayed();

                            sendToAllClients(new ServerMessage(this, "Server: Next round beginning in 15 seconds."));
                            game.createEndOfRoundTask();
                        } else
                        {
                            serverConsole.showMessage(client.getName() + " attempted to play cards " + allChosenCards.toString() + " but these cards are not found inside of cards played.");
                        }
                    } else
                    {
                        if (client.handContains(allChosenCards) && allChosenCards.size() == game.getNumberOfExpectedAnswers())
                        {
                            client.setSelectedCards(allChosenCards);
                            game.addToWhiteCardsPlayed(allChosenCards);
                            for (CAHConnectedClient c: getCAHClients())
                            {
                                if (c.getRole().equals("Q")) {
                                    sendToClient(c, new ServerMessage(this, "Server: A player has chosen their cards."));
                                    sendToClient(c, cardChoiceMessage);
                                } else {
                                    sendToClient(c, new ServerMessage(this, "Server: " + client.getName() + " has chosen their cards."));
                                }
                            }
                        } else
                        {
                            serverConsole.showMessage(client.getName() + " attempted to play cards " + allChosenCards.toString() + " but these cards are not found in client's hand..");
                        }

                        if (allClientsHavePlayedCards())
                        {
                            serverConsole.showMessage("Writing all cards played message to all clients after a client played a card.");
                            resetOutput();
                            sendToAllClients(new AllCardsPlayedMessage(this, game.getPlayedCards()));
                            game.setGameState(Game.GameState.WAITING_FOR_ASKER);
                        }
                    }
                }
                break;
            case WINNING_CARD:
                WinningCardMessage winningCardMessage = (WinningCardMessage)gameMessage;
                JSONObject winningCard = winningCardMessage.getCard();
                serverConsole.showMessage("The winning card is: " + winningCard.get("text"));
                break;
        }
    }

    private boolean allClientsHavePlayedCards()
    {
        boolean allPlayersDone = true;
        for (CAHConnectedClient c: getCAHClients())
        {
            if (c.getNumberOfPlayedCards() < game.getNumberOfExpectedAnswers() && !c.getRole().equals("Q"))
                allPlayersDone = false;
        }
        return allPlayersDone;
    }

    @Override
    protected void playerConnected(int playerID)
    {
        serverConsole.showMessage("New player Connected. Currently: " + getNumberOfConnectedPlayers() + " players connected.");
        sendToClient(playerID, new CAHGameMessage(this, CAHGameMessage.MessageType.CONNECTION_ACCEPTED));
        if (getNumberOfConnectedPlayers() >= MINIMUM_PLAYERS_REQUIRED && !game.hasStarted())
        {
            sendToAllClients(new ServerMessage(this, "Server: The game will begin shortly."));
            if (!beginningCountdownStarted)
                startNewBeginningTimer(15);
        } else
            sendToClient(playerID, new ServerMessage(this, "Server: Waiting for additional players."));
        CAHclients.put(playerID, new CAHConnectedClient(playerID));
    }

    @Override
    protected void playerDisconnected(int playerID)
    {
        serverConsole.showMessage("A player has disconnected.");
        CAHConnectedClient client = getCAHClient(playerID);

        if (game.hasStarted())
        {
            if (getNumberOfConnectedPlayers() < 3)
            {
                //game.shutDown();
                //return;
            }

            switch (game.getGameState())
            {
                case INITIALIZING:
                    if (getNumberOfConnectedPlayers() < MINIMUM_PLAYERS_REQUIRED)
                    {
                        sendToAllClients(new ServerMessage(game, "Server: Minimum players required not met. Waiting for additional players."));
                        stopBeginningTimer();
                    }
                    break;
                case BEGINNING:
                case WAITING_FOR_ASKER:
                case WAITING_FOR_ANSWERS:
                    if (client.getRole().equals("Q"))
                    {
                        sendToAllClients(new ServerMessage(this, "The Black Card Holder has left the game. Resetting round"));
                        game.resetCurrentRound();
                    } else {
                        if (game.cardsPlayedContains(client.getSelectedCards())) {
                            game.removeCardsFromPlayedCards(client.getSelectedCards());
                        }
                        if (allClientsHavePlayedCards()) {
                            serverConsole.showMessage("Sending all cards played message after a client disconnected.");
                            sendToAllClients(new AllCardsPlayedMessage(this, game.getPlayedCards()));
                            game.setGameState(Game.GameState.WAITING_FOR_ASKER);
                        }
                    }
                    break;
            }
        }
        //writeMessageToAllClients(new ServerMessage(game, "Server: " + receivingClient.getName() + " has left."));
        CAHclients.remove(playerID);
    }

    public void setGame(Game _game)
    {
        game = _game;
    }

    public CAHConnectedClient getCAHClient(int id) {
        return CAHclients.get(id);
    }

    public CAHConnectedClient[] getCAHClients()
    {
        return CAHclients.values().toArray(new CAHConnectedClient[0]);
    }

    public void startNewBeginningTimer(int _sec) {
        if (gameBeginningCountdown != null) {
            serverConsole.showMessage("Cancelling old beginning countdown to create new one.");
            gameBeginningCountdown.cancel();
        }

        serverConsole.showMessage("Starting new beginning countdown of " + (_sec) + " seconds.");
        gameBeginningCountdown = new Timer();
        gameBeginningCountdown.schedule(new BeginGameTask(gameBeginningCountdown, game), _sec*1000);
        beginningCountdownStarted = true;
    }

    public void stopBeginningTimer()
    {
        serverConsole.showMessage("Stopping beginning countdown.");
        gameBeginningCountdown.cancel();
        beginningCountdownStarted = false;
    }

    class BeginGameTask extends TimerTask
    {
        Game game;
        Timer t;
        public BeginGameTask(Timer _t, Game _game)
        {
            t = _t;
            game = _game;
        }

        public void run()
        {
            game.begin();
            shutdownNewClientListener();
            t.cancel();
        }
    }

    protected class CAHConnectedClient
    {
        private int ID;
        private String name;
        private String role;
        private JSONArray hand;
        private JSONArray selectedCards;
        private int pointTotal;

        public CAHConnectedClient(int _id)
        {
            name = "A Player";
            hand = new JSONArray();
            selectedCards = new JSONArray();
            pointTotal = 0;
            ID = _id;
        }

        public void dealCard(JSONObject _card)
        {
            hand.add(_card);
        }

        public void setRole(String _role)
        {
            role = _role;
        }

        public String getRole() {
            return role;
        }

        public JSONArray getHand() {
            return hand;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void removeCardFromHand(JSONObject _card)
        {
            this.hand.remove(_card);
        }

        public void removeSelectedCardsFromHand()
        {
            JSONArray copyOfCards = getSelectedCards();
            for (int i = 0; i < copyOfCards.size(); i++)
            {
                removeCardFromHand((JSONObject)copyOfCards.get(i));
            }
        }

        public int getNumberOfPlayedCards() {
            return selectedCards.size();
        }

        public JSONArray getSelectedCards() {
            return selectedCards;
        }

        public int getID()
        {
            return ID;
        }

        public void addToSelectedCards(JSONObject selectedCard) {
            this.selectedCards.add(selectedCard);
        }

        public void awardPoint()
        {
            pointTotal++;
        }

        public int getPointTotal() {
            return pointTotal;
        }

        public void resetForNewGame()
        {
            resetNumberOfPlayedCards();
            role = "";
            hand = new JSONArray();
            selectedCards = new JSONArray();
            pointTotal = 0;
        }

        public void resetNumberOfPlayedCards() {
            selectedCards = new JSONArray();
        }

        public boolean handContains(JSONArray allChosenCards) {
            for (int i = 0 ; i < allChosenCards.size(); i++)
            {
                JSONObject obj = (JSONObject) allChosenCards.get(i);
                if (!hand.contains(obj))
                    return false;
            }
            return true;
        }

        public void setSelectedCards(JSONArray allChosenCards) {
            selectedCards = allChosenCards;
        }
    }
}


