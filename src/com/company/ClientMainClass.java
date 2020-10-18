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
        System.out.println(adapter.getCurProcName());
        adapter.destructor();
        System.out.println("Hello, world!");
    }
}