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
	
	private volatile ArrayList<GObject> mGObjects = new ArrayList<>(); 	//Denna lagrar ritade objekt
	private volatile Vector<ClientConnection> mConnectedClients = new Vector<>(); //Lagrar uppkopplade klienter
	private FEConnection mFEConnection = null; 

	private ServerSocket mServerSocket;
	private Socket mClientSocket;
	
	private ObjectOutputStream mOut = null;
	private ObjectInputStream mIn = null;
	
	private String mPrimaryAddress;
    private String mFEHostName;
	private int mPrimaryPort;
	private int mFEport;
	private int mServerport;
	private boolean isPrimary;
	static Server instance;
	private ServerConnection mServerConnection;

	public static void main(String[] args) {
		if (args.length < 3) {
			System.err.println("Usage: FE hostName, FE port, Serverport");
			System.exit(-1);
		}
		try {			
			instance = new Server(Integer.parseInt(args[2]));
			instance.connectToFE(args[0], Integer.parseInt(args[1]));
			//String hostName, int FEport, int serverPort



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
				//Vad ska h�nda h�r?
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
			System.out.println(mGObjects.size() + "Before removing");

			mGObjects.remove(mGObjects.size()-1);
			System.out.println("Removed a thing");
		}
		else if(object instanceof PingMessage){
			System.out.println("FICK ETT PINGMEDDELANDE!");
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
		//N�r en ny server skapas ska den g� igenom FE
		mFEConnection = new FEConnection(mFEHostName, FEport, mServerport); //Create a new FEconnection
		System.out.println(mFEConnection);
		if(mFEConnection.serverHandshake(this)){
            System.out.println("Detta gick igenom FE");
            mPrimaryAddress = mFEConnection.getPrimaryAddress();
			mPrimaryPort = mFEConnection.getPrimaryPort();
            System.out.println(mFEConnection.getPrimary());

			if(mFEConnection.getPrimary()){
				instance.listenForClientMessages();
			}
			else{
				mServerConnection = new ServerConnection(mPrimaryAddress, mPrimaryPort);
				if(mServerConnection.handshake()){
					Thread serverConThread = new Thread(mServerConnection);
					serverConThread.start();
					listenForServerMessages();

				}
			}


		}
	}

	private void listenForServerMessages(){

		do {
			mGObjects = mServerConnection.receivePaintings();
			if (mServerConnection.getDisconnect()) {
				System.out.println(mFEConnection);
				mFEConnection.sendCrashMsg();
				connectToFE(mFEHostName, mFEport);
				break;
			}

		} while(true);
	}

	class ThreadRemoval implements Runnable {
		@Override
		//Denna metod tar bort bortkopplade klienter var femte sekund
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
			System.out.println("Detta körs");
			boolean wishesToLeave = mConnectedClient.getDisconnect();
			if (mConnectedClient.getConTries() > 10 || wishesToLeave) {
				System.out.printf("A client has crashed");
				mConnectedClients.remove(mConnectedClient);
				break;
			}
		}
	}
	//TODO
	//Beh�ver skapar replikationer, alternativt tv�(??)
	//Beh�ver best�mma ny prim�r
	//Den nya prim�ren ska meddela klienterna om den nya prim�ren
	//Meddela andra servrar
	//Beh�ver godk�nna klienter via FE 
	
	//METODER:
	//clientRemoval();
	//update();
	
	
    public boolean addClient(ClientConnection clientConnection) {
        //Denna metod l�gger till klienterna
    	mConnectedClients.add(clientConnection);
        return true;
    }
}
