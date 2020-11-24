package com.company;

import com.GUI.Controller;
import com.google.gson.*;
import javafx.scene.control.Control;
import javafx.scene.paint.Paint;

import javax.sound.sampled.Port;
import java.io.*;
import java.lang.reflect.Type;
import java.nio.*;
import java.net.*;
import java.nio.channels.SocketChannel;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.concurrent.locks.ReentrantLock;

public class DataProcessor extends Thread {
    private static BufferedReader readCl;
    private int collectInterval;
    private SocketChannel client = null;
    private boolean isServiceToggledOff = true;

    public boolean getIsServiceToggledOff() {
        return isServiceToggledOff;
    }

    public void setIsServiceToggledOff(boolean isServiceToggledOff) {
        this.isServiceToggledOff = isServiceToggledOff;
    }

    public DataProcessor(int collectInterval) {
        this.collectInterval = collectInterval;
        start();
    }

    public void run(String userName, String servAdr, int servPort, ReentrantLock socketLocker) throws IOException {
        try {
            client = SocketChannel.open(new InetSocketAddress(servAdr, servPort));
            while (true) {
                Gson gson = new GsonBuilder().registerTypeAdapter(LocalDateTime.class, new JsonSerializer<LocalDateTime>() {
                    @Override
                    public JsonElement serialize(LocalDateTime src, Type typeOfSrc, JsonSerializationContext context) {
                        DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT);
                        return new JsonPrimitive(formatter.format(src));
                    }
                }).create();

                ByteBuffer buffer = ByteBuffer.allocate(1024 * 10);
                try {
                    buffer.put(("Client data\n" + gson.toJson(getDataFromPC(userName, collectInterval))).getBytes());
                }
                catch (java.nio.BufferOverflowException | java.nio.ReadOnlyBufferException e) {
                    //TODO - handle error correctly
                    Controller.getInstance().showErrorMessage("Could not put message to buffer");
                    return;
                }
                buffer.flip();

                socketLocker.lock();
                if (!isServiceToggledOff) {
                    client.write(buffer);
                }
                socketLocker.unlock();

                Thread.sleep(collectInterval);
            }
        }
        catch (InterruptedException e) {}
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
                client = null;
            }
        }catch(IOException e){
        }
    }

    public void setCollectInterval(int collectInterval){
        //Thread sleeps inside getDataFromPC() -> Dp.getInfo for a second to retrieve precised CPU usage data
        if(collectInterval < 1500) {
            Controller.getInstance().showErrorMessage("Please set interval >= 1500 ms.\nCool your PC) Value 1500ms set.", Paint.valueOf("#9de05c"));
            this.collectInterval = 500;
        }
        else
            this.collectInterval = collectInterval - DataPack.CPUMeasureTime;
    }

    private DataPack getDataFromPC(String userName, int collectInterval) {
        DataPack Dp = new DataPack(userName);
        Dp.getInfo(collectInterval);
        return Dp;
    }

}
