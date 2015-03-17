package com.CRGames.CardsAgainstHumanity;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.*;

public class Game
{
    public enum GameState {
        INITIALIZING, BEGINNING, WAITING_FOR_ANSWERS, WAITING_FOR_ASKER, STARTING_NEW_ROUND,
        STARTING_NEW_GAME, SHUTTING_DOWN
    }

    private GameState gameState;

    private boolean started = false;
    private ServerConsole serverConsole;
    private CAHServer server;
    private JSON_Loader jsonFile;
    private Deck whiteDeck;
    private Deck blackDeck;
    private JSONObject currentBlackCard;
    private List<JSONArray> whiteCardsPlayed;
    private int numberOfExpectedAnswers;

    public Game(ServerConsole _serverConsole, CAHServer _server)
    {
        super();
        _serverConsole.showMessage("Initializing game.");
        gameState = GameState.INITIALIZING;
        serverConsole = _serverConsole;
        server = _server;
        whiteCardsPlayed = new ArrayList<JSONArray>();
        String fileName = "cards.json";
        serverConsole.showMessage("Reading JSON data from " + fileName);
        jsonFile = new JSON_Loader(fileName);
        whiteDeck = new Deck(jsonFile.getAllObjectsWithValue("cardType", "A"));
        blackDeck = new Deck(jsonFile.getAllObjectsWithValue("cardType", "Q"));
    }

    public void begin()
    {
        serverConsole.showMessage("Beginning game.");
        gameState = GameState.BEGINNING;
        started = true;
        server.sendToAllClients(new CAHGameMessage(this, CAHGameMessage.MessageType.GAME_BEGIN));
        assignInitialQuestionAsker();
        dealInitialHands();
    }

    public void assignInitialQuestionAsker()
    {
        serverConsole.showMessage("Assigning initial black card holder.");
        CAHServer.CAHConnectedClient[] CAHClients = server.getCAHClients();
        Random rand = new Random();
        int n = rand.nextInt(CAHClients.length);
        int counter = 0;
        for (CAHServer.CAHConnectedClient c: CAHClients)
        {
            if (counter != n)
            {
                c.setRole("A");
                server.sendToClient(c, new RoleAssignmentMessage(this, "A"));
            }
            else {
                c.setRole("Q");
                server.sendToClient(c, new RoleAssignmentMessage(this, "Q"));
            }
            counter++;
        }
    }

    public void assignNewQuestionAsker()
    {
        serverConsole.showMessage("Assigning new question asker.");
        CAHServer.CAHConnectedClient[] CAHClients = server.getCAHClients();
        int index = 0, counter = 0;
        for (CAHServer.CAHConnectedClient c: CAHClients)
        {
            if (c.getRole().equals("Q")) {

                index = counter;
            }
            counter++;
        }

        if (index == CAHClients.length-1) {
            index = -1;
        }

        counter = 0;
        for (CAHServer.CAHConnectedClient c: CAHClients)
        {
            if (counter == index+1)
            {
                c.setRole("Q");
                server.sendToClient(c, new RoleAssignmentMessage(this, "Q"));
            } else {
                c.setRole("A");
                server.sendToClient(c, new RoleAssignmentMessage(this, "A"));
            }
            counter++;
        }
    }

    public void dealInitialHands()
    {
        serverConsole.showMessage("Dealing initial hands of white cards.");
        generateNewBlackCard();
        CAHServer.CAHConnectedClient[] CAHClients = server.getCAHClients();
        for (CAHServer.CAHConnectedClient c: CAHClients)
        {
            for (int i = 0; i < 5; i++) {
                JSONObject newCard = whiteDeck.getRandomCard();
                c.dealCard(newCard);
                server.sendToClient(c, new NewWhiteCardMessage(this, newCard));
            }
        }
        gameState = GameState.WAITING_FOR_ANSWERS;
        server.sendToAllClients(new NewBlackCardMessage(this, currentBlackCard));
    }

