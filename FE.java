package DCAD;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Vector;

public class FE {
    private ServerSocket mFESocket;
    private Socket mClientSocket;

    private String mPrimaryAddress;
    private int mPrimaryPort;
    private boolean noPrimary;
    private FEClientConnection mPrimaryServer;
    private long mTime;

    private Vector<FEClientConnection> mConnectedServers;

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java Server portnumber");
            System.exit(-1);
        }
        try {
            FE instance = new FE(Integer.parseInt(args[0]));
            instance.listenForClientHandshake();
        } catch (NumberFormatException e) {
            System.err.println("Error: port number must be an integer.");
            System.exit(-1);
        }
    }

    private FE(int portNumber) {
        try {
            mConnectedServers = new Vector<>();
            noPrimary = true;
            mFESocket = new ServerSocket(portNumber);
            mTime = System.currentTimeMillis();
            //Thread thread = new Thread(new ThreadRemoval());
            //thread.start();
        } catch (IOException e) {
            System.err.println("Could not bind Front End-Socket: " + e.getMessage());
        }
    }

    public void waitForConnections() {
        if (mTime + 3000 > System.currentTimeMillis()) {
            try {
                Thread.sleep(mTime + 3000 - System.currentTimeMillis());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void listenForClientHandshake() {
        System.out.println("Waiting for handshake...!");

        do {
            FEClientConnection feClientConnection = null;
            try {
                mClientSocket = mFESocket.accept();
                System.out.println("New connection");
                feClientConnection = new FEClientConnection(mClientSocket, this);

                Thread feClientThread = new Thread(feClientConnection);
                feClientThread.start();


            } catch (IOException e) {
                System.err.println("Error while accepting packet: " + e.getMessage());
            }
        } while (true);
    }

    public synchronized void addServer(FEClientConnection feClientConnection, boolean wasPrimary) {
        mConnectedServers.add(feClientConnection);
        if (mPrimaryServer == null && wasPrimary) {
            mPrimaryServer = feClientConnection;
            noPrimary = false;
        }
    }

    public void setNoPrimary(boolean value) {
        noPrimary = value;
    }

    public synchronized void removeServer(FEClientConnection feClientConnection) {
        if (feClientConnection.isPrimary()) {
            setNoPrimary(true);
            mConnectedServers.remove(feClientConnection);
            if(mConnectedServers.size() == 0){
                mPrimaryServer = null;
            }
            else{
                mPrimaryServer = mConnectedServers.firstElement();
            }

        }
        else{
            mConnectedServers.remove(feClientConnection);
        }

    }

    public FEClientConnection getPrimaryServer() {
        if (noPrimary) {        //If we don't have a primary server yet
            mPrimaryServer = mConnectedServers.firstElement();
            mPrimaryServer.setPrimary(true);
        }
        return mPrimaryServer;
    }

    /*class ThreadRemoval implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    System.out.println(noPrimary);
                    Thread.sleep(3000);
                    removeDisconnected();
                } catch (InterruptedException e) {
                    System.err.println("Failed to sleep thread: " + e.getMessage());
                }
            }
        }
    }
    private void removeDisconnected() {

        for (FEClientConnection mConnectedServer : mConnectedServers) {
            if (mConnectedServer.getConTries() > 10) {
                System.out.printf("A server has crashed");
                if (mConnectedServer.isPrimary()) {
                    System.out.println("PRIMARY DIED!!!!");
                    setNoPrimary(true);
                }
                mConnectedServers.remove(mConnectedServer);
                break;
            }
        }
    }*/
}
