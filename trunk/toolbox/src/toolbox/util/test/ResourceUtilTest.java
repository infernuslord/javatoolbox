package toolbox.util.test;

import junit.framework.TestCase;
import junit.textui.TestRunner;
import org.apache.log4j.BasicConfigurator;
import toolbox.util.ResourceUtil;

/**
 * Unit test for ResourceUtilTest
 */
public class ResourceUtilTest extends TestCase
{
	static
	{
		BasicConfigurator.configure();
	}
	
	/**
	 * Entrypoint
	 */
    public static void main(String[] args)
    {
    	TestRunner tr = new TestRunner();
    	tr.run(ResourceUtilTest.class);	
    }
	
    /**
     * Constructor for ResourceUtilTest
     */
    public ResourceUtilTest(String arg0)
    {
        super(arg0);
    }
    
    
    /**
     * Tests the exportToClass() method
     */
    public void testExportToClass() throws Exception
    {
		String treeOpen = "tree_open.gif";
		String treeClose = "tree_close.gif";
		String cdrive = "cdrive.gif";
		
		ResourceUtil.exportToClass(treeOpen, "toolbox.util.ui", "TreeOpenGIF");    	
		ResourceUtil.exportToClass(treeClose, "toolbox.util.ui", "TreeCloseGIF");
		ResourceUtil.exportToClass(cdrive, "toolbox.util.ui", "HardDriveGIF");
    }
}

