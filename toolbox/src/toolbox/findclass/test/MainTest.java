package toolbox.findclass.test;

import junit.framework.TestCase;
import junit.textui.TestRunner;
import org.apache.log4j.BasicConfigurator;

/**
 * Unit test for findclass
 */
public class MainTest extends TestCase
{
    /**
     * Test entry point
     */
    public static void main(String[] args)
    {
        BasicConfigurator.configure();
        TestRunner.run(MainTest.class);
    }

    /**
     * Arg constructor
     * 
     * @param arg
     */
    public MainTest(String arg)
    {
        super(arg);
    }
    
    /**
     * Test finding a class in a jarfile
     */
    public void testFindInJar() throws Exception
    {
    }

}