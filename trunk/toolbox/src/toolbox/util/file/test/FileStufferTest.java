package toolbox.util.file.test;

import java.io.File;

import org.apache.log4j.Logger;

import junit.framework.TestCase;
import junit.textui.TestRunner;

import toolbox.util.FileUtil;
import toolbox.util.ThreadUtil;
import toolbox.util.file.FileStuffer;

/**
 * Unit test for FileStuffer.
 */
public class FileStufferTest extends TestCase
{
    private static final Logger logger_ = 
        Logger.getLogger(FileStufferTest.class);

    //--------------------------------------------------------------------------
    // Main
    //--------------------------------------------------------------------------
        
    /**
     * Entrypoint.
     *
     * @param args None recognized
     */
    public static void main(String[] args)
    {
        TestRunner.run(FileStufferTest.class);
    }
    
    //--------------------------------------------------------------------------
    //  Unit Tests
    //--------------------------------------------------------------------------
    
    /**
     * Tests running the filestuffer.
     * 
     * @throws Exception on error.
     */
    public void testFileStuffer() throws Exception
    {
        logger_.info("Running testFileStuffer...");
        
        File tmpDir = FileUtil.getTempDir();
        File outfile = new File(tmpDir, "outfile");
        FileStuffer fs = new FileStuffer(outfile, 500);

        assertEquals(500, fs.getDelay());

        fs.start();
        
        ThreadUtil.sleep(4000);
        
        fs.stop();
        
        String contents = FileUtil.getFileContents(outfile.getAbsolutePath());

        logger_.info("File contents: \n" + contents);
        
        outfile.delete();
    }

    
    /**
     * Tests main() for invalid args.
     * 
     * @throws Exception on error.
     */
    public void testMainFailure() throws Exception
    {
        logger_.info("Running testMainFailure...");

        FileStuffer.main(
            new String[] {"one arg ok", "two args ok", "three args bad"});
    }
    
    
    /**
     * Tests printUsage().
     */
    public void testPrintUsage()
    {
        logger_.info("Running testPrintUsage...");
        
        // Passing in no args should trigger usage info to be displayed
        FileStuffer.main(new String[0]);
    }
}