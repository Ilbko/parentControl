package com.company.client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class Server {
    public Socket serverSocket;
    public DataInputStream inputStream;
    public DataOutputStream outputStream;

    public Server(Socket serverSocket) throws IOException {
        this.serverSocket = serverSocket;
        inputStream = new DataInputStream(serverSocket.getInputStream());
        outputStream = new DataOutputStream(serverSocket.getOutputStream());
    }
}
