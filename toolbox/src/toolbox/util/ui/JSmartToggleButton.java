package toolbox.util.ui;

import java.awt.Graphics;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JToggleButton;

import org.apache.log4j.Logger;

import toolbox.util.SwingUtil;
import toolbox.util.dirmon.DirectoryMonitor;

/**
 * <code>JSmartToggleButton<code> adds the following behavior.
 * <p>
 * <ul>
 *   <li>Antialised text
 *   <li>Supports toggling on a property change event for instances where the
 *       state of the button must be kept in sync with a flag belonging to
 *       another component.
 * </ul>
 * <br>
 * <b>Example:</b>
 * <pre class="snippet">
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
public class JSmartToggleButton extends JToggleButton implements AntiAliased {
    
    private static Logger logger_ =  Logger.getLogger(DirectoryMonitor.class);
    
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
    public JSmartToggleButton() {
    }


    /**
     * Creates a JSmartToggleButton.
     * 
     * @param text Button label.
     */
    public JSmartToggleButton(String text) {
        super(text);
    }


    /**
     * Creates a JSmartToggleButton.
     * 
     * @param a Action activated by the button.
     */
    public JSmartToggleButton(Action a) {
        super(a);
    }


    /**
     * Creates a JSmartToggleButton.
     * 
     * @param icon Button icon.
     */
    public JSmartToggleButton(Icon icon) {
        super(icon);
    }


    /**
     * Creates a JSmartToggleButton.
     * 
     * @param text Button label.
     * @param icon Button icon.
     */
    public JSmartToggleButton(String text, Icon icon) {
        super(text, icon);
    }

    // --------------------------------------------------------------------------
    // Public
    //--------------------------------------------------------------------------

    /**
     * Toggles the state of the button based on a property change event
     * for a component.
     *
     * @param comp Component generating the property change event.
     * @param propName Property name to listen for.
     */
    public void toggleOnProperty(JComponent comp, String propName) {
        
        logger_.debug("Adding prop change listener to " + comp + " on prop " + propName);
        
        comp.addPropertyChangeListener(propName, new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent evt) {
                boolean selected = ((Boolean) evt.getNewValue()).booleanValue();
                setSelected(selected);
                //logger_.debug("Setting selected: " + selected);                
            }
        });
    }

    // --------------------------------------------------------------------------
    // AntiAliased Interface
    //--------------------------------------------------------------------------

    /*
     * @see toolbox.util.ui.AntiAliased#isAntiAliased()
     */
    public boolean isAntiAliased() {
        return antiAliased_;
    }


    /*
     * @see toolbox.util.ui.AntiAliased#setAntiAliased(boolean)
     */
    public void setAntiAliased(boolean b) {
        antiAliased_ = b;
    }

    //--------------------------------------------------------------------------
    // Overrides JComponent
    //--------------------------------------------------------------------------

    /*
     * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
     */
    public void paintComponent(Graphics gc) {
        SwingUtil.makeAntiAliased(gc, isAntiAliased());
        super.paintComponent(gc);
    }
}