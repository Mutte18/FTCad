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

    public boolean clientHandshake(){
    		try {
				mOut = new ObjectOutputStream(mClientSocket.getOutputStream());
				System.out.println("Got past mOut");
				mIn = new ObjectInputStream(mClientSocket.getInputStream());
				System.out.println("Got past mIn");
				sendClientConnectMessage();
				awaitPrimaryMessage();
    		} catch (IOException e) {
    			System.err.println("Error reading boolean with ObjectInputStream: " + e.getMessage());
			}
    	 //while(connectionTry == true);

    	return true;
    }

	public boolean serverHandshake(Server server){
		try {
			mOut = new ObjectOutputStream(mClientSocket.getOutputStream());
			System.out.println("Got past mOut");
			mIn = new ObjectInputStream(mClientSocket.getInputStream());
			System.out.println("Got past mIn");
			sendServerConnectMessage(server);
			awaitPrimaryMessage();
		} catch (IOException e) {
			System.err.println("Error reading boolean with ObjectInputStream: " + e.getMessage());
		}
		//while(connectionTry == true);

		return true;
	}

    public void sendConnectMsg(){

	}

	public void sendClientConnectMessage(){
		try {
			mOut.writeObject(new ClientConnectionMessage());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendServerConnectMessage(Server server){
		try {
			mOut.writeObject(new ServerConnectionMessage(mHostName, mServerPort, server));
		} catch (IOException e) {
			//e.printStackTrace();
		}
	}

	public void sendCrashMsg(){
		try {
			mOut.writeObject(new CrashMessage());
			System.out.println(mOut);
			System.out.println("Send the crash message");
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
