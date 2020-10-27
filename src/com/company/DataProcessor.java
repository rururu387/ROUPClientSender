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
    private int PORT;
    private long time_interval;
    private SocketChannel client;
    private boolean isServiceToggledOff = true;

    public boolean getIsServiceToggledOff() {
        return isServiceToggledOff;
    }

    public void setIsServiceToggledOff(boolean isServiceToggledOff) {
        this.isServiceToggledOff = isServiceToggledOff;
    }

    public DataProcessor(int PORT, long time_interval) {
        this.PORT = PORT;
        this.time_interval = time_interval;
        start();
    }

    public void run(String userName, ReentrantLock socketLocker) {
        isServiceToggledOff = false;
        System.out.println(currentThread().getId() + " start with interval");
        try {
            client = SocketChannel.open(new InetSocketAddress("127.0.0.1", PORT));
            while (true) {
                Gson gson = new Gson();
                ByteBuffer buffer = ByteBuffer.allocate(1024 * 10);
                buffer.put(gson.toJson(getDataFromPC(userName)).getBytes());
                buffer.flip();

                socketLocker.lock();
                if (!isServiceToggledOff && client.isOpen()) {
                    client.write(buffer);
                }
                socketLocker.unlock();

                Thread.sleep(time_interval);
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println(currentThread().getId() + " net exception");
        } catch (InterruptedException e) {
            /*e.printStackTrace();
            System.out.println(currentThread().getId() + " sleep() exception");*/
        }
    }

    public void BreakConnection(ReentrantLock socketLocker) throws IOException {
        isServiceToggledOff = true;
        String end_msg = "EndThisConnection";
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        buffer.put(end_msg.getBytes());
        buffer.flip();

        socketLocker.lock();
        client.write(buffer);
        client.finishConnect();
        socketLocker.unlock();
    }

    private DataPack getDataFromPC(String userName) {
        DataPack Dp = new DataPack();
        Dp.getInfo();
        Dp.setUserName(userName);
        //TODO add time mark
        return Dp;
    }

}
