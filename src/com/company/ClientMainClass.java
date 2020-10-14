package com.company;

public class ClientMainClass
{
    static
    {
        System.loadLibrary("Main");
    }

    public static native void showString(String message);

    public static void main(String[] args)
    {
        System.out.println("Hello World!");
        showString("Hi!");
        //System.loadLibrary("JniTest");
    }
}