package toolbox.plugin.jsourceview;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import nu.xom.Attribute;
import nu.xom.Element;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import toolbox.util.Queue;
import toolbox.util.XOMUtil;
import toolbox.util.ui.ImageCache;
import toolbox.util.ui.JHeaderPanel;
import toolbox.util.ui.JSmartButton;
import toolbox.util.ui.JSmartFileChooser;
import toolbox.util.ui.JSmartLabel;
import toolbox.util.ui.JSmartOptionPane;
import toolbox.util.ui.JSmartTextField;
import toolbox.util.ui.JSmartToggleButton;
import toolbox.util.ui.SmartAction;
import toolbox.util.ui.table.JSmartTable;
import toolbox.util.ui.table.SmartTableModel;
import toolbox.util.ui.table.TableSorter;
import toolbox.util.ui.table.action.AutoTailAction;
import toolbox.workspace.IPreferenced;
import toolbox.workspace.IStatusBar;
import toolbox.workspace.WorkspaceAction;

/**
 * JSourceView gathers statistics on one or more source files and presents them
 * in a table format for viewing.
 */
public class JSourceView extends JPanel implements IPreferenced
{
    // TODO: Figure out how to save table column sizes
    // TODO: Custom table cell renders to color code unusually high or low 
    //       numbers, etc
    
    private static final Logger logger_ = Logger.getLogger(JSourceView.class);

    //--------------------------------------------------------------------------
    // UI Component Names & Labels
    //--------------------------------------------------------------------------

    // Labels
    public static final String LABEL_GO_BUTTON         = "Go!";
    public static final String LABEL_CANCEL_BUTTON     = "Cancel";
    
    // Names
    public static final String NAME_DIRECTORY_FIELD    = "directory.field";
    public static final String NAME_CHART_BUTTON       = "chart.button";
    public static final String NAME_SCROLL_LOCK_BUTTON = "scrollLock.button";
    public static final String NAME_SAVE_BUTTON        = "save.button";

    //--------------------------------------------------------------------------
    // IPreferenced Constants
    //--------------------------------------------------------------------------

    /**
     * Root preferences element.
     */
    private static final String NODE_JSOURCEVIEW_PLUGIN = "JSourceViewPlugin";
    
    /**
     * Attribute of JSourceViewPlugin that stores the current directory.
     */
    private static final String ATTR_LAST_DIR = "dir";

    //--------------------------------------------------------------------------
    // Table Constants
    //--------------------------------------------------------------------------
    
    /**
     * Row number column.
     */
    protected static final int COL_NUM = 0;
    
    /**
     * Directory column.
     */
    protected static final int COL_DIR = 1;
    
    /**
     * File name column.
     */
    protected static final int COL_FILE = 2;
    
    /**
     * Total lines of code column.
     */
    protected static final int COL_CODE = 3;
    
    /**
     * Total lines of comments column.
     */
    protected static final int COL_COMMENTS = 4;
    
    /**
     * Total blank lines column.
     */
    protected static final int COL_BLANK = 5;
    
    /**
     * Total lines thrown out column.
     */
    protected static final int COL_THROWN_OUT = 6;
    
    /**
     * Total number of lines column.
     */
    protected static final int COL_TOTAL = 7;

    /**
     * Percentage of code to comments column.
     */
    protected static final int COL_PERCENTAGE = 8;

    /** 
     * Table column names.
     */    
    protected static String COL_NAMES[] = 
    {
        "Num",
        "Directory", 
        "File", 
        "Code", 
        "Comments", 
        "Blank",
        "Thrown Out", 
        "Total", 
        "Percentage"
    };

    //--------------------------------------------------------------------------
    // Fields
    //--------------------------------------------------------------------------
    
    /**
     * Input field for the base directory to start searching for source code.
     */
    private JTextField dirField_;
    
    /**
     * Button that triggers the search.
     */
    JButton goButton_;
    
    /**
     * Activates the file chooser to allow selection of the search directory.
     */
    private JButton pickDirButton_;
    
    /**
     * Label for the status of the file scanning.
     */
    private JLabel scanStatusLabel_;
    
    /**
     * Label for the status of the file parsing.
     */
    private JLabel parseStatusLabel_;
    
    /**
     * Table that contains the gathered source code statistics.
     */
    private JSmartTable table_;
    
    /**
     * Model for the source code statistics table.
     */
    private SmartTableModel tableModel_;
    
    /**
     * Allows sorting of the table by column.
     */
    private TableSorter tableSorter_;
    
    /**
     * Queue that acts as a pipe between the scanner thread and the parsing
     * thread.
     */
    private Queue workQueue_;
    
