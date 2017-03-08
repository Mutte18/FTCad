package DCAD;

import java.net.InetAddress;

public class Client {
	static private GUI 		 mGui;
	private FEConnection 	 mFEConnection = null; // Kopplingen till servern
	private ServerConnection mServerConnection;
	private String mPrimaryAddress;
	private int mPrimaryPort;

	private String mFEhostName;
	private int mFEport;
	private int mServerPort;

	
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

	private Client() {

	}
	
    private void connectToFE(String hostName, int FEport, int serverPort) {
		mFEhostName = hostName;
		mFEport = FEport;
		mServerPort = serverPort;

        mFEConnection = new FEConnection(mFEhostName, mFEport, mServerPort); //Create a new FEconnection
        
        if (mFEConnection.clientHandshake()) {
			mPrimaryAddress = mFEConnection.getPrimaryAddress();
			mPrimaryPort = mFEConnection.getPrimaryPort();
			System.out.println(mPrimaryAddress + "Ska vara serverns?");
			System.out.println(mPrimaryPort + "Ska vara serverns?");

        	//Beh�ver kopplas till servern via ServerConnection
        	mServerConnection = new ServerConnection(mPrimaryAddress, mPrimaryPort);
        	
        	if(mServerConnection.handshake()){
				Thread serverConThread = new Thread(mServerConnection);
				serverConThread.start();
        		listenForServerMessages();
        	}
        } else { System.err.println("Unable to connect to server"); }
    }
    
    private void listenForServerMessages(){
		mGui = new GUI(750, 600, mServerConnection);
		mGui.addToListener();
		do{
			mGui.updateObjectList(mServerConnection.receivePaintings());
			if(mServerConnection.getDisconnect()){
                connectToFE(mFEhostName, mFEport, mServerPort);
                break;
            }
		} while(true);
    }


}
/*class ThreadRemoval implements Runnable {
	@Override
	//Denna metod tar bort bortkopplade klienter var femte sekund
	public void run() {
		while (true) {
			try {
				Thread.sleep(5000);
				tryToConnect();
			} catch (InterruptedException e) {
				System.err.println("Failed to sleep thread: " + e.getMessage());
			}
		}
	}
}*/
