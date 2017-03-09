package DCAD;
//FE st�r f�r Front-end

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;

public class FEConnection implements Runnable{

	private Socket mClientSocket 	= null;
	private String mHostName 		= null;
	
	private ObjectOutputStream mOut 	= null;	//Object skickas, TCP
	private ObjectInputStream mIn		= null;	//Object tas emot, TCP

	private int mServerPort = -1;
	private int mFEPort;
	private String mPrimaryAddress;
	private int mPrimaryPort;
	private boolean isPrimary;
	private int mConTries;
	private boolean mDisconnected;
	private volatile boolean mIsConnected;
	
	
    public FEConnection(String hostName, int FEport, int serverPort) {
        mHostName 	= hostName;
        mFEPort = FEport;
		mServerPort = serverPort;

		try {
            mClientSocket = new Socket(hostName, FEport);
        } catch (IOException e) {
            System.err.println("Could not create ClientSocket: " + e.getMessage());
        }
    }

    public boolean clientHandshake(){
    		try {
				mOut = new ObjectOutputStream(mClientSocket.getOutputStream());
				mIn = new ObjectInputStream(mClientSocket.getInputStream());
				sendClientConnectMessage();
				awaitPrimaryMessage();
    		} catch (IOException e) {
    			System.err.println("Error reading boolean with ObjectInputStream: " + e.getMessage());
			}
    	return true;
    }

	public boolean serverHandshake(){
		try {
			mOut = new ObjectOutputStream(mClientSocket.getOutputStream());
			mIn = new ObjectInputStream(mClientSocket.getInputStream());
			sendServerConnectMessage();
			awaitPrimaryMessage();
		} catch (IOException e) {
			System.err.println("Error reading boolean with ObjectInputStream: " + e.getMessage());
		}
		return true;
	}


	public void sendClientConnectMessage(){
		try {
			mOut.writeObject(new ClientConnectionMessage());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendServerConnectMessage(){
		try {
			mOut.writeObject(new ServerConnectionMessage(mHostName, mServerPort));	//We want to store the address and port of the server in FE
		} catch (IOException e) {
			//e.printStackTrace();
		}
	}

    public boolean awaitPrimaryMessage(){
		PrimaryMsg primaryMsg = null;
		try {
			primaryMsg = (PrimaryMsg) mIn.readObject();
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}
		assert primaryMsg != null;
		mPrimaryAddress = primaryMsg.getPrimaryAddress();
		mPrimaryPort = primaryMsg.getPrimaryPort();
		isPrimary = primaryMsg.getPrimary();

		return true;
	}

	public String getPrimaryAddress(){
		return mPrimaryAddress;
	}

	public int getPrimaryPort(){
		return mPrimaryPort;
	}

	public boolean getPrimary(){
		return isPrimary;
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
				PingMessage pingumessage = new PingMessage();
				mOut.writeObject(pingumessage);


			} catch (IOException e) {
				mConTries++;
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
			System.out.println("KRASHAD");

		} catch (IOException e) {
			System.err.println("Could not close ClientSocket: " + e.getMessage());
		}
	}

}
