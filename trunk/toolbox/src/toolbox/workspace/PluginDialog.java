package toolbox.workspace;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;

import nu.xom.Builder;
import nu.xom.Element;
import nu.xom.Elements;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ClassUtils;
import org.apache.log4j.Logger;
import org.apache.regexp.RESyntaxException;

import toolbox.findclass.FindClass;
import toolbox.findclass.FindClassResult;
import toolbox.util.ExceptionUtil;
import toolbox.util.ResourceUtil;
import toolbox.util.SwingUtil;
import toolbox.util.XOMUtil;
import toolbox.util.collections.ObjectComparator;
import toolbox.util.ui.JSmartButton;
import toolbox.util.ui.JSmartDialog;
import toolbox.util.ui.JSmartLabel;
import toolbox.util.ui.action.DisposeAction;
import toolbox.util.ui.list.JListPopupMenu;
import toolbox.util.ui.list.JSmartList;

/**
 * Dialog that allows user to add/remove/find plugins. Not used too much anymore
 * since the addition of the PluginMenu to the workspace menubar.
 * 
 * @see toolbox.workspace.PluginWorkspace
 */
public class PluginDialog extends JSmartDialog
{
    private static final Logger logger_ = Logger.getLogger(PluginDialog.class);

    //--------------------------------------------------------------------------
    // Constants
    //--------------------------------------------------------------------------

    /**
     * Contains static list of all the plugins.
     */
    public static final String FILE_PLUGINS = "/toolbox/workspace/plugins.xml";

    /**
     * Plugin Node from plugins.xml.
     */
    public static final String NODE_PLUGIN = "Plugin";

    /**
     * Class Attribute from plugins.xml.
     */
    public static final String ATTR_CLASS = "class";

    //--------------------------------------------------------------------------
    // Fields
    //--------------------------------------------------------------------------

    /**
     * Button to remove a plugin from the workspace.
     */
    private JButton removeButton_;

    /**
     * Button to add a plugin to the workspace.
     */
    private JButton addButton_;

    /**
     * Workspace that is the parent of this dialog box.
     */
    private PluginWorkspace workspace_;

    /**
     * List of active (loaded) plugins in the workspace.
     */
    private JList activeList_;

    /**
     * List of inactive plugins that can be added to the workspace.
     */
    private JList inactiveList_;

    /**
     * List model for the list of active plugins.
     */
    private DefaultListModel activeModel_;

    /**
     * List model for the list of inactive plugins.
     */
    private DefaultListModel inactiveModel_;

    //--------------------------------------------------------------------------
    // Constructors
    //--------------------------------------------------------------------------

    /**
     * Creates dialog to add/remove plugins from the workspace.
     *
     * @param parent Plugin workspace.
     */
    protected PluginDialog(PluginWorkspace parent)
    {
        super(parent, "Manage Plugins", false);
        workspace_ = parent;
        buildView();
        pack();
        SwingUtil.centerWindow(parent, this);
        populateActive();
    }

    //--------------------------------------------------------------------------
    // Protected
    //--------------------------------------------------------------------------

