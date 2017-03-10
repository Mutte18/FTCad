package DCAD;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class FEClientConnection implements Runnable {
    private Socket mClientSocket;
    private ThreadSafeObjectStream mOut;
    private ObjectInputStream mIn;

    private String mHostname;
    private int mPortNumber;
    private volatile boolean mDisconnected = false;
    private volatile int mConTries = 0;
    private boolean isServer;
    private boolean isPrimary;
    private FE mFE;
    private boolean isConnected = true;

    public FEClientConnection(Socket clientSocket, FE fe) {
        mClientSocket = clientSocket;
        mFE = fe;

        try {
            mOut = new ThreadSafeObjectStream(new ObjectOutputStream(mClientSocket.getOutputStream()));
            mIn = new ObjectInputStream(mClientSocket.getInputStream());

        } catch (IOException e) {
            System.err.println("Error with Object Stream: " + e.getMessage());
        }
    }

    /*public void receiveClientMessage() {
        try {
            Object o = mIn.readObject();
            handleMessages(o);

        } catch (IOException | ClassNotFoundException e) {
        }
    }*/

    public synchronized void handleMessages(Object object) {
        if (object instanceof ClientConnectionMessage) {    //Nothing special is done if it is a client message
        } else if (object instanceof ServerConnectionMessage) {
            ServerConnectionMessage mSCM = (ServerConnectionMessage) object;
            mHostname = mClientSocket.getInetAddress().toString();
            mPortNumber = mSCM.getPort();
        } else if (object instanceof PingMessage) {
        }
        else if(object instanceof GetPrimaryMessage){
            FEClientConnection primaryServer = mFE.getPrimaryServer();
            try {
                mOut.writeObject(new PrimaryMsg(primaryServer == this ,primaryServer.getHostName(), primaryServer.getPortNumber()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean isServer() {
        return isServer;
    }


    public synchronized void sendRespondMsg(String primaryaddress, int primaryport, boolean isPrimary) {    //Responds to everyone who connects with the IP and port of the primaryServer
        try {
            //Also tells servers that they are backup
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

    public synchronized void openConnections() {
        try {
            Object input = mIn.readObject();
            System.out.println(input + "input");
            if (input instanceof ServerConnectionMessage) {

                ServerConnectionMessage serverConnectionMessage = (ServerConnectionMessage) input;
                mHostname = serverConnectionMessage.getHostname();
                mPortNumber = serverConnectionMessage.getPort();
                System.out.println(serverConnectionMessage.getWasPrimary() + "was primary msesage");
                mFE.addServer(this, serverConnectionMessage.getWasPrimary());
                mFE.waitForConnections();
                FEClientConnection primaryServer = mFE.getPrimaryServer();
                mOut.writeObject(new PrimaryMsg(primaryServer == this, primaryServer.getHostName(), primaryServer.getPortNumber()));
                System.out.println(primaryServer.getHostName() + " " + primaryServer.getPortNumber());
                isConnected = true;

            } else if (input instanceof ClientConnectionMessage) {
                FEClientConnection primaryServer = mFE.getPrimaryServer();
                mOut.writeObject(new PrimaryMsg(primaryServer == this, primaryServer.getHostName(), primaryServer.getPortNumber()));

                isConnected = false;
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {



        openConnections();
        if (isConnected) {
            PingClass pingClass = new PingClass(mOut);
            Thread pingingThread = new Thread(pingClass);
            pingingThread.start();
            while (isConnected) {
                try {
                    mClientSocket.setSoTimeout(5000);
                    Object o = mIn.readObject();        //Receive the message here, forward to server to handle it
                    handleMessages(o);
                } catch (IOException | ClassNotFoundException e) {
                    //mConTries++;
                    isConnected = false;
                }

            }

            try {
                pingClass.shutdown();
                pingingThread.interrupt();
                pingingThread.join();
                mFE.removeServer(this);
                mClientSocket.close();

            } catch (IOException e2) {
            } catch (InterruptedException e3) {
            }
        }

    }
}
