package toolbox.plugin.beanshell;

import java.awt.BorderLayout;
import java.io.IOException;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import bsh.util.JConsole;

import nu.xom.Element;

import org.apache.log4j.Logger;

import toolbox.util.io.JTextAreaOutputStream;
import toolbox.util.ui.JSmartTextArea;
import toolbox.workspace.IPlugin;

/**
 * Beanshell Plugin
 */
public class BeanShellPlugin extends JPanel implements IPlugin
{
    private static final Logger logger_ = 
        Logger.getLogger(BeanShellPlugin.class);
    
    private JSmartTextArea output_;
    private JConsole console_;
    
    
    //--------------------------------------------------------------------------
    // IPlugin Interface 
    //--------------------------------------------------------------------------
    
    /**
     * @see toolbox.workspace.IPlugin#getPluginName()
     */
    public String getPluginName()
    {
        return "Bean Shell";
    }

    /**
     * @see toolbox.workspace.IPlugin#getComponent()
     */
    public JComponent getComponent()
    {
        return this;
    }

    /**
     * @see toolbox.workspace.IPlugin#getDescription()
     */
    public String getDescription()
    {
        return "Beanshell Console";
    }

    /**
     * @see toolbox.workspace.IPlugin#startup(java.util.Map)
     */
    public void startup(Map props)
    {
        buildView();
    }

    /**
     * @see toolbox.workspace.IPlugin#shutdown()
     */
    public void shutdown()
    {
    }

    //--------------------------------------------------------------------------
    // IPreferenced Interface 
    //--------------------------------------------------------------------------
    
    /**
     * @see toolbox.workspace.IPreferenced#applyPrefs(nu.xom.Element)
     */
    public void applyPrefs(Element prefs) throws Exception
    {
    }
    
    /**
     * @see toolbox.workspace.IPreferenced#savePrefs(nu.xom.Element)
     */
    public void savePrefs(Element prefs) throws Exception
    {
    
    }

    //--------------------------------------------------------------------------
    // Protected
    //--------------------------------------------------------------------------
    
    protected void buildView()
    {
        
        output_ = new JSmartTextArea(true, false);
        
        JTextAreaOutputStream taos = new JTextAreaOutputStream(output_);
           
        console_ = new JConsole(System.in, taos);
        
        try
        {
            taos.write("ping".getBytes());
        }
        catch (IOException e)
        {
            logger_.error(e);
        }
        
        JSplitPane splitter = 
            new JSplitPane(JSplitPane.VERTICAL_SPLIT, console_, output_);
            
        setLayout(new BorderLayout());
        add(BorderLayout.CENTER, splitter);
    }
}
