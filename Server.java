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

public class Server implements ListReceiver {

    private volatile ArrayList mGObjects = new ArrayList<>();    //Stores all paintings
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
    private volatile boolean isPrimary;

    public static void main(String[] args) {
        if (args.length < 3) {
            System.err.println("Usage: FE hostName, FE port, Serverport");
            System.exit(-1);
        }
        try {
            instance = new Server(Integer.parseInt(args[2]));
            instance.initializeFE(args[0], Integer.parseInt(args[1]));
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

        while (isPrimary) {
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

                /*Thread thread = new Thread(new ThreadRemoval());
                thread.start();*/
            }
            System.out.println(mConnectedClients.size());
        }
        System.out.println("Slutade vara primary!");
        disconnectClients();
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

    private void initializeFE(String hostName, int FEport){
        while(true){
            connectToFE(hostName, FEport);
        }
    }

    private void connectToFE(String hostName, int FEport) {
        mFEHostName = hostName;
        mFEport = FEport;
        System.out.println("Connecting to FrontEnd");
        mFEConnection = new FEConnection(mFEHostName, mFEport, mServerport, this);
        Thread feThread = new Thread(mFEConnection);
        feThread.start();
        boolean isConnected = true;

        while (true) {

            if (mFEConnection.getPrimaryAddress() != null) {
                String mPrimaryAddress = mFEConnection.getPrimaryAddress();
                int mPrimaryPort = mFEConnection.getPrimaryPort();
                isPrimary = mFEConnection.getPrimary();
                System.out.println("Am I primary? " + mFEConnection.getPrimary());

                if (isPrimary) {        //Acts like a server incase it is primary
                    listenForClientMessages();
                } else {    //Connects to the primary server as a client if the server is a backup
                    disconnectClients();
                    mServerConnection = new ServerConnection(mPrimaryAddress, mPrimaryPort, this);
                    mServerConnection.run();
                    System.out.println("Lost connection");
                    mFEConnection.sendGetPrimary();

                }
            }
            try {
                Thread.sleep(500);


            } catch (InterruptedException e) {
                e.printStackTrace();

            }
        }
    }

    public void setPrimary(boolean value){
        if(isPrimary && !value){
        }
        isPrimary = value;
    }

    private void disconnectClients() {       //Disconnect all clients when FE goes down
        //mToDiconnect = mConnectedClients;
        if (mConnectedClients.size() > 0) {
            for (ClientConnection mConnectedClient : mConnectedClients) {
                mConnectedClient.sendDisconnectMessage();
                mConnectedClient.terminateClient();
                //mConnectedClient.setDisconnect(true);
                //mConnectedClients.remove(mConnectedClient);
            }
            mConnectedClients.clear();
            System.out.println("Clear kördes");
        }
        System.out.println(mConnectedClients.size());

    }

    /*private void listenForServerMessages() {
        while (!isPrimary) {
            mGObjects = mServerConnection.receivePaintings();
            if (mServerConnection.getDisconnect()) {        //If the primary server is down, try to reconnect to FE to become primary
                System.out.println("bServer gick fröbi");
                connectToFE(mFEHostName, mFEport);
                break;
            }
        }
    }*/

    /*class ThreadRemoval implements Runnable {       //Thread to remove disconnected clients from prim server
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
    }*/

    private void pingFE() {
        if (mFEConnection.getDisconnect()) {        //If the primary server is down, try to reconnect to FE to become primary

            connectToFE(mFEHostName, mFEport);
        }
    }

    @Override
    public void receive(ArrayList arrayList) {
        mGObjects = arrayList;
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

    public synchronized void removeClient(ClientConnection clientConnection) {
        mConnectedClients.remove(clientConnection);
    }

   /* private void removeDisconnected() {
        for (ClientConnection mConnectedClient : mConnectedClients) {
            if (mConnectedClient.getConTries() > 10) {
                System.out.printf("A client has crashed");
                mConnectedClients.remove(mConnectedClient);
                System.out.println(mConnectedClients.size());
                break;
            }
        }
    }*/

    public boolean addClient(ClientConnection clientConnection) {
        mConnectedClients.add(clientConnection);
        return true;
    }
}
