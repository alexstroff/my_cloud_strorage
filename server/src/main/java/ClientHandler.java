package src.main.java;

import java.io.*;
import java.net.Socket;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class ClientHandler {
    private Socket socket;

    DataInputStream in;
    DataOutputStream out;
//    BufferedInputStream bis;

    MainServ serv;
    String nick;

    public String getNick() {return nick;}

    public ClientHandler(MainServ serv, Socket socket){
    try {
        this.socket = socket;
        this.serv = serv;
        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());
//        this.bis = new BufferedInputStream(in);



        new Thread(new Runnable() {
            @Override
            public void run() {
                try {

                    while (true) {
                        String msg = in.readUTF();
                        if(msg.startsWith("/reg")){
                            String[] tockens = msg.split(" ");
                            if(DBService.checkClient(tockens[1])){
                                sendMsg("Ник занят попробуйте друдой");
                                DBService.logger(tockens[1], "register faild");
                            }else {
                                DBService.regNewClient(tockens[1], tockens[2], tockens[3]);
                                sendMsg("/regok");
                                DBService.logger(tockens[1], "register");
                            }
                        }
                        if (msg.startsWith("/auth")) {
                            String[] tockens = msg.split(" ");
                            String newNick = DBService.getNickByLoginAndPass(tockens[1], tockens[2]);
                            if(serv.checkNick(newNick)){
                                sendMsg("Логин/ник занят. Введите другой логин");
                                DBService.logger(nick, "logg faild");
                            }
                            else if(newNick != null){
                                sendMsg("/authok");
                                nick = newNick;
                                serv.subscribe(ClientHandler.this);
                                DBService.logger(nick, "logged in");
                                String path = "server/clients/" + nick;
                                File file = new File(path);
                                file.mkdirs();
                                broadcastServerFile();

                                break;
                            }else{
                                sendMsg("Неверный логин/пароль");
                            }
                        }
                    }

                    while (true) {
                        String msg = in.readUTF();

                        if (msg.equals("/end")) {
                            out.writeUTF("/serverClosed");
                            DBService.logger(nick, "logged out");
                            out.flush();
                            break;
                        }
                        if(msg.startsWith("/sendFileToServer")) {
                            String[] tokens = msg.split(" ", 2);
                            System.out.println("server start recive");
                            System.out.println("fileName " + tokens[1]);

                            String filePath = "server/clients/" + nick;

                            String fileName = filePath + "/" + tokens[1];

                            File file = new File(filePath);
                            file.mkdirs();
                            System.out.println("filePath " + fileName);
                            file = new File(fileName);
                            if (!file.exists()) {
                                file.createNewFile();
                            }

                            FileOutputStream fos = new FileOutputStream(file);
                            BufferedInputStream bis = new BufferedInputStream(new DataInputStream(socket.getInputStream()));

                            int x;
                            byte[] buffer = new byte[8192];
                            while (( bis.available()) > 0){
                                if((x = bis.read(buffer)) != -1){
                                    fos.write(buffer, 0, x);
                                }
                            }
                            fos.close();
                            bis.close();
                            System.out.println("recived");
                            broadcastServerFile();
                        }
                        if(msg.startsWith("/delFile")){
                            String[] tockens = msg.split(" ");
                            String path = "server/clients/" + nick + "/" + tockens[1];
                            File file = new File(path);
                            file.delete();
                            broadcastServerFile();

                        }
                        if (msg.startsWith("/getFileFromServer")) {

                            String[] tockens = msg.split(" ", 2);
                            String path = "server/clients/" + nick + "/" + tockens[1];
                            File file = new File(path);
                            System.out.println("from server fileName: " + file.getName());
                            String outMsg = "/sendFileFromServer " + file.getName();

                            out.writeUTF(outMsg);
                            out.flush();
                            long start =  System.currentTimeMillis();
                            System.out.println("START TRANS FROM SERVER");

                            BufferedOutputStream bos = new BufferedOutputStream(out);
                            byte[] buffer = new byte[8192];
                            try(BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));) {
                                int x;

                                while (bis.available() > 1){
                                    if((x = bis.read(buffer)) != -1) {
                                        bos.write(buffer, 0, x);
                                        bos.flush();
                                        System.out.println("X: " + x);
                                    }
                                }
                                bos.close();

                                broadcastServerFile();
                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            System.out.println("TRANS FROM SERV OK");
                            System.out.print("transfer time: ");
                            System.out.println(  System.currentTimeMillis() - start);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        in.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        out.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }serv.unsubscribe(ClientHandler.this);
                }
            }
        }).start();
    } catch (IOException e) {
        e.printStackTrace();
    }
}


    public void sendMsg(String msg) {
        try {
            out.writeUTF(msg);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void broadcastServerFile() {
        String path = "server/clients/" + nick;
        File file = new File(path);
        StringBuilder sb = new StringBuilder();
        sb.append("/serverfilelist ");
        String[] str = file.list();
        for (String fileName:str) {
            sb.append(fileName + " ");
        }
        String out = sb.toString();
        sendMsg(out);
    }
}
