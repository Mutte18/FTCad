package DCAD;

import java.io.Serializable;

/**
 * Created by Mattias on 2017-03-06.
 */
public class ServerConnectionMessage implements Serializable {

    private String mHostname;
    private int mPort;
    private Server mServer;

    public ServerConnectionMessage(String hostname, int port, Server server){
        mHostname = hostname;
        mPort = port;
        mServer = server;
    }

    public String getHostname() {
        return mHostname;
    }

    public int getPort() {
        return mPort;
    }

    public Server getServer(){
        return mServer;
    }
}
