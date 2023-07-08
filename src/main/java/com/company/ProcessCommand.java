package com.company;

public class ProcessCommand {
    public ProcessClass data;
    public CommandType commandType;

    public ProcessCommand() {}
    public ProcessCommand(ProcessClass data, CommandType commandType){
        this.data = data;
        this.commandType = commandType;
    }
}
