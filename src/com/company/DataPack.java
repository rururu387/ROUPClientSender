package com.company;

import java.util.ArrayList;

public class DataPack {//Class which contains gets and contains info about programs

    static {
        System.loadLibrary("ClientMainClass");//including dll
    }

    public ArrayList<ProgramClass>  programs;//list of programs

public DataPack()//this is Constructor👍🏻
{
    programs =new ArrayList<>();
}
    public void getInfo()
    {

        JNIAdapter adapter = new JNIAdapter();//handling c++ code object
        adapter.updateSnap();//update program list on os
        do {
            adapter.getCpuLoadByProcess();
        } while (adapter.toNextProcess());
        try {
            Thread.sleep(1000);
        }
        catch (java.lang.Exception e)
        {
            System.out.println(e);
        }
        adapter.updateSnap();
        do {
            long buffRamUsage= adapter.getRAMLoadByProcess();
            if(buffRamUsage>0)
            {
                String buffName=adapter.getCurProcName();
                buffName=getNormalString(buffName);
                ProgramClass bufProgram=isProcessAlreadyExist(buffName);
                if(bufProgram==null) //there are no program in the list with current name
                { programs.add(new ProgramClass(buffName,adapter.getCurProcID(),adapter.getCurProcThreadCnt(),adapter.getCpuLoadByProcess(),buffRamUsage));
                }else//there are already program in the list with current name
                {
                    bufProgram.merge(adapter.getCurProcID(),adapter.getCurProcThreadCnt(),adapter.getCpuLoadByProcess(),buffRamUsage);
                }

            }

            /*System.out.println("ID: " + adapter.getCurProcID());
            System.out.println("Name: " + adapter.getCurProcName());
            System.out.println("Threads amount: " + adapter.getCurProcThreadCnt());
            System.out.println("CPU usage: " + adapter.getCpuLoadByProcess() + "%");
            System.out.println("RAM usage: " + adapter.getRAMLoadByProcess());
            System.out.println("-----------------------");
*/
        } while (adapter.toNextProcess());
        adapter.destructor();
    }

    private ProgramClass isProcessAlreadyExist(String name)
    {
        for (ProgramClass pr:this.programs) {
            if(pr.getName().equals(name))
                return pr;
        }
        return null;
    }

    public void print()
    {
        for (ProgramClass pc:programs ) {
            pc.print();
        }
    }

    private String getNormalString(String str){//return string without \u0000
        String res=new String();
        for (char sym:str.toCharArray() ) {
            if(sym!='\u0000')
            {
                res+=sym;
            }
            else break;
        }
        return res;
    }

}
