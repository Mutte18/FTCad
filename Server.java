package DCAD;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.UUID;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

public class Server{

    private volatile ArrayList<GObject> mGObjects = new ArrayList<>();    //Stores all paintings
    private Vector<ClientConnection> mConnectedClients = new Vector<>(); //Stores clientConnections
    private Vector<ClientConnection> mToDiconnect;

    private ServerSocket mServerSocket;
    private Socket mClientSocket;

    private String mFEHostName;
    private int mFEport;
    private int mServerport;
    private static Server instance;
    private ServerConnection mServerConnection;
    private FEConnection mFEConnection;
    private boolean isPrimary;

    public static void main(String[] args) {
        if (args.length < 3) {
            System.err.println("Usage: FE hostName, FE port, Serverport");
            System.exit(-1);
        }
        try {
            instance = new Server(Integer.parseInt(args[2]));
            instance.connectToFE(args[0], Integer.parseInt(args[1]));
        } catch (NumberFormatException e) {
            System.err.println("Error: port number must be an integer.");
            System.exit(-1);
        }
    }

    private Server(int portNumber) {
        try {
            mServerSocket = new ServerSocket(portNumber);
            mServerport = portNumber;



        } catch (IOException e) {
            System.err.println("Could not bind ServerSocket: " + e.getMessage());
        }
    }

    private void listenForClientMessages() {
        System.out.println("Waiting for client messages... ");
        disconnectClients();
        while(isPrimary) {
            ClientConnection clientConnection;

            try {
                mClientSocket = mServerSocket.accept();
            } catch (IOException e) {
                System.err.println("Error while accepting packet: " + e.getMessage());
            }

            clientConnection = new ClientConnection(mClientSocket, this);

            if (addClient(clientConnection)) {
                Thread clientThread = new Thread(clientConnection);
                clientThread.start();

                Thread thread = new Thread(new ThreadRemoval());
                thread.start();
            }
            System.out.println(mConnectedClients.size());
        }
    }




    public synchronized void handlePaintings(Object object) {
        if (object instanceof GObject) {
            mGObjects.add((GObject) object);
        } else if (object instanceof DelMsg) {
            mGObjects.remove(mGObjects.size() - 1);
        } else if (object instanceof PingMessage) {
        }
        broadcast();
    }

    public void broadcast() {
        for (Iterator<ClientConnection> itr = mConnectedClients.iterator(); itr.hasNext(); ) {
            itr.next().sendPaintings(mGObjects);
        }
    }

    private void connectToFE(String hostName, int FEport) {
        mFEHostName = hostName;
        mFEport = FEport;
        mFEConnection = new FEConnection(mFEHostName, mFEport, mServerport);

        Thread feThread = new Thread(new FEConnectionTries());
        feThread.start();
        if (mFEConnection.serverHandshake()) {
            String mPrimaryAddress = mFEConnection.getPrimaryAddress();
            int mPrimaryPort = mFEConnection.getPrimaryPort();
            isPrimary = mFEConnection.getPrimary();
            System.out.println("Am I primary? " + mFEConnection.getPrimary());


            disconnectClients();


            Thread FEpingThread = new Thread(mFEConnection);        //Starts a thread so that the clientServer can ping the primary server
            FEpingThread.start();

            if (isPrimary) {        //Acts like a server incase it is primary
                instance.listenForClientMessages();
            }
            else {    //Connects to the primary server as a client if the server is a backup
                mServerConnection = new ServerConnection(mPrimaryAddress, mPrimaryPort);
                if (mServerConnection.handshake()) {
                    Thread serverConThread = new Thread(mServerConnection);        //Starts a thread so that the clientServer can ping the primary server
                    serverConThread.start();
                    listenForServerMessages();
                }
            }
        }
    }

    private void disconnectClients(){       //Disconnect all clients when FE goes down
        //mToDiconnect = mConnectedClients;
        if(mConnectedClients.size() > 0) {
            for (ClientConnection mConnectedClient : mConnectedClients) {
                mConnectedClient.sendDisconnectMessage();
                mConnectedClient.setDisconnect(true);
                //mConnectedClients.remove(mConnectedClient);
            }
            mConnectedClients.clear();
            System.out.println("Clear kördes");
        }
        System.out.println(mConnectedClients.size());

    }

    private void listenForServerMessages() {
        while (!isPrimary){
            mGObjects = mServerConnection.receivePaintings();
            if (mServerConnection.getDisconnect()) {        //If the primary server is down, try to reconnect to FE to become primary
                System.out.println("bServer gick fröbi");
                connectToFE(mFEHostName, mFEport);
                break;
            }
        }
    }

    class ThreadRemoval implements Runnable {       //Thread to remove disconnected clients from prim server
        @Override
        public void run() {
            while (true) {
                try {
                    Thread.sleep(5000);
                    removeDisconnected();
                } catch (InterruptedException e) {
                    System.err.println("Failed to sleep thread: " + e.getMessage());
                }
            }
        }
    }

    private void pingFE() {
        if (mFEConnection.getDisconnect()) {        //If the primary server is down, try to reconnect to FE to become primary
            isPrimary = false;
            connectToFE(mFEHostName, mFEport);
        }
    }

    class FEConnectionTries implements Runnable {
        @Override
        public void run() {
            while (true) {

                pingFE();
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }


            }
        }
    }

    private void removeDisconnected() {
        for (ClientConnection mConnectedClient : mConnectedClients) {
            if (mConnectedClient.getConTries() > 10) {
                System.out.printf("A client has crashed");
                mConnectedClients.remove(mConnectedClient);
                System.out.println(mConnectedClients.size());
                break;
            }
        }
    }

    public boolean addClient(ClientConnection clientConnection) {
        mConnectedClients.add(clientConnection);
        return true;
    }
}
