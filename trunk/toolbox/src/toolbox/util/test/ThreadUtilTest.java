package toolbox.util.test;

import junit.framework.TestCase;
import junit.textui.TestRunner;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Category;
import toolbox.util.ArrayUtil;
import toolbox.util.ThreadUtil;

/**
 * Unit test for ThreadUtil
 */
public class ThreadUtilTest extends TestCase
{
    /** Logger **/
    private static final Category logger = 
        Category.getInstance(ThreadUtilTest.class);
    
    /**
     * Arg constructor
     */
    public ThreadUtilTest(String name)
    {
        super(name);
    }

    /**
     * Entrypoint
     */
    public static void main(String[] args)
    {
        BasicConfigurator.configure();
        TestRunner.run(ThreadUtilTest.class);
    }

    /**
     * Tests sleep()
     */
    public void testSleep() throws Exception
    {
        /* not much to test */
        ThreadUtil.sleep(1000);
    }

    /**
     * Tests runInThread()
     */
    public void testRunInThread() throws Exception
    {
        Tester target = new Tester();
        
        /* call simple method with no args */
        ThreadUtil.runInThread(target, "ping", new Object[0]);
        
        /* call method with args */
        Object[] params = new Object[] 
        {
            "string1",
            new String[] {"element1", "element2"}
        };
       
        Thread t = ThreadUtil.runInThread(target, "pingArgs", params);
        t.join();
        
        assertTrue("ping was not executed", target.pingCalled);
        assertTrue("pingArgs was not executed", target.pingArgsCalled);
    }
 
    /**
     * Test class for testRunInThread()
     */   
    public class Tester
    {
        boolean pingCalled = false;
        boolean pingArgsCalled = false;
        
        /**
         * Simplest method - no args 
         */
        public void ping()
        {
            pingCalled = true;
            logger.info("Called ping()");
        }
        
        /**
         * Method with args of complex type
         */
        public void pingArgs(String str, String[] strArray)
        {
            pingArgsCalled = true;
            logger.info("Called pingArgs(" + str + ", " + 
                ArrayUtil.toString(strArray, false) + ")");
        }
    }
}