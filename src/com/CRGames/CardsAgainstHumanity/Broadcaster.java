package com.CRGames.CardsAgainstHumanity;

import java.net.InetAddress;

/**
 * Created by Colin on 21/02/2015.
 */
import java.io.*;
import java.net.*;
import java.util.*;

public class Broadcaster implements Runnable
{
    private volatile boolean broadcasting = true;
    protected DatagramSocket socket = null;

    public Broadcaster() throws IOException {
        this("Broadcaster Thread");
    }

    public Broadcaster(String name) throws IOException {
        //super(name);
        socket = new DatagramSocket(4445);
    }

    public void run()
    {
        while (broadcasting)
        {
            //System.out.println("isBroadcasting");
            try
            {
                byte[] buf = new byte[256];

                String dString = null;
                dString = new Date().toString();
                buf = dString.getBytes();

                InetAddress group = InetAddress.getByName("230.0.0.1");
                DatagramPacket packet = new DatagramPacket(buf, buf.length, group, 3369);
                socket.send(packet);
                Thread.sleep(5000);
            } catch (IOException e)
            {
                System.out.println("Broadcaster io exception");
                e.printStackTrace();
            } catch (InterruptedException e)
            {
                System.out.println("Broadcaster interrupted exception");
                e.printStackTrace();
            }
        }
        socket.close();
    }

    public void stop()
    {
        broadcasting = false;
    }

    public boolean isRunning() {
        return broadcasting;
    }
}

