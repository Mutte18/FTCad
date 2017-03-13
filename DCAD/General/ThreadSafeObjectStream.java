package DCAD.General;

import java.io.IOException;
import java.io.ObjectOutputStream;

/**
 * Created by Mattias on 2017-03-10.
 */
public class ThreadSafeObjectStream {
    private ObjectOutputStream mOut;
    public ThreadSafeObjectStream(ObjectOutputStream objectOutputStream){
        mOut = objectOutputStream;
    }

    public synchronized void writeObject(Object o) throws IOException{
        mOut.writeObject(o);
    }
}
