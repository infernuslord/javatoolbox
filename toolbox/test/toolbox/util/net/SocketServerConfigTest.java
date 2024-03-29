package toolbox.util.net;

import java.io.File;
import java.util.Properties;

import junit.framework.TestCase;
import junit.textui.TestRunner;

import org.apache.log4j.Logger;

import toolbox.util.FileUtil;
import toolbox.util.net.SocketServerConfig;

/**
 * Unit test for {@link toolbox.util.net.SocketServerConfig}.
 */
public class SocketServerConfigTest extends TestCase
{
    private static final Logger logger_ =
        Logger.getLogger(SocketServerConfigTest.class);

    //--------------------------------------------------------------------------
    // Main
    //--------------------------------------------------------------------------

    /**
     * Entrypoint.
     * 
     * @param args None recognized.
     */
    public static void main(String[] args)
    {
        TestRunner.run(SocketServerConfigTest.class);
    }
    
    //--------------------------------------------------------------------------
    // Unit Tests
    //--------------------------------------------------------------------------
    
    /**
     * Tests the load(File) method. 
     * 
     * @throws Exception on error.
     */
    public void testLoadByFile() throws Exception
    {
        logger_.info("Running testLoadByFile...");
        
        // Create properties file
        String file = FileUtil.createTempFilename();
        
        logger_.debug("Props file: " + file);
        
        try
        {
            StringBuffer sb = new StringBuffer();
            sb.append("socketserver.serverport=1000\n");
            sb.append("socketserver.activeconnections=2000\n");
            sb.append("socketserver.socketqueuesize=3000\n");
            sb.append("socketserver.handlerqueuesize=4000\n");
            sb.append("socketserver.name=server\n");            
            sb.append("socketserver.sockettimeout=5000\n");
            sb.append("socketserver.connectionhandler=" +
                       NullConnectionHandler.class.getName() + "\n");

            FileUtil.setFileContents(file, sb.toString(), false);
            
            logger_.debug("Generated properties file: \n" + 
                FileUtil.getFileContents(file));
            
            // Create config and read props in from file
            SocketServerConfig config = new SocketServerConfig();
            config.load(file);

            logger_.debug("Loaded config: \n " + config);
            assertEquals("ports don't match", 1000, config.getServerPort());
            assertEquals("names don't match", "server", config.getName());
            
            assertEquals("timeouts don't match", 
                5000, config.getSocketTimeout());
                
            assertEquals("handlers don't match", 
                NullConnectionHandler.class.getName(), 
                config.getConnectionHandlerType());
                
            assertEquals("active conns don't match", 
                2000, config.getActiveConnections());
    
            assertEquals("queue sizes don't match", 
                3000, config.getSocketQueueSize());
               
            assertEquals("handler sizes don't match", 
                4000, config.getHandlerQueueSize());
                
            // Load by file via constructor
            new SocketServerConfig(file);
        }
        finally
        {
            try
            {
                File f = new File(file);
                if (f.exists())
                    f.delete();
            }
            catch (Exception e) 
            {
                ; // Ignore
            }
        }
    }
    
    
    /**
     * Tests the load(Properties) method. 
     * 
     * @throws Exception on error.
     */
    public void testLoadByProps() throws Exception
    {
        logger_.info("Running testLoadByProps...");
        
        Properties props = new Properties();
        props.put(SocketServerConfig.PROP_ACTIVE_CONNECTIONS, "3");
        props.put(SocketServerConfig.PROP_HANDLER_QUEUE_SIZE, "5");
        props.put(SocketServerConfig.PROP_SERVER_PORT, "7");
        props.put(SocketServerConfig.PROP_SOCKET_QUEUE_SIZE, "11");
        props.put(SocketServerConfig.PROP_SOCKET_TIMEOUT, "13");
        props.put(SocketServerConfig.PROP_CONNECTION_HANDLER, 
            NullConnectionHandler.class.getName());
        
        SocketServerConfig config = new SocketServerConfig();
        config.load(props);
        
        assertEquals("active conns don't match", 
            3, config.getActiveConnections());
            
        assertEquals("handler size don't match", 
            5, config.getHandlerQueueSize());
            
        assertEquals("server port don't match", 
            7, config.getServerPort());
            
        assertEquals("socket queue don't match", 
            11, config.getSocketQueueSize());
            
        assertEquals("socket timeout don't match", 
            13, config.getSocketTimeout());
        
        assertEquals("socket handlers don't match", 
            NullConnectionHandler.class.getName(), 
            config.getConnectionHandlerType());
            
        logger_.debug(config);
    }
}