package toolbox.util.test;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import junit.textui.TestRunner;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Category;

import toolbox.util.ArrayUtil;


/**
 * ArrayUtil unit test class
 */
public class ArrayUtilTest
    extends TestCase
{
    
    /** Logger **/
    private static final Category logger = 
        Category.getInstance(ArrayUtilTest.class);

    static
    {
        BasicConfigurator.configure();
    }

    /**
     * ArrayUtilTest constructor
     * 
     * @param aName String
     */
    public ArrayUtilTest(String aName)
    {
        super(aName);
    }


    /**
     * Starts the test case and runs the entire suite.
     *
     * @param args An array of unused command-line arguments
     */
    public static void main(String[] args)
    {
        TestRunner tr = new TestRunner();
        tr.run(ArrayUtilTest.class);
    }


    /**
     * A method that the JUnit framework calls via reflection
     * to return the class' entire test suite.
     *
     * @return The class' test suite.
     */
    public static Test suite()
    {
        return new TestSuite(ArrayUtilTest.class);
    }


    /**
     * Test subset() for subset equal to array
     */
    public void testSubsetAll() throws Exception
    {
        int len = 10;
        double[] d = new double[len];

        for (int i = 0; i < d.length; i++)
            d[i] = i;

        double[] e = ArrayUtil.subset(d, 0, d.length - 1);
        assertEquals("subset should be same size as original", d.length, 
                     e.length);

        for (int i = 0; i < d.length; i++)
            assertEquals("values don't match", d[i], e[i], 0);
    }


    /**
     * Test subset() for empty array
     */
    public void testSubsetEmpty() throws Exception
    {
        double[] d = new double[0];
        double[] e = ArrayUtil.subset(d, 0, 0);
        assertEquals("subset should be empty", 0, e.length);
    }


    /**
     * Test subset() for subset first half of array
     */
    public void testSubsetFirstHalf() throws Exception
    {
        int len = 10;
        double[] d = new double[len];

        for (int i = 0; i < d.length; i++)
            d[i] = i;

        double[] e = ArrayUtil.subset(d, 0, (d.length / 2) - 1);
        assertEquals("subset should be half size of original", d.length / 2, 
                     e.length);

        for (int i = 0; i < e.length; i++)
            assertEquals("values don't match", d[i], e[i], 0);
    }


    /**
     * Test subset() for array of length 1
     */
    public void testSubsetOne() throws Exception
    {
        double[] d = new double[]{99};
        double[] e = ArrayUtil.subset(d, 0, 0);
        assertEquals("subset should have one element", 1, e.length);
        assertEquals("values don't match", d[0], e[0], 0);
    }


    /**
     * Test subset() for subset second half of the array
     */
    public void testSubsetSecondHalf() throws Exception
    {
        int len = 10;
        double[] d = new double[len];

        for (int i = 0; i < d.length; i++)
            d[i] = i;

        double[] e = ArrayUtil.subset(d, d.length / 2, d.length - 1);
        assertEquals("subset should be half size of original", d.length / 2, 
                     e.length);

        int ei = 0;

        for (int i = d.length / 2; i < d.length; i++)
        {
            assertEquals("values don't match", d[i], e[ei], 0);
            ei++;
        }
    }


    /**
     * Tests toString()
     */
    public void testToString()
    {
        String[] s = new String[]{
            "one", "two", "three", "four", "five", "six", "seven", "eight", 
            "nine", "ten"
        };
        
        logger.info("[toStrn] " + ArrayUtil.toString(s));
    }


    /**
     * Tests toString() for one element per line
     */
    public void testToStringOnePerLine()
    {
        String[] s = new String[]
        {
            "one", "two", "three", "four", "five", "six", "seven", "eight", 
            "nine", "ten"
        };
        
        logger.info("[toStrE] " + ArrayUtil.toString(s, true));
    }


    /**
     * Tests toString() for empty array
     */
    public void testToStringEmpty()
    {
        String[] s = new String[0];
        logger.info("[toStr1] " + ArrayUtil.toString(s));
    }
}