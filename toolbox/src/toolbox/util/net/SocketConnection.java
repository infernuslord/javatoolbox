package toolbox.util.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

import toolbox.util.SocketUtil;

/**
 * Concrete implementation of an {@link IConnection} that wraps a socket.
 */
public class SocketConnection extends AbstractConnection implements IConnection
{
    // TODO: Implement notification for connectionInterrupted
    
    //--------------------------------------------------------------------------
    // Constants
    //--------------------------------------------------------------------------
    
    /** 
     * Default retry interval is 10 seconds. 
     */
    private static final int DEFAULT_RETRY_INTERVAL = 10;
    
    //--------------------------------------------------------------------------
    // Fields
    //--------------------------------------------------------------------------
    
    /** 
     * Socket to manage.
     */
    private Socket socket_;

    /** 
     * Hostname or IP address of the connection endpoint. 
     */
    private String host_;
    
    /** 
     * Port number of connection endpoint. 
     */
    private int port_;

    /** 
     * Flag to force reconnect if the connection is broken. The reconnect
     * strategy will retry indefinitely. 
     */
    private boolean forceConnect_;    

    /** 
     * Interval in seconds between attempts to reconnect if the connection is 
     * broken. 
     */
    private int retryInterval_;

    /** 
     * Connected state of the connection.
     */
    private boolean connected_;

    //--------------------------------------------------------------------------
    // Constructors
    //--------------------------------------------------------------------------
    
    /**
     * Creates a SocketConnection with no connection endpoint.
     */
    public SocketConnection()
    {
        addConnectionListener(new InternalSocketConnectionListener());
    }
    
    
    /**
     * Creates a SocketConnection with an existing Socket by wrapping the
     * socket. Assumes the socket is already connected to an endpoint.
     * 
     * @param s Existing socket endpoint.
     */
    public SocketConnection(Socket s)
    {
        setHost(s.getInetAddress().getHostName());
        setPort(s.getPort());
        setSocket(s);
        addConnectionListener(new InternalSocketConnectionListener());
        connected_ = true;
    }


    /**
     * Creates a SocketConnection with the given host and port. Connects 
     * by default.
     * 
     * @param host Hostname to connect to.
     * @param port Port number to connect to. 
     * @throws IOException on I/O error.
     * @throws UnknownHostException on invalid hostname.
     */
    public SocketConnection(String host, int port) throws IOException, 
        UnknownHostException
    {
        this(host, port, false);
    }


    /**
     * Creates a SocketConnection with given connection parameters. Connects 
     * on construction by default.
     * 
     * @param host Hostname to connect to.
     * @param port Port number to connect to. 
     * @param forceConnect Forces connection to be established. Retries 
     *        indefinitely until successful.
     * @throws IOException on I/O error.
     * @throws UnknownHostException on invalid hostname.
     */
    public SocketConnection(
        String host, 
        int port, 
        boolean forceConnect) 
        throws IOException, UnknownHostException
    {
        this(host, port, forceConnect, DEFAULT_RETRY_INTERVAL);
    }


    /**
     * Creates a SocketConnection with the given connection parameters. Connects
     * on construction by default.
     * 
     * @param host Hostname to connect to.
     * @param port Port number to connect to. 
     * @param forceConnect Forces connection to be established. Retries 
     *        indefinitely until successful..
     * @param retryInterval Delay between connect attempts if forceConnect 
     *        is true.
     * @throws IOException on I/O error.
     * @throws UnknownHostException on invalid hostname.
     */
    public SocketConnection(
        String host, 
        int port, 
        boolean forceConnect, 
        int retryInterval) 
        throws IOException, UnknownHostException
    {
        addConnectionListener(new InternalSocketConnectionListener());
        setHost(host);
        setPort(port);
        setForceConnect(forceConnect);
        setRetryInterval(retryInterval);
        connect();
    }

    //--------------------------------------------------------------------------
    // IConnection Interface
    //--------------------------------------------------------------------------
    
    /**
     * Opens the connection.
     * 
     * @throws IOException on I/O error.
     */
    public void connect() throws IOException
    {
        if (forceConnect_)
        {
            socket_ = SocketUtil.connectWithRetry(
                getHost(), getPort(), retryInterval_, Integer.MAX_VALUE);
        }
        else
            socket_ = new Socket(getHost(), getPort());
            
        fireConnectionStarted(this);
    }


