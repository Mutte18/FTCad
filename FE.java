package DCAD;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class FE {
    private ServerSocket mFESocket;
    private Socket mClientSocket;

    private String mPrimaryAddress;
    private int mPrimaryPort;
    private boolean noPrimary;

    private ArrayList<FEClientConnection> mConnectedServers;

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
            mConnectedServers = new ArrayList<>();
            noPrimary = true;
            mFESocket = new ServerSocket(portNumber);
            Thread thread = new Thread(new ThreadRemoval());
            thread.start();
        } catch (IOException e) {
            System.err.println("Could not bind Front End-Socket: " + e.getMessage());
        }
    }

    private void listenForClientHandshake() {
        System.out.println("Waiting for handshake...!");

        do {
            FEClientConnection feClientConnection = null;

            try {
                mClientSocket = mFESocket.accept();
                feClientConnection = new FEClientConnection(mClientSocket);
                if (addServer(feClientConnection)) {
                    Thread feClientThread = new Thread(feClientConnection);
                    feClientThread.start();
                    if (noPrimary) {        //If we don't have a primary server yet


                        mPrimaryAddress = feClientConnection.getHostName();
                        mPrimaryPort = feClientConnection.getPortNumber();
                        feClientConnection.setPrimary(true);
                    }
                }
            } catch (IOException e) {
                System.err.println("Error while accepting packet: " + e.getMessage());
            }

            feClientConnection.sendRespondMsg(mPrimaryAddress, mPrimaryPort, noPrimary);
            noPrimary = false;  //Sets it to false so that other servers will be backup
        } while (true);
    }

    private boolean addServer(FEClientConnection feClientConnection) {
        if (feClientConnection.isServer()) {
            mConnectedServers.add(feClientConnection);
            return true;
        } else {
            return false;
        }
    }

    public void setNoPrimary(boolean value) {
        noPrimary = value;
    }

    class ThreadRemoval implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    Thread.sleep(4000);
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
    }
}