    /**
     * Constructs the user interface.
     */
    protected void buildView()
    {
        Dimension prefSize = new Dimension(150, 200);

        Border border = new CompoundBorder(
            BorderFactory.createEmptyBorder(0, 10, 10, 10),
            BorderFactory.createEtchedBorder());

        // List of active plugins
        activeList_ = new JSmartList();
        activeModel_ = new DefaultListModel();
        activeList_.setModel(activeModel_);
        new JListPopupMenu(activeList_);

        JScrollPane activeScroller = new JScrollPane(activeList_);
        activeScroller.setPreferredSize(prefSize);
        activeScroller.setBorder(border);

        // List of found/removed plugins
        inactiveList_ = new JSmartList();
        inactiveModel_ = new DefaultListModel();
        inactiveList_.setModel(inactiveModel_);
        new JListPopupMenu(inactiveList_);

        JScrollPane inactiveScroller = new JScrollPane(inactiveList_);
        inactiveScroller.setPreferredSize(prefSize);
        inactiveScroller.setBorder(border);

        // Add/Remove buttons to move plugins between the lists
        JPanel midButtonPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridx      = 1;
        gbc.gridy      = 1;
        gbc.fill       = GridBagConstraints.HORIZONTAL;
        gbc.weightx    = 1;
        gbc.weighty    = 1;
        gbc.anchor     = GridBagConstraints.SOUTH;
        gbc.insets     = new Insets(5, 0, 5, 0);
        midButtonPanel.add(
            addButton_ = new JSmartButton(new AddNewPluginAction()), gbc);

        gbc.gridy++;
        gbc.anchor     = GridBagConstraints.NORTH;
        midButtonPanel.add(
            removeButton_ = new JSmartButton(new RemovePluginAction()), gbc);

        // Lists and buttons
        JPanel listPanel = new JPanel(new GridBagLayout());
        gbc.gridx      = 1;
        gbc.gridy      = 1;
        gbc.fill       = GridBagConstraints.BOTH;
        gbc.anchor     = GridBagConstraints.SOUTH;
        gbc.gridheight = 1;
        gbc.gridwidth  = 1;
        gbc.insets     = new Insets(0, 0, 0, 0);
        JLabel inactiveLabel = new JSmartLabel("Inactive Plugins");
        inactiveLabel.setHorizontalAlignment(SwingConstants.CENTER);
        inactiveLabel.setVerticalAlignment(SwingConstants.BOTTOM);
        listPanel.add(inactiveLabel, gbc);

        gbc.gridx  = 1;
        gbc.gridy  = 2;
        gbc.insets = new Insets(5, 0, 5, 0);
        gbc.anchor = GridBagConstraints.CENTER;
        listPanel.add(inactiveScroller, gbc);

        gbc.gridx = 2;
        gbc.gridy = 2;
        gbc.weightx = 0.5;
        listPanel.add(midButtonPanel, gbc);

        gbc.gridx      = 3;
        gbc.gridy      = 1;
        gbc.weightx    = 1.0;
        gbc.anchor     = GridBagConstraints.CENTER;
        gbc.insets     = new Insets(0, 0, 0, 0);
        JLabel activeLabel = new JSmartLabel("Active Plugins");
        activeLabel.setHorizontalAlignment(SwingConstants.CENTER);
        activeLabel.setVerticalAlignment(SwingConstants.BOTTOM);
        listPanel.add(activeLabel, gbc);

        gbc.gridx  = 3;
        gbc.gridy  = 2;
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.insets = new Insets(5, 0, 5, 0);
        listPanel.add(activeScroller, gbc);

        // Find/Close Buttons on bottom
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(new JSmartButton(new ListPluginsAction()));
        buttonPanel.add(new JSmartButton(new FindPluginsAction()));
        buttonPanel.add(new JSmartButton(new DisposeAction("Close", this)));

        // Glue everything together in the root pane
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(BorderLayout.CENTER, listPanel);
        getContentPane().add(BorderLayout.SOUTH, buttonPanel);
    }


    /**
     * Populates the active plugins list based on currently loaded plugins.
     */
    protected void populateActive()
    {
        activeModel_.clear();

        for (int i = 0; i < workspace_.getPluginHost().getPlugins().length; i++)
        {
            IPlugin plugin = workspace_.getPluginHost().getPlugins()[i];
            PluginMeta meta = new PluginMeta(plugin);
            activeModel_.addElement(meta);
        }

        removeButton_.setEnabled(activeModel_.size() > 0);
        addButton_.setEnabled(inactiveModel_.size() > 0);
        activeList_.setSelectedIndex(0);
    }


