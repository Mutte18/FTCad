package DCAD;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;

public class FEClientConnection{
	// Detta kan se annorlunda ut senare, KOM IH�G! pga BS
	private FE 		mFE;
	private Socket  mClientSocket;
	private InetAddress mPrimaryAddress;
	private int		mPrimaryPort;

	private ObjectOutputStream 	mOut;
	private ObjectInputStream 	mIn;

	private String mHostname;
	private int mPortnumber;

	public FEClientConnection(Socket clientSocket, FE fe) {
		mClientSocket 	= clientSocket;
		mFE				= fe;		


		ConnectionMsg connectionMsg = null;
		
		try {
			mOut = new ObjectOutputStream(mClientSocket.getOutputStream()); //Skapar ny TCP-con. som kopplar den till socketen
			mIn  = new ObjectInputStream(mClientSocket.getInputStream());
			connectionMsg = (ConnectionMsg) mIn.readObject();
			mHostname = connectionMsg.getHostname();
			mPortnumber = connectionMsg.getPort();
			System.out.println(mPortnumber + "Portnumber??");

		} catch (IOException e) { System.err.println("Error with Object Stream: " + e.getMessage()); } catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}


	
	public synchronized void sendRespondMsg(){
		try {
			mOut.writeObject(new PrimaryMsg(true, mHostname, mPortnumber));
		} catch (IOException e) { System.err.println("Could not write GObject: " + e.getMessage()); }
	}

	public String getHostName(){
		return mHostname;
	}

	public int getPortNumber(){
		return mPortnumber;
	}
	
	
}