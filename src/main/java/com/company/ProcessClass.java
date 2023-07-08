package com.company;

public class ProcessClass {
    public String name;
    public int timeMinutes;

    public ProcessClass() {}
    public ProcessClass(String name){
        this.name = name;
    }
    public ProcessClass(String name, int timeMinutes){
        this.name = name;
        this.timeMinutes = timeMinutes;
    }
}
