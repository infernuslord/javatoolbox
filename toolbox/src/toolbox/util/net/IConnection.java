package toolbox.util.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import toolbox.util.service.Nameable;

/**
 * IConnection models the basic characteristics of a bi-directional stream
 * based connection.
 * 
 * @see toolbox.util.net.IConnectionListener
 */
public interface IConnection extends Nameable
{
    /**
     * Connects to an endpoint.
     * 
     * @throws IOException on I/O error.
     */
    void connect() throws IOException;


    /**
     * Returns true if this connection is connected to an endpoint, false 
     * otherwise.
     * 
     * @return boolean
     */
    boolean isConnected();
    

    /**
     * Closes the connection.
     * 
     * @throws IOException on I/O error.
     */
    void close() throws IOException;


    /**
     * Accessor for the connections' input stream.
     * 
     * @return InputStream.
     * @throws IOException on I/O error.
     */
    InputStream getInputStream() throws IOException;


    /**
     * Accessor for the connections' output stream.
     * 
     * @return OutputStream.
     * @throws IOException on I/O error.
     */
    OutputStream getOutputStream() throws IOException;
    
    
    /**
     * Adds a listener to the connection.
     * 
     * @param listener Connection listener to add.
     */
    void addConnectionListener(IConnectionListener listener);


    /**
     * Removes a listener from the connection.
     * 
     * @param listener Connection listener to remove.
     */
    void removeConnectionListener(IConnectionListener listener);
}