package com.javarush.task.task30.task3008;

import java.io.IOException;
import java.io.StringWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class Server {
        private static Map<String, Connection> connectionMap = new ConcurrentHashMap<>();

    private static class Handler extends Thread{
        private Socket socket;
        public Handler(Socket socket){
            this.socket = socket;
        }
        private String serverHandshake(Connection connection) throws IOException, ClassNotFoundException{
            String name = "";
            Message userMes = null;
            while (true) {
                connection.send(new Message(MessageType.NAME_REQUEST));
                 userMes = connection.receive();
                if (userMes.getType().equals(MessageType.USER_NAME) &&!userMes.getData().equals("") && !connectionMap.containsKey(userMes.getData())){
                    connectionMap.put(userMes.getData(),connection);
                    name = userMes.getData();
                    connection.send(new Message(MessageType.NAME_ACCEPTED,"Имя принято"));
                    break;
                }
            }
            return name;
        }
        private void notifyUsers(Connection connection, String userName) throws IOException, ClassNotFoundException {
            for (Map.Entry<String,Connection> entry : connectionMap.entrySet()){
                if (!entry.getKey().equals(userName)){
                    connection.send( new Message(MessageType.USER_ADDED,entry.getKey()));
                }
            }
        }
        private void serverMainLoop(Connection connection, String userName) throws IOException, ClassNotFoundException {
            while (true) {
                Message message = connection.receive();
                if (message.getType()==(MessageType.TEXT)) {
                    String userText = userName + ": " + message.getData();
                     sendBroadcastMessage(new Message(MessageType.TEXT, userText));

                }else {
                     ConsoleHelper.writeMessage("Неверный формат сообщения");
                }
            }
        }

        @Override
        public void run()  {
            System.out.println(socket.getRemoteSocketAddress());

            String nameOfUser = null;
            try(Connection connection = new Connection(socket)) {
                nameOfUser = serverHandshake(connection);
                sendBroadcastMessage(new Message(MessageType.USER_ADDED,nameOfUser));
                notifyUsers(connection,nameOfUser);
                serverMainLoop(connection,nameOfUser);

            } catch (IOException | ClassNotFoundException e) {
               ConsoleHelper.writeMessage("произошла ошибка при обмене данными с удаленным адресом");
            }

            if (nameOfUser!=null){
                connectionMap.remove(nameOfUser);
                sendBroadcastMessage(new Message(MessageType.USER_REMOVED,nameOfUser));
            }
            ConsoleHelper.writeMessage("соединение с удаленным адресом закрыто.");
        }
    }

    public  static void sendBroadcastMessage(Message message){
        String name = "";
        try {
            for (Map.Entry<String, Connection> entry : connectionMap.entrySet()) {
                entry.getValue().send(message);
                name = entry.getKey();
            }
        }catch(IOException e){
            System.out.println("Ошибка отправки сообщения: "+name);
        }
        }

    public static void main(String[] args) throws IOException {

        try(ServerSocket serverSocket = new ServerSocket(ConsoleHelper.readInt())) {
            ConsoleHelper.writeMessage("Сервер запущен");
            while (true){
                new Handler(serverSocket.accept()).start();
            }
        }catch (IOException e){
            e.printStackTrace();
        }
    }
}
