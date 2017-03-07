package DCAD;
//FE st�r f�r Front-end

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;

public class FEConnection {

	private Socket mClientSocket 	= null;
	private String mHostName 		= null;
	
	private ObjectOutputStream mOut 	= null;	//Object skickas, TCP
	private ObjectInputStream mIn		= null;	//Object tas emot, TCP
	
	private int mServerPort = -1; //KANSKE
	private int mFEPort;
	private volatile boolean mIsConnected;
	private InetAddress hostfan;
	private String mPrimaryAddress;
	private int mPrimaryPort;
	private boolean isPrimary;
	
	
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

    public boolean handshake(){
    	//Om klienten f�r true s� b�rjar den bara lyssna p� servern
    	/*try {
			mOut = new ObjectOutputStream(mClientSocket.getOutputStream()); //Skapar ny TCP-con. som kopplar den till socketen
    	} catch (IOException e) {
			System.err.println("Error with ObjectOutputStream: " + e.getMessage());
		}
    	boolean connectionTry = false; //Denna variabel l�ser input som f�tts via socketen*/
    	

    		try {
				mOut = new ObjectOutputStream(mClientSocket.getOutputStream());
				mIn = new ObjectInputStream(mClientSocket.getInputStream());
				sendConnectMsg();
				awaitPrimaryMessage();
    		} catch (IOException e) {
    			System.err.println("Error reading boolean with ObjectInputStream: " + e.getMessage());
			}
    	 //while(connectionTry == true);

    	return true;
    }

    public void sendConnectMsg(){
		try {
			mOut.writeObject(new ConnectionMsg(mHostName, mServerPort));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

    public boolean awaitPrimaryMessage(){
		PrimaryMsg primaryMsg = null;
		try {
			primaryMsg = (PrimaryMsg) mIn.readObject();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		mPrimaryAddress = primaryMsg.getPrimaryAddress();
		mPrimaryPort = primaryMsg.getPrimaryPort();
		System.out.println("Primaryport" + mPrimaryPort);
		isPrimary = primaryMsg.getPrimary();

		/*System.out.println(mPrimaryAddress);
		System.out.println(mPrimaryPort);
		System.out.println(isPrimary);*/


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
    
    /*Beh�ver veta att servern �r vid liv, 
    beh�ver G fr�n serv f�r att sl�ppa in klient*/

}
