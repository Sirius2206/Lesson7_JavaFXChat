package com.example.chat.Server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;

public class ClientHandler {
    private MyServer myServer;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    private String name;
    private boolean isAuthorized;

    public String getName() {
        return name;
    }

    public ClientHandler (MyServer myServer, Socket socket) {
        try {
            this.myServer = myServer;
            this.socket = socket;
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());
            this.name = "Неизвестный пользователь";
            this.isAuthorized = false;

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
        try {
            new Thread(() -> {
                try {
                    System.out.println("ожидаем");
                    Thread.sleep(120000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (!isAuthorized) {
                    closeConnection();
                    return;
                }
            }).start();
            while (true) {
                String str = in.readUTF();
                if (str.startsWith("/auth")) {
                    String[] parts = str.split("\\s");
                    if (parts.length != 3){
                        sendMsg("Неверное сообщение авторизации");
                        continue;
                    }
                    String nick = myServer.getAuthService().getNickByLoginPass(parts[1], parts[2]);
                    if (nick != null) {
                        if (myServer.isNickAvailable(nick)) {
                            isAuthorized = true;
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
                } else {
                    sendMsg("Неверное сообщение авторизации");
                }
            }
        } catch (SocketException e) {
            System.out.println("Аутентификация не пройдена\n" + e);
        }
    }

    public void readMessages() throws IOException {
        try {
            while (true) {
                String strFromClient = in.readUTF();
                if (strFromClient.startsWith("/")) {
                    if (strFromClient.equals("/end")) {
                        closeConnection();
                        return;
                    }
                    if (strFromClient.startsWith("/w ")) {
                        String[] str = strFromClient.split("\\s");
                        String to = str[1];
                        String message = strFromClient.substring(4 + to.length());
                        myServer.fromToMsg(this, to, message);
                        continue;
                    }
                    if (strFromClient.equals("/who")){
                        getClients();
                        continue;
                    }
                    continue;
                }
                myServer.broadcastMsg(name + " пишет: " + strFromClient);
            }
        } catch (SocketException e){
            if (isAuthorized){
                System.out.println("Соединение с " + this.socket + " прервано клиентом.");

            } else {
                System.out.println("Соединение с " + this.socket + " прервано\n" + e);
            }
        } finally {
            myServer.unsubscribe(this);
        }
    }


    public void sendMsg(String msg) {
        try {
            out.writeUTF (msg);
        } catch (IOException e) {
            System.out.println("sendMsg exception");
            e.printStackTrace();
        }
    }

    public void closeConnection() {
        if (isAuthorized) {
            myServer.unsubscribe(this);
            myServer.broadcastClients();
            myServer.broadcastMsg(name + " выходит из чата.");
        }
        try {
            in.close();
            out.close();
            socket.close();
        } catch (IOException e) {
            System.out.println("closeConnection exception");
            e.printStackTrace();
        }
    }

    public void getClients(){
        List<String> clients = myServer.getNicks();
        String[] clientNames = new String[clients.size()];

        StringBuilder clientsMessage = new StringBuilder();
        for (int i = 0; i < clientNames.length; i++){
            clientNames[i] = clients.get(i);
            clientsMessage.append(clientNames[i]).append(" ");
        }
        sendMsg("/clients " + clientsMessage);
    }

}
