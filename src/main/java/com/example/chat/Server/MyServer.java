package com.example.chat.Server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class MyServer {
private final int PORT = 8189;

private List<ClientHandler> clients;
private AuthService authService;

    public AuthService getAuthService() {
        return authService;
    }

    public MyServer(){
        try {
            ServerSocket server = new ServerSocket(PORT);
            authService = new BaseAuthService();
            authService.start();
            clients = new ArrayList<>();
            while (true) {
                System.out.println("Сервер ожидает подключения");
                Socket socket = server.accept();
                System.out.println("Клиент подключился");
                new ClientHandler(this, socket);
            }
        }catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (authService != null) {
                authService.stop();
            }
        }
    }

    public synchronized boolean isNickAvailable (String nick) {
        for (ClientHandler client : clients) {
            if (client.getName().equals(nick)){
                return false;
            }
        }
        return true;
    }

    public synchronized void broadcastMsg (String msg) {
        for (ClientHandler client : clients) {
            client.sendMsg(msg);
        }
    }
    public synchronized void broadcastClients() {
        for (ClientHandler client : clients) {
            client.getClients();
        }
    }

    public synchronized void fromToMsg (ClientHandler from, String to, String message) {
        for (ClientHandler client : clients) {
            if (client.getName().equals(to)) {
                client.sendMsg(from.getName() + " шепчет: " + message + "\n");
                from.sendMsg("Вы шепчете " + client.getName() + ": " + message + "\n");
                return;
            }
        }
        from.sendMsg("Такой пользователь не найден.\n");
    }

    public synchronized void changeSub (ClientHandler client) {
        clients.remove(client);
        clients.add(client);
        broadcastClients();
    }
    public synchronized void unsubscribe (ClientHandler client){
        clients.remove(client);
        broadcastClients();
    }

    public synchronized void subscribe (ClientHandler client){
        clients.add(client);
        broadcastClients();
    }

    public synchronized List<String> getNicks() {
            List<String> nickList = new ArrayList<>();
            for (ClientHandler c : clients){
                nickList.add(c.getName());
                System.out.println(c.getName());
            }
            return nickList;
        }
    }
