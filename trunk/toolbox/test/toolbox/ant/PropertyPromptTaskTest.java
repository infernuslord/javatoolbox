package toolbox.ant;

import java.io.File;

import junit.framework.TestCase;
import junit.textui.TestRunner;

import org.apache.tools.ant.Main;

import toolbox.junit.testcase.StandaloneTestCase;
import toolbox.util.FileUtil;
import toolbox.util.ResourceUtil;

/**
 * Unit test for PropertyPromptTask. This test is marked as standalone 
 * because the call into Ant results in call to System.exit() on completion.
 */
public class PropertyPromptTaskTest extends TestCase 
                                    implements StandaloneTestCase 
{
    //--------------------------------------------------------------------------
    // Main
    //--------------------------------------------------------------------------
    
    /**
     * Entry point.
     * 
     * @param args None recognized.
     */
    public static void main(String[] args)
    {
        TestRunner.run(PropertyPromptTaskTest.class);
    }

    //--------------------------------------------------------------------------
    // Unit Tests
    //--------------------------------------------------------------------------
    
    /**
     * Tests property prompt task.
     * 
     * @throws Exception on error.
     */
    public void testPropertyPrompt() throws Exception
    {
        File f = ResourceUtil.getResourceAsTempFile(
            "/toolbox/ant/PropertyPromptTaskTest.xml");
        
        Main.main(new String[]{
            "-debug", 
            "-verbose", 
            "-f", 
            f.getCanonicalPath()
        });
        
        FileUtil.delete(f);
    }
}