    /**
     * Populates the inactive plugins list based on currently available
     * plugins visible on the classpath.
     */
    protected void populateInactive()
    {
        inactiveModel_.clear();
        FindClass fc = new FindClass();
        FindClassResult[] candidatePlugins = new FindClassResult[0];
        List legitPlugins = new ArrayList();

        try
        {
            // Find classes that end in "Plugin"
            candidatePlugins = fc.findClass("Plugin$", true);
        }
        catch (RESyntaxException mpe)
        {
            ExceptionUtil.handleUI(mpe, logger_);
        }
        catch (IOException ioe)
        {
            ExceptionUtil.handleUI(ioe, logger_);
        }

        logger_.debug("Results: " + candidatePlugins.length);

        for (int i = 0; i < candidatePlugins.length; i++)
        {
            String clazz     = candidatePlugins[i].getClassFQN();
            String clazzOnly = ClassUtils.getShortClassName(clazz);

            logger_.debug(clazzOnly + " : Inspecting...");

            boolean skip = false;

            // Exclude the plugins that are already loaded
            for (int j = 0; j < workspace_.getPluginHost().getPlugins().length;
                 j++)
            {
                String pluginClass =
                    workspace_.getPluginHost()
                              .getPlugins()[j]
                              .getClass()
                              .getName();

                if (pluginClass.equals(clazz))
                {
                    skip = true;
                    break;
                }
            }

            // Exclude plugins that already occur in the inactive list
            for (int j = 0, n = legitPlugins.size(); j < n; j++)
            {
                PluginMeta pm = (PluginMeta) legitPlugins.get(j);

                if (pm.getClassName().equals(clazz))
                {
                    logger_.debug(clazzOnly + " : Excluding duplicate class");
                    skip = true;
                    break;
                }
            }

            if (!skip)
            {
                logger_.debug(clazzOnly + " : Passed already loaded check");

                Object plugin = null;

                // Exclude plugins that can't be instantiated for whatever
                // reason
                try
                {
                    plugin = Class.forName(clazz).newInstance();
                    logger_.debug(clazzOnly + " : Passed instantiation check");
                }
                catch (Throwable t)
                {
                    logger_.debug(clazzOnly + " : Failed newInstance() : " +
                        t.getMessage());

                    skip = true;
                }

                // Make sure plugin class implements the IPlugin interface
                if (!skip)
                {
                    if (plugin instanceof IPlugin)
                    {
                        logger_.debug(clazzOnly + " : We have a plugin!!!");
                        legitPlugins.add(new PluginMeta((IPlugin) plugin));
                    }
                }
            }
        }

        Collections.sort(legitPlugins, new ObjectComparator("name"));

        for (int i = 0; i < legitPlugins.size(); i++)
            inactiveModel_.addElement(legitPlugins.get(i));

        removeButton_.setEnabled(activeModel_.size() > 0);
        addButton_.setEnabled(inactiveModel_.size() > 0);
    }

    //--------------------------------------------------------------------------
    // Public Static
    //--------------------------------------------------------------------------

    /**
     * Returns a list of plugins read from plugins.xml.
     *
     * @return List[PluginMeta]
     */
    public static List getPluginList()
    {
        InputStream is = null;
        List pluginList = new ArrayList();

        try
        {
            is = ResourceUtil.getResource(FILE_PLUGINS);
            Element root = new Builder().build(is).getRootElement();
            Elements plugins = root.getChildElements(NODE_PLUGIN);

            for (int i = 0; i < plugins.size(); i++)
            {
                String clazz =
                    XOMUtil.getStringAttribute(
                        plugins.get(i), ATTR_CLASS, null);

                if (clazz != null)
                    pluginList.add(new PluginMeta(clazz));
            }
        }
        catch (Exception ioe)
        {
            ExceptionUtil.handleUI(ioe, logger_);
        }
        finally
        {
            IOUtils.closeQuietly(is);
        }

        return pluginList;
    }


    //--------------------------------------------------------------------------
    // AddNewPluginAction
    //--------------------------------------------------------------------------

    /**
     * Adds a plugin - moves the plugin from the inactive list to the active
     * list.
     */
    class AddNewPluginAction extends AbstractAction
    {
        /**
         * Creates a AddNewPluginAction.
         */
        AddNewPluginAction()
        {
            super("Add Plugin >>");
            putValue(Action.MNEMONIC_KEY, new Integer('A'));
        }


        /**
         * @see java.awt.event.ActionListener#actionPerformed(
         *      java.awt.event.ActionEvent)
         */
        public void actionPerformed(ActionEvent e)
        {
            int sizeBefore = inactiveModel_.size();
            int[] selectedIdx = inactiveList_.getSelectedIndices();
            Object[] selected = inactiveList_.getSelectedValues();

            for (int i = 0; i < selected.length; i++)
            {
                try
                {
                    workspace_.registerPlugin(
                        ((PluginMeta) selected[i]).getClassName());

                    inactiveModel_.removeElement(selected[i]);
                }
                catch (Exception ex)
                {
                    ExceptionUtil.handleUI(ex, logger_);
                }
            }

            populateActive();

            // Inactive list selection
            if (selectedIdx[selected.length - 1] == sizeBefore - 1)
                inactiveList_.setSelectedIndex(
                    sizeBefore - selected.length - 1);
            else
                inactiveList_.setSelectedIndex(selectedIdx[0]);

            // Active list selection
            int[] selectedActive = new int[selected.length];

            for (int i = 0, n = activeModel_.size() - selected.length;
                i < selected.length; i++, n++)
            {
                selectedActive[i] = n;
            }

            activeList_.setSelectedIndices(selectedActive);
        }
    }

