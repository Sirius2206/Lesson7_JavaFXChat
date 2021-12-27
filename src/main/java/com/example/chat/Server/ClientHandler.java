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
import java.util.logging.Logger;

public class ClientHandler {
    private final MyServer myServer;
    private final Socket socket;
    private final DataInputStream in;
    private final DataOutputStream out;
    static Logger LOG = Logger.getLogger(ClientHandler.class.getName());

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

            ExecutorService executorService = Executors.newCachedThreadPool();
            executorService.execute(() ->{
                try {
                    authentication();
                    readMessages();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    closeConnection();
                }
            });
            executorService.shutdown();
        }catch (IOException e) {
            LOG.warning("Проблемы при создании обработчика клиента");
            throw new RuntimeException("Проблемы при создании обработчика клиента");
        }
    }

    public void authentication() throws IOException {
        try {
            new Thread(() -> {
                try {
                    LOG.info("Запушен процесс авторизации: " + socket);
                    Thread.sleep(120000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (!isAuthorized) {
                    LOG.warning("Пользователь не авторизовался за 2 минуты: " + socket);
                    closeConnection();
                    LOG.warning("Соединение с " + socket + "закрыто");
                }
            }).start();
            while (true) {
                String str = in.readUTF();
                if (str.startsWith("/auth")) {
                    LOG.info("Происходит попытка авторизации: " + socket);
                    String[] parts = str.split("\\s");
                    if (parts.length != 3){
                        LOG.info("Неверное сообщение авторизации " + socket);
                        sendMsg("Неверное сообщение авторизации");
                        continue;
                    }

                    String nick = myServer.getAuthService().getNickByLoginPass(parts[1], parts[2], in, out);
                    if (nick != null) {
                        if (myServer.isNickAvailable(nick)) {
                            isAuthorized = true;
                            sendMsg("/authok " + nick);
                            name = nick;
                            myServer.broadcastMsg(name + " зашел в чат.");
                            myServer.subscribe(this);
                            return;
                        } else {
                            LOG.warning("Пользователь с таким ником(" + nick + ") уже есть: " + socket);
                            sendMsg("Произошла ошибка при авторизации.");
                        }
                    } else {
                        LOG.warning("Никнейм вернулся со значением null: " + socket);
                        sendMsg("Произошла ошибка при авторизации.");
                    }
                } else {
                    LOG.info("Неверное сообщение авторизации " + socket);
                    sendMsg("Неверное сообщение авторизации");
                }
            }
        } catch (SocketException e) {
            LOG.warning("Произошла ошибка на стороне пользователя: " + socket + ". Аутентификация не пройдена");
            System.out.println("Аутентификация не пройдена\n" + e);
        } catch (SQLException e) {
            LOG.warning("Произошла ошибка при обработке SQL-запроса: " + socket + ". Аутентификация не пройдена");
            System.out.println("Аутентификация не пройдена\n" + e);
        }
    }

    public void readMessages() throws IOException {
        try {
            while (true) {
                String strFromClient = in.readUTF();
                if (strFromClient.startsWith("/")) {
                    if (strFromClient.equals("/end")) {
                        LOG.info("Пользователь " + this.name + "ввел /end. Пользователь отключается.");
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
                        LOG.info("Пользователь" + this.name + " запустил процесс смены ника.");
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
                LOG.warning("Соединение с " + this.socket + " прервано клиентом.");

            } else {
                LOG.warning("Соединение с " + this.socket + " прервано.");
            }
        } catch (SQLException throwables) {
            LOG.warning("Произошла ошибка при обработке SQL-запроса.");
            throwables.printStackTrace();
        } finally {
            myServer.unsubscribe(this);
        }
    }


    public void sendMsg(String msg) {
        try {
            out.writeUTF (msg);
        } catch (IOException e) {
            LOG.warning("sendMsg exception");
            e.printStackTrace();
        }
    }

    public void closeConnection() {
        if (isAuthorized) {
            myServer.unsubscribe(this);
            myServer.broadcastClients();
            myServer.broadcastMsg(name + " выходит из чата.");
            LOG.info("Успешно вышел из чата " + name);
        }
        try {
            in.close();
            out.close();
            socket.close();
            LOG.info("Успешно закрыто соединение с " + name);
        } catch (IOException e) {
            LOG.warning("closeConnection exception");
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
