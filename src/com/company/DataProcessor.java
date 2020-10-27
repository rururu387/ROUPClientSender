package com.company;

import com.google.gson.Gson;

import javax.sound.sampled.Port;
import java.io.*;
import java.nio.*;
import java.net.*;
import java.nio.channels.SocketChannel;
import java.util.concurrent.locks.ReentrantLock;

public class DataProcessor extends Thread {
    private static BufferedReader readCl;
    private long time_interval;
    private SocketChannel client = null;
    private boolean isServiceToggledOff = true;

    public boolean getIsServiceToggledOff() {
        return isServiceToggledOff;
    }

    public void setIsServiceToggledOff(boolean isServiceToggledOff) {
        this.isServiceToggledOff = isServiceToggledOff;
    }

    public DataProcessor(long time_interval) {
        this.time_interval = time_interval;
        start();
    }

    public void run(String userName, String servAdr, int servPort, ReentrantLock socketLocker) throws IOException{
        try {
            client = SocketChannel.open(new InetSocketAddress(servAdr, servPort));
            while (true) {
                Gson gson = new Gson();
                ByteBuffer buffer = ByteBuffer.allocate(1024 * 10);
                buffer.put(gson.toJson(getDataFromPC(userName)).getBytes());
                buffer.flip();

                socketLocker.lock();
                if (!isServiceToggledOff) {
                    client.write(buffer);
                }
                socketLocker.unlock();

                Thread.sleep(time_interval);
            }
        }
        catch (InterruptedException e) {
            /*e.printStackTrace();
            System.out.println(currentThread().getId() + " sleep() exception");*/
        }
    }

    public void BreakConnection(ReentrantLock socketLocker) throws IOException {
        String end_msg = "EndThisConnection";
        //TODO - resolve bug
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        buffer.put(end_msg.getBytes());
        buffer.flip();

        socketLocker.lock();
        if (client != null && client.isOpen()) {
            client.write(buffer);
            client.finishConnect();
        }
        socketLocker.unlock();
    }

    public void interruptConnection(){
        try {
            if (client != null) {
                client.finishConnect();
                client.close();
            }
        }catch(IOException e){
        }
    }

    private DataPack getDataFromPC(String userName) {
        DataPack Dp = new DataPack(userName);
        Dp.getInfo();
        System.out.println("!");
        //TODO add time mark
        return Dp;
    }

}