    /**
     * Thread that scans the file system looking for source files.
     */
    Thread scanDirThread_;
    
    /**
     * Runnable that implements the behavior necessary to look for source files.
     */
    private SourceScanner scanDirWorker_;
    
    /**
     * Thread that parses source files and gathers statistics.
     */
    private Thread parserThread_;
    
    /**
     * Runnable that implements the behavior necessary to parse source files.
     */
    private SourceParser parserWorker_;

    /** 
     * Workspace status bar (in addition to the two we've already got). 
     */
    private IStatusBar workspaceStatusBar_;

    /**
     * Directory chooser.
     */
    private JSmartFileChooser chooser_;
    
    //--------------------------------------------------------------------------
    // Constructors
    //--------------------------------------------------------------------------
    
    /**
     * Creates a JSourceView.
     */    
    public JSourceView()
    {
        buildView();
    }

    //--------------------------------------------------------------------------
    // Public 
    //--------------------------------------------------------------------------
    
    /**
     * Sets the text of the scan status.
     * 
     * @param status Status of the scan activity.
     */
    public void setScanStatus(String status)
    {
        scanStatusLabel_.setText(status);
    }

    
    /**
     * Sets the text of the parse status.
     * 
     * @param status Status of the parse activity.
     */
    public void setParseStatus(String status)
    {
        parseStatusLabel_.setText(status);
    }

    
    /**
     * Workspace status bar!
     * 
     * @param statusBar Workspace statusbar.
     */
    public void setStatusBar(IStatusBar statusBar)
    {
        workspaceStatusBar_ = statusBar;
        
        // The action depends on havign a reference to the workspace status
        // bar (not available yet in buildView()).
        goButton_.setAction(new SearchAction());
        dirField_.setAction(new SearchAction());
    }

    
    /**
     * Returns the workQueue.
     * 
     * @return Queue
     */
    public Queue getWorkQueue()
    {
        return workQueue_;
    }

    
    /**
     * Returns the tableModel.
     * 
     * @return SmartTableModel
     */
    public SmartTableModel getTableModel()
    {
        return tableModel_;
    }
    
    
    /**
     * Returns the tableSorter.
     * 
     * @return TableSorter
     */
    public TableSorter getTableSorter()
    {
        return tableSorter_;
    }
    
    //--------------------------------------------------------------------------
    // IPreferenced Interface
    //--------------------------------------------------------------------------

    /**
     * @see toolbox.workspace.IPreferenced#applyPrefs(nu.xom.Element)
     */
    public void applyPrefs(Element prefs) throws Exception
    {
        Element root = 
            XOMUtil.getFirstChildElement(
                prefs,
                NODE_JSOURCEVIEW_PLUGIN,
                new Element(NODE_JSOURCEVIEW_PLUGIN));
        
        dirField_.setText(XOMUtil.getStringAttribute(root, ATTR_LAST_DIR, ""));
        dirField_.setCaretPosition(0);
        chooser_.applyPrefs(root);
        table_.applyPrefs(root);
    }

    
    /**
     * @see toolbox.workspace.IPreferenced#savePrefs(nu.xom.Element)
     */
    public void savePrefs(Element prefs) throws Exception
    {
        Element root = new Element(NODE_JSOURCEVIEW_PLUGIN);
        
        root.addAttribute(
            new Attribute(ATTR_LAST_DIR, dirField_.getText().trim()));
       
        chooser_.savePrefs(root);
        table_.savePrefs(root);
        
        XOMUtil.insertOrReplace(prefs, root);
    }

    //--------------------------------------------------------------------------
    // Protected
    //--------------------------------------------------------------------------
    
