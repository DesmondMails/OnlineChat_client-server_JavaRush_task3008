package com.javarush.task.task30.task3008.client;

import com.javarush.task.task30.task3008.Connection;
import com.javarush.task.task30.task3008.ConsoleHelper;
import com.javarush.task.task30.task3008.Message;
import com.javarush.task.task30.task3008.MessageType;

import java.io.IOException;
import java.net.Socket;

public class Client {
    protected Connection connection;
    private volatile boolean clientConnected = false;

    public class SocketThread extends Thread{
        protected void processIncomingMessage(String message){
            ConsoleHelper.writeMessage(message);
        }
        protected void informAboutAddingNewUser(String userName){
            ConsoleHelper.writeMessage(userName + " присоединился");
        }
        protected void informAboutDeletingNewUser(String userName){
            ConsoleHelper.writeMessage(userName + " покинул чат");
        }
        protected void notifyConnectionStatusChanged(boolean clientConnected){

           Client.this.clientConnected = clientConnected;

           synchronized (Client.this){
               Client.this.notify();
           }
        }
        protected void clientHandshake() throws IOException, ClassNotFoundException{
            while (true){
               Message message =  Client.this.connection.receive();
               if (message.getType() == MessageType.NAME_REQUEST){
                   String name = getUserName();
                   connection.send(new Message(MessageType.USER_NAME,name));
               }
               else if (message.getType() == MessageType.NAME_ACCEPTED){
                   notifyConnectionStatusChanged(true);
                   return;
               }else{
                   throw new IOException("Unexpected MessageType");
               }
            }
        }
        protected void clientMainLoop() throws IOException, ClassNotFoundException {
            while (true) {
                Message message = connection.receive();
                if (message.getType() == MessageType.TEXT) {
                    processIncomingMessage(message.getData());
                } else if (message.getType() == MessageType.USER_ADDED) {
                    informAboutAddingNewUser(message.getData());
                } else if (message.getType() == MessageType.USER_REMOVED) {
                    informAboutDeletingNewUser(message.getData());
                } else {
                    throw new IOException("Unexpected MessageType");
                }
            }
        }

        @Override
        public void run() {

            try {

                connection = new Connection(new Socket(getServerAddress(),getServerPort()));
                clientHandshake();
                clientMainLoop();
            } catch (IOException  | ClassNotFoundException e) {
                notifyConnectionStatusChanged(false);
            }

        }
    }
    protected String getServerAddress(){
        return ConsoleHelper.readString();
    }
    protected int getServerPort(){
        return ConsoleHelper.readInt();
    }
    protected String getUserName(){
       return ConsoleHelper.readString();
    }
    protected boolean shouldSendTextFromConsole(){
        return true;
    }
    protected SocketThread getSocketThread(){
        return new SocketThread();
    }
    protected void sendTextMessage(String text){
        try {
            connection.send(new Message(MessageType.TEXT,text));
        }catch (IOException e){
            ConsoleHelper.writeMessage(this.getUserName());
            this.clientConnected = false;
        }
    }
    public void run(){
        SocketThread socketThread =  getSocketThread();
        socketThread.setDaemon(true);
        socketThread.start();

            synchronized (this) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    ConsoleHelper.writeMessage("Error");
                }
            }

        if (!clientConnected) {
            ConsoleHelper.writeMessage("Произошла ошибка во время работы клиента.");
        } else {
            ConsoleHelper.writeMessage("Соединение установлено.\n"+
                    "Для выхода наберите команду 'exit'.");
        }
        while (clientConnected){
            String data = ConsoleHelper.readString();
            if (data.equals("exit")){
                break;
            }
            if (shouldSendTextFromConsole()) {
                sendTextMessage(data);
            }
        }

    }

    public static void main(String[] args) {
        Client client = new Client();
        client.run();
    }
}
