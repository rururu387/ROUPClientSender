package com.company;
import javax.sound.sampled.Port;
import java.io.*;
import java.nio.*;
import java.net.*;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Scanner;

import static java.lang.Thread.sleep;


public class ClientTest {
    private static BufferedReader readCl;
    public static ArrayList<ClientEl> clientList = new ArrayList<>();
    public static final int PORT = 5020;

    public static void main(String[] args)
    {
        System.out.print("Enter interval time: ");
        Scanner scan = new Scanner(System.in);
        int time_interval = scan.nextInt();
        //scan.close();
        clientList.add(new ClientEl(PORT,time_interval));

        while(true)
        {
            System.out.println("Add(1) | Delete(2)");
            scan = new Scanner(System.in);
            int res = scan.nextInt();
            if(res == 1) clientList.add(new ClientEl(PORT,time_interval));
            else {
                System.out.print("Choose thread to stop (max "+Integer.toString(clientList.size()-1)+"):");
                scan = new Scanner(System.in);
                res = scan.nextInt();
                clientList.get(res).stop();
                try {
                    clientList.get(res).BreakConnection();
                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println("BreakConnectionError");
                }
                clientList.remove(res);
            }
        }
    }
}
