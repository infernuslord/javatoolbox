/*
 * Copyright (c) 2003 JGoodies Karsten Lentzsch. All Rights Reserved. This
 * software is the proprietary information of Karsten Lentzsch. Use is subject
 * to license terms.
 */
package toolbox.util.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Paint;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.AbstractBorder;

import toolbox.util.FontUtil;
import toolbox.util.ui.textarea.action.AutoTailAction;
import toolbox.util.ui.textarea.action.ClearAction;
import toolbox.util.ui.textarea.action.LineWrapAction;

/**
 * A <code>JPanel</code> subclass that has a drop shadow border and that
 * provides a header with icon, title and tool bar.
 * <p>
 * This class can be used to replace the <code>JInternalFrame</code>, for
 * use outside of a <code>JDesktopPane</code>. The <code>JHeaderPanel</code>
 * is less flexible but often more usable; it avoids overlapping windows and
 * scales well up to IDE size. Several customers have reported that they and
 * their clients feel much better with both the appearance and the UI feel.
 * <p>
 * The JHeaderPanel provides the following bound properties: <i>frameIcon,
 * title, toolBar, content, selected. </i>
 * <p>
 * By default the JHeaderPanel is in <i>selected </i> state. If you don't do
 * anything, multiple simple internal frames will be displayed as selected.
 * 
 * @author Karsten Lentzsch
 * @version $Revision$
 * @see javax.swing.JInternalFrame
 * @see javax.swing.JDesktopPane
 */

public class JHeaderPanel extends JPanel
{
    //--------------------------------------------------------------------------
    // Fields
    //--------------------------------------------------------------------------
    
    /**
     * Title.
     */
    private JLabel titleLabel;
    
    /**
     * Background gradient.
     */
    private GradientPanel gradientPanel;
    
    /**
     * Wraps the title and gradient.
     */
    private JPanel headerPanel;
    
    /**
     * Selected state.
     */
    private boolean isSelected;

    //--------------------------------------------------------------------------
    // Constructors
    //--------------------------------------------------------------------------
    
    // Instance Creation ****************************************************

    /**
     * Constructs a <code>JHeaderPanel</code> with the specified title.
     * 
     * @param title the initial title
     */
    public JHeaderPanel(String title)
    {
        this(null, title, null, null);
    }


    /**
     * Constructs a <code>JHeaderPanel</code> with the specified icon, and
     * title.
     * 
     * @param icon the initial icon
     * @param title the initial title
     */
    public JHeaderPanel(Icon icon, String title)
    {
        this(icon, title, null, null);
    }


    /**
     * Constructs a <code>JHeaderPanel</code> with the specified title, tool
     * bar, and content panel.
     * 
     * @param title the initial title
     * @param bar the initial tool bar
     * @param content the initial content pane
     */
    public JHeaderPanel(String title, JToolBar bar, JComponent content)
    {
        this(null, title, bar, content);
    }


    /**
     * Constructs a <code>JHeaderPanel</code> with the specified icon, title,
     * tool bar, and content panel.
     * 
     * @param icon the initial icon
     * @param title the initial title
     * @param bar the initial tool bar
     * @param content the initial content pane
     */
    public JHeaderPanel(Icon icon, String title, JToolBar bar,
        JComponent content)
    {
        super(new BorderLayout());
        this.isSelected = false;
        this.titleLabel = new JSmartLabel(title, icon, SwingConstants.LEADING);
        FontUtil.setBold(titleLabel);
        JPanel top = buildHeader(titleLabel, bar);

        add(top, BorderLayout.NORTH);
        if (content != null)
        {
            setContent(content);
        }
        setBorder(new ShadowBorder());
        setSelected(true);
        updateHeader();
    }


    // Public API ***********************************************************

    /**
     * Returns the frame's icon.
     * 
     * @return the frame's icon
     */
    public Icon getFrameIcon()
    {
        return titleLabel.getIcon();
    }


    /**
     * Sets a new frame icon.
     * 
     * @param newIcon the icon to be set
     */
    public void setFrameIcon(Icon newIcon)
    {
        Icon oldIcon = getFrameIcon();
        titleLabel.setIcon(newIcon);
        firePropertyChange("frameIcon", oldIcon, newIcon);
    }


    /**
     * Returns the frame's title text.
     * 
     * @return String the current title text
     */
    public String getTitle()
    {
        return titleLabel.getText();
    }


