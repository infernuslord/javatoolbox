package toolbox.util.ui;

import java.awt.Graphics;

import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.text.Document;

import nu.xom.Attribute;
import nu.xom.Element;

import org.apache.commons.lang.Validate;
import org.apache.log4j.Logger;

import toolbox.util.FontUtil;
import toolbox.util.SwingUtil;
import toolbox.util.XOMUtil;
import toolbox.util.ui.action.AntiAliasAction;
import toolbox.util.ui.textarea.action.AutoScrollAction;
import toolbox.util.ui.textarea.action.LineWrapAction;
import toolbox.workspace.IPreferenced;

/**
 * Extends the functionality of JTextArea by adding the following features.
 * <ul>
 *   <li>Autoscrolling of output
 *   <li>Anti-aliased text
 *   <li>Popup menu with cut/copy/paste/save/insert
 *   <li>Capacity limit with the automatic pruning of text
 *   <li>Support to save/restore preferences to XML
 *   <li>Makes sure all appends are executed on the event dispatch thread
 *   <li>Popupmenu access to toggle the built in line wrapping
 * </ul>
 */
public class JSmartTextArea extends JTextArea
    implements AntiAliased, IPreferenced
{
    private static final Logger logger_ =
        Logger.getLogger(JSmartTextArea.class);

    //--------------------------------------------------------------------------
    // XML Constants
    //--------------------------------------------------------------------------

    private static final String NODE_JSMARTTEXTAREA   = "JSmartTextArea";
    private static final String   ATTR_AUTOSCROLL     = "autoscroll";
    private static final String   ATTR_ANTIALIAS      = "antialias";
    private static final String   ATTR_CAPACITY       = "capacity";
    private static final String   ATTR_PRUNING_FACTOR = "pruningFactor";
    private static final String   ATTR_WRAPLINES      = "wrapLines";
    private static final String NODE_FONT             = "Font";

    //--------------------------------------------------------------------------
    // Fields
    //--------------------------------------------------------------------------

    /**
     * Popup menu for this component.
     */
    private JTextComponentPopupMenu popupMenu_;

    /**
     * Flag that toggles antialiasing of text.
     */
    private boolean antiAlias_;

    /**
     * Flag that toggle autoscrolling of the output as it is appended to the
     * textarea.
     */
    private boolean autoScroll_;

    /**
     * Maximum number of characters allowable in the text area before the text
     * gets automatically pruned FIFO style.
     */
    private int capacity_;

    /**
     * Percentage of text to prune once the capacity is reached. Valid values
     * are in the range [1..100].
     */
    private int pruningFactor_;

    //--------------------------------------------------------------------------
    // Constructors
    //--------------------------------------------------------------------------

    /**
     * Creates a JSmartTextArea with autoscroll and antialias turned off by
     * default.
     */
    public JSmartTextArea()
    {
        this("");
    }


    /**
     * Creates a JSmartTextArea with the given text and autoscroll and antialias
     * turned off by default.
     *
     * @param text Initial text of textarea.
     */
    public JSmartTextArea(String text)
    {
        this(text, false, false);
    }


    /**
     * Creates a JSmartTextArea with the given options.
     *
     * @param autoScroll Turns on autoscroll of output.
     * @param antiAlias Turns on antialiasing of the text.
     */
    public JSmartTextArea(boolean autoScroll, boolean antiAlias)
    {
        this("", autoScroll, antiAlias);
    }


    /**
     * Creates a JSmartTextArea with the given text and options.
     *
     * @param text Initial text.
     * @param autoScroll Turns on autoscroll of output.
     * @param antiAlias Turns on antialiasing of the text.
     */
    public JSmartTextArea(String text, boolean autoScroll, boolean antiAlias)
    {
        super(text);
        init();
        setAutoScroll(autoScroll);
        setAntiAliased(antiAlias);
    }


    /**
     * Creates a JSmartTextArea with the given number of rows and columns.
     *
     * @param rows Number of rows.
     * @param columns Number of columns.
     */
    public JSmartTextArea(int rows, int columns)
    {
        super(rows, columns);
        init();
    }


    /**
     * Creates a JSmartTextArea.
     *
     * @param text Initial text.
     * @param rows Number of rows.
     * @param columns Number of columns.
     */
    public JSmartTextArea(String text, int rows, int columns)
    {
        super(text, rows, columns);
        init();
    }


    /**
     * Creates a JSmartTextArea from a Document.
     *
     * @param doc Document.
     */
    public JSmartTextArea(Document doc)
    {
        super(doc);
        init();
    }


    /**
     * Creates a JSmartTextArea from a Document.
     *
     * @param doc Document.
     * @param text Initial text.
     * @param rows Number of rows.
     * @param columns Number of columns.
     */
    public JSmartTextArea(Document doc, String text, int rows, int columns)
    {
        super(doc, text, rows, columns);
        init();
        setAntiAliased(true);
    }

    //--------------------------------------------------------------------------
    // Public
    //--------------------------------------------------------------------------

    /**
     * Convenience method to scroll to the bottom of the text area.
     */
    public void scrollToEnd()
    {
        setCaretPosition(getDocument().getLength());
    }


    /**
     * Returns true if autoscroll is enabled, false otherwise.
     *
     * @return boolean
     */
    public boolean isAutoScroll()
    {
        return autoScroll_;
    }


    /**
     * Sets the autoScroll feature.
     *
     * @param autoScroll True to enable autoscroll, false to disable autoscroll.
     */
    public void setAutoScroll(boolean autoScroll)
    {
        boolean old = isAutoScroll();
        autoScroll_ = autoScroll;
        firePropertyChange("autoscroll", old, autoScroll);
    }

    
    /**
     * Returns the maximum number of characters displayable by the text area
     * before the contents get pruned.
     *
     * @return Max number of displayable characters.
     */
    public int getCapacity()
    {
        return capacity_;
    }


    /**
     * Sets the max capacity of the text area.
     *
     * @param i Max capacity.
     */
    public void setCapacity(int i)
    {
        int old = capacity_;
        capacity_ = i;
        firePropertyChange("capacity", old, capacity_);
    }


    /**
     * Returns the percentage of text that gets pruned from the text area when
     * the capacity is reached.
     *
     * @return  Percent of text to prune (0 - 100).
     */
    public int getPruneFactor()
    {
        return pruningFactor_;
    }


    /**
     * Sets the pruning factor.
     *
     * @param f Pruning factor.
     * @throws IllegalArgumentException if the pruning factor is not between
     *         0 and 100.
     */
    public void setPruneFactor(int f)
    {
        Validate.isTrue(
            f >= 0 && f <= 100,
            "Pruning factor must be an integer between 0 and 100");

        int old = pruningFactor_;
        pruningFactor_ = f;
        firePropertyChange("prune", old, pruningFactor_);
    }

    //--------------------------------------------------------------------------
    // Protected
    //--------------------------------------------------------------------------

    /**
     * JSmartTextArea specific initialization routine for the constructors.
     */
    protected void init()
    {
        buildView();
        setCapacity(Integer.MAX_VALUE);
        setPruneFactor(0);
        setAntiAliased(SwingUtil.getDefaultAntiAlias());
        setAutoScroll(true);
        setLineWrap(false);
    }


    /**
     * Adds a popupmenu to the textarea.
     */
    protected void buildView()
    {
        //
        // Build popup menu and append autoscroll, line wrap, and antialias
        // onto the end.
        //
        popupMenu_ = new JTextComponentPopupMenu(this);
        popupMenu_.addSeparator();

        popupMenu_.add(
            new JSmartCheckBoxMenuItem(
                new AutoScrollAction(this),
                this,
                "autoscroll"));

        popupMenu_.add(
            new JSmartCheckBoxMenuItem(
                new LineWrapAction(this),
                this,
                "lineWrap"));

        popupMenu_.add(
            new JSmartCheckBoxMenuItem(
                new AntiAliasAction(this),
                this,
                "antialias"));
    }

    //--------------------------------------------------------------------------
    // Antialiased Interface
    //--------------------------------------------------------------------------
    
    /**
     * Returns true if antialiasing is enabled, false otherwise.
     *
     * @return boolean
     */
    public boolean isAntiAliased()
    {
        return antiAlias_;
    }


    /**
     * Activates antialiasing of text.
     *
     * @param antiAlias True turns antialiasing on; false turns it off.
     */
    public void setAntiAliased(boolean antiAlias)
    {
        boolean old = antiAlias_;
        antiAlias_ = antiAlias;
        firePropertyChange("antialias", old, antiAlias_);
    }
    
    //--------------------------------------------------------------------------
    // IPreferenced Interface
    //--------------------------------------------------------------------------

    /**
     * @see toolbox.workspace.IPreferenced#applyPrefs(nu.xom.Element)
     */
    public void applyPrefs(Element prefs)
    {
        Element root = XOMUtil.getFirstChildElement(
            prefs, NODE_JSMARTTEXTAREA, new Element(NODE_JSMARTTEXTAREA));

        setAutoScroll(XOMUtil.getBooleanAttribute(
            root, ATTR_AUTOSCROLL, true));

        setAntiAliased(XOMUtil.getBooleanAttribute(
            root, ATTR_ANTIALIAS, true));

        setCapacity(XOMUtil.getIntegerAttribute(
            root, ATTR_CAPACITY, Integer.MAX_VALUE));

        setPruneFactor(XOMUtil.getIntegerAttribute(
            root, ATTR_PRUNING_FACTOR, 0));

        setLineWrap(XOMUtil.getBooleanAttribute(
            root, ATTR_WRAPLINES, false));

        if (XOMUtil.getFirstChildElement(root, NODE_FONT, null) != null)
            setFont(FontUtil.toFont(root.getFirstChildElement(NODE_FONT)));
    }


    /**
     * @see toolbox.workspace.IPreferenced#savePrefs(nu.xom.Element)
     */
    public void savePrefs(Element prefs)
    {
        Element root = new Element(NODE_JSMARTTEXTAREA);
        root.addAttribute(new Attribute(ATTR_AUTOSCROLL, isAutoScroll() + ""));
        root.addAttribute(new Attribute(ATTR_ANTIALIAS, isAntiAliased() + ""));
        root.addAttribute(new Attribute(ATTR_CAPACITY, getCapacity() + ""));
        root.addAttribute(new Attribute(ATTR_WRAPLINES, getLineWrap() + ""));

        root.addAttribute(
            new Attribute(ATTR_PRUNING_FACTOR, getPruneFactor() + ""));

        root.appendChild(FontUtil.toElement(getFont()));
        prefs.appendChild(root);
    }

    //--------------------------------------------------------------------------
    // Overrides java.awt.Component
    //--------------------------------------------------------------------------

    /**
     * Overriden to enable antialiasing.
     *
     * @see java.awt.Component#paint(java.awt.Graphics)
     */
    public void paint(Graphics g)
    {
        SwingUtil.makeAntiAliased(g, isAntiAliased());
        super.paint(g);
    }

    //--------------------------------------------------------------------------
    // Overrides javax.swing.JTextArea
    //--------------------------------------------------------------------------

    /**
     * Appends a string to the textarea. If the current thread is not the
     * event dispatch thread, then it is queued up on the event dispatch
     * thread.
     *
     * @param str String to append.
     * @see javax.swing.JTextArea#append(String)
     */
    public void append(final String str)
    {
        //super.append(str);

        if (SwingUtilities.isEventDispatchThread())
        {
            super.append(str);
            int len = getDocument().getLength();

            // Let the pruning begin...
            if (len > capacity_)
            {
                int nlen = (int) ((float) pruningFactor_ / 100 * len);
                logger_.debug("Pruning " + len + " to " + nlen);
                setText(getText().substring(nlen));
            }

            // Handle autoscrolling
            if (isAutoScroll() && str.indexOf('\n') >= 0)
                scrollToEnd();
        }
        else
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    append(str);
                }
            });
        }
    }


    /**
     * @see javax.swing.JTextArea#setLineWrap(boolean)
     */
    public void setLineWrap(boolean wrap)
    {
        // NOOP override so line wrap gets included in the property sheet.
        super.setLineWrap(wrap);
    }


    /**
     * @see javax.swing.JTextArea#getLineWrap()
     */
    public boolean getLineWrap()
    {
        // NOOP override so line wrap gets included in the property sheet.
        return super.getLineWrap();
    }
}