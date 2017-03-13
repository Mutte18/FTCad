package DCAD.Client;

import DCAD.FrontEnd.FEConnection;
import DCAD.General.ListReceiver;
import DCAD.ServerAndClient.ServerConnection;

import java.util.ArrayList;

public class Client implements ListReceiver {
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
        mGUI = new GUI(750, 600);
        mGUI.addToListener();
    }

    private void connectToFE(String hostName, int FEport) {
        while(true) {
            mFEhostName = hostName;
            mFEport = FEport;

            FEConnection mFEConnection = new FEConnection(mFEhostName, mFEport, 0, null);

            mFEConnection.clientHandshake();
            String mPrimaryAddress = mFEConnection.getPrimaryAddress();
            int mPrimaryPort = mFEConnection.getPrimaryPort();


            mServerConnection = new ServerConnection(mPrimaryAddress, mPrimaryPort, this);
            System.out.println("Connected to Primary server: " + mPrimaryAddress + " " + mPrimaryPort);
            mGUI.setServerConnection(mServerConnection);

            mServerConnection.run();
        }

    }

    @Override
    public void receive(ArrayList arrayList) {
        mGUI.updateObjectList(arrayList);
    }


}