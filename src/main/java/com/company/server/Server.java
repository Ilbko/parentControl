package com.company.server;

import com.company.CommandType;
import com.company.ProcessClass;
import com.company.ProcessCommand;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.json.JSONObject;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Server {
    private ServerSocket serverSocket;
    private ObjectMapper objectMapper;
    private List<Client> clients;

    private Scanner scanner;

    private Thread inputThread;
    private Thread outputThread;

    public Server() throws IOException {
        serverSocket = new ServerSocket(3000);
        objectMapper = new ObjectMapper();
        clients = new ArrayList<Client>();

        scanner = new Scanner(System.in);

        inputThread = new Thread(new TextInputRunnable());
        inputThread.start();
        outputThread = new Thread(new TextOutputRunnable());
        outputThread.start();

        Listener();
    }

    public void SelectUser() throws IOException {
        int menu = 0;

        do {
            TextOutputRunnable.output = "Select a user: ";
            for (int i = 0; i < clients.size(); i++) {
                System.out.println(i + 1 + " " + clients.get(i).hostName +
                        " : " + clients.get(i).clientSocket.getInetAddress());
            }

            System.out.println("0 to exit. ");
            menu = Integer.parseInt(scanner.nextLine());
            if (menu == 0)
                return;
        } while (menu < 1 || menu > clients.size());

        SelectCommand(clients.get(menu - 1));
    }

    public void SelectCommand(Client client) throws IOException {
        int menu = 0;

        TextInputRunnable.isListening = false;
        do {
            TextOutputRunnable.output = "Select a command:" +
                    "\n1. Watch processes;" +
                    "\n2. Open a process;" +
                    "\n3. Close a process;" +
                    "\n4. Set a timer;" +
                    "\n0 to exit. ";
            TextOutputRunnable.output = String.valueOf(TextInputRunnable.isListening);
            menu = Integer.parseInt(scanner.nextLine());
            if (menu == 0) {
                TextInputRunnable.isListening = true;
                return;
            }
        } while (menu < 1 || menu > 4);

        ExecuteCommand(client, menu);
    }

    public void ExecuteCommand(Client client, int menu) throws IOException {
        String processName = "";
        int timeMinutes = 0;
        ProcessClass processClass;
        ProcessCommand processCommand;
        switch (menu) {
            case 1: {
                PrintProcesses(client);
                break;
            }
            case 2: {
                TextOutputRunnable.output = "Enter process name: ";
                processName = scanner.nextLine();

                processClass = new ProcessClass(processName);
                processCommand = new ProcessCommand(processClass, CommandType.OPEN);

                SendCommand(client, processCommand);
                break;
            }
            case 3: {
                TextOutputRunnable.output = "Enter process name: ";
                processName = scanner.nextLine();

                processClass = new ProcessClass(processName);
                processCommand = new ProcessCommand(processClass, CommandType.CLOSE);

                SendCommand(client, processCommand);
                break;
            }
            case 4: {
                TextOutputRunnable.output = "Enter process name: ";
                processName = scanner.nextLine();
                TextOutputRunnable.output = "Enter process maximum uptime (minutes): ";
                timeMinutes = Integer.parseInt(scanner.nextLine());

                processClass = new ProcessClass(processName, timeMinutes);
                processCommand = new ProcessCommand(processClass, CommandType.TIMER);

                SendCommand(client, processCommand);
                break;
            }
        }

        TextInputRunnable.isListening = true;
    }

    public void SendCommand(Client client, ProcessCommand processCommand) throws IOException {
        client.outputStream.writeUTF(objectMapper.writeValueAsString(processCommand));
        TextOutputRunnable.output = "Command sent." + objectMapper.writeValueAsString(processCommand);
    }

    public void Listener() throws IOException {
        TextOutputRunnable.output = "Listening for clients...";
        while(true) {
            Socket clientSocket = serverSocket.accept();

            clients.add(new Client(clientSocket));
            Client joinedClient = clients.get(clients.size() - 1);
            joinedClient.hostName = joinedClient.inputStream.readUTF();

            TextOutputRunnable.output = "Client connected at " + joinedClient.clientSocket.getInetAddress().getHostName() +
                    ": " + joinedClient.hostName;

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
                }
            } catch (SocketException e) {
                clients.remove(client);
                TextOutputRunnable.output = "Client disconnected at " + client.clientSocket.getInetAddress().getHostName() +
                        ": " + client.hostName;
                return;
            }
        }
    }

    public void PrintProcesses(Client client) {
        client.processes.forEach(process -> System.out.println(process.name));
        //client.processes.forEach(process -> TextOutputRunnable.output = process.name);
    }

    private class TextOutputRunnable implements Runnable {
        static Thread thread;
        static String output = "";

        public TextOutputRunnable() { }

        public void start() {
            if (thread == null) {
                thread = new Thread(this);
                thread.start();
            }
        }

        @Override
        public void run() {
            while (true) {
                if (output != "") {
                    System.out.println(output);
                    output = "";
                }
            }
        }
    }

    private class TextInputRunnable implements Runnable {
        static Thread thread;
        static boolean isListening = true;
        Scanner scanner = new Scanner(System.in);

        public TextInputRunnable() { }

        public void start() {
            if (thread == null) {
                thread = new Thread(this);
                thread.start();
            }
        }

        @Override
        public void run() {
            while (isListening) {
                System.out.println("Press Enter to open menu...");
                scanner.nextLine();
                try {
                    SelectUser();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
