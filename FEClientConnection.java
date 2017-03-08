package DCAD;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class FEClientConnection implements Runnable {
    private Socket mClientSocket;
    private ObjectOutputStream mOut;
    private ObjectInputStream mIn;

    private String mHostname;
    private int mPortNumber;
    private volatile boolean mDisconnected = false;
    private volatile int mConTries = 0;
    private boolean isServer;
    private boolean isPrimary;

    public FEClientConnection(Socket clientSocket) {
        mClientSocket = clientSocket;

        try {
            mOut = new ObjectOutputStream(mClientSocket.getOutputStream());
            mIn = new ObjectInputStream(mClientSocket.getInputStream());
            receiveClientMessage();
        } catch (IOException e) {
            System.err.println("Error with Object Stream: " + e.getMessage());
        }
    }

    public void receiveClientMessage() {
        try {
            Object o = mIn.readObject();
            handleMessages(o);

        } catch (IOException | ClassNotFoundException e) {
        }
    }

    public synchronized void handleMessages(Object object) {
        System.out.println(object);
        if (object instanceof ClientConnectionMessage) {            //Nothing special is done if it is a client message
            isServer = false;
        } else if (object instanceof ServerConnectionMessage) {             //If a server tries to connect get the IP and port
            mHostname = ((ServerConnectionMessage) object).getHostname();
            mPortNumber = ((ServerConnectionMessage) object).getPort();
            isServer = true;    //Used for comparision in FE
        }
    }

    public boolean isServer() {
        return isServer;
    }


    public synchronized void sendRespondMsg(String primaryaddress, int primaryport, boolean isPrimary) {    //Responds to everyone who connects with the IP and port of the primaryServer
        try {                                                                                               //Also tells servers that they are backup
            mOut.writeObject(new PrimaryMsg(isPrimary, primaryaddress, primaryport));
        } catch (IOException e) {
            System.err.println("Could not write PrimaryMsg: " + e.getMessage());
        }
    }

    public String getHostName() {
        return mHostname;
    }

    public int getPortNumber() {
        return mPortNumber;
    }

    public synchronized int getConTries() {
        return mConTries;
    }

    public void setPrimary(boolean value) {
        isPrimary = value;
    }

    public boolean isPrimary() {
        return isPrimary;
    }

    @Override
    public void run() {
        boolean isConnected = true;

        while (isConnected && !mDisconnected) {

            try {
                mIn.readObject();
            } catch (IOException | ClassNotFoundException e) {
                mConTries++;
            }
            if (mConTries > 10) {
                isConnected = false;
            }
        }
        try {
            mClientSocket.close();
        } catch (IOException e) {
            System.err.println("Could not close ClientSocket: " + e.getMessage());
        }
    }
}
