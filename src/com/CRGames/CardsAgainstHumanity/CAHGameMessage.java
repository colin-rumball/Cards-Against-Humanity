package com.CRGames.CardsAgainstHumanity;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import com.CRGames.GameNetworking.*;

import java.util.*;

public class CAHGameMessage extends EventObject implements GameMessage
{
    private int IDofSender;
    public static enum MessageType implements MessageTypeInterface {
        CONNECTION_ACCEPTED,
        CHAT_MESSAGE,
        GAME_BEGIN,
        NEW_BLACK_CARD,
        NEW_WHITE_CARD,
        ROLE_ASSIGNMENT,
        CARD_CHOICE,
        NAME_ASSIGNMENT,
        ALL_CARDS_PLAYED,
        WINNING_CARD,
        NEW_ROUND_MESSAGE,
        SERVER_COMMAND,
        START_VOTE_MESSAGE,
        AWARD_POINT_MESSAGE,
        GAME_OVER,
        SERVER_MESSAGE,
        HEART_BEAT_MESSAGE,
        RESET_ROUND_MESSAGE
    }

    private MessageType messageType;

    public CAHGameMessage(Object _src, MessageType _type) {
        super(_src);
        messageType = _type;
        if (_src instanceof CAHClient)
        {
            IDofSender = ((Client)_src).getID();
        } else if (_src instanceof CAHServer)
        {
            IDofSender = 0;
        }
    }

    public MessageType getType() {
        return messageType;
    }

    @Override
    public int getIDofSender() {
        return IDofSender;
    }
}

class NameAssignmentMessage extends CAHGameMessage {
    String name;

    public NameAssignmentMessage(Object _src, String _name) {
        super(_src, MessageType.NAME_ASSIGNMENT);
        name = _name;
    }

    public String getName() {
        return name;
    }
}

class ChatMessage extends CAHGameMessage {
    String message;

    public ChatMessage(Object _src, String _message) {
        super(_src, MessageType.CHAT_MESSAGE);
        message = _message;
    }

    public String getMessage() {
        return message;
    }
}

class ServerMessage extends CAHGameMessage {
    String message;

    public ServerMessage(Object _src, String _message) {
        super(_src, MessageType.SERVER_MESSAGE);
        message = _message;
    }

    public String getMessage() {
        return message;
    }
}

class NewBlackCardMessage extends CAHGameMessage {
    JSONObject card;

    public NewBlackCardMessage(Object _src, JSONObject _card) {
        super(_src, MessageType.NEW_BLACK_CARD);
        card = _card;
    }

    public JSONObject getCard() {
        return card;
    }
}

class NewWhiteCardMessage extends CAHGameMessage {
    JSONObject card;

    public NewWhiteCardMessage(Object _src, JSONObject _card) {
        super(_src, MessageType.NEW_WHITE_CARD);
        card = _card;
    }

    public JSONObject getCard() {
        return card;
    }
}

class RoleAssignmentMessage extends CAHGameMessage {
    String role;

    public RoleAssignmentMessage(Object _src, String _role) {
        super(_src, MessageType.ROLE_ASSIGNMENT);
        role = _role;
    }

    public String getRole() {
        return role;
    }
}

class CardChoiceMessage extends CAHGameMessage {
    JSONArray cards;

    public CardChoiceMessage(Object _src, JSONArray _cards) {
        super(_src, MessageType.CARD_CHOICE);
        cards = _cards;
    }

    public JSONArray getCards() {
        return cards;
    }
}

class AllCardsPlayedMessage extends CAHGameMessage {
    List<JSONArray> cards;

    public AllCardsPlayedMessage(Object _src, List<JSONArray> _cards) {
        super(_src, MessageType.ALL_CARDS_PLAYED);
        cards = _cards;
    }

    public List<JSONArray> getCards() {
        return cards;
    }
}

class WinningCardMessage extends CAHGameMessage {
    JSONObject card;

    public WinningCardMessage(Object _src, JSONObject _card) {
        super(_src, MessageType.WINNING_CARD);
        card = _card;
    }

    public JSONObject getCard() {
        return card;
    }
}