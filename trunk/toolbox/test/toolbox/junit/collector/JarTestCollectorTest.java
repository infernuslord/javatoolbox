package toolbox.junit.collector;

import java.util.Enumeration;

import junit.framework.TestCase;
import junit.runner.TestCollector;
import junit.textui.TestRunner;

import org.apache.log4j.Logger;

import toolbox.junit.collector.JarTestCollector;

/**
 * Unit test for {@link toolbox.junit.collector.JarTestCollector}.
 */
public class JarTestCollectorTest extends TestCase
{
    private static final Logger logger_ =
        Logger.getLogger(JarTestCollectorTest.class);

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
        TestRunner.run(JarTestCollectorTest.class);
    }
    
    //--------------------------------------------------------------------------
    // Unit Tests
    //--------------------------------------------------------------------------
    
    /**
     * Tests collectTests()
     */
    public void testCollectTests()
    {
        logger_.info("Running testCollectTests...");
        
        TestCollector tc = new JarTestCollector();
        
        for (Enumeration e = tc.collectTests(); e.hasMoreElements();)
        {
            String classname = (String) e.nextElement();
            logger_.debug("Testclass: " + classname);
        }
        
    }
}