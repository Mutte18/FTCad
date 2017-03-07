package DCAD;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;

public class FEClientConnection{
	// Detta kan se annorlunda ut senare, KOM IH�G! pga BS
	private Socket  mClientSocket;
	private ObjectOutputStream 	mOut;
	private ObjectInputStream 	mIn;
	private FE mFE;

	private String mHostname;
	private int mPortnumber;

	public FEClientConnection(Socket clientSocket, FE fe) {
		mClientSocket 	= clientSocket;
		mFE = fe;


		ConnectionMsg connectionMsg = null;
		
		try {
			mOut = new ObjectOutputStream(mClientSocket.getOutputStream()); //Skapar ny TCP-con. som kopplar den till socketen
			mIn  = new ObjectInputStream(mClientSocket.getInputStream());
			Object o = mIn.readObject();
			System.out.println(mIn);
			System.out.println(o);
			mFE.handleMessages(o);
			//connectionMsg = (ConnectionMsg) mIn.readObject();

		} catch (IOException e) { System.err.println("Error with Object Stream: " + e.getMessage()); } catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}


	
	public synchronized void sendRespondMsg(String primaryaddress, int primaryport, boolean isPrimary){
		try {
			mOut.writeObject(new PrimaryMsg(isPrimary, primaryaddress, primaryport));
		} catch (IOException e) { System.err.println("Could not write GObject: " + e.getMessage()); }
	}

	public String getHostName(){
		return mHostname;
	}

	public int getPortNumber(){
		return mPortnumber;
	}
	
	
}