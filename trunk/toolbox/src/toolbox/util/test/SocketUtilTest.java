package toolbox.util.test;

import java.io.InterruptedIOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import junit.framework.TestCase;
import junit.textui.TestRunner;

import toolbox.util.SocketUtil;
import toolbox.util.ThreadUtil;

/**
 * Unit test for SocketUtil
 */
public class SocketUtilTest extends TestCase
{
    /**
     * Constructor for SocketUtilTest.
     * 
     * @param name  Name
     */
    public SocketUtilTest(String name)
    {
        super(name);
    }

    /**
     * Entrypoint
     *
     * @param  args  Arguments
     */
    public static void main(String[] args)
    {
        TestRunner.run(SocketUtilTest.class);
    }
    
    /**
     * Tests isReasonAcceptTimeout()
     */
    public void testIsReasonAcceptTimeout() 
    {
        InterruptedIOException iioe = 
            new InterruptedIOException(SocketUtil.MSG_ACCEPT_TIMEOUT);
            
        assertTrue(SocketUtil.isReasonAcceptTimeout(iioe));
    }
    
    /**
     * Tests isReasonSocketClosed()
     */
    public void testIsReasonSocketClosed() 
    {
        SocketException se  = new SocketException(SocketUtil.MSG_SOCKET_CLOSED);
        assertTrue(SocketUtil.isReasonSocketClosed(se));
    }
    
    /**
     * Tests connectWithRetry() for failure scenario
     * 
     * @throws  Exception on error
     */
    public void testConnectWithRetryFailure() throws Exception
    {
        /* try to connect to non-existant socket */
        Socket socket = SocketUtil.connectWithRetry(
            InetAddress.getLocalHost().getHostAddress(), 
            55555, 1, 5);
    }
    
    /**
     * Tests connectWithRetry() for success scenario
     * 
     * @throws Exception on error
     */
    public void testConnectWithRetrySuccess() throws Exception
    {
        /* start socket server */
        ServerSocket ss = new ServerSocket(0);
        
        ThreadUtil.run(ss,"accept", new Object[0]);
        
        /* wait for server to init */
        ThreadUtil.sleep(2000);
        
        /* connect with client */
        Socket socket = SocketUtil.connectWithRetry(
            InetAddress.getLocalHost().getHostAddress(), 
            ss.getLocalPort(), 1, 5);
             
        /* cleanup */
        socket.close();     
    }
}
