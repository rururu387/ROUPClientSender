package com.company;

import com.google.gson.Gson;

import javax.xml.crypto.Data;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimerTask;

public class MyTimerTask  extends TimerTask {
    public ArrayList<String> Jsons;
    public  MyTimerTask(){
        Jsons=new ArrayList<>();
    }
    @Override
    public void run() {
        completeTask();
        System.out.println("Some Data collected at "+new Date()+ "/♂");
    }

    private void completeTask()
    {
        Gson gson =new Gson();
        DataPack Dp=new DataPack();
        Dp.getInfo();
        //Dp.print();
        Jsons.add( gson.toJson(Dp)) ;
        System.out.println(gson.toJson(Dp));
        DataPack gsonDataPack=gson.fromJson(gson.toJson(Dp),DataPack.class);
        gsonDataPack.print();
    }
}
