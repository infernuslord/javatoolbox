package toolbox.util.io.filter.test;

import java.io.File;

import junit.framework.TestCase;
import junit.textui.TestRunner;

import org.apache.log4j.Logger;

import toolbox.util.FileUtil;
import toolbox.util.io.filter.ExtensionFilter;

/**
 * Unit test for ExtensionFilter
 */
public class ExtensionFilterTest extends TestCase
{
    private static final Logger logger_ = 
        Logger.getLogger(ExtensionFilterTest.class);
        
    /** 
     * Test directory for filtering files 
     */
    private File testDir_;
    
    /** 
     * Entrypoint
     * 
     * @param  args  None recognized
     */
    public static void main(String[] args)
    {
        TestRunner.run(ExtensionFilterTest.class);
    }

    //--------------------------------------------------------------------------
    //  Constructors
    //--------------------------------------------------------------------------
    
    /**
     * Constructor for ExtensionFilterTest.
     * 
     * @param  arg0  Test name
     */
    public ExtensionFilterTest(String arg0)
    {
        super(arg0);
    }

    //--------------------------------------------------------------------------
    //  Setup/Teardown
    //--------------------------------------------------------------------------
    
    /** 
     * Create a temporary directory with files to use for testing
     */
    protected void setUp() throws Exception
    {
        testDir_ = new File(FileUtil.getTempFilename());
        testDir_.mkdir();
        String base = testDir_.getAbsolutePath() + File.separator;
        
        FileUtil.setFileContents(base + "file.txt", "testing", false);
        FileUtil.setFileContents(base + "widget.java", "testing", false);
        FileUtil.setFileContents(base + "b2b.xml", "testing", false);
        FileUtil.setFileContents(base + "EVENT.java", "testing", false);
        super.setUp();
    }


    /**
     * Clean up temporary directory 
     */
    protected void tearDown() throws Exception
    {
        FileUtil.cleanDir(testDir_);
        testDir_.delete();
        super.tearDown();
    }
   
    //--------------------------------------------------------------------------
    //  Unit Tests
    //--------------------------------------------------------------------------
    
    /**
     * Tests accept() for a file with a matching extension
     * 
     * @throws Exception on error
     */
    public void testAcceptMatches() throws Exception
    {
        logger_.info("Running testAcceptMatches...");
        
        ExtensionFilter filter = new ExtensionFilter("txt");
        
        File dummyDir = FileUtil.getTempDir();
        
        assertTrue(filter.accept(dummyDir, "file.txt"));
        assertTrue(filter.accept(dummyDir, "file.TXT"));
        assertTrue(filter.accept(dummyDir, "file.tXt"));
        assertTrue(filter.accept(dummyDir, ".txT"));
    }
    
	/**
	 * Tests accept() for a files with a matching extension with a dot
	 * included in the extension
	 * 
	 * @throws Exception on error
	 */
	public void testAcceptMatchesWithDot() throws Exception
	{
		logger_.info("Running testAcceptMatchesWithDot...");
        
		ExtensionFilter filter = new ExtensionFilter(".txt");
        
		File dummyDir = FileUtil.getTempDir();
        
		assertTrue(filter.accept(dummyDir, "file.txt"));
		assertTrue(filter.accept(dummyDir, "file.TXT"));
		assertTrue(filter.accept(dummyDir, "file.tXt"));
		assertTrue(filter.accept(dummyDir, ".txT"));
	}  
	
	/**
	 * Tests accept() for a file which don't match
	 * 
	 * @throws Exception on error
	 */
	public void testAcceptMatchFails() throws Exception
	{
		logger_.info("Running testAcceptMatchFails...");
        
		ExtensionFilter filter = new ExtensionFilter("txt");
        
		File dummyDir = FileUtil.getTempDir();
        
		assertTrue(!filter.accept(dummyDir, "txt"));
		assertTrue(!filter.accept(dummyDir, "x"));
		assertTrue(!filter.accept(dummyDir, "file.txt."));
		assertTrue(!filter.accept(dummyDir, ".txt.tx"));
		assertTrue(!filter.accept(dummyDir, "txt."));
	}  
}