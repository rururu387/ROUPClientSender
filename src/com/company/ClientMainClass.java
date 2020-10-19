package com.company;


import java.util.Timer;
import java.util.TimerTask;

public class ClientMainClass {

    //private native void sayHello(String name);


    public static void main(String[] args) {
        TimerTask timerTask = new MyTimerTask();

        Timer timer = new Timer(true);
        // будем запускать каждых 10 секунд (10 * 1000 миллисекунд)
        timer.scheduleAtFixedRate(timerTask, 0, 20 * 1000);

        try {
            Thread.sleep(70*1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        timer.cancel();

    }

}