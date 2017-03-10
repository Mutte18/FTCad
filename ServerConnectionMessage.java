package DCAD;

import java.io.Serializable;

/**
 * Created by Mattias on 2017-03-06.
 */
public class ServerConnectionMessage implements Serializable {

    private String mHostname;
    private int mPort;
    private boolean mWasPrimary;

    public ServerConnectionMessage(String hostname, int port, boolean wasPrimary){
        mHostname = hostname;
        mPort = port;
        mWasPrimary = wasPrimary;

    }

    public String getHostname() {
        return mHostname;
    }

    public int getPort() {
        return mPort;
    }

    public boolean getWasPrimary() {
        return mWasPrimary;
    }
}
