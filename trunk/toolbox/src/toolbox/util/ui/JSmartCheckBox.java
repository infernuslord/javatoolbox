package toolbox.util.ui;

import java.awt.Graphics;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JComponent;

import toolbox.util.SwingUtil;

/**
 * JSmartCheckBox adds the following behavior.
 * <p>
 * <ul>
 *   <li>Support for antialised text
 * </ul>
 */
public class JSmartCheckBox extends JCheckBox implements AntiAliased
{
    //--------------------------------------------------------------------------
    // Fields
    //--------------------------------------------------------------------------
    
    /**
     * Antialiased flag.
     */
    private boolean antiAliased_ = SwingUtil.getDefaultAntiAlias();

    //--------------------------------------------------------------------------
    // Constructors
    //--------------------------------------------------------------------------
    
    /**
     * Creates a JSmartCheckBox.
     */
    public JSmartCheckBox()
    {
    }


    /**
     * Creates a JSmartCheckBox.
     * 
     * @param text Text label.
     */
    public JSmartCheckBox(String text)
    {
        super(text);
    }


    /**
     * Creates a JSmartCheckBox.
     * 
     * @param text Text label.
     * @param selected Initial selected state.
     */
    public JSmartCheckBox(String text, boolean selected)
    {
        super(text, selected);
    }


    /**
     * Creates a JSmartCheckBox.
     * 
     * @param a Action.
     */
    public JSmartCheckBox(Action a)
    {
        super(a);
    }


    /**
     * Creates a JSmartCheckBox.
     * 
     * @param icon Icon.
     */
    public JSmartCheckBox(Icon icon)
    {
        super(icon);
    }


    /**
     * Creates a JSmartCheckBox.
     * 
     * @param icon Icon.
     * @param selected Initial selected state.
     */
    public JSmartCheckBox(Icon icon, boolean selected)
    {
        super(icon, selected);
    }

    
    /**
     * Creates a JSmartCheckBox.
     * 
     * @param text Text label.
     * @param icon Icon.
     */
    public JSmartCheckBox(String text, Icon icon)
    {
        super(text, icon);
    }

    
    /**
     * Creates a JSmartCheckBox.
     * 
     * @param text Text label.
     * @param icon Icon.
     * @param selected Initial selected state.
     */
    public JSmartCheckBox(String text, Icon icon, boolean selected)
    {
        super(text, icon, selected);
    }

    //--------------------------------------------------------------------------
    // Public
    //--------------------------------------------------------------------------
    
    /**
     * Toggles the state of the checkbox based on a property change event
     * originating from a component.
     * 
     * @param comp Component generating the property change event.
     * @param property Property name to listen for.
     */
    public void toggleOnProperty(JComponent comp, String property)
    {
        comp.addPropertyChangeListener(property, new PropertyChangeListener() 
        {
            public void propertyChange(PropertyChangeEvent evt)
            {
                setSelected(((Boolean) evt.getNewValue()).booleanValue());
            }
        });
    }
    
    //--------------------------------------------------------------------------
    // AntiAliased Interface
    //--------------------------------------------------------------------------
    
    /**
     * @see toolbox.util.ui.AntiAliased#isAntiAliased()
     */
    public boolean isAntiAliased()
    {
        return antiAliased_;
    }


    /**
     * @see toolbox.util.ui.AntiAliased#setAntiAliased(boolean)
     */
    public void setAntiAliased(boolean b)
    {
        antiAliased_ = b;
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
}