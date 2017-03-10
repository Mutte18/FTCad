package DCAD;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Random;
import java.util.UUID;

public class ClientConnection implements Runnable {

    private Server mServer;
    private Socket mClientSocket;

    private ThreadSafeObjectStream mOut;
    private ObjectInputStream mIn;

    private volatile int mConTries = 0;
    private volatile boolean mDisconnected;
    private PingClass mPingClass;
    private Thread mPingThread;

    public ClientConnection(Socket clientSocket, Server server) {
        mServer = server;
        mClientSocket = clientSocket;
        mDisconnected = false;

        try {
            mOut = new ThreadSafeObjectStream(new ObjectOutputStream(mClientSocket.getOutputStream()));
            mIn = new ObjectInputStream(mClientSocket.getInputStream());
        } catch (IOException e) {
            System.err.println("Error with Object Stream: " + e.getMessage());
        }
    }

    public synchronized void sendPaintings(ArrayList gobjects) {
        try {
            ArrayList<GObject> list = new ArrayList<GObject>(gobjects);
            mOut.writeObject(list);
        } catch (IOException e) {
        }
    }

    public void sendDisconnectMessage() {
        try {
            System.out.println("INnan skickat delsmg");
            mOut.writeObject(new DelMsg());
            System.out.println("Efter skickat delsmg");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized int getConTries() {
        return mConTries;
    }

    public boolean getDisconnect() {
        return mDisconnected;
    }

    public void setDisconnect(boolean value) {
        mDisconnected = value;
    }

    public synchronized void terminateClient() {
        mPingClass.shutdown();
        mPingThread.interrupt();
        try {
            mPingThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mServer.removeClient(this);
        try {
            mClientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void run() {
        boolean isConnected = true;
        mPingClass = new PingClass(mOut);
        mPingThread = new Thread(mPingClass);
        mPingThread.start();

        while (isConnected && !mDisconnected) {
            try {
                mClientSocket.setSoTimeout(5000);
                Object o = mIn.readObject();        //Receive the message here, forward to server to handle it
                mServer.handlePaintings(o);

            } catch (IOException | ClassNotFoundException e) {
                isConnected = false;
            }

        }

        terminateClient();


    }
}
