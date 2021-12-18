package com.example.chat;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class ClientChatController{
    private final Socket socket = new Socket("localhost", 8189);
    DataInputStream in = new DataInputStream(socket.getInputStream());
    DataOutputStream out = new DataOutputStream(socket.getOutputStream());
    private String login = "";
    @FXML
    private TextArea chatArea;

    @FXML
    private TextField userInput;

    @FXML
    private TextArea clientsArea;

    public ClientChatController() throws IOException {
    }

    @FXML
    protected void onSendButtonClick() throws IOException {
        String message = userInput.getText();
        if (message.equals("")) return;
        chatArea.appendText("Вы написали: \n" + message + "\n");
        out.writeUTF(message);
        userInput.setText("");

    }

    public void initialize() {

        Thread readMsg = new Thread(() -> {
            String serverMessage;
            chatArea.appendText("""
                    Добро пожаловать в чат!
                    У вас есть 2 минуты, чтобы авторизоваться.
                    """);
            String nickname = "nickname";
            while (true) {
                try {
                    serverMessage = in.readUTF();
                    if (serverMessage.startsWith("/authok")) {
                        String[] parts = serverMessage.split("\\s");
                        login = parts[1];
                        nickname = parts[2];
                        loadHistory(login);
                        chatArea.appendText("Добро пожаловать, " + nickname + "\n");
                        continue;
                    }
                    if (serverMessage.startsWith("/clients")) {
                        clientsArea.setText("");
                        String[] clientMessage = serverMessage.substring(9).split(" ");
                        for (String name : clientMessage) {
                            clientsArea.appendText(name + "\n");
                        }
                        continue;
                    }
                    if (!serverMessage.startsWith(nickname)) {
                        chatArea.appendText(serverMessage + "\n");
                    }


                } catch (IOException e) {
                    chatArea.appendText("Подключение сброшено сервером");
                    return;
                } finally {
                    saveHistory(login);
                }
            }
        });
        readMsg.start();
    }

    public void loadHistory (String login) {
        try {
            List<String> allHistory = Files.readAllLines(Paths.get("history_" + login + ".txt"));
            int allHistorySize = allHistory.size();
            if (allHistorySize <= 100) {
                for (String line : allHistory) {
                    this.chatArea.appendText(line);
                    this.chatArea.appendText("\n");
                }
            } else {
                for (String line : allHistory.subList(allHistorySize - 100, allHistorySize - 1)) {
                    this.chatArea.appendText(line);
                    this.chatArea.appendText("\n");
                }
            }
            chatArea.appendText("---------------------------\n");
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveHistory (String login) {
        if (!login.equals("")) {
            try (FileOutputStream historyOut = new FileOutputStream("history_" + login + ".txt")) {
                String history = chatArea.getText();
                byte[] hb = history.getBytes(StandardCharsets.UTF_8);
                historyOut.write(hb);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}