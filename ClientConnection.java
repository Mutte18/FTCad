package DCAD;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;

public class ClientConnection implements Runnable {
	// Detta kan se annorlunda ut senare, KOM IHÅG! pga BS
	private Server mServer;
	private Socket mClientSocket;

	private ObjectOutputStream mOut;
	private ObjectInputStream mIn;
	
	private volatile int mConTries;
	private volatile boolean mDisconnected;

	public ClientConnection(Socket clientSocket, Server server) {
		mServer 	  = server;
		mClientSocket = clientSocket;
		mDisconnected = false;
		
		try {
			mOut = new ObjectOutputStream(mClientSocket.getOutputStream()); //Skapar ny TCP-con. som kopplar den till socketen
			mIn  = new ObjectInputStream(mClientSocket.getInputStream());
		} catch (IOException e) { System.err.println("Error with Object Stream: " + e.getMessage()); }
	}
	
	public synchronized void sendPaintings(ArrayList gobjects){
		try {
			ArrayList<GObject> list = new ArrayList<GObject>(gobjects);
			mOut.writeObject(list);
		} catch (IOException e) {
			mConTries++;
			System.err.println("Could not write GObject: " + e.getMessage());
		}
	}

	public synchronized int getConTries(){
		return mConTries;
	}
	
	public boolean getDisconnect(){
		return mDisconnected;
	}
	
	public void setDisconnect(boolean value){
		mDisconnected = value;
	}
	
	
	@Override
	public void run() {
		boolean isConnected = true;
		
		while (isConnected && !mDisconnected){
			try{
				Object o = mIn.readObject();
					mServer.handlePaintings(o);
				
			}catch (IOException | ClassNotFoundException e){
				mConTries++;
                System.err.println("Error reading GObject: " + e.getMessage());
			}
			if (mConTries > 10){ 
				isConnected = false; 
			}
		}
		try {
			mClientSocket.close();
		} catch (IOException e) {
			System.err.println("Could not close ClientSocket: " + e.getMessage());
		}
	}
}
