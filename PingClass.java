package DCAD;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

/**
 * Created by Mattias on 2017-03-10.
 */
public class PingClass implements Runnable {
    private ThreadSafeObjectStream mOut;
    private boolean shutdown;
    public PingClass(ThreadSafeObjectStream outputStream){
        mOut = outputStream;
        shutdown = false;
    }

    public void shutdown(){
        shutdown = true;
    }

    @Override
    public void run() {
        while(!shutdown){
            try {
                mOut.writeObject(new PingMessage());
            } catch (IOException e) {

            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {

            }
        }
    }
}
