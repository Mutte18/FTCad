package DCAD;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;

public class ServerConnection {
	private ArrayList<GObject> mGObjects;
	private Socket mClientSocket = null;
	private String mServerName = null;

	private ObjectOutputStream mOut = null;
	private ObjectInputStream mIn = null;

	private volatile boolean mIsConnected;
	private int mServerPort = -1;

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
				System.err.println("Error reading Vector using ObjectInputStream: " + e.getMessage());
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
	
}
