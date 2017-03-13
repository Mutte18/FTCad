package DCAD.ServerAndClient;

import DCAD.*;
import DCAD.Client.ClientConnectionMessage;
import DCAD.General.ListReceiver;
import DCAD.General.PingClass;
import DCAD.General.ThreadSafeObjectStream;
import DCAD.Server.DelMsg;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;

public class ServerConnection {
    private ArrayList<GObject> mGObjects;
    private Socket mClientSocket = null;
    private String mServerName = null;

    private ThreadSafeObjectStream mOut = null;
    private ObjectInputStream mIn = null;

    private volatile boolean mIsConnected;
    private int mServerPort = -1;
    private int mConTries;
    private boolean mDisconnected;
    private ListReceiver mListReceiver;

    public ServerConnection(String serverName, int serverPort, ListReceiver listReceiver) {
        mServerName = serverName;
        mServerPort = serverPort;
        mListReceiver = listReceiver;


    }

    public boolean handshake() {
        // Kopplar mOut till klientens socket
        try {
            mOut = new ThreadSafeObjectStream(new ObjectOutputStream(mClientSocket.getOutputStream()));
            mIn = new ObjectInputStream(mClientSocket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            mOut.writeObject(new ClientConnectionMessage());
        } catch (IOException e) {
            e.printStackTrace();
        }
        mIsConnected = true;
        return true;
    }

    public ArrayList receivePaintings() {
        if (mIsConnected) {
            try {
                mClientSocket.setSoTimeout(5000);
                Object o = mIn.readObject();        //Receive the message here, forward to server to handle it
                handleMessage(o);

            } catch (ClassNotFoundException | IOException e) {
                mIsConnected = false;
                //System.err.println("Error reading Vector using ObjectInputStream: " + e.getMessage());
            }
        }
        return mGObjects;
    }

    public void handleMessage(Object object) {
        if (object instanceof ArrayList) {
            mGObjects = (ArrayList) object;
        } else if (object instanceof DelMsg) {
            System.out.println("tog emot delsmg");
            mDisconnected = true;
        }
    }

    public synchronized void sendGObject(Object object) {    //Sends the painting to the server
        try {
            mOut.writeObject(object);
        } catch (IOException e) {
            System.err.println("Error creating GObject with ObjectOutputStream: " + e.getMessage());
        }
    }

    private void listenForServerMessages() {
        System.out.println("Listening for server messages..");

        while (mIsConnected) {
            mGObjects = receivePaintings();
            mListReceiver.receive(mGObjects);
        }
    }

    public void run() {
        try {
            mClientSocket = new Socket(mServerName, mServerPort);
        } catch (IOException e) {
            e.printStackTrace();
        }
        handshake();
        PingClass pingClass = new PingClass(mOut);
        Thread pingingThread = new Thread(pingClass);
        pingingThread.start();


        listenForServerMessages();
        try {
            pingClass.shutdown();
            pingingThread.interrupt();
            pingingThread.join();
            mClientSocket.close();
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }
}
