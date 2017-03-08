package DCAD;

import java.net.InetAddress;

public class Client {
	private ServerConnection mServerConnection;

	private String mFEhostName;
	private int mFEport;

	
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
	}
	
    private void connectToFE(String hostName, int FEport) {
		mFEhostName = hostName;
		mFEport = FEport;

		FEConnection mFEConnection = new FEConnection(mFEhostName, mFEport, 0);	//Client doesn't need to send a serverPort
        
        if (mFEConnection.clientHandshake()) {
			String mPrimaryAddress = mFEConnection.getPrimaryAddress();
			int mPrimaryPort = mFEConnection.getPrimaryPort();

        	mServerConnection = new ServerConnection(mPrimaryAddress, mPrimaryPort);
        	
        	if(mServerConnection.handshake()){
				Thread serverConThread = new Thread(mServerConnection);		//Starts a thread so that the client can ping the server
				serverConThread.start();
        		listenForServerMessages();
        	}
        } else { System.err.println("Unable to connect to server"); }
    }
    
    private void listenForServerMessages(){							//TODO
		GUI mGui = new GUI(750, 600, mServerConnection);			//Fix so that it doesn't create a new GUI Window everytime it starts a serverConnection
		mGui.addToListener();
		do{
			mGui.updateObjectList(mServerConnection.receivePaintings());
			if(mServerConnection.getDisconnect()){							//If the client lost connection with the primary server, try to access the new one through FE
                connectToFE(mFEhostName, mFEport);
                break;
            }
		} while(true);
    }


}