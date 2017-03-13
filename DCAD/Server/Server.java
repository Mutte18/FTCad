package DCAD.Server;

import DCAD.Client.GObject;
import DCAD.FrontEnd.FEConnection;
import DCAD.General.ListReceiver;
import DCAD.General.PingMessage;
import DCAD.ServerAndClient.ClientConnection;
import DCAD.ServerAndClient.ServerConnection;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;

public class Server implements ListReceiver {

    private volatile ArrayList mGObjects = new ArrayList<>();    //Stores all paintings
    private Vector<ClientConnection> mConnectedClients = new Vector<>(); //Stores clientConnections

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
            }
            System.out.println(mClientSocket.getInetAddress().toString());
            System.out.println("Number of connected clients: " + mConnectedClients.size());
        }
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
        System.out.println("Initializing connection to FE");
        mFEConnection = new FEConnection(mFEHostName, mFEport, mServerport, this);
        Thread feThread = new Thread(mFEConnection);
        feThread.start();

        while (true) {

            if (mFEConnection.getPrimaryAddress() != null) {
                String mPrimaryAddress = mFEConnection.getPrimaryAddress();
                int mPrimaryPort = mFEConnection.getPrimaryPort();
                isPrimary = mFEConnection.getPrimary();

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
        if(isPrimary && !value){            //If the
        }
        isPrimary = value;
    }

    private void disconnectClients() {       //Disconnect all clients when FE goes down
        if (mConnectedClients.size() > 0) {
            for (ClientConnection mConnectedClient : mConnectedClients) {
                mConnectedClient.sendDisconnectMessage();
                mConnectedClient.terminateClient();
            }
            mConnectedClients.clear();
        }
        System.out.println(mConnectedClients.size());

    }


    @Override
    public void receive(ArrayList arrayList) {
        mGObjects = arrayList;
    }


    public synchronized void removeClient(ClientConnection clientConnection) {
        mConnectedClients.remove(clientConnection);
    }

    public boolean addClient(ClientConnection clientConnection) {
        mConnectedClients.add(clientConnection);
        return true;
    }
}
