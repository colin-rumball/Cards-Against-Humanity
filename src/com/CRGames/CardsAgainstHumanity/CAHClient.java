package com.CRGames.CardsAgainstHumanity;

import com.CRGames.GameNetworking.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.util.List;

public class CAHClient extends Client
{
    ClientWindow clientWindow;

    public CAHClient(int _port, ClientWindow _clientWindow) throws IOException {
        super(_port);
        clientWindow = _clientWindow;
    }

    public CAHClient(String _IP, int _port, ClientWindow _clientWindow) throws IOException {
        super(_IP, _port);
        clientWindow = _clientWindow;
    }

    @Override
    protected void connectionClosedByError(String message)
    {
        clientWindow.showMessage("ERROR: Connection to server was lost.", ClientWindow.errorMessage);
        clientWindow.hideEverything();
    }

    @Override
    protected void serverShutdown(String message)
    {
        clientWindow.showMessage("ERROR: Connection to server was lost.", ClientWindow.errorMessage);
        clientWindow.hideEverything();
    }

    @Override
    protected void messageReceived(GameMessage gameMessage)
    {
        switch (((CAHGameMessage)gameMessage).getType())
        {
            case CONNECTION_ACCEPTED:
                clientWindow.stopConnectionTimer();
                clientWindow.ableToType(true);
                send(new NameAssignmentMessage(this, clientWindow.getName()));
                break;
            case CHAT_MESSAGE:
                ChatMessage chatMessage = (ChatMessage)gameMessage;
                clientWindow.showMessage(chatMessage.getMessage(), ClientWindow.chatMessage);
                break;
            case SERVER_MESSAGE:
                ServerMessage serverMessage = (ServerMessage)gameMessage;
                clientWindow.showMessage(serverMessage.getMessage(), ClientWindow.serverMessage);
                break;
            case GAME_BEGIN:
                clientWindow.setBlackCardVisibility(true);
                clientWindow.showScoreDisplayVisibility(true);
                clientWindow.setScoreDisplay("0 Points");
                break;
            case ROLE_ASSIGNMENT:
                RoleAssignmentMessage roleAssignment = (RoleAssignmentMessage)gameMessage;
                clientWindow.setRole(roleAssignment.getRole());
                if (roleAssignment.getRole().equals("A"))
                {
                    clientWindow.showMessage("Role assigned to question answerer.", ClientWindow.systemMessage);
                    clientWindow.showAllWhiteCards();
                } else {
                    clientWindow.showMessage("Role assigned to question asker.", ClientWindow.systemMessage);
                    clientWindow.hideAllWhiteCards();
                }
                break;
            case NEW_BLACK_CARD:
                NewBlackCardMessage newBlackCardMessage = (NewBlackCardMessage)gameMessage;
                JSONObject newCardBlack = newBlackCardMessage.getCard();
                clientWindow.dealNewBlackCard(newCardBlack);
                break;
            case NEW_WHITE_CARD:
                NewWhiteCardMessage newWhiteCardMessage = (NewWhiteCardMessage)gameMessage;
                JSONObject newCardWhite = newWhiteCardMessage.getCard();
                clientWindow.dealWhiteCard(newCardWhite);
                break;
            case CARD_CHOICE:
                CardChoiceMessage cardChoiceMessage = (CardChoiceMessage)gameMessage;
                JSONArray allChosenCards = cardChoiceMessage.getCards();
                clientWindow.updateCardChoices(allChosenCards);
                break;
            case ALL_CARDS_PLAYED:
                clientWindow.showMessage("Server: All players have chosen their cards.", ClientWindow.serverMessage);
                AllCardsPlayedMessage allCardsPlayedMessage = (AllCardsPlayedMessage)gameMessage;
                List<JSONArray> allPlayedCards = allCardsPlayedMessage.getCards();
                clientWindow.setWhiteCardsToResults(allPlayedCards);
                break;
            case WINNING_CARD:
                clientWindow.removeSelectedCardsFromHand();
                WinningCardMessage winningCardMessage = (WinningCardMessage)gameMessage;
                JSONObject winningCard = winningCardMessage.getCard();
                clientWindow.showWinningCard(winningCard);
                break;
            case NEW_ROUND_MESSAGE:
                clientWindow.clearEveryonesSelectedWhiteCards();
                clientWindow.clearMySelectedWhiteCards();
                clientWindow.showAllWhiteCards();
                clientWindow.assignCardTextToHand();
                break;
            case RESET_ROUND_MESSAGE:
                clientWindow.clearEveryonesSelectedWhiteCards();
                clientWindow.clearMySelectedWhiteCards();
                clientWindow.showAllWhiteCards();
                clientWindow.assignCardTextToHand();
                break;
            /*case START_VOTE_MESSAGE:
                StartVoteMessage startVoteMessage = new StartVoteMessage(_client, _client.readInput());
                myClient.showMessage("A vote has been cast.", Client.systemMessage);
                break;*/
            case AWARD_POINT_MESSAGE:
                clientWindow.awardPoint();
                clientWindow.showMessage("You have been awarded 1 point!", ClientWindow.systemMessage);
                clientWindow.setScoreDisplay(Integer.toString(clientWindow.getScore()) + " Points");
                break;
            case GAME_OVER:
                clientWindow.resetForNewGame();
                break;
        }
    }
}
