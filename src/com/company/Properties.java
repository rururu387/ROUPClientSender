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
    private  final int DEFAULTPORT = 5020;
    private int collectInterval = 10000;

    //This is an amount of time that is required to stop service.
    //Program may freeze for time milliseconds when disabled
    //but getting and sending data must proceed within this time in order to evade memory leaks
    private int serviceDisablingTime = 500;

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

    public int getServiceDisablingTime()
    {
        return serviceDisablingTime;
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
            Controller.getInstance().showErrorMessage("Could find path to update properties file file");
            return;
        }
        try
        {
            propertiesFWriter.write(properties);
            propertiesFWriter.close();
        }
        catch(IOException e)
        {
            Controller.getInstance().showErrorMessage("Could not update properties file");
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
            Controller.getInstance().showErrorMessage("Properties file not found. Using default parameters");
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
            Controller.getInstance().showErrorMessage("Could not read config file. Using default parameters");
            return;
        }
    }

    public void setInvalidFieldsToDefault()
    {
        if (port <= 1024 || port > 65535)
        {
            port = DEFAULTPORT;
            Controller.getInstance().showErrorMessage("Value invalid. 1024 < port <= 65535. Using default value");
        }

        if (collectInterval < 1000)
        {
            collectInterval = 10000;
            Controller.getInstance().showErrorMessage("Collect interval must be > 1000 ms. Using default value");
        }

        if (collectInterval < 2000)
        {
            Controller.getInstance().showErrorMessage("Warning! Program may work incorrect if PC is overloaded");
        }

        if (serviceDisablingTime <= 500 || serviceDisablingTime >= 3000)
        {
            serviceDisablingTime = 700;
            Controller.getInstance().showErrorMessage("Value invalid. 500ms < service disabling < 3s. Using default value");
        }
    }
}
