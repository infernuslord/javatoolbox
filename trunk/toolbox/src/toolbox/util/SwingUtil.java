package toolbox.util;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Window;
import java.util.Enumeration;

import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.apache.log4j.Logger;

import toolbox.util.ui.AntiAliased;

/**
 * Swing Utility Class.
 */
public final class SwingUtil
{
    private static final Logger logger_ = Logger.getLogger(SwingUtil.class);

    //--------------------------------------------------------------------------
    // Static Fields
    //--------------------------------------------------------------------------

    /**
     * Global antialias flag that all 'smart' components are aware of (toolbox
     * java classes that begin with JSmart..
     */
    private static boolean defaultAntiAlias_ = true;

    /**
     * Phantom frame which is invisible.
     */
    private static JFrame phantomFrame_;

    //--------------------------------------------------------------------------
    // Constructors
    //--------------------------------------------------------------------------

    /**
     * Prevent construction.
     */
    private SwingUtil()
    {
    }

    //--------------------------------------------------------------------------
    // Window Stuff
    //--------------------------------------------------------------------------

    /**
     * Sets the size of a window to a given percentage of the users desktop.
     *
     * @param w Window to size.
     * @param percentWidth Percent width from 1 to 100.
     * @param percentHeight Percent height from 1 to 100.
     */
    public static void setWindowSizeAsPercentage(Window w, int percentWidth,
        int percentHeight)
    {
        Dimension desktopSize = w.getToolkit().getScreenSize();

        Dimension windowSize =
            new Dimension(
                (desktopSize.width * percentWidth) / 100,
                (desktopSize.height * percentHeight) / 100);

        w.setSize(windowSize);
    }


    /**
     * Moves the window to the center of the screen.
     *
     * @param w Window to move.
     */
    public static void centerWindow(Window w)
    {
        Dimension d = w.getSize();
        Dimension size = w.getToolkit().getScreenSize();

        int left = size.width / 2 - d.width / 2;
        int top = size.height / 2 - d.height / 2;

        w.setLocation(left, top);
    }


    /**
     * Centers a child window relative to its parent window.
     *
     * @param parent Parent window.
     * @param child Child window.
     */
    public static void centerWindow(Window parent, Window child)
    {
        Dimension parentSize = parent.getSize();
        Dimension childSize = child.getSize();

        Point loc = parent.getLocation();
        child.setLocation(
            (parentSize.width - childSize.width) / 2 + loc.x,
            (parentSize.height - childSize.height) / 2 + loc.y);
    }


    /**
     * Finds the frame for a given component.
     *
     * @param component Component to find parent frame for.
     * @return Frame that component is a child of or null if the component does
     *         not have a parent frame or if the parent frame is not a Frame
     *         (could be a Dialog).
     * @see javax.swing.SwingUtilities#getWindowAncestor(Component)
     */
    public static Frame getFrameAncestor(Component component)
    {
        // Find parent window
        Window w = SwingUtilities.getWindowAncestor(component);

        // Check if frame and return
         return (w != null && w instanceof Frame) ? (Frame) w : new Frame();
    }


    /**
     * Sets a window's size by a given percentage based on the current size. A
     * call with the arguments 40 and -20 respectively would increase the width
     * by 40% and decrease the height by 20%. 
     *
     * @param w Window to expand.
     * @param widthPercentage Percentage to increase the width.
     * @param heightPercentage Percentage to increase the height.
     */
    public static void setSizeAsPercentage(
        Window w,
        int widthPercentage,
        int heightPercentage)
    {
        Dimension d = w.getSize();

        d.width = (int)
            (d.width + (d.width * (widthPercentage / (float) 100)));

        d.height = (int)
            (d.height + (d.height * (heightPercentage / (float) 100)));

        w.setSize(d);
    }

    //--------------------------------------------------------------------------
    // Cursor Stuff
    //--------------------------------------------------------------------------

    /**
     * Sets the cursor to the default cursor on the given component.
     *
     * @param c Component to set the cursor on.
     */
    public static void setDefaultCursor(Component c)
    {
        c.setCursor(Cursor.getDefaultCursor());

        if (c instanceof Container)
        {
            Component[] comps = ((Container) c).getComponents();
            for (int i = 0; i < comps.length; setDefaultCursor(comps[i++]));
        }
    }


    /**
     * Sets the cursor to the wait cursor on the given component.
     *
     * @param c Component to set the cursor on.
     * @return int
     */
    public static int setWaitCursor(Component c)
    {
        int cnt = 1;

        c.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        if (c instanceof Container)
        {
            Component[] comps = ((Container) c).getComponents();
            for (int i = 0; i < comps.length; cnt += setWaitCursor(comps[i++]));
        }

        return cnt;
    }

    //--------------------------------------------------------------------------
    // Desktop Stuff
    //--------------------------------------------------------------------------

