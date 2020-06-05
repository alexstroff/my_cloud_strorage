import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;

import java.io.*;
import java.net.Socket;

public class Controller {

    public HBox transferPannel;
    @FXML
    TextField setLoginField;

    @FXML
    TextField setNicknameField;

    @FXML
    TextField setPasswordField;

    @FXML
    ListView<String> clientList;

    @FXML
    ListView<String> servertFileList;

    @FXML
    ListView<String> clientFileList;

    @FXML
    TextArea textArea;

    @FXML
    TextField textField;

    @FXML
    HBox bottomPanel;

    @FXML
    HBox regPanel;

    @FXML
    HBox upperPanel;

    @FXML
    TextField loginField;

    @FXML
    PasswordField passwordField;


    Socket socket;
    DataInputStream in;
    DataOutputStream out;

    String clientFileName;
    String serverFileName;


    final String IP_ADRESS = "localhost";
    final int PORT = 8189;

    private boolean isAuthorised;

    public void  setAuthorised(boolean isAuthorised){
        this.isAuthorised = isAuthorised;
        if(!isAuthorised){
            upperPanel.setVisible(true);
            upperPanel.setManaged(true);
            transferPannel.setVisible(false);
            transferPannel.setManaged(false);

            bottomPanel.setVisible(false);
            bottomPanel.setManaged(false);
            regPanel.setManaged(false);
            regPanel.setVisible(false);
        }else {
            upperPanel.setVisible(false);
            upperPanel.setManaged(false);
            bottomPanel.setVisible(true);
            bottomPanel.setManaged(true);
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
            bottomPanel.setVisible(true);
            bottomPanel.setManaged(true);
            regPanel.setManaged(false);
            regPanel.setVisible(false);
            transferPannel.setVisible(true);
            transferPannel.setManaged(true);
        }else {
            upperPanel.setVisible(true);
            upperPanel.setManaged(true);
            bottomPanel.setVisible(false);
            bottomPanel.setManaged(false);
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
                            String srt = in.readUTF();
                            if (srt.equals("/authok")) {
                                setAuthorised(true);
                                textArea.appendText("Авторизация прошла успешно" + "\n");
                                File file = new File("client/files");
                                file.mkdirs();
                                break;
                            }else if (srt.equals("/regok")) {
                                textArea.appendText("Регистрация прошла успешно" + "\n");
                                setRegistration(true);
                            }else {
                                textArea.appendText(srt + "\n");
                            }
                        }


                        while (true) {
                            broadcastClientFile();

                            String str = in.readUTF();
                            if (str.startsWith("/")) {
                                if (str.equals("/serverclosed")) break;
                                if (str.startsWith("/clientslist ")) {
                                    String[] tokens = str.split(" ");
                                    Platform.runLater(() -> {
                                        clientList.getItems().clear();
                                        for (int i = 1; i < tokens.length; i++) {
                                            clientList.getItems().add(tokens[i]);
                                        }
                                    });
                                }
                                if (str.startsWith("/serverfilelist ")) {
                                    String[] tokens = str.split(" ");
                                    Platform.runLater(() -> {
                                        servertFileList.getItems().clear();
                                        for (int i = 1; i < tokens.length; i++) {
                                            servertFileList.getItems().add(tokens[i]);
                                        }
                                    });
                                }

                                if(str.startsWith("/sendFileFromServer")){
                                    sendFileFromServer(str);
                                }
                            } else {
                                textArea.appendText(str + "\n");
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            socket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        setAuthorised(false);
                    }
                }
            }).start();
        } catch (IOException e) {

            e.printStackTrace();
        }
    }


    public void sendMsg() {
        try {
            out.writeUTF(textField.getText());
            textField.clear();
            textField.requestFocus();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void broadcastClientFile(){
        File file = new File("client/files");
        StringBuilder sb = new StringBuilder();

        String[] str = file.list();
        for (String fileName:str) {
            sb.append(fileName + " ");
        }
        String out = sb.toString();

        String[] tokens = out.split(" ");
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
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void tryToAuth(ActionEvent event) {
        if(socket == null || socket.isClosed()){
            connect();
        }
        try {
            out.writeUTF("/auth " + loginField.getText() + " " + passwordField.getText());
            loginField.clear();
            passwordField.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void selectClient(MouseEvent mouseEvent) {
    }


    public void regpanel(ActionEvent event) {
        upperPanel.setVisible(false);
        upperPanel.setManaged(false);
        bottomPanel.setVisible(false);
        bottomPanel.setManaged(false);
        regPanel.setManaged(true);
        regPanel.setVisible(true);
    }

    public void reg(ActionEvent event) {
        if(socket == null || socket.isClosed()){
            connect();
        }
        try {
            out.writeUTF("/reg " + setNicknameField.getText() + " " + setLoginField.getText() + " " +
                    setPasswordField.getText());
            setNicknameField.clear();
            setLoginField.clear();
            setPasswordField.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendFileToServer(ActionEvent actionEvent) {
        System.out.println("fileName on but: " + clientFileName);
        String filePath = "client/files/" + clientFileName;

        File file = new File(filePath);
        System.out.println("file name " + file.getName());
        System.out.println("file length: " + file.length());

        String str = "/sendFile " + clientFileName;
        try {
            System.out.println("send filename: " + str);
            out.writeUTF(str);
        } catch (IOException e) {
            e.printStackTrace();
        }

        long start =  System.currentTimeMillis();
        System.out.println("START TRANS");

        try(FileInputStream fis = new FileInputStream(file);
           ) {
            BufferedOutputStream bos = new BufferedOutputStream(out);

            int x;
            byte[] buffer = new byte[8192];
//            while ((x = fis.read(buffer)) != -1){
//                out.write(buffer, 0, x);
//            }

            while (fis.available() > 0){
                if ((x = fis.read(buffer)) != -1 ) {
                    bos.write(buffer, 0, x);
                    bos.flush();
                }
            }

//            while ((x = fis.read(buffer)) != -1 ){
//                bos.write(buffer, 0, x);
//                bos.flush();
//            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("str: " + str);
        System.out.println("client send");

        System.out.print("transfer time: ");
        System.out.println(  System.currentTimeMillis() - start);
    }



    public void selectServer(MouseEvent mouseEvent) {
    }

    public void selectClientFile(MouseEvent mouseEvent) {
        clientFileName = clientFileList.getSelectionModel().getSelectedItem();
        System.out.println("clientFileName on click: " + clientFileName);
    }

    public void selectServerFile(MouseEvent mouseEvent) {
        serverFileName = servertFileList.getSelectionModel().getSelectedItem();
        System.out.println("serverFileName on click: " + serverFileName);
    }

    public void sendFileToClient(ActionEvent actionEvent) {
        String str = "/getFileFromServer " + serverFileName;
        try {
            out.writeUTF(str);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    private void sendFileFromServer(String str) {
        System.out.println("client start recive from server");
        String[] tockens = str.split(" ");

        String filePath = "client/files/";

        String fileName = filePath  + "/" + tockens[1];

        File file = new File(fileName);
        try {
            if(!file.exists()){
                file.createNewFile();
            }
            FileOutputStream fos = new FileOutputStream(file);



            BufferedInputStream bis = new BufferedInputStream(in);

            int x;
            byte[] buffer = new byte[8192];
            while (bis.available() > 0){
                if((x = bis.read(buffer)) != -1){
                    fos.write(buffer,0 ,x);
                    fos.flush();
                }
            }
            fos.close();
            System.out.println("client recived");

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}