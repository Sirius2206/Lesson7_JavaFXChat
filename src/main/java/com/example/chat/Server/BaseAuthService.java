package com.example.chat.Server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BaseAuthService implements AuthService {
    private static Connection connection;
    private static Statement stmt;

    private class Entry {
        private String login;
        private String pass;
        private String nick;



        public Entry(String login, String pass, String nick) {
            this.login = login;
            this.pass = pass;
            this.nick = nick;
        }
    }

    private List<Entry> entries;

    public static void connect() throws SQLException {
    }

    public static void disconnect() {
        try {
            if (stmt != null) {
                stmt.close();
            }

            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void start() {
        System.out.println("Сервис аутентификации запущен");
        try {
            connect();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            disconnect();
        }
    }

    public void stop() {
        System.out.println("Сервис аутентификации остановлен");

    }

    public BaseAuthService() {
        entries = new ArrayList<>();
        entries.add(new Entry("login1", "pass1", "nick1"));
        entries.add(new Entry("login2", "pass2", "nick2"));
        entries.add(new Entry("login3", "pass3", "nick3"));
        entries.add(new Entry("login4", "pass4", "nick4"));
    }


    public String getNickByLoginPass(String login, String pass, DataInputStream in, DataOutputStream out) throws SQLException {
        connection = DriverManager.getConnection("jdbc:sqlite:SiriusChatDatabase.db");
        stmt = connection.createStatement();
        String nick = null;
        try {
            ResultSet rs = stmt.executeQuery("SELECT * FROM Accounts WHERE Login = '" + login + "' AND Password = '"+ pass + "';");
            if (!rs.next()) {
                return null;
            }
            if (rs.getString("Nickname") == null){
                out.writeUTF("Похоже у вас еще не выбран ник, напишите желаемое имя.");
                while (nick == null) {
                    nick = in.readUTF();
                    rs = stmt.executeQuery("SELECT Nickname FROM Accounts WHERE Nickname = '" + nick + "';");
                    if (rs.next())
                    {
                        out.writeUTF("Такой пользователь уже существует, выберите другое имя.");
                        nick = null;
                    }
                }
                stmt.executeUpdate("UPDATE Accounts SET Nickname = '" + nick + "' WHERE Login = '" + login + "' AND Password = '"+ pass + "';");
                System.out.println(nick);
                return nick;
            }

            return rs.getString("Nickname");
        } catch (SQLException | IOException e){
            e.printStackTrace();
        }
        return null;
    }

    public boolean renameUser(String to, String from) throws SQLException {
        try {
            stmt.executeUpdate("UPDATE Accounts SET Nickname = '" + to + "' WHERE Nickname = '" + from + "';");
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}
