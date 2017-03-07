package DCAD;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;

public class ServerConnection implements Runnable {
	private ArrayList<GObject> mGObjects;
	private Socket mClientSocket = null;
	private String mServerName = null;

	private ObjectOutputStream mOut = null;
	private ObjectInputStream mIn = null;

	private volatile boolean mIsConnected;
	private int mServerPort = -1;
	private int mConTries;
	private boolean mDisconnected;

	public ServerConnection(String serverName, int serverPort) {
		mServerName = serverName;
		mServerPort = serverPort;

		try {
			mClientSocket = new Socket(serverName, serverPort);
		} catch (IOException e) {
			System.err.println("Could not create ClientSocket: " + e.getMessage());
		}
	}

	public boolean handshake() {
		// Kopplar mOut till klientens socket
		try {
			mOut = new ObjectOutputStream(mClientSocket.getOutputStream());
			mIn = new ObjectInputStream(mClientSocket.getInputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			mOut.writeObject(new ConnectionMsg("le", mServerPort));
		} catch (IOException e) {
			e.printStackTrace();
		}
		mIsConnected = true; // N�r handshaken �r genomf�rd s�tts uppkopplingen till sann
		return true;
	}

	public ArrayList receivePaintings() {
		if (mIsConnected) {
			try {
				mGObjects = (ArrayList<GObject>) mIn.readObject();
			} catch (ClassNotFoundException | IOException e) {
				//System.err.println("Error reading Vector using ObjectInputStream: " + e.getMessage());
			}
		}
		return mGObjects;
	}

	public synchronized void sendGObject(Object object) {
		// Denna tar emot klientens ritning som skickas till servern
		try {
			mOut.writeObject(object);

		} catch (IOException e) {
			System.err.println("Error creating GObject with ObjectOutputStream: " + e.getMessage());
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

		while (isConnected && !mDisconnected) {
			try {
				PingMessage pingMessage = new PingMessage();
				mOut.writeObject(pingMessage);

			} catch (IOException e) {
				mConTries++;
				//System.err.println("Error reading GObject: " + e.getMessage()); MÅSTE VI HA DEN HÄR OUTPUTEN HÄR?!
			}
			if (mConTries > 10) {
				isConnected = false;
			}
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		try {
			mClientSocket.close();
			setDisconnect(true);

			System.out.println("DENNA SKA NU RECONNECTA");
		} catch (IOException e) {
			System.err.println("Could not close ClientSocket: " + e.getMessage());
		}
	}
}
