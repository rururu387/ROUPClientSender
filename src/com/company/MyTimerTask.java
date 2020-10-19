package com.company;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Date;
import java.util.TimerTask;

public class MyTimerTask  extends TimerTask {
    public ArrayList<String> Jsons;
    private Gson gson;
    public  MyTimerTask(){
        Jsons=new ArrayList<>();
        gson =new Gson();
    }
    @Override
    public void run() {//this code will execute repeatedly with frequency n second
        Jsons.add( gson.toJson(getDataFromPC())) ;
        System.out.println(gson.toJson(getDataFromPC()));
        getDataFromPC().print();
        System.out.println("Some Data collected at "+new Date()+ "/♂\uD83D\uDE43");
    }

    private DataPack getDataFromPC()
    {
        DataPack Dp=new DataPack();
        Dp.getInfo();
        return  Dp;
    }
}
