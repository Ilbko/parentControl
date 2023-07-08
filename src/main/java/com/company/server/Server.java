package com.company.server;

import com.company.CommandType;
import com.company.ProcessClass;
import com.company.ProcessCommand;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.*;

public class Server {
    private ServerSocket serverSocket;
    private ObjectMapper objectMapper;
    private List<Client> clients;

    private List<ProcessRule> processRules;
    private Date todayDate;

    private Scanner scanner;

    private Thread inputThread;
    private Thread outputThread;

    public Server() throws IOException, ParseException {
        serverSocket = new ServerSocket(3000);
        objectMapper = new ObjectMapper();
        clients = new ArrayList<Client>();
        processRules = new ArrayList<ProcessRule>();
        todayDate = new Date();

        scanner = new Scanner(System.in);

        outputThread = new Thread(new TextOutputRunnable());
        outputThread.start();

        TextOutputRunnable.output = "Connecting to database...";
        GetSettingsAndConnectDB();

        inputThread = new Thread(new TextInputRunnable());
        inputThread.start();

        CheckRules();
        Listener();
    }

    public void OutputAndLog(String data, Client client) {
        TextOutputRunnable.output = data + " (" + client.clientSocket.getInetAddress().getHostName() + ": " + client.hostName + ")";
        Logger.Log(data, client);
    }

    public void GetSettingsAndConnectDB() throws IOException, ParseException {
        JSONParser parser = new JSONParser();
        JSONObject json = (JSONObject) parser.parse(
                new FileReader("./src/main/resources/settings.json"));

        TextOutputRunnable.output = Logger.Connect(json.get("dbip").toString(),
                json.get("dblogin").toString(),
                json.get("dbpassword").toString());
    }

    public void CheckRules() {
        TimerTask task = new TimerTask() {
            public void run() {
                try {
                    processRules.forEach(processRule -> {
                        processRule.client.processes.forEach(processClass -> {
                            if (processClass.name.toLowerCase(Locale.ROOT).contains(processRule.processClass.name))
                                processRule.elapsedTimeMinutes += 0.5;
                        });
                        if (processRule.processClass.timeMinutes <= processRule.elapsedTimeMinutes) {
                            try {
                                SendCommand(processRule.client, new ProcessCommand(processRule.processClass, CommandType.CLOSE));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                if (todayDate.getDay() != new Date().getDay()){
                    TextOutputRunnable.output = "Day changed, clearing rules...";
                    processRules.clear();
                    todayDate = new Date();
                }
                    TextOutputRunnable.output = "Timer ticked! (0.5 minutes)";
                } catch (RuntimeException e){
                    TextOutputRunnable.output = e.toString();
                    return;
                }
                CheckRules();
            }
        };
        Timer timer = new Timer("ServerTimer");

        long delay = 30000L;
        timer.schedule(task, delay);
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

            try {
                menu = Integer.parseInt(scanner.nextLine());
            } catch (NumberFormatException e) {
                return;
            }
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
                    "\n5. List timer rules;" +
                    "\n0 to exit. ";
            TextOutputRunnable.output = String.valueOf(TextInputRunnable.isListening);
            menu = Integer.parseInt(scanner.nextLine());
            if (menu == 0) {
                TextInputRunnable.isListening = true;
                return;
            }
        } while (menu < 1 || menu > 5);

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
                OutputAndLog("Started a process " + processClass.name, client);
                break;
            }
            case 3: {
                TextOutputRunnable.output = "Enter process name: ";
                processName = scanner.nextLine();

                processClass = new ProcessClass(processName.toLowerCase(Locale.ROOT));
                processCommand = new ProcessCommand(processClass, CommandType.CLOSE);

                SendCommand(client, processCommand);
                OutputAndLog("Closed a process " + processClass.name, client);
                break;
            }
            case 4: {
                TextOutputRunnable.output = "Enter process name: ";
                processName = scanner.nextLine();
                TextOutputRunnable.output = "Enter process maximum uptime (minutes): ";
                timeMinutes = Integer.parseInt(scanner.nextLine());

                processClass = new ProcessClass(processName.toLowerCase(Locale.ROOT), timeMinutes);
                //processCommand = new ProcessCommand(processClass, CommandType.TIMER);

                processRules.add(new ProcessRule(client, processClass, 0));
                OutputAndLog("Created a rule for " + processClass.name + ": " + Integer.toString(timeMinutes) + " minutes.", client);
                //SendCommand(client, processCommand);
                break;
            }
            case 5: {
                processRules.forEach(processRule -> {
                    if (processRule.client == client)
                        System.out.println(processRule.toString());
                });
                break;
            }
        }

        TextInputRunnable.isListening = true;
    }

    public void SendCommand(Client client, ProcessCommand processCommand) throws IOException {
        client.outputStream.writeUTF(objectMapper.writeValueAsString(processCommand));
        //TextOutputRunnable.output = "Command sent. " + objectMapper.writeValueAsString(processCommand);
        OutputAndLog(processCommand.commandType.toString() + " command sent.", client);
    }

    public void Listener() throws IOException {
        TextOutputRunnable.output = "Listening for clients...";
        while (true) {
            Socket clientSocket = serverSocket.accept();

            clients.add(new Client(clientSocket));
            Client joinedClient = clients.get(clients.size() - 1);
            joinedClient.hostName = joinedClient.inputStream.readUTF();

            /*TextOutputRunnable.output = "Client connected at " + joinedClient.clientSocket.getInetAddress().getHostName() +
                    ": " + joinedClient.hostName;*/
            OutputAndLog("Client connected.", joinedClient);

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
                /*TextOutputRunnable.output = "Client disconnected at " + client.clientSocket.getInetAddress().getHostName() +
                        ": " + client.hostName;*/
                OutputAndLog("Client disconnected.", client);
                return;
            }
        }
    }

    public void PrintProcesses(Client client) {
        client.processes.forEach(process -> System.out.println(process.name));
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