    /**
     * Closes the connection.
     * 
     * @throws IOException on I/O error.
     */
    public void close() throws IOException
    {
        if (socket_ != null)
        {
            fireConnectionClosing(this);
            socket_.close();
            fireConnectionClosed(this);
        }
    }


    /**
     * Accessor for the input stream.
     * 
     * @return InputStream.
     * @throws IOException on I/O error.
     */
    public InputStream getInputStream() throws IOException
    {
        return socket_.getInputStream();
    }


    /**
     * Accessor for the output stream.
     * 
     * @return OutputStream.
     * @throws IOException on I/O error.
     */
    public OutputStream getOutputStream() throws IOException
    {
        return socket_.getOutputStream();
    }


    /**
     * Returns true if connected, false otherwise.
     * 
     * @return boolean
     */
    public boolean isConnected()
    {
        return connected_;
    }

    //--------------------------------------------------------------------------
    //  Accessor/Mutators
    //--------------------------------------------------------------------------
    
    /**
     * Mutator for the socket.
     * 
     * @param socket Socket.
     */
    protected void setSocket(Socket socket)
    {
        socket_ = socket;
    }


    /**
     * Accessor for the socket.
     * 
     * @return Socket
     */
    public Socket getSocket()
    {
        return socket_;
    }
    
    
    /**
     * Returns the host.
     * 
     * @return String
     */
    public String getHost()
    {
        return host_;
    }


    /**
     * Returns the port.
     * 
     * @return int
     */
    public int getPort()
    {
        return port_;
    }


    /**
     * Sets the host.
     * 
     * @param host The host to set.
     */
    public void setHost(String host)
    {
        host_ = host;
    }


    /**
     * Sets the port.
     * 
     * @param port The port to set.
     */
    public void setPort(int port)
    {
        port_ = port;
    }

    
    /**
     * Returns the forceConnect.
     * 
     * @return boolean
     */
    public boolean isForceConnect()
    {
        return forceConnect_;
    }


    /**
     * Returns the retryInterval.
     * 
     * @return int
     */
    public int getRetryInterval()
    {
        return retryInterval_;
    }


    /**
     * Sets the forceConnect.
     * 
     * @param forceConnect The forceConnect to set.
     */
    public void setForceConnect(boolean forceConnect)
    {
        forceConnect_ = forceConnect;
    }


    /**
     * Sets the retryInterval.
     * 
     * @param retryInterval The retryInterval to set.
     */
    public void setRetryInterval(int retryInterval)
    {
        retryInterval_ = retryInterval;
    }

    //--------------------------------------------------------------------------
    // Overrides java.lang.Object
    //--------------------------------------------------------------------------
    
    /**
     * Return string containing the connection name, host, and port.
     * 
     * @return String
     */
    public String toString()
    {
        return getName() + " connection@" + getHost() + ":" + getPort();
    }

    //--------------------------------------------------------------------------
    //  Inner Classes
    //--------------------------------------------------------------------------
    
    /**
     * Internal socket connection listener that keeps track of the connected
     * state based on socket events.
     */
    class InternalSocketConnectionListener implements IConnectionListener 
    {
        /**
         * @see toolbox.util.net.IConnectionListener#connectionClosed(
         *      toolbox.util.net.IConnection)
         */
        public void connectionClosed(IConnection connection)
        {
            connected_ = false;
        }
        
        
        /**
         * @see toolbox.util.net.IConnectionListener#connectionClosing(
         *      toolbox.util.net.IConnection)
         */
        public void connectionClosing(IConnection connection)
        {
        }

        /**
         * @see toolbox.util.net.IConnectionListener#connectionInterrupted(
         *      toolbox.util.net.IConnection)
         */
        public void connectionInterrupted(IConnection connection)
        {
            connected_ = false;
        }
        
        /**
         * @see toolbox.util.net.IConnectionListener#connectionStarted(
         *      toolbox.util.net.IConnection)
         */
        public void connectionStarted(IConnection connection)
        {
            connected_ = true;
        }
    }
}