    /**
     * Constructs the user interface.
     */
    protected void buildView()
    {
        setLayout(new BorderLayout());
        
        dirField_ = new JSmartTextField(35);
        dirField_.setName(NAME_DIRECTORY_FIELD);
        
        goButton_ = new JSmartButton();
        pickDirButton_ = new JSmartButton(new PickDirectoryAction());
        
        JPanel topPanel = new JPanel();
        scanStatusLabel_ = new JSmartLabel(" ");
        parseStatusLabel_ = new JSmartLabel(" ");
        
        topPanel.setLayout(new FlowLayout());
        topPanel.add(new JSmartLabel("Directory"));
        
        GridBagLayout gbl = new GridBagLayout();
        GridBagConstraints gbc = new GridBagConstraints();
        
        Dimension d = new Dimension(12, dirField_.getPreferredSize().height);
        pickDirButton_.setPreferredSize(d);
        
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weightx = 9;
        JPanel p = new JPanel(gbl);
        p.add(dirField_, gbc);
        gbc.gridx = 2;
        gbc.weightx = 1;
        p.add(pickDirButton_, gbc);
        topPanel.add(p);
        topPanel.add(goButton_);

        // Setup sortable table
        tableModel_  = new SmartTableModel(COL_NAMES, 0);
        tableSorter_ = new TableSorter(tableModel_);
        table_       = new JSmartTable(tableSorter_);
        // REMOVE: tableSorter_.addMouseListenerToHeaderInTable(table_);
        tableSorter_.setTableHeader(table_.getTableHeader());
        
        // Set alternating row renderer
        table_.setDefaultRenderer(Integer.class, new SourceTableCellRenderer());
        table_.setDefaultRenderer(String.class, new SourceTableCellRenderer());
        
        tweakTable();
        
        add(topPanel, BorderLayout.NORTH);
        
        JButton pieChart = 
            JHeaderPanel.createButton(
                ImageCache.getIcon(ImageCache.IMAGE_PIE_CHART),
                "Show Pie Chart", 
                new ShowPieChartAction());

        JSmartToggleButton tail = 
            JHeaderPanel.createToggleButton(
                ImageCache.getIcon(ImageCache.IMAGE_LOCK),
                "Auto scroll",
                new AutoTailAction(table_));
        
        tail.toggleOnProperty(table_, JSmartTable.PROP_AUTOTAIL);
        
        JButton save =
            JHeaderPanel.createButton(
                ImageCache.getIcon(ImageCache.IMAGE_SAVE),
                "Save results",
                new SaveResultsAction());
        
        // Set names so unit test can find them
        pieChart.setName(NAME_CHART_BUTTON);
        tail.setName(NAME_SCROLL_LOCK_BUTTON);
        save.setName(NAME_SAVE_BUTTON);
        
        JToolBar tb = JHeaderPanel.createToolBar();
        tb.add(pieChart);
        tb.add(tail);
        tb.add(save);
        
        JHeaderPanel header = 
            new JHeaderPanel(
                "Results", 
                tb, 
                new JScrollPane(table_));
        
        add(header, BorderLayout.CENTER);
        
        JPanel jpanel = new JPanel();
        jpanel.setLayout(new BorderLayout());
        jpanel.add(scanStatusLabel_, BorderLayout.NORTH);
        jpanel.add(parseStatusLabel_, BorderLayout.SOUTH);
        add(jpanel, BorderLayout.SOUTH);
        
        chooser_ = new JSmartFileChooser();
    }
    
    
    /**
     * Tweaks the table columns for width and extents.
     */
    protected void tweakTable()
    {
        table_.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        TableColumnModel columnModel = table_.getColumnModel();

        // Tweak file number column
        TableColumn column = columnModel.getColumn(COL_NUM);
        column.setMinWidth(50);
        column.setPreferredWidth(50);        
        column.setMaxWidth(100); 
        
        column = columnModel.getColumn(COL_CODE);
        column.setMinWidth(50);
        column.setPreferredWidth(50);
        column.setMaxWidth(130);
    
        int min  = 50;
        int pref = 70;
        int max  = 150;
    
        column = columnModel.getColumn(COL_BLANK);
        column.setMinWidth(min);
        column.setPreferredWidth(pref);
        column.setMaxWidth(max);

        column = columnModel.getColumn(COL_COMMENTS);
        column.setMinWidth(min);
        column.setPreferredWidth(pref);
        column.setMaxWidth(max);
        
        column = columnModel.getColumn(COL_THROWN_OUT);
        column.setMinWidth(min);
        column.setPreferredWidth(pref);
        column.setMaxWidth(max);
        
        column = columnModel.getColumn(COL_TOTAL);
        column.setMinWidth(min);
        column.setPreferredWidth(pref);
        column.setMaxWidth(max);
        
        column = columnModel.getColumn(COL_PERCENTAGE);
        column.setMinWidth(min);
        column.setPreferredWidth(pref);
        column.setMaxWidth(max);
    }
    
    //--------------------------------------------------------------------------
    // SearchAction
    //--------------------------------------------------------------------------

