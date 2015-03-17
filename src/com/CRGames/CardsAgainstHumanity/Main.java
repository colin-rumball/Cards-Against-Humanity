package com.CRGames.CardsAgainstHumanity;


import javax.swing.*;
import java.io.IOException;

public class Main extends JFrame
{
    public void attachShutDownHook(final CAHServer server){
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                server.shutDown();
            }
        });
        System.out.println("Shut Down Hook Attached.");
    }

    public static void main(String[] args)
    {
        Object[] options = {"Client",
                "Server", "Cancel"};
        int n = JOptionPane.showOptionDialog(new JFrame(),
                "Run as client or server?",
                "Client or Server",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]);
            if (n == 0)
            {
                String name = JOptionPane.showInputDialog("Enter your name");
                if (name != null && name.length() > 1)
                {
                    ClientWindow clientWindow = new ClientWindow(name);
                    clientWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                }
            } else if (n == 1)
            {
                ServerConsole serverConsole = new ServerConsole();
                serverConsole.showMessage("Initializing Server.");
                CAHServer server;
                try
                {
                    server = new CAHServer(serverConsole);
                    serverConsole.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                    serverConsole.showMessage("Starting Server.");
                    server.start();
                    serverConsole.setServer(server);
                    Game game = new Game(serverConsole, server);
                    server.setGame(game);
                    Main sample = new Main();
                    sample.attachShutDownHook(server);
                } catch (IOException e)
                {
                    System.out.println("Server io exception at initialization.");
                }
            } else
                System.exit(0);
    }
}
