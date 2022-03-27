package com.company.server;

import com.company.ProcessClass;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

public class Server {
    private ServerSocket serverSocket;
    private ObjectMapper objectMapper;
    private List<Client> clients;

    public Server() throws IOException {
        serverSocket = new ServerSocket(3000);
        objectMapper = new ObjectMapper();
        clients = new ArrayList<Client>();

        System.out.println("Listening for clients...");
        Listener();
    }

    public void Listener() throws IOException {
        while(true) {
            Socket clientSocket = serverSocket.accept();
            System.out.println("New client accepted!");
            clients.add(new Client(clientSocket));
            new Thread(() -> {
                try {
                    ClientListener(clients.get(clients.size() - 1));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    public void ClientListener(Client client) throws IOException {
        String jsonStr;

        while (!client.clientSocket.isClosed()) {
            try {
                jsonStr = client.inputStream.readUTF();
                if (jsonStr != null) {
                    client.processes = objectMapper.readValue(jsonStr, new TypeReference<List<ProcessClass>>(){});
                    PrintProcesses(client);
                }
            } catch (SocketException e) {
                clients.remove(client);
            }
        }
    }

    public void PrintProcesses(Client client) {
        System.out.println(client.processes);
        client.processes.forEach(process -> System.out.println(process.name));
    }
}
