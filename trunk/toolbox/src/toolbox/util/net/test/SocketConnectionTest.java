package toolbox.util.net.test;

import java.io.IOException;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import org.apache.log4j.Logger;

import junit.framework.TestCase;
import junit.textui.TestRunner;

import toolbox.util.SocketUtil;
import toolbox.util.ThreadUtil;
import toolbox.util.concurrent.BlockingQueue;
import toolbox.util.concurrent.Mutex;
import toolbox.util.net.IConnection;
import toolbox.util.net.IConnectionListener;
import toolbox.util.net.SocketConnection;

/**
 * Unit test for SocketConnection
 */
public class SocketConnectionTest extends TestCase
{
    private static final Logger logger_ = 
        Logger.getLogger(SocketConnectionTest.class);

    //--------------------------------------------------------------------------
    // Main
    //--------------------------------------------------------------------------
    
    /**
     * Entry point
     * 
     * @param  args  None
     */
    public static void main(String[] args)
    {
        TestRunner.run(SocketConnectionTest.class);
    }
    
    //--------------------------------------------------------------------------
    //  Unit Tests
    //--------------------------------------------------------------------------
    
    /**
     * Tests the default constructor
     * 
     * @throws Exception on error
     */
    public void testDefaultConstructor() throws Exception
    {
        logger_.info("Running testDefaultConstructor...");
        
        Server server = new Server(false);
        SocketConnection sc = new SocketConnection();

        server.start();        
        sc.setHost("localhost");
        sc.setPort(server.getPort());
        sc.connect();
        sc.close();
    }
        
    /**
     * Tests the (host,port) constructor
     * 
     * @throws  Exception on error
     */    
    public void testHostPortConstructor() throws Exception
    {
        logger_.info("Running testHostPortConstructor...");
        
        Server server = new Server(false);
        server.start();        
        
        SocketConnection sc = 
            new SocketConnection("localhost", server.getPort());
            
        sc.connect();
        sc.close();
    }
        
    /**
     * Tests the force connect constructor
     * 
     * @throws  Exception on error
     */    
    public void testForceConnectConstructor() throws Exception
    {
        logger_.info("Running testForceConnectConstructor...");
        
        final int port = SocketUtil.getFreePort();
        Server server = new Server(port,false);
        final Mutex mutex = new Mutex();
        
        new Thread(new Runnable()
        {
            public void run()
            {
                try
                {   
                    logger_.info("Mutex : acquired by client");
                    
                    mutex.acquire();
                    SocketConnection sc = 
                        new SocketConnection("localhost", port, true, 2);
                        
                    logger_.info("Client: Connected after retry!");
                    sc.connect();
                    sc.close();
                    mutex.release();
                }
                catch (Exception e)
                {
                    logger_.error("testForceConnectConstructor", e);
                }
            }
        }).start();
        
        ThreadUtil.sleep(6000);
        
        server.start();
        logger_.info("Mutex : server waiting to acquire...");
        mutex.acquire();
        logger_.info("Mutex : acquired by server");
        server.stop();
    }        
   
    /**
     * Tests the getInputStream() method
     * 
     * @throws  Exception on error
     */
    public void testGetInputStream() throws Exception
    {
        logger_.info("Running testGetInputStream...");
        
        Server s = new Server();
        s.start();
        s.waitForStart();
        
        int port = s.getPort();
        Socket socket = new Socket("localhost", port);
        SocketConnection connection = new SocketConnection(socket);
        
        assertNotNull("input stream is null", connection.getInputStream());
        
        socket.close();
        s.stop();
    }

    /**
     * Tests the getOutputStream() method
     * 
     * @throws  Exception on error
     */
    public void testGetOutputStream() throws Exception
    {
        logger_.info("Running testGetOutputStream...");
        
        Server server = new Server();
        
        server.start();
        server.waitForStart();
        int port = server.getPort();
        Socket socket = new Socket("localhost", port);
        SocketConnection connection = new SocketConnection(socket);

        assertNotNull("output stream is null", connection.getOutputStream());
        
        socket.close();
        server.stop();
    }

    /**
     * Tests the notifications genereated by IConnectionListener
     * 
     * @throws  Exception on error
     */
    public void testConnectionListener() throws Exception
    {
        // TODO: Update test to verify connectionInterrupted() once implemented
        
        logger_.info("Running testConnectionListener...");
        
        Server s = new Server();
        s.start();
        s.waitForStart();
        
        SocketConnection connection = new SocketConnection();
        Listener listener = new Listener();
        connection.addConnectionListener(listener);
        connection.setHost("localhost");
        connection.setPort(s.getPort());
        connection.connect();
        listener.waitForStarted();
        connection.close();
        listener.waitForClosing();
        listener.waitForClose();
        connection.removeConnectionListener(listener);
        s.stop();
    }

    /**
     * Tests SocketConnection lifecycle
     * 
     * @throws  Exception on error
     */
    public void testConnectionLifeCycle() throws Exception
    {
        logger_.info("Running testConnectionLifeCycle...");
        
        Server s = new Server(true);
        s.start();
        s.waitForStart();
         
        SocketConnection connection = new SocketConnection();
        connection.addConnectionListener(new Listener());
        connection.setHost("localhost");
        connection.setPort(s.getPort());
        
        for (int i=0; i<100; i++)
        {
            logger_.info("Connection " + i);
            
            try
            {
                connection.connect();
                connection.close();
            }
            catch (ConnectException ce)
            {
                logger_.info("Failed to connect to server: " + ce.getMessage());
            }
            finally
            {
                ThreadUtil.sleep(1);
            }
        }
        
        s.stop();
    }

