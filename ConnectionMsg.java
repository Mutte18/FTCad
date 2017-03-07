package DCAD;

import java.io.Serializable;

/**
 * Created by Mattias on 2017-03-06.
 */
public class ConnectionMsg implements Serializable {

    private String mHostname;
    private int mPort;

    public ConnectionMsg(String hostname, int port){
        mHostname = hostname;
        mPort = port;
    }

    public String getHostname() {
        return mHostname;
    }

    public int getPort() {
        return mPort;
    }
}
