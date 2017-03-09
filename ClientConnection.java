package DCAD;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Random;
import java.util.UUID;

public class ClientConnection implements Runnable {

	private Server mServer;
	private Socket mClientSocket;

	private ObjectOutputStream mOut;
	private ObjectInputStream mIn;
	
	private volatile int mConTries = 0;
	private volatile boolean mDisconnected;

	public ClientConnection(Socket clientSocket, Server server) {
		mServer 	  = server;
		mClientSocket = clientSocket;
		mDisconnected = false;
		
		try {
			mOut = new ObjectOutputStream(mClientSocket.getOutputStream());
			mIn  = new ObjectInputStream(mClientSocket.getInputStream());
		} catch (IOException e) { System.err.println("Error with Object Stream: " + e.getMessage()); }
	}
	
	public synchronized void sendPaintings(ArrayList gobjects){
		try {
			ArrayList<GObject> list = new ArrayList<GObject>(gobjects);
			mOut.writeObject(list);
		} catch (IOException e) {
			mConTries++;
		}
	}

	public synchronized int getConTries(){
		return mConTries;
	}
	
	public boolean getDisconnect(){
		return mDisconnected;
	}

	@Override
	public void run() {
		boolean isConnected = true;
		
		while (isConnected && !mDisconnected){
			try{
				Object o = mIn.readObject();		//Receive the message here, forward to server to handle it
					mServer.handlePaintings(o);
				
			}catch (IOException | ClassNotFoundException e){
				mConTries++;
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
