package com.CRGames.CardsAgainstHumanity;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ServerConsole extends JFrame
{
    private JTextField userText;
    private JTextArea console;
    private CAHServer server;

    public ServerConsole()
    {
        super("CAH-Server");
        userText = new JTextField();
        userText.setEditable(false);
        userText.addActionListener(
                new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        try {
                            server.sendToAllClients(new ServerMessage(server, "Server: "+e.getActionCommand()));
                        } catch (Exception exception){}
                        showMessage("Server: "+e.getActionCommand());
                        userText.setText("");
                    }
                }
        );
        add(userText, BorderLayout.NORTH);
        console = new JTextArea();
        console.setEditable(false);
        add(new JScrollPane(console));
        console.setLineWrap(true);
        console.setWrapStyleWord(true);
        console.setEditable(false);
        setSize(800, 400);
        setVisible(true);
        setResizable(false);
    }

    public void showMessage(final String _message)
    {
        SwingUtilities.invokeLater(
                new Runnable() {
                    @Override
                    public void run() {
                        console.append("\n" + _message);
                    }
                }
        );
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

    public void setServer(CAHServer _server) {
        this.server = _server;
        ableToType(true);
    }
}
