package toolbox.workspace.host;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;

import org.apache.log4j.Logger;

import toolbox.util.ExceptionUtil;
import toolbox.util.ui.JSmartMenu;
import toolbox.util.ui.JSmartMenuItem;
import toolbox.util.ui.JSmartOptionPane;
import toolbox.workspace.IPlugin;
import toolbox.workspace.PluginException;
import toolbox.workspace.PluginWorkspace;
import toolbox.workspace.WorkspaceAction;

/**
 * Manages multiple plugin hosts and the behavior required to switch between
 * them.
 * <p>
 * <ul>
 *   <li>A recepticle is the only container an external entity (in our case the
 *       PluginWorkspace) needs to expose plugins.
 *   <li>A PluginHostManager manages a multiple PluginHosts.
 *   <li>Only one PluginHost is active at a given time.
 *   <li>A PluginHostManager facilitates the dynamic switching out of one
 *       PluginHost for another at runtime.
 * </ul>
 *
 * @see toolbox.workspace.host.PluginHost
 */
public class PluginHostManager
{
    private static final Logger logger_ =
        Logger.getLogger(PluginHostManager.class);

    //--------------------------------------------------------------------------
    // Constants
    //--------------------------------------------------------------------------

    /**
     * Tab panel plugin host.
     */
    public static final String PLUGIN_HOST_TABBED =
        "toolbox.workspace.host.TabbedPluginHost";

    /**
     * Internal frame plugin host.
     */
    public static final String PLUGIN_HOST_DESKTOP =
        "toolbox.workspace.host.DesktopPluginHost";

    /**
     * Array of all know plugin host types.
     */
    public static final String[] pluginHosts_ = new String[]
    {
        PLUGIN_HOST_TABBED,
        PLUGIN_HOST_DESKTOP
    };

    //--------------------------------------------------------------------------
    // Fields
    //--------------------------------------------------------------------------

    /**
     * The currently active plugin host.
     */
    private PluginHost current_;

    /**
     * Wrapper component for the plugin hosts UI 'component'.
     */
    private JComponent recepticle_;

    /**
     * Reference to the workspace.
     */
    private PluginWorkspace workspace_;

    //--------------------------------------------------------------------------
    // Constructor
    //--------------------------------------------------------------------------

    /**
     * Creates a PluginHostManager for the given workspace.
     *
     * @param workspace Parent workspace.
     */
    public PluginHostManager(PluginWorkspace workspace)
    {
        workspace_ = workspace;
        recepticle_ = new JPanel(new BorderLayout());
    }

    //--------------------------------------------------------------------------
    // Public
    //--------------------------------------------------------------------------

    /**
     * Returns the UI recepticle for the plugin host.
     *
     * @return JComponent
     */
    public JComponent getPluginRecepticle()
    {
        return recepticle_;
    }


    /**
     * Returns the currently active plugin host.
     *
     * @return PluginHost
     */
    public PluginHost getPluginHost()
    {
        return current_;
    }


    /**
     * Creates a plugin hosts given its class name.
     * Use PluginHostManager.HOST_* constants.
     *
     * @param pluginHostClass Class name of plugin host to create.
     * @param props Properties map.
     * @throws PluginException on plugin error.
     */
    public void setPluginHost(String pluginHostClass, Map props)
        throws PluginException
    {
        boolean firstTime = (current_ == null);
        PluginHost previous = current_;

        try
        {
            current_ = (PluginHost)
                Class.forName(pluginHostClass).newInstance();
        }
        catch (InstantiationException e)
        {
            throw new PluginException(e);
        }
        catch (IllegalAccessException e)
        {
            throw new PluginException(e);
        }
        catch (ClassNotFoundException e)
        {
            throw new PluginException(e);
        }

        if (firstTime)
        {
            current_.startup(props);
        }
        else
        {
            transferAssets(previous, current_);
            recepticle_.remove(previous.getComponent());
        }

        recepticle_.add(current_.getComponent());
    }


    /**
     * Creates a menu that exposes functionality of the plugin host manager to
     * the user.
     *
     * @return Newly created menu.
     */
    public JMenu createMenu()
    {
        JMenu menu = new JSmartMenu("Plugin Host");

        for (int i = 0; i < pluginHosts_.length; i++)
        {
            try
            {
                PluginHost pluginHost = (PluginHost)
                    Class.forName(pluginHosts_[i]).newInstance();

                JMenuItem menuItem = new JSmartMenuItem(
                    new ActivatePluginHostAction(pluginHost));

                menu.add(menuItem);
            }
            catch (Exception e)
            {
                ExceptionUtil.handleUI(e, logger_);
            }
        }

        return menu;
    }

    //--------------------------------------------------------------------------
    // Protected
    //--------------------------------------------------------------------------

    /**
     * Transfers the assets from one plugin host to another.
     *
     * @param source Plugin host to transfer assets from.
     * @param dest Plugin host to transfer assets to.
     */
    protected void transferAssets(PluginHost source, PluginHost dest)
    {
        logger_.debug(
            "Transferring " +
            source.getPlugins().length + " plugins from " +
            source.getClass().getName() + " --> " +
            dest.getClass().getName());

        dest.startup(source.getStartupConfig());

        //
        // Transfer over the plugins
        //
        IPlugin[] sourcePlugins = source.getPlugins();

        for (int i = 0; i < sourcePlugins.length; i++)
        {
            IPlugin plugin = sourcePlugins[i];
            source.exportPlugin(plugin);
            dest.importPlugin(plugin);
        }

        //
        // Transfer over the pluginost's listeners
        //
        PluginHostListener[] listeners = source.getPluginHostListeners();

        for (int i = 0; i < listeners.length; i++)
        {
            source.removePluginHostListener(listeners[i]);
            dest.addPluginHostListener(listeners[i]);
        }

        source.shutdown();
    }

    //--------------------------------------------------------------------------
    // ActivatePluginHostAction
    //--------------------------------------------------------------------------

    /**
     * Switches out the current PluginHost for the given PluginHost.
     */
    class ActivatePluginHostAction extends WorkspaceAction
    {
        /**
         * The plugin host to activate.
         */
        private PluginHost newPluginHost_;


        /**
         * Creates an ActivatePluginHostAction.
         *
         * @param pluginHost Plugin host to activate.
         */
        ActivatePluginHostAction(PluginHost pluginHost)
        {
            super(pluginHost.getName(), false, null, null);
            newPluginHost_ = pluginHost;
        }


        /**
         * @see toolbox.util.ui.SmartAction#runAction(
         *      java.awt.event.ActionEvent)
         */
        public void runAction(ActionEvent e) throws Exception
        {
            String selected = newPluginHost_.getClass().getName();

            if (selected.equals(current_.getClass().getName()))
            {
                JSmartOptionPane.showMessageDialog(
                    null, "Plugin host " + selected + " is already active.");
            }

            recepticle_.remove(current_.getComponent());
            transferAssets(current_, newPluginHost_);
            recepticle_.add(newPluginHost_.getComponent());
            current_ = newPluginHost_;
            recepticle_.validate();

            current_.applyPrefs(workspace_.getPreferences());
        }
    }
}