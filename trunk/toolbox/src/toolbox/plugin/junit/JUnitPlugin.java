package toolbox.plugin.junit;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import nu.xom.Attribute;
import nu.xom.Element;

import org.apache.log4j.Logger;
import org.apache.log4j.Priority;

import toolbox.junit.PackageTester;
import toolbox.log4j.JTextAreaAppender;
import toolbox.util.ArrayUtil;
import toolbox.util.ClassUtil;
import toolbox.util.FontUtil;
import toolbox.util.XOMUtil;
import toolbox.util.ui.JHeaderPanel;
import toolbox.util.ui.JSmartButton;
import toolbox.util.ui.JSmartTextField;
import toolbox.util.ui.list.JListPopupMenu;
import toolbox.util.ui.list.JSmartList;
import toolbox.util.ui.list.RegexListModelFilter;
import toolbox.workspace.IPlugin;

/**
 * Simple plugin that allows running of JUnit tests by package.
 */
public class JUnitPlugin extends JPanel implements IPlugin
{
    private static final Logger logger_ = Logger.getLogger(JUnitPlugin.class);

    //--------------------------------------------------------------------------
    // Constants 
    //--------------------------------------------------------------------------
    
    // XML Preferences nodes and attributes. 
    private static final String NODE_JUNIT_PLUGIN = "JUnitPlugin";
    private static final String ATTR_FILTER       = "filter";

    //--------------------------------------------------------------------------
    // Fields 
    //--------------------------------------------------------------------------
    
    /**
     * User enters text to filter down the number of listed packages here.
     */
    private JTextField filterField_;
    
    /**
     * Shows the list of filtered packages. When the filter is .*, all packages
     * are shown.
     */
    private JList packageList_;
    
    /**
     * Model for the pacakge list.
     */
    private DefaultListModel packageModel_;
    
    /**
     * Model that supports regular expressions for the filter.
     */
    private RegexListModelFilter filterModel_;
           
    //--------------------------------------------------------------------------
    // Constructors
    //--------------------------------------------------------------------------

    /**
     * Creates a JUnitPlugin.
     */
    public JUnitPlugin()
    {
    }

    //--------------------------------------------------------------------------
    // Private
    //--------------------------------------------------------------------------

    /**
     * Constructs the user interface.
     */
    protected void buildView()
    {
        // the selection panel
        JPanel selectPanel = new JPanel(new BorderLayout());
        
        packageModel_  = new DefaultListModel();        
        filterModel_ = new RegexListModelFilter(packageModel_);
        
        selectPanel.add(BorderLayout.NORTH, 
            filterField_ = new JSmartTextField(12));
        
        selectPanel.add(BorderLayout.CENTER, 
            new JScrollPane(packageList_ = new JSmartList(filterModel_)));
            
        packageList_.setFont(FontUtil.getPreferredMonoFont());
        new JListPopupMenu(packageList_);
        filterField_.setFont(FontUtil.getPreferredMonoFont());
        filterField_.addKeyListener(new FilterKeyListener());

        // Output panel
        JPanel outputPanel = new JPanel(new BorderLayout());
        JTextAreaAppender appender = new JTextAreaAppender();
        Logger.getRootLogger().addAppender(appender);        
        appender.setThreshold(Priority.DEBUG);
        
        JTextArea area = appender.getTextArea();
        outputPanel.add(BorderLayout.CENTER, new JScrollPane(area));
        area.setFont(FontUtil.getPreferredMonoFont());
        
        // build button panel
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.setBorder(BorderFactory.createEtchedBorder());
        
        buttonPanel.add(
            new JSmartButton(new GetPackageListAction()));
            
        buttonPanel.add(
            new JSmartButton(new TestPackagesAction()));

        // configure the root panel        
        setLayout(new BorderLayout());
        
        add(BorderLayout.WEST, 
            new JHeaderPanel("Test Packages", null, selectPanel));
        
        add(BorderLayout.CENTER, 
            new JHeaderPanel("Output", null, outputPanel));
        
        add(BorderLayout.SOUTH, buttonPanel);        
    }     
    
    
    /**
     * Updates the package list as a result of a change in the filter criteria.
     */
    protected void updatePackageList()
    {
        // Have to do this cuz the filter model is immutable
        packageList_.setModel(new DefaultListModel());
        filterModel_.setRegex(filterField_.getText());
        packageList_.setModel(filterModel_);
        packageList_.scrollRectToVisible(new Rectangle(0, 0, 0, 0));
    }