    /**
     * Tiles internal frames upon the desktop.
     *
     * @param desktop Desktop on which to tile windows
     *
     * <pre>
     *
     * Based upon the following tiling algorithm:
     *
     * - take the sqroot of the total frames rounded down, that gives
     *   the number of columns.
     *
     * - divide the total frames by the # of columns to get the #
     *   of rows in each column, and any remainder is distributed
     *   amongst the remaining rows from right to left)
     *
     * eg)
     *     1  frame,  remainder 0, 1 row
     *     2  frames, remainder 0, 2 rows
     *     3  frames, remainder 0, 3 rows
     *     4  frames, remainder 0, 2 rows x 2 columns
     *     5  frames, remainder 1, 2 rows in column I,
     *                             3 rows in column II
     *     10 frames, remainder 1, 3 rows in column I,
     *                             3 rows in column II,
     *                             4 rows in column III
     *     16 frames, 4 rows x 4 columns
     *
     * Pseudocode:
     *
     *     while (frames)
     *     {
     *         numCols = (int)sqrt(totalFrames);
     *         numRows = totalFrames / numCols;
     *         remainder = totalFrames % numCols;
     *
     *         if ((numCols-curCol) <= remainder)
     *             numRows++; // add an extra row for this column
     *     }
     *
     * </pre>
     */
    public static void tile(JDesktopPane desktop)
    {
        Rectangle viewP = desktop.getBounds();
        int totalNonIconFrames = 0;
        JInternalFrame[] frames = desktop.getAllFrames();

        for (int i = 0; i < frames.length; i++)
        {
            if (!frames[i].isIcon())
            {
                // don't include iconified frames...
                totalNonIconFrames++;
            }
        }

        int curCol = 0;
        int curRow = 0;
        int i = 0;

        if (totalNonIconFrames > 0)
        {
            // compute number of columns and rows then tile the frames
            int numCols = (int) Math.sqrt(totalNonIconFrames);

            int frameWidth = viewP.width / numCols;

            for (curCol = 0; curCol < numCols; curCol++)
            {
                int numRows = totalNonIconFrames / numCols;
                int remainder = totalNonIconFrames % numCols;

                if ((numCols - curCol) <= remainder)
                    numRows++; // add an extra row for this guy

                int frameHeight = viewP.height / numRows;

                for (curRow = 0; curRow < numRows; curRow++)
                {
                    while (frames[i].isIcon())
                    {
                        // find the next visible frame
                        i++;
                    }

                    frames[i].setBounds(
                        curCol * frameWidth,
                        curRow * frameHeight,
                        frameWidth,
                        frameHeight);

                    i++;
                }
            }
        }
    }


    /**
     * Cascades all internal frames on a desktop.
     *
     * @param desktop Desktop on with to cascade all internal frames.
     */
    public static void cascade(JDesktopPane desktop)
    {
        JInternalFrame[] frames = desktop.getAllFrames();
        JInternalFrame frame;
        int cnt = 0;

        for (int i = frames.length - 1; i >= 0; i--)
        {
            frame = frames[i];

            // Don't include iconified frames in the cascade
            if (!frame.isIcon())
            {
                // Fix me
                frame.setSize(new Dimension(200, 200)
                 /*f.getInitialDimensions()*/);

                frame.setLocation(cascade(desktop, frame, cnt++));
            }
        }
    }


    /**
     * Cascades the given internal frame based upon the current number of
     * internal frames.
     *
     * @param desktop Desktop.
     * @param frame Internal frame to cascade.
     * @return Point object representing the location assigned to the internal
     *         frame upon the virtual desktop.
     */
    public static Point cascade(JDesktopPane desktop, JInternalFrame frame)
    {
        return cascade(desktop, frame, desktop.getAllFrames().length);
    }


    /**
     * Cascades the given internal frame based upon supplied count.
     *
     * @param desktop Desktop upon which frame is visible.
     * @param f Internal frame to cascade.
     * @param count Count to use in cascading the internal frame.
     * @return Point object representing the location assigned to the internal
     *         frame upon the virtual desktop.
     */
    private static Point cascade(
        JDesktopPane desktop,
        JInternalFrame f,
        int count)
    {
        int windowWidth = f.getWidth();
        int windowHeight = f.getHeight();

        int xoffset = 30;
        int yoffset = 30;

        Rectangle viewP = desktop.getBounds();

        // get # of windows that fit horizontally
        int numFramesWide = (viewP.width - windowWidth) / xoffset;

        if (numFramesWide < 1)
        {
            numFramesWide = 1;
        }

        // get # of windows that fit vertically
        int numFramesHigh = (viewP.height - windowHeight) / yoffset;

        if (numFramesHigh < 1)
        {
            numFramesHigh = 1;
        }

        // position relative to the current viewport (viewP.x/viewP.y)
        // (so new windows appear onscreen)
        int xLoc = viewP.x +
                   xoffset *
                   ((count + 1) -
                   (numFramesWide - 1) *
                   (count / numFramesWide));

        int yLoc = viewP.y +
                   yoffset *
                   ((count + 1) -
                   numFramesHigh *
                   (count / numFramesHigh));

        return new Point(xLoc, yLoc);
    }

    //--------------------------------------------------------------------------
    //  Widget/Layout Stuff
    //--------------------------------------------------------------------------

