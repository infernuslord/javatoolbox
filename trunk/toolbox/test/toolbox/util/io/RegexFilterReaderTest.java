package toolbox.util.io;

import java.io.StringReader;

import junit.framework.TestCase;
import junit.textui.TestRunner;

import org.apache.log4j.Logger;

/** 
 * Unit test for RegexFilterReader.
 */
public class RegexFilterReaderTest extends TestCase
{
    private static final Logger logger_ = 
        Logger.getLogger(RegexFilterReaderTest.class);

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
        TestRunner.run(RegexFilterReaderTest.class);
    }

    //--------------------------------------------------------------------------
    // Unit Tests
    //--------------------------------------------------------------------------
    
    /**
     * Tests the constructors.
     * 
     * @throws Exception on error.
     */
    public void testConstructors() throws Exception
    {
        logger_.info("Running testConstructors...");
        
        RegexFilterReader rfr = 
            new RegexFilterReader(new StringReader("boo"));
        
        assertEquals("boo", rfr.readLine());
            
        RegexFilterReader rfr2 = 
            new RegexFilterReader(new StringReader("ya"), "y", true);
            
        assertNotNull(rfr);
        assertNotNull(rfr2);
    }
        
    
    /**
     * Tests readLine() for a simple test case.
     * 
     * @throws Exception on error.
     */
    public void testReadLine() throws Exception
    {
        logger_.info("Running testReadLine...");
        
        String data = "one\ntwo\nthree\nfour\n";
        
        StringReader sr = new StringReader(data);
        RegexFilterReader rr = new RegexFilterReader(sr, "three", true);
        
        assertEquals("three", rr.readLine());
        assertNull(rr.readLine());
    }
}