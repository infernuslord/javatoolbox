package toolbox.jtail.filter.test;

import junit.framework.TestCase;
import junit.textui.TestRunner;

import org.apache.log4j.Logger;

import toolbox.jtail.filter.RegexLineFilter;

/**
 * Unit test for RegexLineFilter
 */
public class RegexLineFilterTest extends TestCase
{
    private static final Logger logger_ = 
        Logger.getLogger(RegexLineFilterTest.class);
        
    //--------------------------------------------------------------------------
    // Main 
    //--------------------------------------------------------------------------
    
    public static void main(String[] args)
    {
        TestRunner.run(RegexLineFilterTest.class);
    }

    //--------------------------------------------------------------------------
    // Unit Tests
    //--------------------------------------------------------------------------
    
    public void testFilter() throws Exception
    {
        logger_.info("Running testFilter...");
        
        RegexLineFilter filter = new RegexLineFilter();
        filter.setEnabled(true);
        String str = "abcdef";
        
        // Match - case insensetive
        filter.setRegularExpression("abc");        
        assertEquals(str, filter.filter(str));
        
        // No match - case insensetive
        filter.setRegularExpression("xyz");
        assertNull(filter.filter(str));
        
        // Match case - case sensetive
        filter.setMatchCase(true);
        filter.setRegularExpression("abc");        
        assertEquals(str, filter.filter(str));
        
        // No match - case sensetive 
        filter.setRegularExpression("ABC");
        assertNull(filter.filter(str));
    }
}