package toolbox.util.ui;

import java.awt.Graphics;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JToggleButton;

import toolbox.util.SwingUtil;

/**
 * <code>JSmartToggleButton<code> adds the following behavior.
 * <p>
 * <ul>
 *   <li>Support for antialised text
 *   <li>Supports toggling on a property change event for instances where the 
 *       state of the button must be kept in sync with a flag beloging to 
 *       another component.
 * </ul>
 * Intended usage:
 * <pre>
 * JSmartToggleButton b = new JSmartToggleButton();
 * 
 * //
 * // Whenever the 'selected' event occurs, the toggle button will get 
 * // notified and toggle  its state.
 * //
 * MyComponent mc = new MyComponent();
 * b.toggleOnProperty(mc, "selected");
 * </pre>
 */
public class JSmartToggleButton extends JToggleButton implements AntiAliased
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
     * Creates a JSmartToggleButton.
     */
    public JSmartToggleButton()
    {
    }


    /**
     * Creates a JSmartToggleButton.
     * 
     * @param text Button label.
     */
    public JSmartToggleButton(String text)
    {
        super(text);
    }


    /**
     * Creates a JSmartToggleButton.
     * 
     * @param a Action activated by the button.
     */
    public JSmartToggleButton(Action a)
    {
        super(a);
    }


    /**
     * Creates a JSmartToggleButton.
     * 
     * @param icon Button icon.
     */
    public JSmartToggleButton(Icon icon)
    {
        super(icon);
    }


    /**
     * Creates a JSmartToggleButton.
     * 
     * @param text Button label.
     * @param icon Button icon.
     */
    public JSmartToggleButton(String text, Icon icon)
    {
        super(text, icon);
    }

    //--------------------------------------------------------------------------
    // Public
    //--------------------------------------------------------------------------
    
    /**
     * Toggles the state of the button based on a property change event
     * for a component.
     * 
     * @param comp  Component generating the property change event.
     * @param propName Property name to listen for.
     */
    public void toggleOnProperty(JComponent comp, String propName)
    {
        comp.addPropertyChangeListener(propName, new PropertyChangeListener() 
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