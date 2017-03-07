package DCAD;

import java.net.InetAddress;

public class Client {
	static private GUI 		 mGui;
	private FEConnection 	 mFEConnection = null; // Kopplingen till servern
	private ServerConnection mServerConnection;
	private String mPrimaryAddress;
	private int mPrimaryPort;

	
	public static void main(String[] args) {
		if (args.length < 3) {
			System.err.println("Usage: java Client hostname FEportnumber username serverportnumber");
			System.exit(-1);
		}
		try {
			Client instance = new Client();
			instance.connectToFE(args[0], Integer.parseInt(args[1]), Integer.parseInt(args[2]));
		} catch (NumberFormatException e) {
			System.err.println("Error: port number must be an integer.");
			System.exit(-1);
		}
	}

	private Client() {}
	
    private void connectToFE(String hostName, int FEport, int serverPort) {
        mFEConnection = new FEConnection(hostName, FEport, serverPort); //Create a new FEconnection
        
        if (mFEConnection.handshake()) {
			mPrimaryAddress = mFEConnection.getPrimaryAddress();
			mPrimaryPort = mFEConnection.getPrimaryPort();
			System.out.println(mPrimaryAddress + "Ska vara serverns?");
			System.out.println(mPrimaryPort + "Ska vara serverns?");

        	//Beh�ver kopplas till servern via ServerConnection
        	mServerConnection = new ServerConnection(mPrimaryAddress, mPrimaryPort);
        	
        	if(mServerConnection.handshake()){
        		listenForServerMessages();
        	}
        } else { System.err.println("Unable to connect to server"); }
    }
    
    private void listenForServerMessages(){
		mGui = new GUI(750, 600, mServerConnection);
        mGui.addToListener();		
		do{
			mGui.updateObjectList(mServerConnection.receivePaintings());
		} while(true);
    }    
}
