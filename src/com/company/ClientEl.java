package com.company;
import com.google.gson.Gson;

import javax.sound.sampled.Port;
import java.io.*;
import java.nio.*;
import java.net.*;
import java.nio.channels.SocketChannel;


public class ClientEl extends  Thread {
    private static BufferedReader readCl;
    private int PORT;
    private long time_interval;
    private SocketChannel client;
    public ClientEl(int PORT, long time_interval)
    {
        this.PORT = PORT;
        this.time_interval = time_interval;
        start();
    }

    public void run()
    {
        System.out.println(currentThread().getId()+" start with interval");
        try {
            client = SocketChannel.open(new InetSocketAddress("127.0.0.1", PORT));
            int msg_num=0;
            while(true)
            {
                Gson gson = new Gson();
                ByteBuffer buffer = ByteBuffer.allocate(1024*10);
                buffer.put(gson.toJson(getDataFromPC()).getBytes());
                buffer.flip();
                client.write(buffer);
                sleep(time_interval);
            }
        }catch (IOException e){
            e.printStackTrace();
            System.out.println(currentThread().getId()+" net exception");
        } catch (InterruptedException e) {
            e.printStackTrace();
            System.out.println(currentThread().getId()+" sleep() exception");
        }
    }
    public void BreakConnection() throws IOException {
        String end_msg = "EndThisConnection";
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        buffer.put(end_msg.getBytes());
        buffer.flip();
        client.write(buffer);
        client.finishConnect();
    }
    private DataPack getDataFromPC()
    {
        DataPack Dp=new DataPack();
        Dp.getInfo();
        Dp.userName = "Vasya";
        return  Dp;

    }

}
