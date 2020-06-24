package src.main.java;

import java.io.*;
import java.net.Socket;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class ClientHandler {
    private Socket socket;

    DataInputStream in;
    DataOutputStream out;

    MainServ serv;
    String nick;

    public String getNick() {return nick;}

    public ClientHandler(MainServ serv, Socket socket){
    try {
        this.socket = socket;
        this.serv = serv;
        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {

                    while (true) {
                        String msg = in.readUTF();
                        if(msg.startsWith("/reg")){
                            String[] tockens = msg.split("!@");
                            if(DBService.checkClient(tockens[1])){
                                sendMsg("/isBusy Ник занят попробуйте друдой");
                                DBService.logger(tockens[1], "register faild");
                            }else {
                                DBService.regNewClient(tockens[1], tockens[2], tockens[3]);
                                sendMsg("/regok");
                                DBService.logger(tockens[1], "register");
                            }
                        }
                        if (msg.startsWith("/auth")) {
                            String[] tockens = msg.split("!@");
                            String newNick = DBService.getNickByLoginAndPass(tockens[1], tockens[2]);
                            if(serv.checkNick(newNick)){
                                sendMsg("/authProblem Логин/ник занят. Введите другой логин");
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

                                break;
                            }else{
                                sendMsg("/errlog Неверный логин/пароль");
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
                        if(msg.equals("/broadcatsserverfiles")){
                            broadcastServerFile();

                        }
                        if(msg.startsWith("/sendFileToServer")) {
                            String[] tokens = msg.split("!@", 3);
                            String filePath = "server/clients/" + nick;
                            String fileName = filePath + "/" + tokens[1];
                            long fileSize = Long.parseLong(tokens[2]);
                            System.out.println(fileSize);
                            File file = new File(filePath);
                            file.mkdirs();
                            file = new File(fileName);
                            if (!file.exists()) {
                                file.createNewFile();
                            }
                            System.out.println("start transfer");
                            int x, y = 0;
                            long start = System.currentTimeMillis();

                            byte[] buffer = new byte[10240];
                            FileOutputStream fos = new FileOutputStream(file);
                            try {
                                BufferedInputStream bis = new BufferedInputStream(in);
                                while (in.available() != 0){
                                    x = in.read(buffer);
                                    fos.write(buffer, 0, x);
                                    fos.flush();
                                }
                                System.out.println("y: " + y);
                                System.out.print("Transfer done, transfer time: ");
                                System.out.println(System.currentTimeMillis() - start);
                            }catch (IOException e){
                                e.printStackTrace();
                            }
                        }
                        if(msg.startsWith("/delFile")){
                            String[] tockens = msg.split("!@");
                            String path = "server/clients/" + nick + "/" + tockens[1];
                            File file = new File(path);
                            file.delete();

                        }
                        if (msg.startsWith("/getFileFromServer")) {
                            String[] tockens = msg.split("!@", 2);
                            String path = "server/clients/" + nick + "/" + tockens[1];
                            File file = new File(path);
                            String outMsg = "/sendFileFromServer!@" + file.getName() + "!@" + file.length();
                            out.writeUTF(outMsg);
                            System.out.println("start");
                            byte[] buffer = new byte[10240];
                            try(BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));) {
                                int x;
                                while ((x = bis.read(buffer)) != -1){
                                    out.write(buffer, 0, x);
                                    out.flush();
                                }
                                System.out.println("OK");
                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
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
        sb.append("/serverfilelist!@");
        String[] str = file.list();
        for (String fileName:str) {
            sb.append(fileName + "!@");
        }
        String outMsg = sb.toString();
        sendMsg(outMsg);
    }
}
