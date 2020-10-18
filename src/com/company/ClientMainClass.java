package com.company;

public class ClientMainClass {
    static {
        System.loadLibrary("ClientMainClass");
    }

    //private native void sayHello(String name);

    public static void main(String[] args)
    {
        JNIAdapter adapter = new JNIAdapter();
        adapter.updateSnap();
        do {
            adapter.getCpuLoadByProcess();
        } while (adapter.toNextProcess() == true);
        try {
            Thread.sleep(1000);
        }
        catch (java.lang.Exception e)
        {
            System.out.println(e);
        }
        adapter.updateSnap();
        do {
            System.out.println("ID: " + adapter.getCurProcID());
            System.out.println("Name: " + adapter.getCurProcName());
            System.out.println("Threads amount: " + adapter.getCurProcThreadCnt());
            System.out.println("CPU usage: " + adapter.getCpuLoadByProcess() + "%");
            System.out.println("RAM usage: " + adapter.getRAMLoadByProcess());
            System.out.println("-----------------------");
        } while (adapter.toNextProcess() == true);
        System.out.println(adapter.getCurProcName());
        System.out.println(adapter.getProgramNameByActiveWindow());
        adapter.destructor();
        System.out.println("Hello, world!");
    }
}