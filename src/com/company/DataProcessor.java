package com.company;

import com.GUI.Controller;
import com.google.gson.*;
import javafx.scene.paint.Paint;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.*;
import java.lang.reflect.Type;
import java.nio.*;
import java.net.*;
import java.nio.channels.ByteChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Base64;

public class DataProcessor extends Thread
{
    private int collectInterval;
    private SocketChannel client = null;
    private boolean isServiceToggledOff = true;

    public boolean getIsServiceToggledOff()
    {
        return isServiceToggledOff;
    }

    public void setIsServiceToggledOff(boolean isServiceToggledOff)
    {
        this.isServiceToggledOff = isServiceToggledOff;
    }

    public DataProcessor(int collectInterval)
    {
        this.collectInterval = collectInterval;
        start();
    }

    public byte[] getPBKDF2SecurePassword(String userName, String password)
    {
        try
        {
            byte[] salt;
            if (userName.equals(""))
            {
                //TODO - delete this when debug not needed
                salt = "defaultPassword".getBytes(StandardCharsets.UTF_8);
            } else
            {
                salt = Controller.getInstance().getUserName().getBytes(StandardCharsets.UTF_8);
            }
            KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 65536, 128);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            return factory.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e)
        {
            Controller.getInstance().showErrorMessage("Can't encrypt password:\nAlgorithm or KeySpec exception");
            Controller.getInstance().onTurnedOff();
            return null;
        } catch (Exception e)
        {
            Controller.getInstance().showErrorMessage("Can't encrypt password\nUnknown reason");
            Controller.getInstance().onTurnedOff();
            return null;
        }
    }

    /*//Function to debug password processing
    private static final byte[] HEX_ARRAY = "0123456789ABCDEF".getBytes(StandardCharsets.US_ASCII);
    public static String bytesToHex(byte[] bytes) {
        byte[] hexChars = new byte[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars, StandardCharsets.UTF_8);
    }*/

    public void run(String userName, String password, String servAdr, int servPort)
    {
        byte[] securedPassword = getPBKDF2SecurePassword(userName, password);
        if (securedPassword == null)
        {
            interruptConnection();
        }
        try
        {
            try
            {
                client = SocketChannel.open(new InetSocketAddress(servAdr, servPort));
            } catch (IOException e)
            {
                Controller.getInstance().showErrorMessage("Could not establish connection\nServer may be down");
                Controller.getInstance().onTurnedOff();
                interruptConnection();
                return;
            }
            GsonBuilder gsonBuilder = new GsonBuilder().registerTypeAdapter
                    (LocalDateTime.class, new JsonSerializer<LocalDateTime>()
                    {
                        @Override
                        public JsonElement serialize(LocalDateTime src, Type typeOfSrc, JsonSerializationContext context)
                        {
                            DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT);
                            return new JsonPrimitive(formatter.format(src));
                        }
                    });
            gsonBuilder.registerTypeAdapter(byte[].class, new JsonSerializer<byte[]>()
            {
                @Override
                public JsonElement serialize(byte[] bytes, Type type, JsonSerializationContext jsonSerializationContext)
                {
                    //System.out.println(bytesToHex(bytes));
                    return new JsonPrimitive(Base64.getEncoder().encodeToString(bytes));
                }
            });
            Gson gson = gsonBuilder.create();
            while (!isServiceToggledOff)
            {
                ByteBuffer buffer = ByteBuffer.allocate(1024 * 10);
                //TODO - prevent sending malformed datapacks
                DataPack dataPack = null;
                if (isServiceToggledOff)
                {
                    return;
                }
                dataPack = getDataFromPC(userName, securedPassword, collectInterval);

                if (isServiceToggledOff)
                {
                    return;
                }
                String dataJson = gson.toJson(dataPack);

                if(isServiceToggledOff)
                {
                    return;
                }
                buffer.put(("Client data\n" + dataJson).getBytes(StandardCharsets.UTF_8));
                //buffer.put(("Client data\n" + gson.toJson(getDataFromPC(userName, securedPassword, collectInterval))).getBytes(StandardCharsets.UTF_8));
                buffer.flip();

                if (isServiceToggledOff)
                {
                    return;
                }
                try
                {
                    client.write(buffer);
                } catch (IOException e)
                {
                    Controller.getInstance().showErrorMessage("Sending info to server failed\n retry in " + collectInterval / 1000 + "s");
                }
                buffer.clear();
                try
                {
                    client.read(buffer);
                } catch (IOException error)
                {
                    Controller.getInstance().showErrorMessage("Did not receive\nrespond from server");
                    interruptConnection();
                    Controller.getInstance().onTurnedOff();
                    return;
                }
                String serverRespond = new String(buffer.array()).trim();
                if (serverRespond.startsWith("Data is being processed"))
                {
                    Controller.getInstance().showErrorMessage("Login and password\nare correct", Paint.valueOf("#9de05c"));
                } else if (serverRespond.startsWith("Data is ignored"))
                {
                    Controller.getInstance().showErrorMessage("Data is ignored\nCheck login and password");
                    interruptConnection();
                    Controller.getInstance().onTurnedOff();
                    return;
                }

                int maxTransactionTime = Properties.getInstance().getServiceDisablingTime();
                for (int sleepCounter = 0; sleepCounter < collectInterval; sleepCounter += maxTransactionTime - 100)
                {
                    if (isServiceToggledOff)
                        return;
                    if (sleepCounter + maxTransactionTime - 100 <= collectInterval)
                    {
                        Thread.sleep(maxTransactionTime - 100);
                    }
                    else
                    {
                        Thread.sleep(collectInterval - sleepCounter);
                    }
                }
            }
        } catch (InterruptedException e)
        {
        }
    }


    public void register(String userName, String password, String servAdr, int servPort)
    {
        byte[] securedPassword = getPBKDF2SecurePassword(userName, password);
        if (securedPassword == null)
        {
            interruptConnection();
        }

        try
        {
            client = SocketChannel.open(new InetSocketAddress(servAdr, servPort));
        } catch (IOException e)
        {
            Controller.getInstance().showErrorMessage("Error connecting to server \nRegister failed");
            return;
        }
        ByteBuffer sendDataBuffer = ByteBuffer.allocate(1024 * 10);
        try
        {
            sendDataBuffer.put(("Register client sender\n" + userName + "\n" + Base64.getEncoder().encodeToString(securedPassword) + "\n").getBytes(StandardCharsets.UTF_8));
        } catch (java.nio.BufferOverflowException | java.nio.ReadOnlyBufferException e)
        {
            //TODO - handle error correctly
            Controller.getInstance().showErrorMessage("Could not put message to buffer");
            return;
        }
        sendDataBuffer.flip();
        String clientData = new String(sendDataBuffer.array()).trim();
        try
        {
            client.write(sendDataBuffer);
        } catch (IOException e)
        {
            Controller.getInstance().showErrorMessage("Error writing data to server\nRegister failed");
            return;
        }
        ByteBuffer respondBuffer = ByteBuffer.allocate(1024);
        try
        {
            client.read(respondBuffer);
        } catch (IOException e)
        {
            Controller.getInstance().showErrorMessage("Error reading data from server\nRegister failed");
            return;
        }

        String serverRespond = new String(respondBuffer.array()).trim();
        if (serverRespond.equals("Register successful"))
        {
            Controller.getInstance().showErrorMessage("Register successful", Paint.valueOf("#9de05c"));
        } else if (serverRespond.equals("Register failed"))
        {
            Controller.getInstance().showErrorMessage("Register failed");
        }

    }

    public void BreakConnection() throws IOException
    {
        setIsServiceToggledOff(true);
        String end_msg = "EndThisConnection";
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        buffer.put(end_msg.getBytes(StandardCharsets.UTF_8));
        buffer.flip();

        if (client != null && client.isOpen())
        {
            client.write(buffer);
            client.finishConnect();
        }
    }

    public void interruptConnection()
    {
        try
        {
            if (client != null)
            {
                client.finishConnect();
                client.close();
                client = null;
            }
        } catch (IOException e)
        {
        }
    }

    public void setCollectInterval(int collectInterval)
    {
        //Thread sleeps inside getDataFromPC() -> Dp.getInfo for a second to retrieve precised CPU usage data
        if (collectInterval < 1500)
        {
            Controller.getInstance().showErrorMessage("Please set interval >= 1500 ms.\nCool your PC) Value 1500ms set.", Paint.valueOf("#9de05c"));
            this.collectInterval = 500;
        } else
            this.collectInterval = collectInterval - DataPack.CPUMeasureTime;
    }

    private DataPack getDataFromPC(String userName, byte[] securedPassword, int collectInterval)
    {
        DataPack Dp = new DataPack(userName, securedPassword);
        Dp.getInfo(collectInterval);
        return Dp;
    }

}
