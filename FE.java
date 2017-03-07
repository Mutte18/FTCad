package DCAD;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class FE {
	private ServerSocket mFESocket;
	private Socket 		 mClientSocket;
	
	private String 		mPrimaryAddress; //vill lagra den f�rsta serverns port och adress
	private int 		mPrimaryPort;	 // dvs prim�ren

	private ObjectOutputStream mOut = null;
	private boolean noPrimary;

	public static void main(String[] args) {
		if (args.length < 1) {
			System.err.println("Usage: java Server portnumber");
			System.exit(-1);
		}
		try {
			FE instance = new FE(Integer.parseInt(args[0]));
			instance.listenForClientHandshake();
		} catch (NumberFormatException e) {
			System.err.println("Error: port number must be an integer.");
			System.exit(-1);
		}
	}

	private FE(int portNumber) {
		try {
			noPrimary = true;
			mFESocket = new ServerSocket(portNumber);
		} catch (IOException e) {
			System.err.println("Could not bind Front End-Socket: " + e.getMessage());
		}
	}

	private void listenForClientHandshake() {
		System.out.println("Waiting for handshake...!");

		do {
			FEClientConnection feClientConnection = null;
			try {
				mClientSocket = mFESocket.accept();

				feClientConnection = new FEClientConnection(mClientSocket, this);
				if(noPrimary){
					mPrimaryAddress = feClientConnection.getHostName();
					mPrimaryPort = feClientConnection.getPortNumber();
					System.out.println(mPrimaryAddress);
					System.out.println(mPrimaryPort);
					System.out.println("satte en till primary");
					noPrimary = false;
				}

				System.out.println("Kommer bara hit om vi skickar nåt");
			} catch (IOException e) {
				System.err.println("Error while accepting packet: " + e.getMessage());
			}	//H���������������������������������R!
			
				// �NDRA ALLA STRING TILL INET
			
			

			feClientConnection.sendRespondMsg();
		} while (true);
	}
	
	// Veta vilken prim�rSer
	// Skicka klienten till prim�ren

}
