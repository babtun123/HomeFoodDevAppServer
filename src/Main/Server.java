//package Main;/*
//Project: HomeAppDeliveryApp
//Group 1:
//    Kiryl Baravikou
//    Muhammad Muzzammil
//    Avadh Dhaval Patel
//    Samuel Shodiya
//Course: CS 440
// */

package Main;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class Server {

    private int count = 1;
    private ArrayList<ClientThread> clients = new ArrayList<>();
    private HashMap<String, ClientThread> clientMap = new HashMap<>();
    private HashMap<String, ClientThread> driverMap = new HashMap<>();
    private TheServer server;
    private Consumer<Serializable> callback;

    ArrayList<String> driverName = new ArrayList<>();

    Server(Consumer<Serializable> call) {
        callback = call;
        server = new TheServer();
        server.start();
    }

    // Method to send a message to a specific client by client name
    public void sendMessageToClient(String clientName, Serializable message) {
        ClientThread clientThread = clientMap.get(clientName);
        if (clientThread != null) {
            clientThread.sendMessage(message);
        } else {
            callback.accept("Client with name " + clientName + " not found.");
        }
    }

    public void sendMessageToAllDrivers(Serializable message) {

        for (Map.Entry<String, ClientThread> entry : driverMap.entrySet()) {
            ClientThread driverThread = entry.getValue();
            driverThread.sendMessage(message);
        }

    }

    public class TheServer extends Thread {

        public void run() {
            try (ServerSocket mysocket = new ServerSocket(5555)) {

                System.out.println("Server is waiting for a client!");

                while (true) {
                    Socket clientSocket = mysocket.accept();

                    ClientThread c = new ClientThread(clientSocket, count);

                    callback.accept("Client has connected to server: " + "Client #" + count);

                    clients.add(c);
                    c.start();

                    count++;
                }
            } catch (Exception e) {
                callback.accept("Server socket did not launch");
            }
        }
    }

    class ClientThread extends Thread {

        private Socket connection;
        private int count;
        private ObjectInputStream in;
        private ObjectOutputStream out;
        private String clientName;

        ClientThread(Socket s, int count) {
            this.connection = s;
            this.count = count;
            try {
                out = new ObjectOutputStream(connection.getOutputStream());
                in = new ObjectInputStream(connection.getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void sendMessage(Serializable message) {
            try {
                out.writeObject(message);
                out.flush();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void readClientInfo() {
            try {

                // Read the client's name from the initial message
                Serializable info = (Serializable) in.readObject();

                System.out.print("Info: " + info);

                if (info instanceof String) {
                    String initialMessage = (String) info;
                    if (initialMessage.startsWith("customerName:")) { // customer
                        // Extract the client name from the message
                        clientName = initialMessage.substring("customerName:".length());

                        System.out.print("Customer Name: " + clientName);
                        // Add the client to the map using their name
                        clientMap.put(clientName, this);
                    }

                    else if (initialMessage.startsWith("cook:")) { // restaurant app
                        // Extract the client name from the message
                        clientName = initialMessage.substring("cook:".length());

                        System.out.print("Cook Name: " + clientName);
                        // Add the client to the map using their name
                        clientMap.put(clientName, this);
                    }

                    else if (initialMessage.startsWith("driverName:")) { // driverName:

                        clientName = initialMessage.substring("driverName:".length());

                        System.out.print("Driver Name: " + clientName);

                        // We will use this to send all driver messages
                        // Therefore storing all their names in this arrau list
                        driverName.add(clientName);

                        clientMap.put(clientName, this);
                        driverMap.put(clientName, this);
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        public void run() {
            try {

                readClientInfo();

                while (true) {

                    Serializable info = (Serializable) in.readObject();
                    callback.accept("Client: " + clientName + ": " + info); // from main we accept message here

                    // Process the received message and send a response
                    String response = processMessage(info); // Messages get filtered here from any client


                }
            } catch (EOFException e) {
                // Client disconnected
                callback.accept("Client " + clientName + " left!");
                clients.remove(this);
                clientMap.remove(clientName); // Remove the client from the map when they leave
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Implement your message processing logic here
        private String processMessage(Serializable receivedMessage) {

            // Customer's full order in an array list
            if(receivedMessage instanceof ArrayList<?>){
                sendMessageToClient("Restaurant", receivedMessage);
            }

            // Cook app has started the order for a certain customer
            else if(receivedMessage.toString().startsWith("Placed")){
                String[] word = receivedMessage.toString().split(" ");
                String customer = word[1];
                sendMessageToClient(customer, "Your order is now being prepared.");
            }

            else if(receivedMessage.toString().startsWith("Completed")){
                String[] word = receivedMessage.toString().split(" ");
                String customer = word[1];
                sendMessageToClient(customer, "Order Completed, Finding Nearest Driver to Deliver");

                // Looping through all drivers and sending them each a message to take this guy's order:
//                sendMessageToAllDrivers(customer + ": Pickup Order Please");
                sendMessageToAllDrivers("NewOrder " + customer);
            }

            else if(receivedMessage.toString().startsWith("pickedUp")){
                String[] word =  receivedMessage.toString().split(" ");
                String customer = word[1];
                sendMessageToClient(customer, "Your Driver has Picked Up the Order");
            }
            else if(receivedMessage.toString().startsWith("Accepted")){

                String[] word =  receivedMessage.toString().split(" ");
                String customer = word[1];
                sendMessageToClient(customer, "You have been assigned a driver with id: " + clientName);
            }
            else if(receivedMessage.toString().startsWith("DriverMoney")){
                String[] word =  receivedMessage.toString().split(" ");
                String driver = word[1];
                double money = Double.parseDouble(word[2]) * 0.25;
                sendMessageToClient(driver, "Earning " + money);
            }
            else if(receivedMessage.toString().startsWith("droppedOff")){
                String[] word =  receivedMessage.toString().split(" ");
                String customer = word[1];
                sendMessageToClient(customer, "Delivered");
            }

            return "Nothing to send back";
        }
    }

    public static void main(String[] args) {
        Server server = new Server(message -> {
            System.out.println("Server callback: " + message);
        });
    }
}
