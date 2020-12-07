package com.company;

public class ProgramClass {//class which contains info about program
    private String name;
    private long ID;
    private int threadAmount;
    private double cpuUsage;
    private long ramUsage;

    public ProgramClass(String name, long ID, int threadAmount, double cpuUsage, long ramUsage) {
        this.name = name;
        this.ID = ID;
        this.threadAmount = threadAmount;
        this.cpuUsage = cpuUsage;
        this.ramUsage = ramUsage;
    }

    public String getName() {
        return name;
    }

    //Method to merge Program with equal names
    public void merge(long ID, int threadAmount, double cpuUsage, long ramUsage)
    {
        if (this.ID > ID)
            this.ID = ID;
        this.threadAmount += threadAmount;
        this.cpuUsage += cpuUsage;
        this.ramUsage += ramUsage;
    }

    public void print() {
        System.out.println("ID: " + ID);
        System.out.println("Name: " + name);
        System.out.println("Threads amount: " + threadAmount);
        System.out.println("CPU usage: " + cpuUsage + "%");
        System.out.println("RAM usage: " + ramUsage);
        System.out.println("-----------------------");
    }
}

