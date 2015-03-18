package com.CRGames.CardsAgainstHumanity;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.Timer;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.text.DefaultCaret;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

public class ClientWindow extends JFrame {
    public static SimpleAttributeSet chatMessage;
    public static SimpleAttributeSet errorMessage;
    public static SimpleAttributeSet systemMessage;
    public static SimpleAttributeSet serverMessage;

    private JTextField userText, scoreDisplay;
    private JTextPane chatWindow;
    private JTextArea blackCardTextArea;
    private List<JTextArea> whiteCardTextAreas;
    private List<JButton> choiceButtons;
    private Container pane;

    private int score, numberOfCardChoicesReceived;
    private JSONArray hand;
    private List<JSONArray> everyonesSelectedWhiteCards;
    private JSONArray mySelectedCards;
    private JSONObject blackCard;

    private CAHClient client;
    private String name;
    private String role;
    private String serverIP;
    private Timer connectionAttemptTimer;

    public ClientWindow(String _name) {
        super("CAH-Client");

        chatMessage = new SimpleAttributeSet();
        errorMessage = new SimpleAttributeSet();
        systemMessage = new SimpleAttributeSet();
        serverMessage = new SimpleAttributeSet();

        StyleConstants.setFontFamily(chatMessage, "Arial");
        StyleConstants.setForeground(chatMessage, new Color(30, 30, 30));

        StyleConstants.setFontFamily(errorMessage, "Arial");
        StyleConstants.setForeground(errorMessage, Color.RED);

        StyleConstants.setFontFamily(systemMessage, "Arial");
        StyleConstants.setForeground(systemMessage, new Color(164, 96, 53));

        StyleConstants.setFontFamily(serverMessage, "Arial");
        StyleConstants.setForeground(serverMessage, new Color(46, 93, 47));


        numberOfCardChoicesReceived = 0;

        whiteCardTextAreas = new ArrayList<JTextArea>();
        choiceButtons = new ArrayList<JButton>();

        hand = new JSONArray();
        everyonesSelectedWhiteCards = new ArrayList<JSONArray>();
        mySelectedCards = new JSONArray();

        name = _name;
        serverIP = "";
        score = 0;

        scoreDisplay = new JTextField();
        scoreDisplay.setEditable(false);
        scoreDisplay.setHorizontalAlignment(JTextField.CENTER);
        scoreDisplay.setCaretColor(Color.BLACK);

        userText = new JTextField();
        userText.setEditable(false);
        userText.setCaretColor(Color.BLACK);
        final ClientWindow thisClientWindow = this;
        userText.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    client.send(new ChatMessage(client, thisClientWindow.getName() + ": " + e.getActionCommand().toString()));
                } catch (IllegalStateException illegalStateException) {
                    ableToType(false);
                }
                userText.setText("");
            }
        });

        chatWindow = new JTextPane();
        chatWindow.setEditable(false);

        DefaultCaret caret = (DefaultCaret) chatWindow.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

        pane = this.getContentPane();

        pane.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;

        c.gridheight = 1;
        c.weightx = 1.0;
        c.weighty = 0.4;
        c.gridwidth = 4;
        c.gridx = 0;
        c.gridy = 0;
        pane.add(new JScrollPane(chatWindow), c);

        c.weighty = 0.005;
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 4;
        pane.add(userText, c);

        c.gridx = 4;
        c.gridwidth = 1;
        pane.add(scoreDisplay, c);

        c.gridx = 4;
        c.gridy = 0;
        c.weighty = 0.4;
        c.gridheight = 1;
        c.gridwidth = 1;
        blackCardTextArea = newCard("");
        blackCardTextArea.setDisabledTextColor(new Color(255, 255, 255));
        blackCardTextArea.setBackground(Color.BLACK);
        blackCardTextArea.setCaretColor(new Color(255, 255, 255));
        blackCardTextArea.setForeground(new Color(255, 255, 255, 255));

        Border whiteBorder = BorderFactory.createLineBorder(Color.WHITE);
        blackCardTextArea.setBorder(BorderFactory.createCompoundBorder(whiteBorder,
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        pane.add(new JScrollPane(blackCardTextArea), c);

        for (int i = 0; i < 5; i++) {
            c.gridx = i;
            c.gridy = 2;
            c.weighty = 0.4;
            c.gridheight = 2;
            c.gridwidth = 1;
            whiteCardTextAreas.add(newCard(""));
            pane.add(new JScrollPane(whiteCardTextAreas.get(i)), c);
            whiteCardTextAreas.get(i).setVisible(false);

            c.gridy = 4;
            c.weighty = 0.05;
            choiceButtons.add(new JButton("Choose"));
            final int buttonIndex = i;
            choiceButtons.get(i).addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    thisClientWindow.makeCardSelection(buttonIndex);
                }
            });

            pane.add(choiceButtons.get(i), c);
        }

        setSize(850, 500);
        setVisible(true);
        setResizable(false);
        hideAllWhiteCards();
        blackCardTextArea.setVisible(false);

        JOptionPane.showMessageDialog(this,
                "When the game starts each player will draw 5 white cards." +
                        "\n One person is given 1 black card and thus they are the question" +
                        "\nasker. Everyone else answers the question or fills in the " +
                        "\nblank(s) by selecting one or more white cards from their" +
                        "\nhands. These cards are then sent anonymously to the black card holder. " +
                        "\nThe black card holder then picks the funniest white card(s)" +
                        "\nthat creates a good combination with the black card, and " +
                        "\nwhoever submitted the selected white card gets 1 Point." +
                        "\nFirst to 5 points wins the game.",
                "How To Play",
                JOptionPane.PLAIN_MESSAGE);

        Object[] options = {"Automatic",
                "Manually", "Cancel"};
        while ((serverIP == null || serverIP.equals("")) && client == null) {
            int n = JOptionPane.showOptionDialog(this,
                    "Would you like to automatically locate the server or manually input the IP of a server?",
                    "Locate Server",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    options[0]);
            try {
                if (n == 0) {
                    connectToServerAutomatically();
                } else if (n == 1) {
                    connectToServerManually();
                } else
                    System.exit(0);
            } catch (EOFException e) {
                showMessage("Client Terminated the connection.", ClientWindow.systemMessage);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                //closeConnection();
            }
        }
    }

    private void makeCardSelection(int buttonIndex) {
        try {
            if (Integer.parseInt(blackCard.get("numAnswers").toString()) == mySelectedCards.size() + 1) {
                for (JButton _button : choiceButtons) {
                    _button.setEnabled(false);
                }
            } else {
                choiceButtons.get(buttonIndex).setEnabled(false);
            }
        } catch (Exception e) {
        }

        if (role.equals("Q")) {
            mySelectedCards.addAll(everyonesSelectedWhiteCards.get(buttonIndex));
            this.whiteCardTextAreas.get(buttonIndex).setBackground(Color.GREEN);
            for (JButton _button : choiceButtons) {
                _button.setEnabled(false);
            }
        } else {
            JSONObject selectedCard = (JSONObject) hand.get(buttonIndex);
            this.whiteCardTextAreas.get(buttonIndex).setBackground(Color.YELLOW);
            mySelectedCards.add(selectedCard);
        }

        if (Integer.parseInt(blackCard.get("numAnswers").toString()) == mySelectedCards.size()) {
            client.resetOutput();
            client.send(new CardChoiceMessage(client, mySelectedCards));
        }

    }

    public void removeSelectedCardsFromHand() {
        for (int i = 0; i < mySelectedCards.size(); i++) {
            hand.remove(mySelectedCards.get(i));
        }
    }

    public JTextArea newCard(String str) {
        JTextArea label = new JTextArea(str);
        label.setLineWrap(true);
        label.setWrapStyleWord(true);
        label.setEditable(false);
        label.setDisabledTextColor(Color.BLACK);
        label.setMargin(new Insets(100, 100, 100, 100));
        //label.setForeground(new Color(255,255,255,255));
        Border black = BorderFactory.createLineBorder(Color.black);
        label.setBorder(BorderFactory.createCompoundBorder(black,
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        return label;
    }

    public void dealNewBlackCard(JSONObject _newCard) {
        this.blackCardTextArea.setText(_newCard.get("text").toString());
        this.blackCard = _newCard;
        this.mySelectedCards = new JSONArray();
    }

    public void dealWhiteCard(JSONObject _newCard) {
        this.hand.add(_newCard);
        for (JTextArea _card : this.whiteCardTextAreas) {
            if (_card.getText().equals("")) {
                _card.setText(_newCard.get("text").toString());
                return;
            }
        }
    }

    public void assignCardTextToHand() {
        int index = 0;
        for (JTextArea card : this.whiteCardTextAreas) {
            card.setText(((JSONObject) hand.get(index)).get("text").toString());
            card.setBackground(Color.WHITE);
            index++;
        }
    }

    public void setWhiteCardsToResults(List<JSONArray> _cards) //TODO: refine
    {
        clearEveryonesSelectedWhiteCards();
        hideAllWhiteCards();
        int numOfExpectedAnswers = Integer.parseInt(getBlackCard().get("numAnswers").toString());
        everyonesSelectedWhiteCards = new ArrayList<JSONArray>();
        for (int i = 0; i < _cards.size(); i++) {
            JSONArray arr = _cards.get(i);
            everyonesSelectedWhiteCards.add(arr);
            this.whiteCardTextAreas.get(i).setText("");
            for (int j = 0; j < numOfExpectedAnswers; j++) {
                JSONObject _card = (JSONObject) arr.get(j);
                this.whiteCardTextAreas.get(i).append(_card.get("text").toString());
                this.whiteCardTextAreas.get(i).setVisible(true);
                if (role.equals("A") && mySelectedCards.contains(_card))
                    this.whiteCardTextAreas.get(i).setBackground(Color.YELLOW);
                else
                    this.whiteCardTextAreas.get(i).setBackground(Color.WHITE);
                if (j + 1 != numOfExpectedAnswers)
                    this.whiteCardTextAreas.get(i).append("\n------------------------------\n");
            }
            if (this.role.equals("Q")) {
                choiceButtons.get(i).setEnabled(true);
            }
        }
        numberOfCardChoicesReceived = 0;
    }

    private void connectToServerManually() throws IOException {
        serverIP = JOptionPane.showInputDialog("Enter the server IP address.");
        if (!(serverIP == null || serverIP.equals(""))) {
            client = new CAHClient(serverIP, 3359, this);
        }
    }

    private void connectToServerAutomatically() throws IOException {
        showMessage("Attempting to locate server for 30 seconds.", ClientWindow.systemMessage);
        connectionAttemptTimer = new Timer();
        connectionAttemptTimer.schedule(new AutomaticConnectionTask(connectionAttemptTimer), 30000);
        client = new CAHClient(3359, this);
    }

    public void stopConnectionTimer() {
        connectionAttemptTimer.cancel();
    }

    public void showMessage(final String _message, final SimpleAttributeSet attributeSet) {
        SwingUtilities.invokeLater(
                new Runnable() {
                    @Override
                    public void run() {
                        try {
                            chatWindow.getDocument().insertString(chatWindow.getDocument().getLength(), "\n" + _message, attributeSet);
                        } catch (Exception e) {
                        }
                    }
                }
        );
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    @Override
    public String getName() {
        return name;
    }

    public void hideEverything()
    {
        hideAllWhiteCards();
        ableToType(false);
        setBlackCardVisibility(false);
        setScoreDisplay("");
        showScoreDisplayVisibility(false);
    }

    public void hideAllWhiteCards()
    {
        for (JTextArea _card: this.whiteCardTextAreas)
        {
            _card.setVisible(false);
        }
        for (JButton _button: this.choiceButtons)
        {
            _button.setEnabled(false);
        }
    }

    public void showAllWhiteCards()
    {
        for (JTextArea _card: this.whiteCardTextAreas)
        {
            _card.setVisible(true);
        }
        for (JButton _button: this.choiceButtons)
        {
            _button.setEnabled(true);
        }
    }

    public void updateCardChoices(JSONArray _newCards) //TODO: refine
    {
        this.everyonesSelectedWhiteCards.add(_newCards);
        int numOfExpectedAnswers = Integer.parseInt(getBlackCard().get("numAnswers").toString());
        whiteCardTextAreas.get(numberOfCardChoicesReceived).setText("");
        for (int i = 0; i < _newCards.size(); i++)
        {
            JSONObject obj = (JSONObject) _newCards.get(i);
            whiteCardTextAreas.get(numberOfCardChoicesReceived).append(obj.get("text").toString());
            whiteCardTextAreas.get(numberOfCardChoicesReceived).setVisible(true);
            if (role.equals("A") && mySelectedCards.contains(obj))
                whiteCardTextAreas.get(numberOfCardChoicesReceived).setBackground(Color.YELLOW);
            else
                whiteCardTextAreas.get(numberOfCardChoicesReceived).setBackground(Color.WHITE);

            if (i+1 != numOfExpectedAnswers)
                whiteCardTextAreas.get(numberOfCardChoicesReceived).append("\n------------------------------\n");
            }
        numberOfCardChoicesReceived++;
    }

    public void showWinningCard(JSONObject winningCard)
    {
        for (JTextArea card: this.whiteCardTextAreas)
        {
            if (card.getText().contains(winningCard.get("text").toString()))
            {
                card.setBackground(Color.GREEN);
            } else if (card.getBackground() != Color.YELLOW)
            {
                card.setBackground(Color.WHITE);
            }
        }
    }

    public void setBlackCardVisibility(boolean tof) {
        this.blackCardTextArea.setVisible(tof);
    }

    public void awardPoint()
    {
        score++;
    }

    public void clearEveryonesSelectedWhiteCards() {
        this.everyonesSelectedWhiteCards.clear();
    }

    public void clearMySelectedWhiteCards() {
        this.mySelectedCards.clear();
    }

    public int getScore() {
        return score;
    }

    public void ableToType(final boolean tof)
    {
        SwingUtilities.invokeLater(
                new Runnable() {
                    @Override
                    public void run() {
                        userText.setEditable(tof);
                    }
                }
        );
    }

    public void resetForNewGame()
    {
        scoreDisplay.setText("0 Points");
        blackCardTextArea.setText("");
        for (JTextArea card: whiteCardTextAreas)
        {
            card.setText("");
            card.setBackground(Color.WHITE);
        }
        setRole("");
        score = 0;
        numberOfCardChoicesReceived = 0;
        hand = new JSONArray();
        everyonesSelectedWhiteCards = new ArrayList<JSONArray>();
        mySelectedCards = new JSONArray();
        hideAllWhiteCards();
    }

    public JSONObject getBlackCard() {
        return blackCard;
    }

    public void setScoreDisplay(String scoreDisplay) {
        this.scoreDisplay.setText(scoreDisplay);
    }

    public void showScoreDisplayVisibility(boolean tof) {
        this.scoreDisplay.setVisible(tof);
        this.scoreDisplay.setBackground(Color.WHITE);
        Border black = BorderFactory.createLineBorder(Color.black);
        scoreDisplay.setBorder(BorderFactory.createCompoundBorder(black,
                BorderFactory.createEmptyBorder(1, 10, 1, 10)));
    }

    private class AutomaticConnectionTask extends TimerTask {
        Timer t;
        public AutomaticConnectionTask(Timer _t)
        {
            t = _t;
        }

        public void run()
        {
            showMessage("Failed to find server after 30 seconds of looking.", ClientWindow.errorMessage);
            client.disconnect();
            client = null;
            t.cancel();
        }
    }
}

