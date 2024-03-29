package toolbox.util;

import junit.framework.TestCase;
import junit.textui.TestRunner;
import org.apache.log4j.Logger;

import toolbox.util.RollingCounter;

/**
 * Unit test for {@link toolbox.util.RollingCounter}.
 */
public class RollingCounterTest extends TestCase
{
    private static final Logger logger_ = 
        Logger.getLogger(RollingCounterTest.class);

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
        TestRunner.run(RollingCounterTest.class);
    }

    //--------------------------------------------------------------------------
    // Unit Tests
    //--------------------------------------------------------------------------
    
    /**
     * Tests increment() in a small range.
     */
    public void testRangeIsSmall()
    {
        logger_.info("Running testRangeIsSmall...");
        
        RollingCounter c = new RollingCounter(1, 5, 1);
        
        StringBuffer sb = new StringBuffer();
        
        for (int j = 0; j < 4; j++)
        {
            int cnt = 1;

            for (int i = 1; i <= 5; i++)
            {
                assertEquals("count doesn't match", cnt++, c.getCount());
                c.increment();
            }
        }
        
        for (int i = 0; i < 20; i++)
        {
            sb.append(c.getCount() + " ");
            c.increment();
        }

        logger_.debug(sb);
    }
    
    
    /**
     * Tests increment() in a range of size one.
     */
    public void testRangeIsOne()
    {
        logger_.info("Running testRangeIsOne...");
        
        RollingCounter c = new RollingCounter(1, 1, 1);

        for (int i = 0; i < 20; i++)
        {
            //System.out.print(c.getCount() + " ");
            c.increment();
        }
        //System.out.println();
    }
    
    
    /**
     * Test a range that is negative.
     */
    public void testRangeIsNegative()
    {
        logger_.info("Running testRangeIsNegative...");
        
        RollingCounter c = new RollingCounter(-10, -5, -10);
        
        for (int i = 0; i < 20; i++)
        {
            //System.out.print(c.getCount() + " ");
            c.increment();
        }
        //System.out.println();
    }
    
    
    /**
     * Test a range that is negative to positive.
     */
    public void testRangeIsSigned()
    {
        logger_.info("Running testRangeIsSigned...");
        
        RollingCounter c = new RollingCounter(-5, 5, -5);
        
        for (int i = 0; i < 22; i++)
        {
            //System.out.print(c.getCount() + " ");
            c.increment();
        }
    }

    
    /**
     * Tests the listener.
     */
    public void testListener()
    {
        logger_.info("Running testListener...");
        
        class Ear implements RollingCounter.IRollingCounterListener
        {
            /**
             * @see toolbox.util.RollingCounter.IRollingCounterListener#
             *      afterRoll(toolbox.util.RollingCounter)
             */
            public void afterRoll(RollingCounter rc)
            {
                System.out.print("\n[");
                //System.out.print("*" + rc.getCount() + "*");
            }
            
            
            /**
             * @see toolbox.util.RollingCounter.IRollingCounterListener#
             *      beforeRoll(toolbox.util.RollingCounter)
             */
            public void beforeRoll(RollingCounter rc)
            {
                System.out.print("]");
                //System.out.print("*" + rc.getCount() + "*");                
            }
        }
        
        Ear ear = new Ear();
        RollingCounter rc = new RollingCounter(1, 5, 1);
        rc.addRollingCounterListener(ear);

        for (int i = 0; i < 33; i++)
        {
            System.out.print(rc.getCount());
            rc.increment();
        }       
    }
    
    
    /**
     * Tests the listener.
     */
    public void testListener2()
    {
        logger_.info("Running testListener2...");
        
        class Ear implements RollingCounter.IRollingCounterListener
        {
            /**
             * @see toolbox.util.RollingCounter.IRollingCounterListener#
             *      afterRoll(toolbox.util.RollingCounter)
             */
            public void afterRoll(RollingCounter rc)
            {
                System.out.print("\n[");
                //System.out.print("*" + rc.getCount() + "*");
            }
            
            
            /**
             * @see toolbox.util.RollingCounter.IRollingCounterListener#
             *      beforeRoll(toolbox.util.RollingCounter)
             */
            public void beforeRoll(RollingCounter rc)
            {
                System.out.print("]");
                //System.out.print("*" + rc.getCount() + "*");                
            }
        }
        
        Ear ear = new Ear();
        RollingCounter rc = new RollingCounter(1, 1, 1);
        rc.addRollingCounterListener(ear);

        for (int i = 0; i < 33; i++)
        {
            System.out.print(rc.getCount());
            rc.increment();
        }       
    }    

    
    /**
     * Tests isAtStart()
     */
    public void testIsAtStart()
    {
        logger_.info("Running testIsAtStart...");
        
        RollingCounter c = new RollingCounter(1, 10, 1);
        
        assertTrue(c.isAtStart());
        
        c.increment();
        
        assertTrue(!c.isAtStart());
    }
    
    
    /**
     * Tests toString()
     */
    public void testToString()
    {
        logger_.info("Running testToString...");
        
        RollingCounter c = new RollingCounter(1, 5, 2);
        logger_.debug("toString: " + c);
    }
}