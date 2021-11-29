package com.example.chat.Server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientHandler {
    private MyServer myServer;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    private String name;

    public String getName() {
        return name;
    }

    public ClientHandler (MyServer myServer, Socket socket) {
        try {
            this.myServer = myServer;
            this.socket = socket;
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());
            this.name = "";

            new Thread(() -> {
                try {
                    authentication();
                    readMessages();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    closeConnection();
                }
            }).start();
        }catch (IOException e) {
            throw new RuntimeException("Проблемы при создании обработчика клиента");
        }
    }

    public void authentication() throws IOException {
        while (true) {
            String str = in.readUTF();
            if (str.startsWith("/auth")) {
                String[] parts = str.split("\\s");
                String nick = myServer.getAuthService().getNickByLoginPass(parts[1], parts[2]);
                if (nick != null) {
                    if (myServer.isNickAvailable(nick)) {
                        sendMsg("/authok " + nick);
                        name = nick;
                        myServer.broadcastMsg(name + " зашел в чат.");
                        myServer.subscribe(this);
                        return;
                    } else {
                        sendMsg("Произошла ошибка при авторизации.");
                    }
                } else {
                    sendMsg("Произошла ошибка при авторизации.");
                }
            }
        }
    }

    public void readMessages() throws IOException{
        while(true) {
            String strFromClient = in.readUTF();
            System.out.println(name + " пишет: " +strFromClient);
            if (strFromClient.startsWith("/")) {
                if (strFromClient.equals("/end")) {
                    closeConnection();
                    return;
                }
                if (strFromClient.startsWith("/w")) {
                    String[] str = strFromClient.split("\\s");
                    String to = str[1];
                    String message = strFromClient.substring(4 + to.length());
                    myServer.fromToMsg(this, to, message);
                    continue;
                }
                continue;
            }
            myServer.broadcastMsg(name + " пишет: " +strFromClient);
        }
    }


    public void sendMsg(String msg) {
        try {
            out.writeUTF (msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void closeConnection() {
        myServer.unsubscribe(this);
        myServer.broadcastMsg(name + "выходит из чата.");
        try {
            in.close();
            out.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