    /**
     * Sets a new title text.
     * 
     * @param newText the title text tp be set
     */
    public void setTitle(String newText)
    {
        String oldText = getTitle();
        titleLabel.setText(newText);
        firePropertyChange("title", oldText, newText);
    }


    /**
     * Returns the current toolbar, null if none has been set before.
     * 
     * @return the current toolbar - if any
     */
    public JToolBar getToolBar()
    {
        return headerPanel.getComponentCount() > 1 ? (JToolBar) headerPanel
            .getComponent(1) : null;
    }


    /**
     * Sets a new tool bar in the header.
     * 
     * @param newToolBar the tool bar to be set in the header
     */
    public void setToolBar(JToolBar newToolBar)
    {
        JToolBar oldToolBar = getToolBar();
        if (oldToolBar == newToolBar)
        {
            return;
        }
        if (oldToolBar != null)
        {
            headerPanel.remove(oldToolBar);
        }
        if (newToolBar != null)
        {
            newToolBar.setOpaque(false);
            newToolBar.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
            headerPanel.add(newToolBar, BorderLayout.EAST);
        }
        updateHeader();
        firePropertyChange("toolBar", oldToolBar, newToolBar);
    }


    /**
     * Returns the content - null, if none has been set.
     * 
     * @return the current content
     */
    public Component getContent()
    {
        return hasContent() ? getComponent(1) : null;
    }


    /**
     * Sets a new panel content; replaces any existing content, if existing.
     * 
     * @param newContent the panel's new content
     */
    public void setContent(Component newContent)
    {
        Component oldContent = getContent();
        if (hasContent())
        {
            remove(oldContent);
        }
        add(newContent, BorderLayout.CENTER);
        firePropertyChange("content", oldContent, newContent);
    }


    /**
     * Answers if the panel is currently selected (or in other words active) or
     * not. In the selected state, the header background will be rendered
     * differently.
     * 
     * @return boolean a boolean, where true means the frame is selected
     *         (currently active) and false means it is not
     */
    public boolean isSelected()
    {
        return isSelected;
    }


    /**
     * This panel draws its title bar differently if it is selected, which may
     * be used to indicate to the user that this panel has the focus, or should
     * get more attention than other simple internal frames.
     * 
     * @param newValue a boolean, where true means the frame is selected
     *        (currently active) and false means it is not
     */
    public void setSelected(boolean newValue)
    {
        boolean oldValue = isSelected();
        isSelected = newValue;
        updateHeader();
        firePropertyChange("selected", oldValue, newValue);
    }


    // Building *************************************************************

    /**
     * Creates and answers the header panel, that consists of: an icon, a title
     * label, a tool bar, and a gradient background.
     * 
     * @param label the label to paint the icon and text
     * @param bar the panel's tool bar
     * @return the panel's built header area
     */
    private JPanel buildHeader(JLabel label, JToolBar bar)
    {

        gradientPanel = new GradientPanel(new BorderLayout(),
            getHeaderBackground());

        label.setOpaque(false);

        gradientPanel.add(label, BorderLayout.WEST);
        gradientPanel.setBorder(BorderFactory.createEmptyBorder(3, 4, 3, 1));

        headerPanel = new JPanel(new BorderLayout());
        headerPanel.add(gradientPanel, BorderLayout.CENTER);
        setToolBar(bar);
        headerPanel.setBorder(new RaisedHeaderBorder());
        headerPanel.setOpaque(false);
        return headerPanel;
    }


    /**
     * Updates the header.
     */
    private void updateHeader()
    {
        gradientPanel.setBackground(getHeaderBackground());
        gradientPanel.setOpaque(isSelected());
        titleLabel.setForeground(getTextForeground(isSelected()));
        headerPanel.repaint();
    }


    /**
     * Updates the UI. In addition to the superclass behavior, we need to
     * update the header component.
     */
    public void updateUI()
    {
        super.updateUI();
        if (titleLabel != null)
        {
            updateHeader();
        }
    }


    // Helper Code **********************************************************

    /**
     * Checks and answers if the panel has a content component set.
     * 
     * @return true if the panel has a content, false if it's empty
     */
    private boolean hasContent()
    {
        return getComponentCount() > 1;
    }