    /**
     * Tests the isConnected() method
     * 
     * @throws  Exception on error
     */
    public void testIsConnected() throws Exception
    {
        try
        {
            logger_.info("Running testIsConnected...");
            
            Server server = new Server(true);
            server.start();        
            server.waitForStart();
            
            SocketConnection sc = 
                new SocketConnection("localhost", server.getPort());
                
            assertTrue("socket should be connted", sc.isConnected());
            
            sc.close();
            
            assertTrue("socket should be closed", !sc.isConnected());
            
            sc.connect();
            assertTrue("socket should be reconnected", sc.isConnected());
            
            sc.close();
            server.stop();
        }
        catch (Throwable t)
        {
            logger_.error("testIsConnected", t);
        }
    }

    //--------------------------------------------------------------------------
    //  Test helper classes
    //--------------------------------------------------------------------------

    /**
     * Socket connection listener used to make sure notification is working
     */
    class Listener implements IConnectionListener
    {
        private BlockingQueue closed_ = new BlockingQueue();
        private BlockingQueue closing_ = new BlockingQueue();
        private BlockingQueue interrupted_ = new BlockingQueue();
        private BlockingQueue started_ = new BlockingQueue(); 
        
        public void connectionClosed(IConnection connection)
        {
            logger_.info("Notification: Connection closed " + connection);
            
            try
            {
                closed_.push("Closed");
            }
            catch (InterruptedException e)
            {
                logger_.error(e);
            }
        }
        
        public void connectionClosing(IConnection connection)
        {
            logger_.info("Notification: Connection closing " + connection);
            
            try
            {
                closing_.push("Closing");
            }
            catch (InterruptedException e)
            {
                logger_.error(e);
            }
        }
        
        public void connectionInterrupted(IConnection connection)
        {
            logger_.info("Notification: Connection interrupted" + connection);
            
            try
            {
                interrupted_.push("Interrupted");
            }
            catch (InterruptedException e)
            {
                logger_.error(e);
            }
        }
        
        public void connectionStarted(IConnection connection)
        {
            logger_.info("Notification: Connection started" + connection);
            
            try
            {
                started_.push("Started");
            }
            catch (InterruptedException e)
            {
                logger_.error(e);
            }
        }
        
        public void waitForClose() throws InterruptedException
        {
            closed_.pull();
        }
        
        public void waitForClosing() throws InterruptedException
        {
            closing_.pull();
        }
        
        public void waitForInterrupted() throws InterruptedException
        {
            interrupted_.pull();
        }

        public void waitForStarted() throws InterruptedException
        {
            started_.pull();
        }
    }

    /**
     * Internal test socket server
     */
    class Server implements Runnable
    {
        private BlockingQueue serverStarted_;
        private ServerSocket socket_;
        private boolean longLived_ = false;
        private int port_;
        private boolean keepGoing_ = true;
                    
        //----------------------------------------------------------------------
        // Constructors
        //----------------------------------------------------------------------
                            
        /**
         * Default constructor 
         */
        public Server()
        {
            this(0,false);
        }

        /**
         * Creates a long lived Server
         */
        public Server(boolean longLived)
        {
            this(0, longLived);
        }

        /**
         * Creates a Server
         * 
         * @param port
         * @param longLived
         */
        public Server(int port, boolean longLived)
        {
            port_ = port;
            longLived_ = longLived;
            serverStarted_ = new BlockingQueue();
        }

        //----------------------------------------------------------------------
        // Public
        //----------------------------------------------------------------------
        
        /**
         * Starts the socket server
         */
        public void start() throws IOException
        {
            socket_ = new ServerSocket(port_);
            
            Thread t = new Thread(this);
            t.start();
        }

        /**
         * Stops the socket server
         */
        public void stop() throws IOException
        {
            keepGoing_ = false;
            socket_.close();
        }

        /**
         * Retrieves the server socket port
         */
        public int getPort() 
        {
            return socket_.getLocalPort();
        }
        
        /**
         * Once called, will not return until the socket server is ready to
         * accept new connections.
         * 
         * @throws InterruptedException
         */
        public void waitForStart() throws InterruptedException
        {
            serverStarted_.pull();
        }
        
        //----------------------------------------------------------------------
        // Implements Runnable
        //----------------------------------------------------------------------
        
        /**
         * Runs server on thread
         */
        public void run()
        {
            while(keepGoing_)
            {
                try
                {
                    logger_.info("Server: Waiting to accept...");
                    serverStarted_.push("Started");
                    
                    if (!socket_.isClosed())
                    {
                        socket_.accept();
                        keepGoing_ = longLived_;
                        logger_.info("Server: After accept..");
                    }
                }
                catch (SocketException se)
                {
                    logger_.info(se.getMessage());
                }
                catch(IOException e)
                {
                    logger_.error("run", e);
                } 
                catch (InterruptedException e)
                {
                    logger_.error("run2", e);
                }
            }
        }
    }
}
