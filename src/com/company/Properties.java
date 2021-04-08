package com.company;
import com.GUI.Controller;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

import java.io.*;

public class Properties {
    private static final String propertiesPath = "client.properties";
    private static Properties thisAppProperties = null;
    private String name = "GooseDefault";
    private String servAdr = "127.0.0.1";
    private int port = 5020;
    private int collectInterval = 10000;
    private int retryNumOnError = 3;
    private int maxNotRespondedDataPacks = 5;
    private int maxDataPackLength = 130000;

    //This is an amount of time that is required to stop service.
    //Program may freeze for time milliseconds when disabled
    //but getting and sending data must proceed within this time in order to evade memory leaks
    //private int serviceDisablingTime = 500;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getServAdr() {
        return servAdr;
    }

    public int getPort() {
        return port;
    }

    public int getCollectInterval() {
        return collectInterval;
    }

    public int getRetryNumOnError()
    {
        return retryNumOnError;
    }

    public int getMaxNotRespondedDataPacks()
    {
        return maxNotRespondedDataPacks;
    }

    public int getMaxDataPackLength()
    {
        return maxDataPackLength;
    }

    public static Properties getInstance()
    {
        return thisAppProperties;
    }

    public Properties()
    {
        thisAppProperties = this;
    }

    public String toString()
    {
        String str = "";
        str += "servAdr: " + servAdr + "\n";
        str += "port: " + port + "\n";
        str += "Info collecting interval: " + collectInterval + "\n";
        return str;
    }

    public Properties(Properties properties)
    {
        this.name = properties.getName();
        this.servAdr = properties.getServAdr();
        this.port = properties.getPort();
        this.collectInterval = properties.getCollectInterval();
        this.retryNumOnError = properties.getRetryNumOnError();
        this.maxNotRespondedDataPacks = properties.getMaxNotRespondedDataPacks();
        this.maxDataPackLength = properties.getMaxDataPackLength();
    }

    public void update()
    {
        Properties currentProperties = new Properties(this);
        Properties.deserializeProperties();
        this.name = currentProperties.getName();
    }

    public static void serializeProperties()
    {
        //Make changes in case properties are changed while app was working
        Properties.getInstance().setName(Controller.getInstance().getUserName());

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String properties = gson.toJson(Properties.getInstance());
        FileWriter propertiesFWriter;
        try
        {
            propertiesFWriter = new FileWriter(propertiesPath);
        }
        catch (IOException e) {
            Controller.getInstance().showStatusMessage("Could find path to update properties file file");
            return;
        }
        try
        {
            propertiesFWriter.write(properties);
            propertiesFWriter.close();
        }
        catch(IOException e)
        {
            Controller.getInstance().showStatusMessage("Could not update properties file");
            return;
        }
    }

    public static void deserializeProperties()
    {
        //Getting properties from file
        FileReader configFileReader = null;
        try
        {
            configFileReader = new FileReader(propertiesPath);
        } catch (FileNotFoundException e)
        {
            Controller.getInstance().showStatusMessage("Properties file not found. Using default parameters");
            new Properties();
            return;
        }
        Gson gson = new Gson();
        try
        {
            thisAppProperties = gson.fromJson(configFileReader, Properties.class);
            configFileReader.close();
        }
        catch (JsonSyntaxException | JsonIOException | IOException e)
        {
            Controller.getInstance().showStatusMessage("Could not read config file. Using default parameters");
            new Properties();
            return;
        }
    }

    public void setInvalidFieldsToDefault()
    {
        if (port <= 1024 || port > 65535)
        {
            port = 5020;
            Controller.getInstance().showStatusMessage("Value invalid. 1024 < port <= 65535. Using default value");
        }

        if (collectInterval < 1000)
        {
            collectInterval = 10000;
            Controller.getInstance().showStatusMessage("Collect interval must be > 1000 ms. Using default value");
        }

        if (maxDataPackLength > 200000 || maxDataPackLength < 30000)
        {
            maxDataPackLength = 130000;
            Controller.getInstance().showStatusMessage("Value invalid. 30000 < MaxDataPathLength < 200000. Using default value");
        }

        if (collectInterval < 2000)
        {
            Controller.getInstance().showStatusMessage("Warning! Program may work incorrect if PC is overloaded");
        }
    }
}
