package toolbox.util.io;

import junit.framework.TestCase;
import junit.textui.TestRunner;

import org.apache.log4j.Logger;

import toolbox.util.StringUtil;
import toolbox.util.ThreadUtil;

/**
 * Unit test for {@link toolbox.util.io.StringInputStream}. 
 */
public class StringInputStreamTest extends TestCase
{
    private static final Logger logger_ = 
        Logger.getLogger(StringInputStreamTest.class);
       
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
        TestRunner.run(StringInputStreamTest.class);
    }
    
    //--------------------------------------------------------------------------
    // Unit Tests
    //--------------------------------------------------------------------------
        
    /**
     * Tests the read() method.
     * 
     * @throws Exception on error.
     */
    public void testRead() throws Exception
    {
        logger_.info("Running testRead...");
        
        String str = "hello";
        StringInputStream sis = new StringInputStream(str);
        byte[] readBuf = new byte[str.length()];
        int numRead = sis.read(readBuf);
        assertEquals("Number of chars read on equals", str.length(), numRead);
        String compare = new String(readBuf);
        assertEquals("String read from stream doesn't match", str, compare);
    }
    
    
    /**
     * Tests the read() method when stream is empty.
     * 
     * @throws Exception on error.
     */
    public void testReadEmpty() throws Exception
    {
        logger_.info("Running testReadEmpty...");
        
        StringInputStream sis = new StringInputStream("");
        
        assertEquals("should not be able to read from stream", 
            0, sis.available());
            
        assertEquals("read() should return -1", -1, sis.read());
    }
    
    
    /**
     * Tests available() method.
     * 
     * @throws Exception on error.
     */
    public void testAvailable() throws Exception
    {
        logger_.info("Running testAvailable...");
        
        // Case zero
        StringInputStream sis = new StringInputStream("");
        assertEquals("available should be zero", 0, sis.available());
        
        // Case one
        sis = new StringInputStream("x");
        assertEquals("available should be one", 1, sis.available());
        
        // Case many 
        String many = "qiwuerpoqierupqiwuerpqowiuerpoqiwuerpqiurp";
        sis = new StringInputStream(many);
        assertEquals("available is incorrect", many.length(), sis.available());
    }
    
    
    /** 
     * Tests read on an empty stream with ignore EOF set to true.
     * 
     * @throws Exception on error.
     */
    public void testReadEmptyIgnoreEOF() throws Exception
    {
        logger_.info("Running testReadEmptyIgnoreEOF...");
        
        StringInputStream sis = new StringInputStream(true);
 
        int iterations = 3;
        
        ThreadUtil.run(
            this,
            "stuffStream",
            new Object[] {
                sis,
                new Integer(1000),
                "x",
                new Integer(iterations)});
        
        for (int i = 0; i < iterations; i++)                  
        {
            if (i == 2)
                logger_.debug(StringUtil.NL + sis.toString());
                
            int c = sis.read();
            logger_.debug("Read: " + (char) c);
            assertEquals('x', (char) c);
        }
        
        sis.setIgnoreEOF(false);
        assertEquals(-1, sis.read());
    }   
 
    
    /**
     * Tests append().
     * 
     * @throws Exception on error.
     */
    public void testAppend() throws Exception
    {
        logger_.info("Running testAppend...");
        
        StringInputStream sis = new StringInputStream();
        
        int c = -1;
        
        sis.append("x");
        c = sis.read();
        assertEquals('x', (char) c);

        sis.append("ab");
        assertEquals('a', (char) sis.read());
        assertEquals('b', (char) sis.read());
        
        sis.append(null);
        assertEquals(0, sis.available());
        
        sis.append("");
        assertEquals(0, sis.available());
    }
 
    //--------------------------------------------------------------------------
    //  Helper Methods
    //--------------------------------------------------------------------------
    
    /**
     * Stuffs a stream.
     * 
     * @param sis Stream to stuff.
     * @param delay Delay in ms.
     * @param s String being stuffed.
     * @param iterations Number of iterations.
     */
    public void stuffStream(
        StringInputStream sis,
        int delay,
        String s,
        int iterations) 
    {
        for (int i = 0; i < iterations; i++)
        {
            sis.append(s);
            ThreadUtil.sleep(delay);
        }
    }
}