    /**
     * Wraps a component in a JPanel using a flowlayout.
     *
     * @param component Component to wrap.
     * @return JPanel
     */
    public static JPanel wrap(JComponent component)
    {
        JPanel panel = new JPanel(new FlowLayout());
        panel.add(component);
        return panel;
    }


    /**
     * Wraps a component tightly in a JPanel using BorderLayout.
     *
     * @param component Component to wrap.
     * @return JPanel
     */
    public static JPanel wrapTight(JComponent component)
    {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(BorderLayout.CENTER, component);
        return panel;
    }


    /**
     * Converts a JPopupMenu into its equivalent JMenu. The components are
     * cannibalized from the JPopupMenu so it is no longer valid after this
     * call.
     *
     * @param popup Popupmenu to convert.
     * @return JMenu
     */
    public static JMenu popupToMenu(JPopupMenu popup)
    {
        JMenu menu = new JMenu(popup.getLabel());

        while (popup.getComponentCount() > 0)
        {
            Component c = popup.getComponent(0);
            menu.add(c);
            menu.validate();
        }

        return menu;
    }

    //--------------------------------------------------------------------------
    // AntiAliased
    //--------------------------------------------------------------------------

    /**
     * Returns true if antialiasing for all 'smart' components is enabled, false
     * otherwise.
     *
     * @return boolean
     */
    public static boolean getDefaultAntiAlias()
    {
        return defaultAntiAlias_;
    }


    /**
     * Sets the flag for antialiasing all 'smart' components.
     *
     * @param b Antialias flag.
     */
    public static void setDefaultAntiAlias(boolean b)
    {
        defaultAntiAlias_ = b;
    }


    /**
     * Sets the antialiased flag on a tree of components.
     *
     * @param c Root component.
     * @param b Antialias flag.
     */
    public static void setAntiAliased(Component c, boolean b)
    {
        if (c instanceof AntiAliased)
        {
            //logger_.debug(
            //    "AA set to " + b + " on component " +
            //        ClassUtils.getShortClassName(c.getClass()));

            ((AntiAliased) c).setAntiAliased(b);
        }

        if (c instanceof Container)
        {
            Component[] comps = ((Container) c).getComponents();
            for (int i = 0; i < comps.length; setAntiAliased(comps[i++], b));
        }
    }


    /**
     * Turns on antialiasing for a graphics context.
     *
     * @param graphics Graphics context.
     * @param antiAliased Set to true to turn antialiasing on for the graphics
     *        context; false to turn it off.
     */
    public static void makeAntiAliased(Graphics graphics,
        boolean antiAliased)
    {
        //((Graphics2D)g).setRenderingHint
        //  (RenderingHints.KEY_ANTIALIASING,
        //   RenderingHints.VALUE_ANTIALIAS_ON);

        ((Graphics2D) graphics).setRenderingHint(
            RenderingHints.KEY_TEXT_ANTIALIASING,
            (antiAliased ? RenderingHints.VALUE_TEXT_ANTIALIAS_ON
                       : RenderingHints.VALUE_TEXT_ANTIALIAS_OFF));
    }

    //--------------------------------------------------------------------------
    // JTree
    //--------------------------------------------------------------------------

    /**
     * Expands or collapses all the nodes in a tree.
     *
     * @param tree JTree to affect.
     * @param expand True to expand all nodes, false to collapse all nodes.
     */
    public static void expandAll(JTree tree, boolean expand)
    {
        TreeNode root = (TreeNode) tree.getModel().getRoot();
        expandAll(tree, new TreePath(root), expand);
    }


    /**
     * Expands or collapses all the nodes in a tree.
     *
     * @param tree JTree to affect.
     * @param parent The node from which to collapse or expand all children.
     * @param expand True to expand child nodes, false to collapse.
     */
    public static void expandAll(JTree tree, TreePath parent, boolean expand)
    {
        // Traverse children
        TreeNode node = (TreeNode) parent.getLastPathComponent();

        if (node.getChildCount() >= 0)
        {
            for (Enumeration e = node.children(); e.hasMoreElements();)
            {
                TreeNode n = (TreeNode) e.nextElement();
                TreePath path = parent.pathByAddingChild(n);
                expandAll(tree, path, expand);
            }
        }

        // Expansion or collapse must be done bottom-up
        if (expand)
        {
            tree.expandPath(parent);
        }
        else
        {
            tree.collapsePath(parent);
        }
    }

    //--------------------------------------------------------------------------
    // Phantom Frame
    //--------------------------------------------------------------------------

    /**
     * Adds a component to an invisible frame for the purposes of a LAF change
     * propagating to the component before the compnent is made visible.
     *
     * @param c Component to temporarily associate with an invisible frame.
     */
    public static void attachPhantom(Component c)
    {
        if (phantomFrame_ == null)
            phantomFrame_ = new JFrame();

        phantomFrame_.getContentPane().add(c);
    }


    /**
     * Adds a component to an invisible frame for the purposes of a LAF change
     * propagating to the component before the compnent is made visible.
     *
     * @param c Component to temporarily associate with an invisible frame.
     */
    public static void detachPhantom(Component c)
    {
        if (phantomFrame_ == null)
            phantomFrame_ = new JFrame();

        phantomFrame_.getContentPane().remove(c);
    }
}