    /**
     * Action that triggers the search/scanning/parsing process to produce
     * source code statistics.
     */
    class SearchAction extends WorkspaceAction
    {
        /**
         * Creates a new SearchAction. 
         */
        SearchAction()
        {
            super(LABEL_GO_BUTTON, true, null, workspaceStatusBar_);
        }

        
        /**
         * @see toolbox.util.ui.SmartAction#runAction(
         *      java.awt.event.ActionEvent)
         */
        public void runAction(ActionEvent e) throws Exception
        {
            if (goButton_.getText().equals(LABEL_GO_BUTTON))
            {
                goButton_.setText(LABEL_CANCEL_BUTTON);
                String dir = dirField_.getText();
                workQueue_ = new Queue();
                tableModel_.setRowCount(0);
            
                // To avoid a whole mess of sorting going on while the table is
                // being populated, just disable the sorter temporarily. This
                // is turned back on when the parser thread completes.
                
                // REMOVE: tableSorter_.setEnabled(false);
            
                scanDirWorker_ = 
                    new SourceScanner(JSourceView.this, new File(dir));
                
                scanDirThread_ = new Thread(scanDirWorker_);
                scanDirThread_.start();
            
                parserWorker_  = new SourceParser(JSourceView.this);
                parserThread_  = new Thread(parserWorker_);
                parserThread_.start();
                
                if (scanDirThread_ != null && scanDirThread_.isAlive())
                    scanDirThread_.join();
                
                if (parserThread_ != null && parserThread_.isAlive())    
                    parserThread_.join();
            }
            else
            {
                goButton_.setText(LABEL_GO_BUTTON);
                scanDirWorker_.cancel();
                parserWorker_.cancel();
            
                try
                {
                    scanDirThread_.join();
                    parserThread_.join();
                }
                catch (InterruptedException ie)
                {
                    ; // Ignore
                }
            
                setScanStatus("Operation canceled");
                setParseStatus("");
            }
        }
    }

    //--------------------------------------------------------------------------
    // PickDirectoryAction
    //--------------------------------------------------------------------------
    
    /**
     * Allows user to pick a source directory through the file chooser instead 
     * of typing one in.
     */
    class PickDirectoryAction extends SmartAction
    {
        /**
         * Creates a new PickDirectoryAction. 
         */
        PickDirectoryAction()
        {
            super("...", true, false, null);
        }
        
        
        /**
         * @see toolbox.util.ui.SmartAction#runAction(
         *      java.awt.event.ActionEvent)
         */
        public void runAction(ActionEvent e) throws Exception
        {
            chooser_.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            
            if (chooser_.showDialog(JSourceView.this, "Select Directory") 
                == JFileChooser.APPROVE_OPTION)
            {
                dirField_.setText(
                    chooser_.getSelectedFile().getCanonicalPath());
            }
        }
    }

    //--------------------------------------------------------------------------
    // SaveResultsAction
    //--------------------------------------------------------------------------
    
    /**
     * Saves the results table to a file.
     */
    class SaveResultsAction extends SmartAction
    {
        /**
         * Creates a SaveResultsAction.
         */
        SaveResultsAction()
        {
            super("Save Results ...", true, false, null);
        }
        
        
        /**
         * @see toolbox.util.ui.SmartAction#runAction(
         *      java.awt.event.ActionEvent)
         */
        public void runAction(ActionEvent e) throws Exception
        {
            if (getTableModel().getRowCount() == 0)
            {
                JSmartOptionPane.showMessageDialog(
                    JSourceView.this,
                    "Nothing to save.",
                    (String) getValue(AbstractAction.NAME),
                    JOptionPane.WARNING_MESSAGE);
            }
            else
            {
                String s = JOptionPane.showInputDialog(
                    JSourceView.this,
                    "Save to file",
                    "Save to file",
                    JOptionPane.QUESTION_MESSAGE);
            
                if (!StringUtils.isBlank(s))
                    tableModel_.saveToFile(s);
            }
        }
    }
    
    //--------------------------------------------------------------------------
    // ShowPieChartAction
    //--------------------------------------------------------------------------
    
    /**
     * Shows a pie chart of the statistics results.
     */
    class ShowPieChartAction extends SmartAction
    {
        /**
         * Creates a ShowPieChartAction.
         */
        ShowPieChartAction()
        {
            super("Show Pie Chart", true, false, null);
        }
        
        
        /**
         * @see toolbox.util.ui.SmartAction#runAction(
         *      java.awt.event.ActionEvent)
         */
        public void runAction(ActionEvent e) throws Exception
        {
            if (getTableModel().getRowCount() == 0)
            {
                JSmartOptionPane.showMessageDialog(
                    JSourceView.this,
                    "No statistics availble",
                    (String) getValue(AbstractAction.NAME),
                    JOptionPane.WARNING_MESSAGE);
            }
            else
            {
                PieChart pieChart = new PieChart(parserWorker_.getTotals());
                JSmartOptionPane.showMessageDialog(JSourceView.this, pieChart);
            }
        }
    }

    /**
     * 
     */
    public void destroy()
    {
        tableModel_.destroy();
    }
}