    //--------------------------------------------------------------------------
    // RemovePluginAction
    //--------------------------------------------------------------------------

    /**
     * Deactivates/removes a plugin and moves the plugin from the active list
     * to the inactive list.
     */
    class RemovePluginAction extends AbstractAction
    {
        /**
         * Creates a RemovePluginAction.
         */
        RemovePluginAction()
        {
            super("<< Remove Plugin");
            putValue(Action.MNEMONIC_KEY, new Integer('R'));
        }


        /**
         * @see java.awt.event.ActionListener#actionPerformed(
         *      java.awt.event.ActionEvent)
         */
        public void actionPerformed(ActionEvent e)
        {
            int sizeBefore = activeModel_.size();
            int[] selectedIdx = activeList_.getSelectedIndices();
            Object[] selected = activeList_.getSelectedValues();

            try
            {
                // Deregister selected plugins
                for (int i = 0; i < selected.length; i++)
                {
                    workspace_.deregisterPlugin(
                        ((PluginMeta) selected[i]).getClassName(), true);

                    inactiveModel_.addElement(selected[i]);
                }
            }
            catch (Exception ex)
            {
                ExceptionUtil.handleUI(ex, logger_);
            }

            populateActive();

            // Active list selection
            if (selectedIdx[selected.length - 1] == sizeBefore - 1)
                activeList_.setSelectedIndex(sizeBefore - selected.length - 1);
            else
                activeList_.setSelectedIndex(selectedIdx[0]);

            // Inactive list selection
            int[] selectedInactive = new int[selected.length];

            for (int i = 0, n = inactiveModel_.size() - selected.length;
                i < selected.length; i++, n++)
            {
                selectedInactive[i] = n;
            }

            inactiveList_.setSelectedIndices(selectedInactive);
        }
    }

    //--------------------------------------------------------------------------
    // FindPluginsAction
    //--------------------------------------------------------------------------

    /**
     * Finds plugins on the classpath and populates the inactive plugins list.
     */
    class FindPluginsAction extends WorkspaceAction
    {
        /**
         * Creates a FindPluginsAction.
         */
        FindPluginsAction()
        {
            super("Find Plugins",
                  true,
                  PluginDialog.this,
                  workspace_.getStatusBar());

            putValue(Action.MNEMONIC_KEY, new Integer('F'));
        }


        /**
         * @see toolbox.util.ui.SmartAction#runAction(
         *      java.awt.event.ActionEvent)
         */
        public void runAction(ActionEvent e)
        {
            populateInactive();
        }
    }

    //--------------------------------------------------------------------------
    // ListPluginsAction
    //--------------------------------------------------------------------------

    /**
     * Lists the plugins from a predetermined list for cases where the
     * classpath can't be accessed or search efficiently (WebStart).
     */
    class ListPluginsAction extends WorkspaceAction
    {
        /**
         * Creates a ListPluginsAction.
         */
        ListPluginsAction()
        {
            super("List Plugins",
                  true,
                  PluginDialog.this,
                  workspace_.getStatusBar());

            putValue(Action.MNEMONIC_KEY, new Integer('L'));
        }


        /**
         * @see toolbox.util.ui.SmartAction#runAction(
         *      java.awt.event.ActionEvent)
         */
        public void runAction(ActionEvent e)
        {
            inactiveModel_.clear();
            List legitPlugins = getPluginList();

            // Exclude the plugins that are already loaded
            for (int j = 0;
                j < workspace_.getPluginHost().getPlugins().length;
                j++)
            {
                String pluginClass =
                    workspace_
                        .getPluginHost()
                        .getPlugins()[j]
                        .getClass()
                        .getName();

                for (int i = 0; i < legitPlugins.size(); i++)
                {
                    PluginMeta meta = (PluginMeta) legitPlugins.get(i);
                    if (meta.getClassName().equals(pluginClass))
                        legitPlugins.remove(meta);
                }
            }

            Collections.sort(legitPlugins, new ObjectComparator("name"));

            for (int i = 0; i < legitPlugins.size(); i++)
                inactiveModel_.addElement(legitPlugins.get(i));

            removeButton_.setEnabled(activeModel_.size() > 0);
            addButton_.setEnabled(inactiveModel_.size() > 0);
        }
    }
}