    public void dealNewCards()
    {
        serverConsole.showMessage("Dealing new white cards out.");
        CAHServer.CAHConnectedClient[] CAHClients = server.getCAHClients();
        for (CAHServer.CAHConnectedClient c: CAHClients)
        {
            if (c.getRole().equals("A"))
            {
                for (int i = 0; i < Integer.parseInt(currentBlackCard.get("numAnswers").toString()); i++)
                {
                    JSONObject newCard = whiteDeck.getRandomCard();
                    c.dealCard(newCard);
                    server.sendToClient(c, new NewWhiteCardMessage(this, newCard));
                }
            }
        }
        generateNewBlackCard();
    }

    public void generateNewBlackCard()
    {
        currentBlackCard = blackDeck.getRandomCard();
        server.sendToAllClients(new NewBlackCardMessage(this, currentBlackCard));
        numberOfExpectedAnswers = Integer.parseInt(currentBlackCard.get("numAnswers").toString());
    }

    public void whiteCardPlayed(JSONArray _cards)
    {
        whiteCardsPlayed.add(_cards);
    }

    public List<JSONArray> getPlayedCards() {
        return this.whiteCardsPlayed;
    }

    public void clearWhiteCardsPlayed() {
        serverConsole.showMessage("Clearing white cards played this round.");
        whiteCardsPlayed.clear();
    }

    public void endCurrentRound()
    {
        server.sendToAllClients(new ServerMessage(server, "Server: Beginning next round."));
        dealNewCards();
        server.sendToAllClients(new CAHGameMessage(server, CAHGameMessage.MessageType.NEW_ROUND_MESSAGE));
        assignNewQuestionAsker();
        resetAllClientsForNewRound();
        setGameState(GameState.WAITING_FOR_ANSWERS);
    }

    public void createEndOfRoundTask()
    {
        serverConsole.showMessage("Creating timer to start new round in " + (15000 / 1000) + " seconds.");
        Timer timer = new Timer();
        timer.schedule(new EndOfRoundTask(timer, this), 15000);
        setGameState(Game.GameState.STARTING_NEW_ROUND);
    }

    public void resetCurrentRound()
    {
        serverConsole.showMessage("Resetting current round.");
        server.sendToAllClients(new CAHGameMessage(server, CAHGameMessage.MessageType.RESET_ROUND_MESSAGE));
        clearWhiteCardsPlayed();
        assignNewQuestionAsker();
        resetAllClientsForNewRound();
        setGameState(Game.GameState.WAITING_FOR_ANSWERS);
    }

    public void resetAllClientsForNewRound() {
        CAHServer.CAHConnectedClient[] CAHClients = server.getCAHClients();
        for (CAHServer.CAHConnectedClient c: CAHClients)
        {
            c.resetNumberOfPlayedCards();
        }
    }

    public boolean hasStarted() {
        return started;
    }

    public void resetForNewGame()
    {
        CAHServer.CAHConnectedClient[] CAHClients = server.getCAHClients();
        for (CAHServer.CAHConnectedClient c: CAHClients)
        {
            c.resetForNewGame();
            whiteDeck.refillAndShuffle(jsonFile.getAllObjectsWithValue("cardType", "A"));
            blackDeck.refillAndShuffle(jsonFile.getAllObjectsWithValue("cardType", "Q"));
            currentBlackCard = null;
            whiteCardsPlayed = new ArrayList<JSONArray>();
        }
    }

    public int getNumberOfExpectedAnswers() {
        return numberOfExpectedAnswers;
    }

    public boolean cardsPlayedContains(JSONArray _cards)
    {
        return whiteCardsPlayed.contains(_cards);
    }

    public void removeCardsFromPlayedCards(JSONArray cards) {
        whiteCardsPlayed.remove(cards);
    }

    public void addToWhiteCardsPlayed(JSONArray allChosenCards) {
        serverConsole.showMessage("Adding cards to played white cards. Cards: " + allChosenCards.toString());
        whiteCardsPlayed.add(allChosenCards);
    }

    public void setGameState(GameState gameState) {
        this.gameState = gameState;
    }

    public GameState getGameState() {
        return this.gameState;
    }

    class EndOfRoundTask extends TimerTask
    {
        Game game;
        Timer t;
        public EndOfRoundTask(Timer _t, Game _game)
        {
            t = _t;
            game = _game;
        }

        public void run()
        {
            game.endCurrentRound();
            t.cancel();
        }
    }
}