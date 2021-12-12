package com.example.chat.Server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.sql.SQLException;

public interface AuthService {
    void start();
    void stop();
    String getNickByLoginPass(String login, String pass, DataInputStream in, DataOutputStream out) throws SQLException;

    boolean renameUser(String substring, String name) throws SQLException;
}
