package DCAD.FrontEnd;

import DCAD.Client.ClientConnectionMessage;
import DCAD.General.PingClass;
import DCAD.General.PingMessage;
import DCAD.General.PrimaryMsg;
import DCAD.General.ThreadSafeObjectStream;
import DCAD.Server.GetPrimaryMessage;
import DCAD.Server.ServerConnectionMessage;

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

    public synchronized void handleMessages(Object object) {
        if (object instanceof ClientConnectionMessage) {    //Nothing special is done if it is a client message
        }
        else if (object instanceof ServerConnectionMessage) {
            ServerConnectionMessage mSCM = (ServerConnectionMessage) object;
            mHostname = mClientSocket.getInetAddress().toString();
            mPortNumber = mSCM.getPort();
        }
        else if (object instanceof PingMessage) {
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

    public String getHostName() {
        return mHostname;
    }

    public int getPortNumber() {
        return mPortNumber;
    }

    /*public void setPrimary(boolean value) {
        isPrimary = value;
    }

    public boolean isPrimary() {
        return isPrimary;
    }*/

    public synchronized void openConnections() {
        try {
            Object input = mIn.readObject();
            if (input instanceof ServerConnectionMessage) {

                ServerConnectionMessage serverConnectionMessage = (ServerConnectionMessage) input;
                mHostname = serverConnectionMessage.getHostname();
                mPortNumber = serverConnectionMessage.getPort();
                mFE.addServer(this, serverConnectionMessage.getWasPrimary());
                mFE.waitForConnections();
                FEClientConnection primaryServer = mFE.getPrimaryServer();
                mOut.writeObject(new PrimaryMsg(primaryServer == this, primaryServer.getHostName(), primaryServer.getPortNumber()));
                System.out.println("The new Primary server is: " + primaryServer.getHostName() + " " + primaryServer.getPortNumber());
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
                    Object o = mIn.readObject();
                    handleMessages(o);
                } catch (IOException | ClassNotFoundException e) {
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
