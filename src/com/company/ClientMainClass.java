package com.company;

public class ClientMainClass {
    static {
        System.loadLibrary("ClientMainClass");
    }

    private native void sayHello(String name);

    public static void main(String[] args) {
        new ClientMainClass().sayHello("Dave");
    }
}