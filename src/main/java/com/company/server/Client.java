package com.company.server;

import com.company.ProcessClass;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.List;

public class Client {
    public Socket clientSocket;
    public List<ProcessClass> processes;
    public DataInputStream inputStream;
    public DataOutputStream outputStream;

    public Client(Socket clientSocket) throws IOException {
        this.clientSocket = clientSocket;
        inputStream = new DataInputStream(clientSocket.getInputStream());
        outputStream = new DataOutputStream(clientSocket.getOutputStream());
    }
}
