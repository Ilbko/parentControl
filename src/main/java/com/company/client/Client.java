package com.company.client;

import com.company.ProcessClass;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.Socket;
import java.util.*;

public class Client {
    private Server server;
    private ObjectMapper objectMapper;
    private List<ProcessClass> processes;

    public Client() throws IOException {
        server = new Server(new Socket("localhost", 3000));
        objectMapper = new ObjectMapper();
        processes = new ArrayList<ProcessClass>();

        ServerSender();
        TimerMethod();
    }

    public void TimerMethod(){
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

    public void GetProcesses(){
        processes.clear();
        ProcessHandle.allProcesses().forEach(process -> {
            Optional<String> name = process.info().command();
            if (name.isPresent())
                processes.add(new ProcessClass(name.get()));
        });

        processes.forEach(process -> System.out.println(process.name));
    }
}
