package com.company.server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Logger {
    private static Connection db;

    public static String Connect(String connIP, String login, String password) {
        try {
            db = DriverManager.getConnection("jdbc:sqlserver://" + connIP + ";databaseName=parentcontroldb;Protocol=3;SSL=false;SslMode=Disable;TrustServerCertificate=True",
                    login,
                    password);
            return "Connection to database was successful.";
        } catch (SQLException e) {
            return "Unsuccessful attempt of connecting to database: " + e.getMessage();
        }
    }

    public static void Log(String data, Client client) {
        try (Statement statement = db.createStatement()) {
            String sqlString = "EXEC InsertLog @Log_Data = '" + data
                    + "', @Log_ClientName = '" + client.hostName
                    + "', @Log_ClientIP = '" + client.clientSocket.getInetAddress().getHostName()
                    + "'";
            statement.execute(sqlString);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
