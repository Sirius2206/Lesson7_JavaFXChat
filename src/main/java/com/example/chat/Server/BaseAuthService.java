package com.example.chat.Server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.*;
import java.util.logging.Logger;

public class BaseAuthService implements AuthService {
    private static Connection connection;
    private static Statement stmt;
    static Logger LOG = Logger.getLogger(BaseAuthService.class.getName());

    public static void connect(){
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
        LOG.info("Сервис аутентификации запущен");
        try {
            connect();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            disconnect();
        }
    }

    public void stop() {
        LOG.info("Сервис аутентификации остановлен");

    }

    public String getNickByLoginPass(String login, String pass, DataInputStream in, DataOutputStream out) throws SQLException {
        connection = DriverManager.getConnection("jdbc:sqlite:SiriusChatDatabase.db");
        stmt = connection.createStatement();
        String nick = null;
        try {
            LOG.info("Происходит поиск пользователя с логином " + login);
            ResultSet rs = stmt.executeQuery("SELECT * FROM Accounts WHERE Login = '" + login + "' AND Password = '"+ pass + "';");
            if (!rs.next()) {
                LOG.info("Пользователь с логином " + login + " не найден.");
                return null;
            }
            LOG.info("Пользователь с логином " + login + " найден.");
            if (rs.getString("Nickname") == null){
                LOG.info("Пользователь с логином " + login + " создает никнейм.");
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
                LOG.info("Пользователь с логином " + login + " успешно авторизован.");

                System.out.println(nick);
                return nick;
            }
            LOG.info("Пользователь с логином " + login + " успешно авторизован.");
            return rs.getString("Nickname");
        } catch (SQLException | IOException e){
            LOG.warning("Произошла ошибка при авторизации пользователя");
            e.printStackTrace();
        }
        LOG.warning("Не получилось авторизовать пользователя" + login);
        return null;
    }

    public boolean renameUser(String to, String from){
        try {
            LOG.info("Пользователь" + from + " запустил смену никнейма на " + to);
            stmt.executeUpdate("UPDATE Accounts SET Nickname = '" + to + "' WHERE Nickname = '" + from + "';");
            LOG.info("Никнейм успешно изменен.");
            return true;
        } catch (SQLException e) {
            LOG.warning("Не получилось изменить никнейм из-за ошибки, связанной с БД.");
            e.printStackTrace();
        }
        LOG.warning("Не получилось изменить никнейм");
        return false;
    }
}
