import java.io.*;
import java.net.Socket;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class ClientHandler {
    private Socket socket;
    DataInputStream in;
    DataOutputStream out;
    BufferedInputStream bis;

    MainServ serv;
    String nick;
    static final Logger LOGGER = Logger.getLogger(ClientHandler.class.getName());


    public String getNick() {return nick;}


    public ClientHandler(MainServ serv, Socket socket){
    try {
        this.socket = socket;
        this.serv = serv;
        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());
        this.bis = new BufferedInputStream(socket.getInputStream());


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
                                LOGGER.info("Попытка регистрации ника " + tockens[1] + ". Ник занят.");
                            }else {
                                DBService.regNewClient(tockens[1], tockens[2], tockens[3]);
                                sendMsg("/regok");
                                DBService.logger(tockens[1], "register");
                                LOGGER.info("Регистрация нового клиента. НИК: " + tockens[1] );
                            }
                        }
                        if (msg.startsWith("/auth")) {
                            String[] tockens = msg.split(" ");
                            String newNick = DBService.getNickByLoginAndPass(tockens[1], tockens[2]);
                            if(serv.checkNick(newNick)){
                                sendMsg("Логин/ник занят. Введите другой логин");
                                DBService.logger(nick, "logg faild");
                                LOGGER.info("Логин/ник " + nick + " занят. Введите другой логин");

                            }
                            else if(newNick != null){
                                sendMsg("/authok");
                                nick = newNick;
                                serv.subscribe(ClientHandler.this);
                                DBService.logger(nick, "logged in");
                                LOGGER.info(nick + " connected");
                                String path = "server/clients/" + nick;
                                File file = new File(path);
                                file.mkdirs();
                                break;
                            }else{
                                sendMsg("Неверный логин/пароль");
                                LOGGER.info("Неверный логин/пароль");
                            }
                        }
                    }

                    while (true) {
                        broadcastServerFile();
                        String msg = in.readUTF();

                        if (msg.equals("/end")) {
                            out.writeUTF("/serverClosed");
                            DBService.logger(nick, "logged out");
                            LOGGER.info(nick + " вышел из чата");
                            break;
                        }
                        if(msg.startsWith("/sendFile")) {
                            String[] tokens = msg.split(" ");
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

                            int x, y;
                            byte[] buffer = new byte[8192];
                            while (in.available() > 0) {
                                if ((x = in.read(buffer)) != -1) {
                                    fos.write(buffer, 0, x);
                                    fos.flush();
                                }
                            }


//                            while (bis.available() > 0) {
//                                if ((x = bis.read(buffer)) != -1) {
//                                    fos.write(buffer, 0, x);
//                                    fos.flush();
//                                }
//                            }
//                            while ((x = bis.read(buffer)) != -1){
//                                System.out.println("x on server: " + x);
//                                fos.write(buffer,0 ,x);
//                                fos.flush();
//                            }
                            fos.close();
                            sendMsg("transOK");
                            System.out.println("recived");
                            broadcastServerFile();


                        } else if (msg.startsWith("/getFileFromServer")) {

                            String[] tockens = msg.split(" ");
                            String path = "server/clients/" + nick + "/" + tockens[1];
                            File file = new File(path);
                            System.out.println("from server fileName: " + file.getName());
                            String outMsg = "/sendFileFromServer " + file.getName();

                            out.writeUTF(outMsg);
                            long start =  System.currentTimeMillis();
                            System.out.println("START TRANS FROM SERVER");

                            try(FileInputStream fis = new FileInputStream(file)) {
                                BufferedOutputStream bos = new BufferedOutputStream(out);

                                int x;
                                byte[] buffer = new byte[8192];

                                while (fis.available() > 0){
                                    if ((x = fis.read(buffer)) != -1 ) {
                                        bos.write(buffer, 0, x);
                                        bos.flush();
                                    }
                                }

                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            System.out.println("client send");

                            System.out.print("transfer time: ");
                            System.out.println(  System.currentTimeMillis() - start);





                        }else if(msg.startsWith("/w")) {
                            serv.sendPrivateMsg(nick, msg);
                            String[] tockens = msg.split(" ", 3);
                            LOGGER.info(nick + " отправил личное сообщение " + tockens[1]);
                        }else if(msg.startsWith("/bl")){
                            String tockens[] =msg.split(" ");
                            if(DBService.getIdByNickname(tockens[1]) != null){
                                DBService.addToBlackList(nick, tockens[1]);
                                sendMsg("Пользователь: " + tockens[1] + " в черном списке.");
                                String log = "add " + tockens[1] + " to blacklist";
                                DBService.logger(nick, log);
                                LOGGER.info(nick + " " + log);
                                serv.broadcastClientsList();
                            }else sendMsg("Вы хотите добавить в черный список несуществующего пользователя");
                            LOGGER.info("Попытка добавить в черный список пользователем " + nick + " несуществующего клиента");
                        }else serv.broadcastMsg(nick + " " +nick + ": " + msg);

                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    LOGGER.info("Что-то пошло не так");
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
//            System.out.println(fileName);
        }

        String out = sb.toString();
        sendMsg(out);
//        System.out.println(out);

    }

}