    //--------------------------------------------------------------------------
    // IPlugin interface
    //--------------------------------------------------------------------------
    
    /**
     * @see toolbox.workspace.IPlugin#startup(Map)
     */
    public void startup(Map params)
    {
        buildView();        
    }


    /**
     * @see toolbox.workspace.IPlugin#getPluginName()
     */
    public String getPluginName()
    {
        return "JUnit";
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
        return "JUnit add-on that enables running of test cases by package.";
    }


    /**
     * @see toolbox.workspace.IPreferenced#applyPrefs(nu.xom.Element)
     */
    public void applyPrefs(Element prefs) throws Exception
    {
        Element root = XOMUtil.getFirstChildElement(
            prefs, NODE_JUNIT_PLUGIN, new Element(NODE_JUNIT_PLUGIN));
        
        filterField_.setText(
            XOMUtil.getStringAttribute(root, ATTR_FILTER, ".*test"));
    }

    
    /**
     * @see toolbox.workspace.IPreferenced#savePrefs(nu.xom.Element)
     */
    public void savePrefs(Element prefs)
    {
        Element root = new Element(NODE_JUNIT_PLUGIN);
        root.addAttribute(new Attribute(ATTR_FILTER, filterField_.getText()));
        XOMUtil.insertOrReplace(prefs, root);    
    }


    /**
     * @see toolbox.workspace.IPlugin#shutdown()
     */
    public void shutdown()
    {
    }

    //--------------------------------------------------------------------------
    // GetPackageListAction
    //--------------------------------------------------------------------------
    
    /**
     * Gets the list of packages on the classpath and populates the package
     * list box.
     */
    class GetPackageListAction extends AbstractAction
    {
        /**
         * Creates a GetPackageListAction.
         */
        public GetPackageListAction()
        {
            super("Get Packages");
            putValue(MNEMONIC_KEY, new Integer('G'));
            putValue(SHORT_DESCRIPTION, 
                "Retrives the list of all know packages on the classpath");
        }

        
        /**
         * @see java.awt.event.ActionListener#actionPerformed(
         *      java.awt.event.ActionEvent)
         */
        public void actionPerformed(ActionEvent e)
        {
            String[] packages = ClassUtil.getPackagesInClasspath();

            packageList_.setModel(new DefaultListModel());
            packageModel_ = new DefaultListModel();
                        
            for (int i = 0; i < packages.length; i++)
                packageModel_.addElement(packages[i]);
            
            filterModel_ = new RegexListModelFilter(packageModel_);
            packageList_.setModel(filterModel_);
            
            updatePackageList();
        }
    }

    //--------------------------------------------------------------------------
    // TestPackagesAction 
    //--------------------------------------------------------------------------
    
    /**
     * Runs the unit tests in the selected packges in the package list box.
     */
    class TestPackagesAction extends AbstractAction
    {
        /**
         * Creates a TestPackagesAction. 
         */
        public TestPackagesAction()
        {
            super("Test Packages");
            putValue(MNEMONIC_KEY, new Integer('T'));
            putValue(SHORT_DESCRIPTION, 
                "Runs JUnit tests in the selected packages");
        }
        
        
        /**
         * @see java.awt.event.ActionListener#actionPerformed(
         *      java.awt.event.ActionEvent)
         */
        public void actionPerformed(ActionEvent e)
        { 
            Object[] selected = packageList_.getSelectedValues();
            PackageTester pt = new PackageTester();
            
            for (int i = 0; i < selected.length; i++)
                pt.addPackage(selected[i].toString());    
                
            logger_.info("Running tests on: " + ArrayUtil.toString(selected));
            
            pt.run();
        }
    }
    
    //--------------------------------------------------------------------------
    //  FilterKeyListener
    //--------------------------------------------------------------------------

    /**
     * Enabled dynamic filtering  of regex as it is typed.
     */    
    class FilterKeyListener extends KeyAdapter
    {
        /**
         * Last known value.
         */
        private String oldValue_ = "";
        
        /**
         * @see java.awt.event.KeyListener#keyReleased(java.awt.event.KeyEvent)
         */
        public void keyReleased(KeyEvent e)
        {
            super.keyReleased(e);
            
            String newValue = filterField_.getText().trim() + e.getKeyChar();
 
            // Only refresh if the filter has changed           
            if (!newValue.equals(oldValue_))
            {                
                oldValue_ = newValue;
                updatePackageList();            
            }
        }
    }
}