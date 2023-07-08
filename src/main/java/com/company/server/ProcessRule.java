package com.company.server;

import com.company.ProcessClass;
import com.company.server.Client;

public class ProcessRule {
    public Client client;
    public ProcessClass processClass;
    public float elapsedTimeMinutes;

    public ProcessRule () { }
    public ProcessRule(Client client, ProcessClass processClass, int elapsedTimeSeconds){
        this.client = client;
        this.processClass = processClass;
        this.elapsedTimeMinutes = elapsedTimeSeconds;
    }

    public String toString() {
        return this.processClass.name + ", " + this.elapsedTimeMinutes
                + " out of " + this.processClass.timeMinutes + " minutes passed";
    }
}
