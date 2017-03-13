package DCAD.ServerAndClient;

import DCAD.Server.DelMsg;
import DCAD.Client.GObject;
import DCAD.General.PingClass;
import DCAD.Server.Server;
import DCAD.General.ThreadSafeObjectStream;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;

public class ClientConnection implements Runnable {

    private Server mServer;
    private Socket mClientSocket;

    private ThreadSafeObjectStream mOut;
    private ObjectInputStream mIn;


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
            mOut.writeObject(new DelMsg());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void terminateClient() {        //Shuts down the client thread, removes the client from the server list and closes the socket.
        mPingClass.shutdown();
        mPingThread.interrupt();
        try {
            mPingThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("A client has crashed!");
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
                Object o = mIn.readObject();        //Gives the client 5 seconds to try to receive a ping message, otherwise connection will fail
                mServer.handlePaintings(o);

            } catch (IOException | ClassNotFoundException e) {
                isConnected = false;
            }

        }
        terminateClient();


    }
}
