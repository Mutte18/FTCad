package DCAD;

import java.net.InetAddress;
import java.util.ArrayList;

public class Client implements ListReceiver{
    private ServerConnection mServerConnection;

    private String mFEhostName;
    private int mFEport;
    private GUI mGUI;


    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: java Client hostname FEportnumber username serverportnumber");
            System.exit(-1);
        }
        try {
            Client instance = new Client();
            instance.connectToFE(args[0], Integer.parseInt(args[1]));
        } catch (NumberFormatException e) {
            System.err.println("Error: port number must be an integer.");
            System.exit(-1);
        }
    }

    private Client() {
        mGUI = new GUI(750, 600);            //Fix so that it doesn't create a new GUI Window everytime it starts a serverConnection
        mGUI.addToListener();
    }

    private void connectToFE(String hostName, int FEport) {
        while(true) {
            mFEhostName = hostName;
            mFEport = FEport;

            FEConnection mFEConnection = new FEConnection(mFEhostName, mFEport, 0, null);    //Client doesn't need to send a serverPort

            mFEConnection.clientHandshake();
            String mPrimaryAddress = mFEConnection.getPrimaryAddress();
            int mPrimaryPort = mFEConnection.getPrimaryPort();


            mServerConnection = new ServerConnection(mPrimaryAddress, mPrimaryPort, this);
            System.out.println(mPrimaryAddress + " " + mPrimaryPort);
            mGUI.setServerConnection(mServerConnection);

            mServerConnection.run();
        }

    }


    private void listenForServerMessages() {                            //TODO

        do {

            if (mServerConnection.getDisconnect()) {                            //If the client lost connection with the primary server, try to access the new one through FE
                try {
                    Thread.sleep(1000); //Sleep one second so that the client doesnt try to connect ahead of servers
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("We reconnected, boys!!");
                connectToFE(mFEhostName, mFEport);

                break;
            }
        } while (true);
    }


    @Override
    public void receive(ArrayList arrayList) {
        mGUI.updateObjectList(arrayList);
    }


}