package toolbox.util.collections;

import junit.framework.TestCase;
import junit.textui.TestRunner;

import org.apache.log4j.Logger;

/**
 * Unit test for {@link toolbox.util.collections.EvictingList}. 
 */
public class EvictingListTest extends TestCase
{
    private static final Logger logger_ =
        Logger.getLogger(EvictingListTest.class);
        
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
        TestRunner.run(EvictingListTest.class);
    }

    //--------------------------------------------------------------------------
    // Unit Tests
    //--------------------------------------------------------------------------
    
    /**
     * Add none. 
     */
    public void testAddSizeZero()
    {
        logger_.info("Running testAddSizeZero...");

        try
        {
            new EvictingList(0);
            fail("Size <=0 is invalid");
        }
        catch (IllegalArgumentException iae)
        {
            assertTrue("Passed", true);
        }
    }

    
    /**
     * Add just one. 
     */
    public void testAddSizeOne()
    {
        logger_.info("Running testAddSizeOne...");
        
        EvictingList list = new EvictingList(1);
        
        list.add("first");
        assertEquals(list.get(0), "first");
        list.add("second");
        assertEquals(list.get(0), "second");
        assertEquals(1, list.size());
    }

    
    /**
     * Add a whole bunch. 
     */
    public void testAddSizeMany()
    {
        logger_.info("Running testAddSizeMany...");

        int size = 5;
        EvictingList list = new EvictingList(5);

        for (int i = 1; i <= size; i++)
            list.add(i + "");

        for (int i = size + 1; i <= size + size; i++)
            list.add(i + "");

        for (int i = 0, j = size + size; i < size; i++, j--)
            assertEquals(j + "", list.get(i));
    }
}