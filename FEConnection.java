package DCAD;
//FE st�r f�r Front-end

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;

public class FEConnection implements Runnable {

    private Socket mClientSocket = null;
    private String mHostName = null;

    private ThreadSafeObjectStream mOut = null;    //Object skickas, TCP
    private ObjectInputStream mIn = null;    //Object tas emot, TCP

    private int mServerPort = -1;
    private int mFEPort;
    private String mPrimaryAddress;
    private int mPrimaryPort;
    private boolean isPrimary;
    private int mConTries;
    private boolean mDisconnected;
    private volatile boolean mIsConnected;
    private Server mServer;


    public FEConnection(String hostName, int FEport, int serverPort, Server server) {
        mHostName = hostName;
        mFEPort = FEport;
        mServerPort = serverPort;
        mServer = server;

    }

    public void clientHandshake() {
        try {
            mClientSocket = new Socket(mHostName, mFEPort);
            mOut = new ThreadSafeObjectStream(new ObjectOutputStream(mClientSocket.getOutputStream()));
            mIn = new ObjectInputStream(mClientSocket.getInputStream());
        } catch (IOException e) {
        }
        sendClientConnectMessage();
        awaitPrimaryMessage();
    }

    public void serverHandshake() {
        try {
            mClientSocket = new Socket(mHostName, mFEPort);
            mOut = new ThreadSafeObjectStream(new ObjectOutputStream(mClientSocket.getOutputStream()));
            mIn = new ObjectInputStream(mClientSocket.getInputStream());
        } catch (IOException e) {
        }
        sendServerConnectMessage();
    }


    public void sendClientConnectMessage() {
        try {

            mOut.writeObject(new ClientConnectionMessage());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendServerConnectMessage() {
        try {
            System.out.println(isPrimary + " " + "Before sending serverIsPrimary");
            mOut.writeObject(new ServerConnectionMessage(mHostName, mServerPort, isPrimary));    //We want to store the address and port of the server in FE

        } catch (IOException e) {
            //e.printStackTrace();
        }

    }

    public void awaitPrimaryMessage() {

        try {
            Object object = mIn.readObject();

            if (object instanceof PingMessage) {

            } else if (object instanceof PrimaryMsg) {

                //PrimaryMsg primaryMsg = (PrimaryMsg) object;
                mPrimaryAddress = ((PrimaryMsg) object).getPrimaryAddress();
                mPrimaryPort = ((PrimaryMsg) object).getPrimaryPort();
                //isPrimary = ((PrimaryMsg) object).getPrimary();
                System.out.println(mPrimaryAddress + mPrimaryPort  + " Client " + object );
            }

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

    }

    public void listenToFrontEndMessages() {
        while (true) {
            try {
                Object object = mIn.readObject();

                if (object instanceof PingMessage) {
                } else if (object instanceof PrimaryMsg) {
                    PrimaryMsg primaryMsg = (PrimaryMsg) object;
                    mPrimaryAddress = primaryMsg.getPrimaryAddress();
                    mPrimaryPort = primaryMsg.getPrimaryPort();
                    isPrimary = primaryMsg.getPrimary();
                    mServer.setPrimary(isPrimary);
                    System.out.println("I am Primary" + " " + isPrimary);
                }


            } catch (IOException e) {
                return;
            } catch (ClassNotFoundException e) {
            }
        }
    }

    public String getPrimaryAddress() {
        return mPrimaryAddress;
    }

    public int getPrimaryPort() {
        return mPrimaryPort;
    }

    public boolean getPrimary() {
        return isPrimary;
    }

    public boolean getDisconnect() {
        return mDisconnected;
    }

    public void setDisconnect(boolean value) {
        mDisconnected = value;
    }

    public void sendGetPrimary(){
        try {
            mOut.writeObject(new GetPrimaryMessage());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while (true) {
            try {


                serverHandshake();
                mClientSocket.setSoTimeout(5000);
                PingClass pingClass = new PingClass(mOut);
                Thread pingingThread = new Thread(pingClass);
                pingingThread.start();

                listenToFrontEndMessages();

                pingClass.shutdown();
                pingingThread.interrupt();
                pingingThread.join();
                mClientSocket.close();

            } catch (IOException e) {
                System.err.println("Connection refused, trying again");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e1) {

                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }
}
