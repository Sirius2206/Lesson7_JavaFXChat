package com.example.chat;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientChatController {
    private Socket socket = new Socket("localhost", 8189);
    DataInputStream in = new DataInputStream(socket.getInputStream());
    DataOutputStream out = new DataOutputStream(socket.getOutputStream());
    @FXML
    private TextArea chatArea;

    @FXML
    private TextField userInput;

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
            Thread readMsg = new Thread(() ->{
                String serverMessage;
                String nickname = "";
                while (true) {
                    try {
                        serverMessage = in.readUTF();
                        if (serverMessage.startsWith("/authok")){
                            nickname = serverMessage.substring(8).split("\n")[0];
                        }
                        if (!serverMessage.startsWith(nickname)){
                            chatArea.appendText(serverMessage + "\n");
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }});
            readMsg.start();
    }
}