    /**
     * Determines and answers the header's text foreground color. Tries to
     * lookup a special color from the L&amp;F. In case it is absent, it uses
     * the standard internal frame forground.
     * 
     * @param selected true to lookup the active color, false for the inactive
     * @return the color of the foreground text
     */
    protected Color getTextForeground(boolean selected)
    {
        Color c = UIManager
            .getColor(selected ? "SimpleInternalFrame.activeTitleForeground"
                : "SimpleInternalFrame.inactiveTitleForeground");
        if (c != null)
        {
            return c;
        }
        return UIManager
            .getColor(selected ? "InternalFrame.activeTitleForeground"
                : "Label.foreground");

    }


    /**
     * Determines and answers the header's background color. Tries to lookup a
     * special color from the L&amp;F. In case it is absent, it uses the
     * standard internal frame background.
     * 
     * @return the color of the header's background
     */
    protected Color getHeaderBackground()
    {
        Color c = UIManager
            .getColor("SimpleInternalFrame.activeTitleBackground");
        if (c != null)
            return c;

        //if (LookUtils.IS_LAF_WINDOWS_XP_ENABLED)
        //    c = UIManager.getColor("InternalFrame.activeTitleGradient");
        return c != null ? c : UIManager
            .getColor("InternalFrame.activeTitleBackground");
    }


    // Helper Classes *******************************************************

    // A custom border for the raised header pseudo 3D effect.
    private static class RaisedHeaderBorder extends AbstractBorder
    {

        private static final Insets INSETS = new Insets(1, 1, 1, 0);


        public Insets getBorderInsets(Component c)
        {
            return INSETS;
        }


        public void paintBorder(Component c, Graphics g, int x, int y, int w,
            int h)
        {

            g.translate(x, y);
            g.setColor(UIManager.getColor("controlLtHighlight"));
            g.fillRect(0, 0, w, 1);
            g.fillRect(0, 1, 1, h - 1);
            g.setColor(UIManager.getColor("controlShadow"));
            g.fillRect(0, h - 1, w, 1);
            g.translate(-x, -y);
        }
    }

    // A custom border that has a shadow on the right and lower sides.
    private static class ShadowBorder extends AbstractBorder
    {

        private static final Insets INSETS = new Insets(1, 1, 3, 3);


        public Insets getBorderInsets(Component c)
        {
            return INSETS;
        }


        public void paintBorder(Component c, Graphics g, int x, int y, int w,
            int h)
        {

            Color shadow = UIManager.getColor("controlShadow");
            if (shadow == null)
            {
                shadow = Color.GRAY;
            }
            Color lightShadow = new Color(shadow.getRed(), shadow.getGreen(),
                shadow.getBlue(), 170);
            Color lighterShadow = new Color(shadow.getRed(), shadow.getGreen(),
                shadow.getBlue(), 70);
            g.translate(x, y);

            g.setColor(shadow);
            g.fillRect(0, 0, w - 3, 1);
            g.fillRect(0, 0, 1, h - 3);
            g.fillRect(w - 3, 1, 1, h - 3);
            g.fillRect(1, h - 3, w - 3, 1);
            // Shadow line 1
            g.setColor(lightShadow);
            g.fillRect(w - 3, 0, 1, 1);
            g.fillRect(0, h - 3, 1, 1);
            g.fillRect(w - 2, 1, 1, h - 3);
            g.fillRect(1, h - 2, w - 3, 1);
            // Shadow line2
            g.setColor(lighterShadow);
            g.fillRect(w - 2, 0, 1, 1);
            g.fillRect(0, h - 2, 1, 1);
            g.fillRect(w - 2, h - 2, 1, 1);
            g.fillRect(w - 1, 1, 1, h - 2);
            g.fillRect(1, h - 1, w - 2, 1);
            g.translate(-x, -y);
        }
    }

    // A panel with a horizontal gradient background.
    private static class GradientPanel extends JPanel
    {

        private GradientPanel(LayoutManager lm, Color background)
        {
            super(lm);
            setBackground(background);
        }


        public void paintComponent(Graphics g)
        {
            super.paintComponent(g);
            if (!isOpaque())
            {
                return;
            }
            
            Color control = UIManager.getColor("control");
            
            // WORKAROUND: FH look and fool has no control color defined.
            if (control == null)
                control = UIManager.getColor("Label.background");
            
            int width = getWidth();
            int height = getHeight();

            Graphics2D g2 = (Graphics2D) g;
            Paint storedPaint = g2.getPaint();
            
            g2.setPaint(
                new GradientPaint(0, 0, getBackground(), width, 0, control));
            
            g2.fillRect(0, 0, width, height);
            g2.setPaint(storedPaint);
        }
    }
    
