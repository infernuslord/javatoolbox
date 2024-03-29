package toolbox.util;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;
import junit.textui.TestRunner;

import org.apache.log4j.Logger;
import toolbox.util.MapUtil;

/**
 * Unit test for {@link toolbox.util.MapUtil}.
 */
public class MapUtilTest extends TestCase
{
    private static final Logger logger_ = 
        Logger.getLogger(MapUtilTest.class);

    //--------------------------------------------------------------------------
    // Main
    //--------------------------------------------------------------------------
            
    /**
     * Entrypoint
     * 
     * @param args None recognized
     */
    public static void main(String[] args)
    {
        TestRunner.run(MapUtilTest.class);
    }

    //--------------------------------------------------------------------------
    // Unit Tests
    //--------------------------------------------------------------------------
        
    /**
     * Tests toString()
     */
    public void testToString()
    {
        Map map = new HashMap();
        
        map.put("name", "john");
        map.put("age", "34");
        map.put("misc", "stuff");
        
        logger_.debug("\n" + MapUtil.toString(map));
    }
}