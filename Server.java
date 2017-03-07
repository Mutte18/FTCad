package DCAD;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;

public class Server {
	
	private volatile ArrayList<GObject> mGObjects = new ArrayList<>(); 	//Denna lagrar ritade objekt
	private volatile Vector<ClientConnection> mConnectedClients = new Vector<>(); //Lagrar uppkopplade klienter
	//private ConcurrentHashMap<String, Thread> mClientThreads = new ConcurrentHashMap<String, Thread>();
	private FEConnection mFEConnection = null; 

	private ServerSocket mServerSocket;
	private Socket mClientSocket;
	
	private ObjectOutputStream mOut = null;
	private ObjectInputStream mIn = null;
	
	private String mPrimaryAddress;
	private int mFEport;
	private int mServerport;
	private boolean isPrimary;
	static Server instance;
	static int kek;

	public static void main(String[] args) {
		if (args.length < 3) {
			System.err.println("Usage: FE hostName, FE port, Serverport");
			System.exit(-1);
		}
		try {			
			instance = new Server(Integer.parseInt(args[2]));
			instance.connectToFE(args[0], Integer.parseInt(args[1]));
			kek = Integer.parseInt(args[2]);


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
		broadcast();	
		System.out.println(mGObjects.size());
	}
	
	public void broadcast(){
		for(Iterator<ClientConnection> itr = mConnectedClients.iterator(); itr.hasNext();){
			itr.next().sendPaintings(mGObjects);
		}
	}

	private void connectToFE(String hostName, int FEport) {
		//N�r en ny server skapas ska den g� igenom FE
		mFEConnection = new FEConnection(hostName, FEport, mServerport); //Create a new FEconnection
        if(mFEConnection.handshake()){
			mPrimaryAddress = mFEConnection.getPrimaryAddress();
			System.out.println(" This is my address " + mPrimaryAddress);
			System.out.println("This is my port" + mServerport);
			if(mFEConnection.getPrimary()){
				System.out.println("I am supposed to be primray!");
				instance.listenForClientMessages();

			}

		}
	}
	
	class ThreadRemoval implements Runnable {
		@Override
		//Denna metod tar bort bortkopplade klienter var femte sekund
		public void run() {
			while (true) {
				try {
					Thread.sleep(5000);
					//clientRemoval(); Denna metod beh�ver uppdateras
				} catch (InterruptedException e) {
					System.err.println("Failed to sleep thread: " + e.getMessage());
				}
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
