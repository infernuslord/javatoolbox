package toolbox.util.ui.tabbedpane;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.Icon;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;

import org.apache.log4j.Logger;

import toolbox.util.ArrayUtil;
import toolbox.util.SwingUtil;
import toolbox.util.ui.AntiAliased;

/**
 * A JTabbedPane which has an icon on each tab to close the tab.
 */
public class JSmartTabbedPane extends JTabbedPane implements AntiAliased
{
    private static final Logger logger_ =
        Logger.getLogger(JSmartTabbedPane.class);
    
    /**
     * Listeners
     */
    private SmartTabbedPaneListener[] listeners_;

    //--------------------------------------------------------------------------
    // Constructors
    //--------------------------------------------------------------------------
    
    /**
     * Creates a JSmartTabbedPane
     */
    public JSmartTabbedPane()
    {
        setUI(new SmartTabbedPaneUI(SwingConstants.LEFT));
        addMouseListener(new MouseListener());
        listeners_ = new SmartTabbedPaneListener[0];
        
        //addPropertyChangeListener(new DebugPropertyChangeListener());
    }

    /**
     * @param tabPlacement
     */
    public JSmartTabbedPane(int tabPlacement)
    {
        super(tabPlacement);
        setUI(new SmartTabbedPaneUI(SwingConstants.LEFT));
        addMouseListener(new MouseListener());
        listeners_ = new SmartTabbedPaneListener[0];
    }

    /**
     * @param tabPlacement
     * @param tabLayoutPolicy
     */
    public JSmartTabbedPane(int tabPlacement, int tabLayoutPolicy)
    {
        super(tabPlacement, tabLayoutPolicy);
        setUI(new SmartTabbedPaneUI(SwingConstants.LEFT));
        addMouseListener(new MouseListener());
        listeners_ = new SmartTabbedPaneListener[0];
    }

    //--------------------------------------------------------------------------
    // Overrides JTabbedPane
    //--------------------------------------------------------------------------

    /**
     * @see javax.swing.JTabbedPane#addTab(java.lang.String, java.awt.Component)
     */
    public void addTab(String title, Component component)
    {
        addTab(title, component, null);
    }
    
    //--------------------------------------------------------------------------
    // Public
    //--------------------------------------------------------------------------
    
    /***
     * Adds a tab
     * 
     * @param title title
     * @param component component
     * @param extraIcon icon
     */
    public void addTab(String title, Component component, Icon extraIcon)
    {
        super.addTab(title, new SmartTabbedPaneIcon(extraIcon), component);
    }

    //--------------------------------------------------------------------------
    // Event Support
    //--------------------------------------------------------------------------

    /**
     * Adds a tabbed pane listener
     * 
     * @param  listener  Listener to add
     */
    public void addSmartTabbedPaneListener(SmartTabbedPaneListener listener)
    {
        listeners_ = 
            (SmartTabbedPaneListener[]) ArrayUtil.add(listeners_, listener);
    }

    /**
     * Removes a tabbed pane listener
     * 
     * @param listener Listener to remove
     */
    public void removeSmartTabbedPaneListener(SmartTabbedPaneListener listener)
    {
        listeners_ = 
            (SmartTabbedPaneListener[]) ArrayUtil.remove(listeners_, listener);
    }

    /**
     * Fires notification to all listeners that a tab is about to be close
     * via the close icon on the tab.
     * 
     * @param tabIndex Zero based index of tab being closed
     */
    protected void fireTabClosing(int tabIndex)
    {
        for (int i=0; 
             i<listeners_.length; 
             listeners_[i++].tabClosing(this, tabIndex));        
    }
    
    //--------------------------------------------------------------------------
    // AntiAliased Interface
    //--------------------------------------------------------------------------
    
    /**
     * @see toolbox.util.ui.AntiAliased#isAntiAlias()
     */
    public boolean isAntiAliased()
    {
        // TODO: fix me
        return false;
        //return SwingUtil.isAntiAliased();
    }

    /**
     * @see toolbox.util.ui.AntiAliased#setAntiAlias(boolean)
     */
    public void setAntiAliased(boolean b)
    {
    }
    
    //--------------------------------------------------------------------------
    // Overrides JComponent
    //--------------------------------------------------------------------------

    /**
     * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
     */
    public void paintComponent(Graphics gc)
    {
        SwingUtil.makeAntiAliased(gc, isAntiAliased());
        super.paintComponent(gc);
    }
    
    //--------------------------------------------------------------------------
    // Inner Classes
    //--------------------------------------------------------------------------
    
    /**
     * Listens for a click in the close icon and then removes the tab.
     */    
    class MouseListener extends MouseAdapter
    {
        public void mouseClicked(MouseEvent e)
        {
            int tabNumber = getUI().tabForCoordinate(
                JSmartTabbedPane.this, e.getX(), e.getY());
        
            if (tabNumber >= 0)
            {
                Rectangle rect = ((SmartTabbedPaneIcon) 
                    getIconAt(tabNumber)).getBounds();
                
                if (rect.contains(e.getX(), e.getY()))
                {
                    fireTabClosing(tabNumber);
                    removeTabAt(tabNumber);
                }
            }
        }
    }
}