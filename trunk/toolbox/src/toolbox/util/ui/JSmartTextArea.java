package toolbox.util.ui;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTextArea;

import org.apache.log4j.Category;

/**
 * Extends the functionality of JTextArea by adding
 * <ul>
 *  <li>Tailing of output</li>
 *  <li>Toggle tailing of output via RMB</li>
 *  <li>Anti-aliased text</li>
 * </ul>
 */
public class JSmartTextArea extends JTextArea
{
    /** Logger **/
    private static final Category logger_ =
        Category.getInstance(JSmartTextArea.class);
    
    private JPopupMenu          popup_;
    private JCheckBoxMenuItem   autoScrollItem_;
    private JCheckBoxMenuItem   antiAliasItem_;
    
    private Map           renderMap_;
    private Color         darkblue   = new Color(63, 64, 124);
    private Color         darkrose   = new Color(159, 61, 100);
    private GradientPaint myGradient = new GradientPaint(0, 0, darkblue, 0, 50,
                                       darkrose);

    //--------------------------------------------------------------------------
    //  Constructors
    //--------------------------------------------------------------------------

    /**
     * Default constructor
     */
    public JSmartTextArea()
    {
        this("");
    }

    /**
     * Constructor for JSmartTextArea.
     * 
     * @param text  Initial text of textarea
     */
    public JSmartTextArea(String text)
    {
        this(text, false, false);
    }


    /**
     * Constructor for JSmartTextArea.
     * 
     * @param  autoScroll  Turns on autoscroll of output
     * @param  antialias   Turns on antialiasing of the font
     */
    public JSmartTextArea(boolean autoScroll, boolean antiAlias)
    {
        this("", autoScroll, antiAlias);
    }


    /**
     * Constructor for JSmartTextArea.
     * 
     * @param  text        Initial text
     * @param  autoScroll  Turns on autoscroll of output
     * @param  antialias   Turns on antialiasing of the font
     */
    public JSmartTextArea(String text, boolean autoScroll, boolean antiAlias)
    {
        super(text);
        buildView();
        setAutoScroll(autoScroll);
        setAntiAlias(antiAlias);
    }

    //--------------------------------------------------------------------------
    //  Overridden Methods
    //--------------------------------------------------------------------------
    
    /**
     * Override paint to enable antialiasing
     */    
    public void paint(Graphics g) 
    {
        if (isAntiAlias())
        {
            Graphics2D g2 = (Graphics2D) g;
    
            if (renderMap_ == null)
            {
                renderMap_ = new HashMap();
                
                renderMap_.put(
                    RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    
                renderMap_.put(
                    RenderingHints.KEY_FRACTIONALMETRICS,
                    RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            }
                
            g2.setRenderingHints(renderMap_);
            g2.setPaint(myGradient);
            super.paint(g2);
        }
        else
            super.paint(g);
    }
    
    
    /**
     * Appends a string to the textarea
     * 
     * @param   str  String to append
     * @see     javax.swing.JTextArea#append(String)
     */
    public void append(String str)
    {
        super.append(str);
        
        if (isAutoScroll())
            scrollToEnd();
    }
    
    //--------------------------------------------------------------------------
    // Protected
    //--------------------------------------------------------------------------    
    
    /**
     * Adds the popupmenu to the textarea
     */
    protected void buildView()
    {
        // Build popup menu and add register with textarea
        popup_ = new JPopupMenu("Popup");        
        autoScrollItem_ = new JCheckBoxMenuItem(new AutoScrollAction());
        antiAliasItem_  = new JCheckBoxMenuItem(new AntiAliasAction());
        popup_.add(autoScrollItem_);
        popup_.add(antiAliasItem_);
        addMouseListener(new PopupListener());
    }    
    
    //--------------------------------------------------------------------------
    //  Public 
    //--------------------------------------------------------------------------
    
    /**
     * Scrolls to the end of the text area
     */
    public void scrollToEnd()
    {
        setCaretPosition(getDocument().getLength());
    }
    
    //--------------------------------------------------------------------------
    //  Accessors/Mutators
    //--------------------------------------------------------------------------
    
    /**
     * Determines if the autoscroll feature is active
     * 
     * @return  True is autoscroll is enable, false otherwise
     */
    public boolean isAutoScroll()
    {
        return autoScrollItem_.isSelected();
    }


    /**
     * Sets the autoScroll feature
     * 
     * @param autoScroll  True to enable autoscroll, false to disable autoscroll
     */
    public void setAutoScroll(boolean autoScroll)
    {
        autoScrollItem_.setSelected(autoScroll);
    }

    
    /**
     * Returns the antiAlias.
     * 
     * @return boolean
     */
    public boolean isAntiAlias()
    {
        return antiAliasItem_.isSelected();
    }


    /**
     * Sets the antiAlias.
     * 
     * @param antiAlias The antiAlias to set
     */
    public void setAntiAlias(boolean antiAlias)
    {
        antiAliasItem_.setSelected(antiAlias);
    }
    
    //--------------------------------------------------------------------------
    //  Inner Classes
    //--------------------------------------------------------------------------
    
    class AutoScrollAction extends AbstractAction 
    {
        public AutoScrollAction()
        {
            super("AutoScroll");
        }
        
        public void actionPerformed(ActionEvent e)
        {
            // NO OP
        }
    }    

    class AntiAliasAction extends AbstractAction 
    {
        public AntiAliasAction()
        {
            super("AntiAlias");
        }
        
        public void actionPerformed(ActionEvent e)
        {
            // NO OP
        }
    }    

    
    /**
     * Popup menu listener for RMB on the Classpath list
     */ 
    class PopupListener extends MouseAdapter
    {
        public void mousePressed(MouseEvent e)
        {
            maybeShowPopup(e);
        }

        public void mouseReleased(MouseEvent e)
        {
            maybeShowPopup(e);
        }

        private void maybeShowPopup(MouseEvent e)
        {
            if (e.isPopupTrigger())
                popup_.show(e.getComponent(), e.getX(), e.getY());
        }
    }
    
    
}