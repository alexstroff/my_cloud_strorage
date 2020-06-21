package src.main.java;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;

public class MainServ {
    private Vector<ClientHandler> clients;

    public MainServ() {
        clients = new Vector<>();
        ServerSocket server = null;
        Socket socket = null;
        try {
            DBService.connect();
            server = new ServerSocket(8189);
            System.out.println("Сервер запущен!");

            while (true) {
                socket = server.accept();
                new ClientHandler(this, socket);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                server.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            DBService.disconnect();
        }
    }

    public void subscribe(ClientHandler client){
        clients.add(client);
        System.out.println("Клиент " + client.nick +  " подключился");
    }

    public void unsubscribe(ClientHandler client) {
        clients.remove(client);
        System.out.println("Клиент " + client.nick +  " отключился");
    }


    public boolean checkNick(String newNick) {
        boolean check = false;
        for(ClientHandler e: clients){
            if(e.nick.equals(newNick)) check = true;
        }
        return check;
    }
}
