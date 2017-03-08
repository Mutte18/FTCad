package DCAD;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.UUID;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

public class Server implements Serializable{
	
	private volatile ArrayList<GObject> mGObjects = new ArrayList<>(); 	//Stores all paintings
	private volatile Vector<ClientConnection> mConnectedClients = new Vector<>(); //Stores clientConnections

	private ServerSocket mServerSocket;
	private Socket mClientSocket;

	private String mFEHostName;
	private int mFEport;
	private int mServerport;
	private static Server instance;
	private ServerConnection mServerConnection;

	public static void main(String[] args) {
		if (args.length < 3) {
			System.err.println("Usage: FE hostName, FE port, Serverport");
			System.exit(-1);
		}
		try {			
			instance = new Server(Integer.parseInt(args[2]));
			instance.connectToFE(args[0], Integer.parseInt(args[1]));
		} catch (NumberFormatException e) {
			System.err.println("Error: port number must be an integer.");
			System.exit(-1);
		}
	}

	private Server(int portNumber) {
		try {
			mServerSocket = new ServerSocket(portNumber);
			mServerport = portNumber;
			Thread thread = new Thread(new ThreadRemoval());
			thread.start();

		} catch (IOException e) {
			System.err.println("Could not bind ServerSocket: " + e.getMessage());
		}
	}
	
	private void listenForClientMessages(){
		System.out.println("Waiting for client messages... ");
		
		do{
			ClientConnection clientConnection;
			
			try{
				mClientSocket = mServerSocket.accept();
			}catch (IOException e) {
                System.err.println("Error while accepting packet: " + e.getMessage());
            }
			
			clientConnection = new ClientConnection(mClientSocket, this);
			
			if(addClient(clientConnection)){
				Thread clientThread = new Thread(clientConnection);
				clientThread.start();
			}
		} while(true);
	}
	
	public synchronized void handlePaintings(Object object){
		if(object instanceof GObject){
			mGObjects.add((GObject) object);
		}
		else if(object instanceof DelMsg){
			mGObjects.remove(mGObjects.size()-1);
		}
		else if(object instanceof PingMessage){
		}
		broadcast();
	}
	
	public void broadcast(){
		for(Iterator<ClientConnection> itr = mConnectedClients.iterator(); itr.hasNext();){
			itr.next().sendPaintings(mGObjects);
		}
	}

	private void connectToFE(String hostName, int FEport) {
        mFEHostName = hostName;
        mFEport = FEport;
		FEConnection mFEConnection = new FEConnection(mFEHostName, mFEport, mServerport);

		if(mFEConnection.serverHandshake()){
			String mPrimaryAddress = mFEConnection.getPrimaryAddress();
			int mPrimaryPort = mFEConnection.getPrimaryPort();
			System.out.println("Am I primary? " + mFEConnection.getPrimary());

			if(mFEConnection.getPrimary()){		//Acts like a server incase it is primary
				instance.listenForClientMessages();
			}
			else{	//Connects to the primary server as a client if the server is a backup
				mServerConnection = new ServerConnection(mPrimaryAddress, mPrimaryPort);
				if(mServerConnection.handshake()){
					Thread serverConThread = new Thread(mServerConnection);		//Starts a thread so that the clientServer can ping the primary server
					serverConThread.start();
					listenForServerMessages();
				}
			}
		}
	}

	private void listenForServerMessages(){
		do {
			mGObjects = mServerConnection.receivePaintings();
			if (mServerConnection.getDisconnect()) {		//If the primary server is down, try to reconnect to FE to become primary
				connectToFE(mFEHostName, mFEport);
				break;
			}
		} while(true);
	}

	class ThreadRemoval implements Runnable {
		@Override
		public void run() {
			while (true) {
				try {
					Thread.sleep(5000);
					removeDisconnected();
				} catch (InterruptedException e) {
					System.err.println("Failed to sleep thread: " + e.getMessage());
				}
			}
		}
	}

	private void removeDisconnected() {
		for (ClientConnection mConnectedClient : mConnectedClients) {
			boolean wishesToLeave = mConnectedClient.getDisconnect();
			if (mConnectedClient.getConTries() > 10 || wishesToLeave) {
				System.out.printf("A client has crashed");
				mConnectedClients.remove(mConnectedClient);
				break;
			}
		}
	}

    public boolean addClient(ClientConnection clientConnection) {
    	mConnectedClients.add(clientConnection);
        return true;
    }
}
