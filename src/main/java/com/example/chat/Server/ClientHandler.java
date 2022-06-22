package com.example.chat.Server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClientHandler {
    private final MyServer myServer;
    private final Socket socket;
    private final DataInputStream in;
    private final DataOutputStream out;
    private final static ExecutorService clientsPool = Executors.newCachedThreadPool();

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


            clientsPool.execute(() ->{
                try {
                    authentication();
                    readMessages();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    closeConnection();
                }
            });
            clientsPool.shutdown();
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

                    String nick = myServer.getAuthService().getNickByLoginPass(parts[1], parts[2], in, out);
                    if (nick != null) {
                        if (myServer.isNickAvailable(nick)) {
                            isAuthorized = true;
                            sendMsg("/authok " + parts[1] + " " + nick);
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
        } catch (SocketException | SQLException e) {
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
                    if (strFromClient.startsWith("/rename ")){
                        String newName = strFromClient.substring("/rename ".length());
                        if (myServer.getAuthService().renameUser(newName, this.name)) {
                            System.out.println(this.name);
                            this.name = newName;
                            System.out.println(this.name);
                            myServer.broadcastClients();
                        }
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
        } catch (SQLException throwable) {
            throwable.printStackTrace();
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
