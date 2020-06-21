package src.main.java;

import java.sql.*;
import java.util.Date;

public class DBService {
    private static Connection connection;
    private static Statement stmt;

    public static void connect(){
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:users.db");
            stmt = connection.createStatement();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public static String getNickByLoginAndPass(String login, String pass) {
        String sql = String.format("select nickname from users where login = '%s' and password = '%s'", login, pass.hashCode());
        ResultSet rs = null;
        try {
            rs = stmt.executeQuery(sql);
            if(rs.next()){
                return rs.getString(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return  null;
    }


    public static void disconnect(){
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public static boolean checkClient(String nickname){
        String sql = String.format("select nickname from users");
        ResultSet rs = null;
        try {
            rs = stmt.executeQuery(sql);
            while (rs.next()) {
                if(rs.getString(1).equals(nickname)) return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return  false;
    }

    public static void regNewClient(String nickname, String login, String password) {
        try {
            String query = "INSERT INTO users (nickname ,login, password) VALUES (?, ?, ?);";
            PreparedStatement ps = connection.prepareStatement(query);
            ps.setString(1, nickname);
            ps.setString(2, login);
            ps.setInt(3, password.hashCode());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void logger(String nick, String action) {
        try {
            String query = "INSERT INTO logger (user_nickname ,action , action_date) VALUES (?, ?, ?);";
            PreparedStatement ps = connection.prepareStatement(query);
            ps.setString(1, nick);
            ps.setString(2, action);
            ps.setString(3, new Date().toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
