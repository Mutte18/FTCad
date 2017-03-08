package DCAD;

import java.io.Serializable;

/**
 * Created by Mattias on 2017-03-06.
 */
public class ServerConnectionMessage implements Serializable {

    private String mHostname;
    private int mPort;

    public ServerConnectionMessage(String hostname, int port){
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
