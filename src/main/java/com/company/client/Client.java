package com.company.client;

import com.company.CommandType;
import com.company.ProcessClass;
import com.company.ProcessCommand;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.*;

public class Client {
    private Server server;
    private ObjectMapper objectMapper;
    private List<ProcessClass> processes;

    public Client() throws IOException, ParseException {
        server = ConnectConfig();
        server.outputStream.writeUTF(GetComputerName());
        objectMapper = new ObjectMapper();
        processes = new ArrayList<ProcessClass>();

        ServerSender();
        TimerMethod();
        ServerListener();
    }

    public Server ConnectConfig() throws IOException, ParseException {
        JSONParser parser = new JSONParser();
        JSONObject json = (JSONObject) parser.parse(
                new FileReader("./src/main/resources/settings.json"));
        return new Server(new Socket(json.get("host").toString(),
                Integer.parseInt(json.get("port").toString())));
    }

    public void TimerMethod() {
        System.out.println("Timer started.");
        TimerTask task = new TimerTask(){
            public void run() {
                try {
                    ServerSender();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        Timer timer = new Timer("Timer");

        long delay = 5000L;
        timer.schedule(task, delay);
    }

    public void ServerSender() throws IOException {
        System.out.println("Repeatedly sending data to server...");
        GetProcesses();
        List<ObjectNode> objectNodes = new ArrayList<ObjectNode>();

        processes.forEach(process -> {
            ObjectNode objectNode = objectMapper.createObjectNode();
            objectNode.put("name", process.name);

            objectNodes.add(objectNode);
        });

        ArrayNode arrayNode = objectMapper.createArrayNode();
        arrayNode.addAll(objectNodes);

        String json = arrayNode.toString();
        server.outputStream.writeUTF(json);
    }

    public void ServerListener() throws IOException {
        System.out.println("Listening to server...");
        String jsonStr = "";
        ProcessCommand processCommand;
        while (true) {
            try {
                System.out.println(jsonStr);
                jsonStr = server.inputStream.readUTF();
                if (jsonStr != null) {
                    processCommand = objectMapper.readValue(jsonStr, new TypeReference<ProcessCommand>(){});
                    ExecuteCommand(processCommand);
                }
            } catch (SocketException e) {
                System.out.println("Lost connection to server!");
                return;
            }
        }
    }

    public void ExecuteCommand(ProcessCommand processCommand) throws IOException {
        System.out.println("Executing command of \"" + processCommand.commandType + "\" type...");
        switch(processCommand.commandType){
            case OPEN -> new ProcessBuilder(processCommand.data.name).start();
            case CLOSE -> {
                ProcessHandle.allProcesses().forEach(process -> {
                    Optional<String> name = process.info().command();
                    if (name.isPresent() && name.get().toLowerCase(Locale.ROOT).contains(processCommand.data.name))
                        process.destroyForcibly();
                });
            }
        }
    }

    public void GetProcesses(){
        processes.clear();
        ProcessHandle.allProcesses().forEach(process -> {
            Optional<String> name = process.info().command();
            if (name.isPresent())
                processes.add(new ProcessClass(name.get()));
        });

        //processes.forEach(process -> System.out.println(process.name));
    }

    public String GetComputerName()
    {
        Map<String, String> env = System.getenv();

        if (env.containsKey("COMPUTERNAME"))
            return env.get("COMPUTERNAME");
        else if (env.containsKey("HOSTNAME"))
            return env.get("HOSTNAME");
        else
            return "Unknown computer";
    }

}