    // Customizations ==========================================================
    
    /**
     * Creates a toolbar specifically to be added to a JHeaderPanel.
     * 
     * @return JToolBar
     */
    public static JToolBar createToolBar()
    {
        JToolBar tb = new JToolBar();
        tb.setRollover(true);
        tb.setFloatable(false);
        return tb;
    }
    
    
    /**
     * Creates a toolbar specifically to be added to a JHeaderPanel 
     * prepopulated with commonly used text area buttons.
     * 
     * @param textArea JSmartTextArea.
     * @return JToolBar
     */
    public static JToolBar createToolBar(JSmartTextArea textArea)
    {
       JToolBar tb = createToolBar();
       
       tb.add(createToggleButton(
           new LineWrapAction(textArea), 
           textArea, 
           JSmartTextArea.PROP_LINEWRAP));
       
       tb.add(createToggleButton(
           new AutoTailAction(textArea), 
           textArea, 
           JSmartTextArea.PROP_AUTOTAIL));
       
       tb.add(createButton(new ClearAction(textArea)));
       return tb;
    }
    
    
    /**
     * Creates a button specifically for a toolbar to be placed in a 
     * JHeaderPanel.
     * 
     * @param action Action to execute.
     * @return JButton
     */
    public static JButton createButton(Action action)
    {
        JButton jb = new JSmartButton(action);
        prepButton(jb);
        return jb;
    }

    
    /**
     * Common prep procedure for button to play nice in a toolbar.
     * 
     * @param jb Button to prep.
     */
    private static void prepButton(AbstractButton jb)
    {
        jb.setFocusPainted(false);
        jb.setRolloverIcon(jb.getIcon());
        jb.setMargin(new Insets(0, 0, 0, 0));
        jb.setText(null);
    }
    
    
    /**
     * Creates a button specifically for a toolbar to be placed in a 
     * JHeaderPanel.
     * 
     * @param icon Button's icon.
     * @param tooltip Buttons tooltip.
     * @param action Action to execute.
     * @return JButton
     */
    public static JButton createButton(Icon icon, String tooltip, Action action)
    {
        JButton jb = new JSmartButton(action);
        jb.setIcon(icon);
        jb.setToolTipText(tooltip);
        prepButton(jb);
        return jb;
    }


    /**
     * Creates a toggle button specifically for a toolbar to be placed in a 
     * JHeaderPanel.
     * 
     * @param icon Button's icon.
     * @param tooltip Buttons tooltip.
     * @param action Action to execute.
     * @return JToggleButton
     */
    public static JSmartToggleButton createToggleButton(
        Icon icon, 
        String tooltip, 
        Action action)
    {
        JSmartToggleButton jb = new JSmartToggleButton(action);
        jb.setIcon(icon);
        jb.setToolTipText(tooltip);
        prepButton(jb);
        return jb;
    }

    
    /**
     * Creates a toggle button specifically for a toolbar to be placed in a 
     * JHeaderPanel.
     * 
     * @param icon Button's icon.
     * @param tooltip Buttons tooltip.
     * @param action Action to execute.
     * @param propertyChangeSource Source of the property change event that 
     *        will toggle this button.
     * @param property Name of the property change event to listen for.
     * @return JToggleButton
     */
    public static JSmartToggleButton createToggleButton(
        Icon icon, 
        String tooltip, 
        Action action,
        JComponent propertyChangeSource,
        String property)
    {
        JSmartToggleButton jb = createToggleButton(icon, tooltip, action);
        jb.toggleOnProperty(propertyChangeSource, property);
        return jb;
    }
    
    
    /**
     * Creates a button specifically for a toolbar to be placed in a 
     * JHeaderPanel.
     * 
     * @param action Action to execute.
     * @param propertyChangeSource Source of the property change event that 
     *        will toggle this button.
     * @param property Name of the property change event to listen for.
     * @return JSmartToggleButton
     */
    public static JSmartToggleButton createToggleButton(
        Action action, 
        JComponent propertyChangeSource,
        String property)
    {
        JSmartToggleButton jb = new JSmartToggleButton(action);
        prepButton(jb);
        jb.toggleOnProperty(propertyChangeSource, property);
        return jb;
    }

    
    /**
     * @return Returns the gradientPanel.
     */
    protected GradientPanel getGradientPanel()
    {
        return gradientPanel;
    }
}