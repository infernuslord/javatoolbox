package toolbox.findclass.test;

import junit.framework.TestCase;
import junit.textui.TestRunner;
import org.apache.log4j.BasicConfigurator;
import toolbox.findclass.FindClass;
import toolbox.findclass.FindClassCollector;

/**
 * Unit test for findclass
 */
public class FindClassTest extends TestCase
{
    /**
     * Test entry point
     */
    public static void main(String[] args)
    {
        BasicConfigurator.configure();
        TestRunner.run(FindClassTest.class);
    }

    /**
     * Arg constructor
     * 
     * @param arg
     */
    public FindClassTest(String arg)
    {
        super(arg);
    }
    
    /**
     * Test finding a class in a jarfile
     */
    public void testFindInJar() throws Exception
    {
        FindClass finder = new FindClass();
        FindClassCollector collector = new FindClassCollector();
        finder.addFindClassListener(collector);
        
        finder.findClass("Info$", false);
        
        
        
    }

}