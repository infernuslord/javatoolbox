package toolbox.plugin.netmeter;

import junit.framework.TestCase;
import junit.textui.TestRunner;

import org.apache.log4j.Logger;

/**
 * Unit test for {@link toolbox.plugin.netmeter.Client}.
 */
public class ClientTest extends TestCase
{
    private static final Logger logger_ = Logger.getLogger(ClientTest.class);
    
    //--------------------------------------------------------------------------
    // Main
    //--------------------------------------------------------------------------
    
    public static void main(String[] args)
    {
        TestRunner.run(ClientTest.class);
    }
    
    //--------------------------------------------------------------------------
    // Unit Tests
    //--------------------------------------------------------------------------

    /**
     * Tests invalid state transitions within the client.
     */
    public void testInvalidTransitions() throws Exception
    {
        logger_.info("Running testInvalidTransitions...");
        
        Client c = new Client();
        
        try
        {
            c.start();
        }
        catch (IllegalStateException ise)
        {
            logger_.debug("SUCCESS: " + ise);
        }
        
        try
        {
            c.stop();
        }
        catch (IllegalStateException ise)
        {
            logger_.debug("SUCCESS: " + ise);
        }
        
        try
        {
            c.destroy();
        }
        catch (IllegalStateException ise)
        {
            logger_.debug("SUCCESS: " + ise);
        }
    }
}
