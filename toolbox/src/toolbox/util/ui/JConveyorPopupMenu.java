package toolbox.util.ui;

import javax.swing.Action;
import javax.swing.JMenuItem;

import org.apache.commons.lang.Validate;

/**
 * JPopupMenu that works like a conveyor belt. New items get inserted at the top
 * of the menu and items get pushed off the bottom of the menu when the 
 * capacity is reached.
 */
public class JConveyorPopupMenu extends JSmartPopupMenu
{
    //--------------------------------------------------------------------------
    // Fields
    //--------------------------------------------------------------------------
    
    /** 
     * Max number of items that can be displayed by the popup menu.
     */
    private int capacity_;
    
    //--------------------------------------------------------------------------
    // Constructors
    //--------------------------------------------------------------------------
    
    /**
     * Creates a JConveyorPopupMenu with a default max capacity of 10.
     */
    public JConveyorPopupMenu()
    {
        this(10);
    }
    
    
    /**
     * Creates a JConveyorPopupMenu.
     * 
     * @param capacity Max number of menu items allowed in the menu before
     *        items at the bottom of the menu start getting pushed off.
     */
    public JConveyorPopupMenu(int capacity)
    {
        this("", capacity);
    }

    
    /**
     * Creates a JConveyorPopupMenu.
     * 
     * @param title Popup menu title.
     * @param capacity Max number of menu items allowed in the menu before
     *        items at the bottom of the menu start getting pushed off.
     */
    public JConveyorPopupMenu(String title, int capacity)
    {
        super(title);
        setCapacity(capacity);
    }
    
    //--------------------------------------------------------------------------
    // Public 
    //--------------------------------------------------------------------------
    
    /**
     * Sets the max capacity of the popup menu.
     * 
     * @param capacity Capacity > 0
     * @throws IllegalArgumentException if capacity < 1.
     */
    public void setCapacity(int capacity)
    {
        Validate.isTrue(capacity > 0, "Capacity must be > 0");
        capacity_ = capacity;    
    }
    
    //--------------------------------------------------------------------------
    // Overrides javax.swing.JPopupMenu
    //--------------------------------------------------------------------------
        
    /**
     * @see javax.swing.JPopupMenu#add(javax.swing.Action)
     */
    public JMenuItem add(Action action)
    {
        return add(createActionComponent(action));
    }
    
    
    /**
     * @see javax.swing.JPopupMenu#add(javax.swing.JMenuItem)
     */
    public JMenuItem add(JMenuItem menuItem)
    {
        insert(menuItem, 0);
        
        if (getComponentCount() > capacity_)
            remove(capacity_);
        
        return menuItem;
    }
}