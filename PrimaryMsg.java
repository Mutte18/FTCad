package DCAD;

import java.io.Serializable;
import java.net.InetAddress;

public class PrimaryMsg implements Serializable {
	private boolean 	mPrimary;
	private String 		mPrimaryAddress;
	private int 		mPrimaryPort;
	
	
	public PrimaryMsg(boolean primary, String primaryAddress, int primaryPort){
		mPrimary 		= primary;
		mPrimaryAddress = primaryAddress;
		mPrimaryPort 	= primaryPort;	
	}
	
	public boolean getPrimary(){
		return mPrimary;
	}
	
	public String getPrimaryAddress(){
		return mPrimaryAddress;
	}
	
	public int getPrimaryPort(){
		return mPrimaryPort;
	}
}
