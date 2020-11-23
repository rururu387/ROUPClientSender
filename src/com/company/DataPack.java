package com.company;

import com.GUI.Controller;

import java.time.LocalDateTime;
import java.util.ArrayList;

public class DataPack {//Class which contains gets and contains info about program

    static {
        System.loadLibrary("ClientMainClass");//including dll
    }

    private String userName;
    private LocalDateTime creationDate;
    private String activeWindowProcessName;
    private ArrayList<ProgramClass> programs;//list of programs
    private int collectInterval;
    public static final int CPUMeasureTime = 1000;

    public void setUserName(String userName)
    {
        this.userName = userName;
    }


    public DataPack()//this is Constructor👍🏻
    {
        programs = new ArrayList<>();
    }

    public DataPack(String userName)//this is Constructor👍🏻
    {
        this.userName = userName;
        programs = new ArrayList<>();
    }

    public void getInfo(int collectInterval) //Сбор информации
    {
        this.collectInterval = collectInterval;
        JNIAdapter adapter = new JNIAdapter();//handling c++ code object
        adapter.updateSnap();//update program list on os

        activeWindowProcessName = getNormalString(adapter.getProgramNameByActiveWindow());
        if (activeWindowProcessName == "Unknown program"){
            Controller.getInstance().showErrorMessage("Couldn't get foreground program name.\n It's OS-protected");
        }
        else if (activeWindowProcessName == "Foreground process query error"){
            Controller.getInstance().showErrorMessage("Foreground program query error!");
            activeWindowProcessName = "Unknown program";
        }

        do {
            adapter.getCpuLoadByProcess();
        } while (adapter.toNextProcess());
        try {
            Thread.sleep(CPUMeasureTime);
        } catch (java.lang.Exception e) {
            System.out.println(e);
        }
        adapter.updateSnap();
        long buffSizeTMax = adapter.getSizeTMax();
        do {
            long buffRamUsage = adapter.getRAMLoadByProcess();
            double buffCpuLoadByProcess = adapter.getCpuLoadByProcess();
            if (buffRamUsage > 0) {
                String buffName = adapter.getCurProcName();
                buffName = getNormalString(buffName);
                ProgramClass bufProgram = isProcessAlreadyExist(buffName);

                if (bufProgram == null) //there is no program in the list with current name
                {
                    programs.add(new ProgramClass(buffName, adapter.getCurProcID(), adapter.getCurProcThreadCnt(), buffCpuLoadByProcess, buffRamUsage));
                } else//there already is a program in the list with current name
                {
                    if (buffCpuLoadByProcess == -1){
                        Controller.getInstance().showErrorMessage("Could not get CPU load of\nsome process(-es)");
                    }
                    if (buffRamUsage == buffSizeTMax){
                        Controller.getInstance().showErrorMessage("Could not get RAM usage of\nsome process(-es)");
                    }
                    bufProgram.merge(adapter.getCurProcID(), adapter.getCurProcThreadCnt(), buffCpuLoadByProcess, buffRamUsage);
                }

            }
        } while (adapter.toNextProcess());
        adapter.destructor();
        creationDate = LocalDateTime.now();
    }

    private ProgramClass isProcessAlreadyExist(String name) {
        for (ProgramClass pr : this.programs) {
            if (pr.getName().equals(name))
                return pr;
        }
        return null;
    }

    public void print() {
        for (ProgramClass pc : programs) {
            pc.print();
        }
    }

    private String getNormalString(String str) {//return string without \u0000
        String res = new String();
        for (char sym : str.toCharArray()) {
            if (sym != '\u0000') {
                res += sym;
            } else break;
        }
        return res;
    }
}
