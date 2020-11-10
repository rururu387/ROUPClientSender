package com.company;

import java.util.ArrayList;
import java.util.Date;

public class DataPack {//Class which contains gets and contains info about program

    static {
        System.loadLibrary("ClientMainClass");//including dll
    }


    private String userName;
    private Date creationDate;
    private String activeWindow;
    private ArrayList<ProgramClass> programs;//list of programs

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

    public void getInfo() //Сбор информации
    {
        JNIAdapter adapter = new JNIAdapter();//handling c++ code object
        adapter.updateSnap();//update program list on os
        activeWindow = getNormalString(adapter.getProgramNameByActiveWindow());
        do {
            adapter.getCpuLoadByProcess();
        } while (adapter.toNextProcess());
        try {
            Thread.sleep(1000);
        } catch (java.lang.Exception e) {
            System.out.println(e);
        }
        adapter.updateSnap();
        do {
            long buffRamUsage = adapter.getRAMLoadByProcess();
            if (buffRamUsage > 0) {
                String buffName = adapter.getCurProcName();
                buffName = getNormalString(buffName);
                ProgramClass bufProgram = isProcessAlreadyExist(buffName);
                if (bufProgram == null) //there are no program in the list with current name
                {
                    programs.add(new ProgramClass(buffName, adapter.getCurProcID(), adapter.getCurProcThreadCnt(), adapter.getCpuLoadByProcess(), buffRamUsage));
                } else//there are already program in the list with current name
                {
                    bufProgram.merge(adapter.getCurProcID(), adapter.getCurProcThreadCnt(), adapter.getCpuLoadByProcess(), buffRamUsage);
                }

            }
        } while (adapter.toNextProcess());
        adapter.destructor();
        creationDate = new Date(System.currentTimeMillis());
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
