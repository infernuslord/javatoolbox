package toolbox.util.io.filter;

import java.io.File;

import junit.framework.TestCase;
import junit.textui.TestRunner;

import org.apache.log4j.Logger;

import toolbox.util.FileUtil;

/**
 * Unit test for {@link toolbox.util.io.filter.RegexFilter}.
 */
public class RegexFilterTest extends TestCase
{
    private static final Logger logger_ =
        Logger.getLogger(RegexFilterTest.class);
        
    //--------------------------------------------------------------------------
    // Fields
    //--------------------------------------------------------------------------
    
    /** 
     * Test directory for filtering files. 
     */
    private File testDir_;
    
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
        TestRunner.run(RegexFilterTest.class);
    }

    //--------------------------------------------------------------------------
    // Overrides TestCase
    //--------------------------------------------------------------------------
    
    /** 
     * Create a temporary directory with files to use for testing.
     * 
     * @throws Exception on error.
     */
    protected void setUp() throws Exception
    {
        testDir_ = FileUtil.createTempDir();
        String base = testDir_.getAbsolutePath() + File.separator;
        
        FileUtil.setFileContents(base + "file.txt", "testing", false);
        FileUtil.setFileContents(base + "widget.java", "testing", false);
        FileUtil.setFileContents(base + "b2b.xml", "testing", false);
        FileUtil.setFileContents(base + "EVENT.java", "testing", false);
        super.setUp();
    }

    
    /**
     * Clean up temporary directory.
     * 
     * @throws Exception on error. 
     */
    protected void tearDown() throws Exception
    {
        FileUtil.removeDir(testDir_);
        super.tearDown();
    }
   
    //--------------------------------------------------------------------------
    //  Unit Tests
    //--------------------------------------------------------------------------
    
    /**
     * Tests accept() for not matching any files.
     * 
     * @throws Exception on error.
     */
    public void testAcceptMatchesZero() throws Exception
    {
        logger_.info("Running testAcceptMatchesZero...");
        
        RegexFilter filter = new RegexFilter("xyz", false);
        String matches[] = testDir_.list(filter);
        assertEquals("No matches should have been found", 0, matches.length);
    } 

    
    /**
     * Tests accept() for not matching one file.
     * 
     * @throws Exception on error.
     */
    public void testAcceptMatchesOne() throws Exception
    {
        logger_.info("Running testAcceptMatchesOne...");
                
        RegexFilter filter = new RegexFilter("^b2b", false);
        String matches[] = testDir_.list(filter);
        assertEquals("One match should have been found", 1, matches.length);
        assertEquals("One match should have been found", "b2b.xml", matches[0]);
    } 
    
    
    /**
     * Tests accept() for matching many files.
     * 
     * @throws Exception on error.
     */
    public void testAcceptMatchesMany() throws Exception
    {
        logger_.info("Running testAcceptMatchesMany...");
        
        RegexFilter filter = new RegexFilter("java$", false);
        String matches[] = testDir_.list(filter);
        
        assertEquals("Two matches should have been found", 2, matches.length);
        
        for (int i = 0; i < matches.length; i++)
            assertTrue("java file should have been found", 
                matches[i].endsWith("java"));
    }
    
    
    /**
     * Tests accept() for case sensetivity.
     * 
     * @throws Exception on error.
     */
    public void testAcceptMatchesCase() throws Exception
    {
        logger_.info("Running testAcceptMatchesCase...");
        
        // Match found        
        RegexFilter filter = new RegexFilter("^b2b", true);
        String matches[] = testDir_.list(filter);
        assertEquals("One match should have been found", 1, matches.length);
        assertEquals("One match should have been found", "b2b.xml", matches[0]);
        
        // Match not found
        filter = new RegexFilter("^B2B", true);
        matches = testDir_.list(filter);
        assertEquals("No matches should have been found", 0, matches.length);
    } 
}