package main.java;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Controller {

    public HBox transferPannel;

    @FXML

    Label autLable;

    @FXML

    Label regLable;

    @FXML

    TextField setLoginField;

    @FXML
    TextField setNicknameField;

    @FXML
    TextField setPasswordField;


    @FXML
    ListView<String> servertFileList;

    @FXML
    ListView<String> clientFileList;

    @FXML
    HBox regPanel;

    @FXML
    HBox upperPanel;

    @FXML
    TextField loginField;

    @FXML
    PasswordField passwordField;

    @FXML
    Button upButClient;

    @FXML
    Button updateServerFilelist;


    Socket socket;
    DataInputStream in;
    DataOutputStream out;


    private String clientFileName;
    private String serverFileName;
    private final String rootPath = "client/files";
    private List<String> path = new ArrayList<>();

    private String nick;

    private String str;



    final String IP_ADRESS = "localhost";
    final int PORT = 8189;

    public static boolean isAlive = true;

    private boolean isAuthorised;

    public void  setAuthorised(boolean isAuthorised){
        this.isAuthorised = isAuthorised;
        if(!isAuthorised){
            upperPanel.setVisible(true);
            upperPanel.setManaged(true);
            transferPannel.setVisible(false);
            transferPannel.setManaged(false);
            regPanel.setManaged(false);
            regPanel.setVisible(false);
        }else {
            upperPanel.setVisible(false);
            upperPanel.setManaged(false);
            regPanel.setManaged(false);
            regPanel.setVisible(false);
            transferPannel.setVisible(true);
            transferPannel.setManaged(true);
        }
    }

    public void  setRegistration(boolean isAuthorised){
        this.isAuthorised = isAuthorised;
        if(!isAuthorised){
            upperPanel.setVisible(false);
            upperPanel.setManaged(false);
            regPanel.setManaged(false);
            regPanel.setVisible(false);
            transferPannel.setVisible(true);
            transferPannel.setManaged(true);
        }else {
            upperPanel.setVisible(true);
            upperPanel.setManaged(true);
            regPanel.setManaged(false);
            regPanel.setVisible(false);
            transferPannel.setVisible(false);
            transferPannel.setManaged(false);
        }
    }

    public void connect() {
        try {
            socket = new Socket(IP_ADRESS, PORT);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        while (true) {
                            str = in.readUTF();
                            if (str.equals("/authok")) {
                            setAuthorised(true);
                            File file = new File(rootPath);
                            file.mkdirs();
                            path.add(rootPath);
                            broadcastClientFile(dirPath(path));
                            broadcastServertFileList();
                            System.out.println("client connected");
                            break;
                            }else if (str.equals("/regok")) {
                                setRegistration(true);
                            }
                            else if(str.startsWith("/isBusy")){
                                String[] tockens = str.split(" ", 2);
                                regLableSetText(tockens[1]);
                            }else if(str.startsWith("/authProblem")){
                                String[] tockens = str.split(" ", 2);
                                autLableSetText(tockens[1]);
                            }else if(str.startsWith("/errlog")){
                                String[] tockens = str.split(" ", 2);
                                autLableSetText(tockens[1]);
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();

        } catch (IOException e) {

            e.printStackTrace();
        }
    }

    private void regLableSetText(String str) {

        Platform.runLater(() -> regLable.setText(str));

    }

    public void broadcastClientFile(String path){

        File file = new File(path);
        StringBuilder sb = new StringBuilder();

        String[] str = file.list();
        for (String fileName:str) {
            sb.append(fileName + "!@");
        }
        String out = sb.toString();
        String[] tokens = out.split("!@");
        Platform.runLater(() -> {
            clientFileList.getItems().clear();
            for (int i = 0; i < tokens.length; i++) {
                clientFileList.getItems().add(tokens[i]);
            }
        });
    }

    public void Dispose() {
        System.out.println("Отправляем сообщение на сервер о завершении работы");
        try {
            if (out != null) {
                out.writeUTF("/end");
                out.flush();
                isAlive = false;
                setAuthorised(false);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void tryToAuth(ActionEvent event) {
        if(socket == null || socket.isClosed()){
            connect();
        }
        if((loginField.getText().isEmpty()) || (passwordField.getText().isEmpty())) {
            autLableSetText("Введите логин и пароль");
        }else {
            try {
                nick = loginField.getText();
                out.writeUTF("/auth" + "!@" + nick + "!@" + passwordField.getText());
                out.flush();
                loginField.clear();
                passwordField.clear();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void autLableSetText(String str) {
        Platform.runLater(() -> autLable.setText(str));
    }

    public void regpanel(ActionEvent event) {
        upperPanel.setVisible(false);
        upperPanel.setManaged(false);
        regPanel.setManaged(true);
        regPanel.setVisible(true);
    }

    public void reg(ActionEvent event) {
        if(socket == null || socket.isClosed()){
            connect();
        }
        if(setNicknameField.getText().isEmpty() || setLoginField.getText().isEmpty() || setPasswordField.getText().isEmpty()){
            regLableSetText("Введите Ник, Логин и пароль");
        }else{
            try {
                out.writeUTF("/reg" + "!@" + setNicknameField.getText() + "!@" + setLoginField.getText() + "!@" +
                        setPasswordField.getText());
                out.flush();
                setNicknameField.clear();
                setLoginField.clear();
                setPasswordField.clear();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendFileToServer(ActionEvent actionEvent) {
        String filePath = dirPath(path) + "/" + clientFileName;
        File file = new File(filePath);
        if(file.getName() != null) {
            String outMsg = "/sendFileToServer" + "!@" + clientFileName + "!@" + file.length();
            System.out.println(file.length());
            try {
                this.out.writeUTF(outMsg);
            } catch (IOException e) {
                e.printStackTrace();
            }
            int x, y = 0;
            byte[] buffer = new byte[10240];
            try {
                FileInputStream fis = new FileInputStream(file);
                while ((x = fis.read(buffer)) != -1){
                    out.write(buffer, 0, x);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("transfer OK");
            broadcastServertFileList();
        }
    }



    private void broadcastServertFileList() {
        try {
            out.writeUTF("/broadcatsserverfiles");
        } catch (IOException e) {
            e.printStackTrace();
        }
            try {
                str = in.readUTF();
                if (str.startsWith("/serverfilelist")) {
                    String[] tokens = str.split("!@");
                    Platform.runLater(() -> {
                        servertFileList.getItems().clear();
                        for (int i = 1; i < tokens.length; i++) {
                            servertFileList.getItems().add(tokens[i]);
                        }
                    });
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
    }

    public void upClientDir(ActionEvent actionEvent) {
        if(path.size() >1){
            path.remove(path.size() - 1);
            broadcastClientFile(dirPath(path));
        }
    }

    public void selectClientFile(MouseEvent mouseEvent) {
        path.add("/" + clientFileList.getSelectionModel().getSelectedItem());
        File file = new File(dirPath(path));
        if (!file.isDirectory()){
            path.remove(path.size() - 1);
        }else broadcastClientFile(dirPath(path));
        clientFileName = clientFileList.getSelectionModel().getSelectedItem();
    }

    private String dirPath(List<String> path) {
        StringBuilder sb = new StringBuilder();
        for (String s: path){
            sb.append(s);
        }
        String dirPath = sb.toString();
        return dirPath;
    }

    public void selectServerFile(MouseEvent mouseEvent) {
        serverFileName = servertFileList.getSelectionModel().getSelectedItem();
    }

    public void sendFileToClient(ActionEvent actionEvent) {

        String out = "/getFileFromServer!@" + serverFileName;
        try {
            this.out.writeUTF(out);
            this.out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        sendFileFromServer();

    }
    private void sendFileFromServer() {

        try {
            String str = in.readUTF();
            if (str.startsWith("/sendFileFromServer")) {
                String[] tockens = str.split("!@", 3);
                long fileSize = Long.parseLong(tockens[2]);
                String fileName = dirPath(path) + "/" + tockens[1];
                File file = new File(fileName);
                if (!file.exists()) {
                    try {
                        file.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                try (FileOutputStream fos = new FileOutputStream(file);
                     BufferedInputStream bis = new BufferedInputStream(socket.getInputStream())) {
                    int x;
                    byte[] buffer = new byte[10240];
                    while ((in.available()) != 0) {
                        x = in.read(buffer);
                        fos.write(buffer, 0, x);
                        fos.flush();
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                broadcastClientFile(dirPath(path));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void deleteFileServer(ActionEvent actionEvent) {
        String out = "/delFile!@" + serverFileName;
        try {
            this.out.writeUTF(out);
            this.out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        broadcastServertFileList();
    }

    public void deleteFileClient(ActionEvent actionEvent) {
        String filePath = dirPath(path) + "/" + clientFileName;
        File file = new File(filePath);
        file.delete();
        broadcastClientFile(dirPath(path));
    }

    public void back(ActionEvent actionEvent) {
        upperPanel.setVisible(true);
        upperPanel.setManaged(true);
        regPanel.setManaged(false);
        regPanel.setVisible(false);

    }

    public void updateServer(ActionEvent actionEvent) {
        broadcastServertFileList();
    }
}
