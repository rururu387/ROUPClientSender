package com.company;

import com.GUI.Controller;
import com.google.gson.*;
import javafx.application.Platform;
import javafx.scene.paint.Paint;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.*;
import java.lang.reflect.Type;
import java.nio.*;
import java.net.*;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Base64;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class DataProcessor extends Thread
{
    static
    {
        //Including dll
        try
        {
            String directory = new File(".").getCanonicalPath();
            System.load(new File(directory + File.separator + "src" + File.separator + "libs" + File.separator + "ClientMainClass.dll").getCanonicalPath());
        } catch (IOException e)
        {
            Controller.getInstance().showErrorMessage("Couldn't load DLL (a module to get data from PC)");
        }
    }

    class RegisterData
    {
        private String userName;
        private byte[] securedPassword;

        public RegisterData(String userName, byte[] securedPassword)
        {
            this.userName = userName;
            this.securedPassword = securedPassword;
        }

        public String getUserName()
        {
            return userName;
        }

        public byte[] getSecuredPassword()
        {
            return securedPassword;
        }
    }

    private int collectInterval;
    private SocketChannel client = null;
    Selector selector = null;

    //In java assignments to most primitive datatypes are atomic
    private boolean isServiceToggledOff = true;

    Gson gson = createGsonInstance();

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    String jsonString = null;
    Lock jsonStringLock = new ReentrantLock();

    ScheduledFuture<?> dataPackHandle = null;

    Lock regDataLock = new ReentrantLock();
    RegisterData regData = null;

    public boolean getIsServiceToggledOff()
    {
        return isServiceToggledOff;
    }

    public void setIsServiceToggledOff(boolean isServiceToggledOff)
    {
        this.isServiceToggledOff = isServiceToggledOff;
    }

    public boolean connectToServer(String servAdr, int port)
    {
        try
        {
            client = SocketChannel.open(new InetSocketAddress(servAdr, port));
        }
        catch (IOException e)
        {
            Controller.getInstance().showErrorMessage("Could not establish connection. Server may be down");
            return false;
        }

        try
        {
            client.configureBlocking(false);
        }
        catch(IOException e)
        {
            Controller.getInstance().showErrorMessage("Could not configure connection");
            return false;
        }

        return true;
    }

    public boolean isConnected()
    {
        return client != null && client.isConnected();
    }

    private static Gson createGsonInstance()
    {
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
        return gsonBuilder.create();
    }

    public byte[] getPBKDF2SecurePassword(String userName, String password)
    {
        try
        {
            byte[] salt;
            salt = userName.getBytes(StandardCharsets.UTF_8);
            KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 65536, 128);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            return factory.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e)
        {
            Controller.getInstance().showErrorMessage("Can't encrypt password: algorithm or KeySpec exception");
            Controller.getInstance().onTurnedOff();
            return null;
        } catch (Exception e)
        {
            Controller.getInstance().showErrorMessage("Can't encrypt password unknown reason");
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

    public void run()
    {
        int connectionErrorsCounter = 0;
        int respondAwaits = 0;
        while (!isServiceToggledOff)
        {
            try
            {
                selector = Selector.open();
                SelectionKey key = client.register(selector, SelectionKey.OP_READ);
            } catch (IOException e)
            {
                Controller.getInstance().showErrorMessage("Could not create selector to interact with server");
                isServiceToggledOff = true;
                continue;
            }

            try
            {
                selector.select();
            } catch (IOException e)
            {
                Controller.getInstance().showErrorMessage("Could not use selector to manage connections");
                isServiceToggledOff = true;
                continue;
            }

            if (isServiceToggledOff)
            {
                continue;
            }

            Set selectedKeys = selector.selectedKeys();
            Iterator keyIterator = selectedKeys.iterator();

            while (keyIterator.hasNext())
            {
                SelectionKey key = (SelectionKey) keyIterator.next();

                if (key.isReadable())
                {
                    ByteBuffer readBuffer = ByteBuffer.allocate(100);
                    try
                    {
                        client.read(readBuffer);
                    } catch (IOException error)
                    {
                        Controller.getInstance().showErrorMessage("Did not receive respond from server");
                        interruptConnection();
                        Controller.getInstance().onTurnedOff();
                        return;
                    }
                    String serverRespond = new String(readBuffer.array()).trim();
                    if (serverRespond.startsWith("Data is being processed"))
                    {
                        Controller.getInstance().showErrorMessage("Login and password are correct", Paint.valueOf("#9de05c"));
                    }
                    else if (serverRespond.startsWith("Data is ignored"))
                    {
                        Controller.getInstance().showErrorMessage("Data is ignored. Check if login and password are correct.");
                        interruptConnection();
                        Controller.getInstance().onTurnedOff();
                        return;
                    }
                    else if (serverRespond.equals("Register successful"))
                    {
                        Controller.getInstance().showErrorMessage("Register successful", Paint.valueOf("#9de05c"));
                    } else if (serverRespond.equals("Register failed"))
                    {
                        Controller.getInstance().showErrorMessage("Register failed. There may already be user with such login");
                    }
                        respondAwaits = 0;
                    }
                    /*No other actions are planned yet*/
            }

            if (isServiceToggledOff)
            {
                continue;
            }

            System.out.println("Connection errors: " + connectionErrorsCounter + ", respond awaits: " + respondAwaits + ".");
            if (connectionErrorsCounter == Properties.getInstance().getRetryNumOnError() || respondAwaits == Properties.getInstance().getMaxNotRespondedDataPacks())
            {
                //TODO - try to reconnect here
                Platform.runLater(() -> { Controller.getInstance().showStage(); });
                if (connectionErrorsCounter == Properties.getInstance().getRetryNumOnError())
                {
                    Controller.getInstance().showErrorMessage("Too many connection errors. Toggling service off.");
                }
                else if (respondAwaits == Properties.getInstance().getMaxNotRespondedDataPacks())
                {
                    Controller.getInstance().showErrorMessage("Server is not responding. It may be shut down. Toggled off.");
                }
                finishConnection();
                Controller.getInstance().onTurnedOff();
            }

            jsonStringLock.lock();
            if (jsonString != null)
            {
                ByteBuffer buffer = ByteBuffer.allocate(1024 * 10);
                //TODO - prevent sending malformed datapacks

                buffer.put(("Client data\n" + jsonString).getBytes(StandardCharsets.UTF_8));
                buffer.flip();

                try
                {
                    client.write(buffer);
                }
                catch(IOException e)
                {
                    Controller.getInstance().showErrorMessage("Sending info to server failed. Retry in " + collectInterval / 1000 + "s");
                    connectionErrorsCounter++;
                    continue;
                }

                respondAwaits++;
                connectionErrorsCounter = 0;
                jsonString = null;
            }
            jsonStringLock.unlock();

            if (isServiceToggledOff)
            {
                continue;
            }

            regDataLock.lock();
            if (regData != null)
            {
                Controller.getInstance().showErrorMessage("Register in process...", Paint.valueOf("#9de05c"));
                ByteBuffer registerDataBuffer = ByteBuffer.allocate(1024 * 10);
                try
                {
                    registerDataBuffer.put(("Register client sender\n" + regData.getUserName() + "\n" + Base64.getEncoder().encodeToString(regData.getSecuredPassword()) + "\n").getBytes(StandardCharsets.UTF_8));
                }
                catch (java.nio.BufferOverflowException | java.nio.ReadOnlyBufferException e)
                {
                    Controller.getInstance().showErrorMessage("Could not put message to buffer. Registration was not successful!");
                    continue;
                }

                registerDataBuffer.flip();
                String clientData = new String(registerDataBuffer.array()).trim();
                try
                {
                    client.write(registerDataBuffer);
                } catch (IOException e)
                {
                    Controller.getInstance().showErrorMessage("Error writing data to server\nRegister failed");
                    regData = null;
                    continue;
                }
                regData = null;
                setIsServiceToggledOff(false);
            }
            regDataLock.unlock();
        }
    }

    public void register(String servAdr, int port, String userName, String password)
    {
        if (!connectToServer(servAdr, port))
        {
            Controller.getInstance().onTurnedOff();
            return;
        }

        setIsServiceToggledOff(false);
        this.start();
        byte[] securedPassword = getPBKDF2SecurePassword(userName, password);
        if (securedPassword == null)
        {
            Controller.getInstance().showErrorMessage("Could not register. Password encrypting failed.");
            return;
        }

        regDataLock.lock();
        regData = new RegisterData(userName, securedPassword);
        regDataLock.unlock();

        selector.wakeup();
        try
        {
            this.join(500);
        }
        catch (InterruptedException e)
        {
            Controller.getInstance().showErrorMessage("Registration interrupted");
        }
        finishConnection();
    }

    public void collectAndSendData(String servAdr, int port, String userName, String password, int collectInterval)
    {
        if (!connectToServer(servAdr, port))
        {
            Controller.getInstance().onTurnedOff();
            return;
        }

        setIsServiceToggledOff(false);
        this.start();
        byte[] securedPassword = getPBKDF2SecurePassword(userName, password);
        if (securedPassword == null)
        {
            Controller.getInstance().showErrorMessage("Could not register. Password encrypting failed.");
            return;
        }

        dataPackHandle = scheduler.scheduleAtFixedRate(new Runnable()
        {
            @Override
            public void run()
            {
                if (isServiceToggledOff)
                {
                    dataPackHandle.cancel(true);
                    return;
                }

                jsonStringLock.lock();
                jsonString = getJsonData(userName, securedPassword, collectInterval);
                jsonStringLock.unlock();

                selector.wakeup();

                return;
            }
        }, 0, Properties.getInstance().getCollectInterval(), MILLISECONDS);
    }

    public void finishConnection()
    {
        setIsServiceToggledOff(true);
        if (selector != null)
        {
            selector.wakeup();
            String end_msg = "EndThisConnection";
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            buffer.put(end_msg.getBytes(StandardCharsets.UTF_8));
            buffer.flip();

            if (client.isOpen())
            {
                try
                {
                    client.write(buffer);
                    client.finishConnect();
                } catch (IOException e)
                {
                    Controller.getInstance().showErrorMessage("Emergency connection closure.");
                    interruptConnection();
                }
            }
        }

        if (this.isAlive())
        {
            try
            {
                this.join(2000);
            } catch (InterruptedException e)
            {
                Controller.getInstance().showErrorMessage("Process already interrupted.");
            }
        }
        Controller.getInstance().setDataProcessor(new DataProcessor());
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
            Controller.getInstance().showErrorMessage("Failed to close connection! application restart may be needed.");
        }
    }

    private String getJsonData(String userName, byte[] securedPassword, int collectInterval)
    {
        DataPack dataPack = getDataFromPC(userName, securedPassword, collectInterval);

        if (isServiceToggledOff)
        {
            return "";
        }
        return gson.toJson(dataPack);
    }

    private DataPack getDataFromPC(String userName, byte[] securedPassword, int collectInterval)
    {
        DataPack Dp = new DataPack(userName, securedPassword);
        Dp.getInfo(collectInterval);
        return Dp;
    }

}
