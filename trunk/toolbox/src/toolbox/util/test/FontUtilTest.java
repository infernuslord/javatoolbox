package toolbox.util.test;

import java.awt.Font;

import javax.swing.JPanel;

import junit.framework.TestCase;
import junit.textui.TestRunner;

import nu.xom.Element;

import org.apache.log4j.Logger;

import toolbox.util.FontUtil;
import toolbox.util.XOMUtil;

/**
 * Unit test for FontUtil.
 */
public class FontUtilTest extends TestCase
{
    private static final Logger logger_ = Logger.getLogger(FontUtilTest.class);
    
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
        TestRunner.run(FontUtilTest.class);
    }

    //--------------------------------------------------------------------------
    // Unit Tests 
    //--------------------------------------------------------------------------

    /**
     * Makes sure the toElement() and toFont() methods work.
     * 
     * @throws Exception on error.
     */
    public void testToElementToFont() throws Exception
    {
        logger_.info("Running testToElement...");
        
        Element forward = FontUtil.toElement(FontUtil.getPreferredMonoFont());
        String xml = XOMUtil.toXML(forward);

        logger_.info("\n" + xml);

        Font reverse = FontUtil.toFont(forward);
        assertNotNull(reverse);
    }


    /**
     * Tests to make sure that a components internal font is set to bold.
     */
    public void testSetBold()
    {
        logger_.info("Running testSetBold...");
        
        Font f = new Font("Dialog", Font.PLAIN, 10);
        JPanel p = new JPanel();
        p.setFont(f);
        FontUtil.setBold(p);
        assertTrue(p.getFont().isBold());
    }

    
    /**
     * Tests retrieving the preferred monospaced font.
     */
    public void testGetPreferredMonoFont()
    {
        logger_.info("Running testGetPreferredMonoFont...");
        
        Font f = FontUtil.getPreferredMonoFont();
        assertNotNull(f);
        logger_.info("Preferred mono font: " + f.getName());
    }


    /**
     * Tests retrieving the preferred serif font.
     */
    public void testGetPreferredSerifFont()
    {
        logger_.info("Running testGetPreferredSerifFont...");
        
        Font f = FontUtil.getPreferredSerifFont();
        assertNotNull(f);
        logger_.info("Preferred serif font: " + f.getName());
    }

    
    /**
     * Tests grow()
     */
    public void testGrow()
    {
        logger_.info("Running testGrow...");
        
        Font f = FontUtil.getPreferredMonoFont();
        int before = f.getSize();
        Font g = FontUtil.grow(f, 8);
        int after = g.getSize();
        assertEquals(f.getSize() + 8, g.getSize());
    }
    
    
    /**
     * Tests shrink()
     */
    public void testShrink()
    {
        logger_.info("Running testShrink...");
        
        Font f = FontUtil.getPreferredMonoFont();
        int before = f.getSize();
        Font g = FontUtil.shrink(f, 4);
        int after = g.getSize();
        assertEquals(f.getSize() - 4, g.getSize());
    }
    
    
    /**
     * Tests setSize()
     */
    public void testSetSize()
    {
        logger_.info("Running testSetSize...");
        
        int expected = 48;
        Font before = FontUtil.getPreferredSerifFont();
        Font after  = FontUtil.setSize(before, expected);
        assertEquals(expected, after.getSize());
    }
}