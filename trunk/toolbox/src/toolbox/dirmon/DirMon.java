package toolbox.dirmon;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.apache.log4j.Logger;

import toolbox.util.PropertiesUtil;
import toolbox.util.ResourceUtil;
import toolbox.util.SwingUtil;
import toolbox.util.dirmon.DirectoryMonitor;
import toolbox.util.dirmon.recognizer.FileChangedRecognizer;
import toolbox.util.dirmon.recognizer.FileCreatedRecognizer;
import toolbox.util.dirmon.recognizer.FileDeletedRecognizer;
import toolbox.util.ui.JSmartButton;
import toolbox.util.ui.JSmartFileChooser;
import toolbox.util.ui.JSmartLabel;
import toolbox.util.ui.JSmartOptionPane;
import toolbox.util.ui.JSmartTextField;
import toolbox.util.ui.SmartAction;
import toolbox.util.ui.plaf.LookAndFeelUtil;
import toolbox.util.ui.tabbedpane.JSmartTabbedPane;
import toolbox.util.ui.tabbedpane.SmartTabbedPaneListener;
import toolbox.util.ui.textcomponent.FileAutoCompleter;

/**
 * GUI front end to {@link toolbox.util.dirmon.DirectoryMonitor} with the 
 * following features.
 * <ul>
 *  <li>Pass in list of directories to monitor on the command line.
 *  <li>Pass in a file containing a list of directories to monitor on the
 *      command line using @ symbol.
 *  <li>Tabpanel interface to manage multiple directory monitors.
 *  <li>Sits in the system tray so as not to be intrusive.
 *  <li>Allows monitoring of multiple directories each with its own monitor.
 *  <li>Events generated by a directory monitor go to both single and aggregated
 *      tables.
 *  <li>Control panel to start/stop/pause/resume an existing directory monitor.
 * </ul>
 */
public class DirMon extends JFrame implements SmartTabbedPaneListener {

    // -------------------------------------------------------------------------
    // TODOS
    // -------------------------------------------------------------------------
    
    // TODO: Change event table to show amount of time elapsed since event was
    //       generated.
    
    // TODO: Change the ServiceView/ControllerView to used toggle buttons for
    //       start/stop and suspend/resume instead of two buttons for each.
    //       Also allow optionally icons or text or both.
    
    // TODO: Find some way to save all the preferences set on the UI
    
    // TODO: For directories that are deleted, remove subdirectories from the
    //       list of scanned dirs. THis is really a DirectoryMonitor thing.
    
    // TODO: For directories that are creates, add subdirectories to the list
    //       of scanned dirs. THis is really a DirectoryMonitor thing.
    
    // TODO: Clicking on the toaster should bring the application to the 
    //       foreground
    
    // TODO: Add monitor subdirectory checkbox to gui
    
    private static Logger logger_ = Logger.getLogger(DirectoryMonitor.class);
    
    // -------------------------------------------------------------------------
    // Constants
    // -------------------------------------------------------------------------

    /**
     * Default delay between scans is 30 minutes.
     */
    private static final int DEFAULT_DELAY_MINUTES = 30;

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------
    
    /**
     * Notifies JToaster (responsible for notification windows that show up on
     * the desktop) when directory activity occurs so that it can respond 
     * accordingly.
     */
    private DesktopNotifier desktopNotifier_;
    
    /**
     * Notifies the system tray when it needs to update its tooltip, icon state
     * or anything else.
     */
    private SystemTrayUpdater systemTrayUpdater_;

    /**
     * Debug and error logging generated by all active directory monitors are
     * dumped to this console.
     */
    private ConsoleView consoleView_;

    /**
     * Events generated by all active directory monitors are dumped to this 
     * table.
     */
    private EventTableView allEventsTableView_;

    // -------------------------------------------------------------------------
    // UI Fields
    // -------------------------------------------------------------------------
    
    /**
     * Each directory monitor instance gets its own tab in this tab panel.
     */
    private JSmartTabbedPane tabbedPane_;
    
    /**
     * Button that triggers the creation of a new directory monitor.
     */
    private JSmartButton monitorButton_;

    /**
     * Allows a user to choose a directory to monitor via a file chooser 
     * instead of having to type it in directly.
     */
    private JSmartButton dirChooserButton_;

    /**
     * Textfield that contains the directory to create a new monitor for.
     */
    private JSmartTextField dirField_;
    
    /**
     * Contains number of minutes delay between each directory scan.
     */
    private JSmartTextField delayField_;

    /**
     * Helps the user choose a directory via a custom file chooser.
     */
    private JSmartFileChooser dirChooser_;
    
    // -------------------------------------------------------------------------
    // Main
    // -------------------------------------------------------------------------

    /**
     * Main entrypoint to the directory monitor.
     * 
     * @param args Space delimited set of directories to monitor or an @ symbol
     *        followed by the name of a file containing a list of directories
     *        to monitor. File should contain dirs in the following format:
     *        
     *        <pre>
     *        dirmon.num=2
     *        dirmon.1.name=Docs
     *        dirmon.1.dir=c:\\mydocs
     *        dirmon.2.name=Java Code
     *        dirmon.2.dir=c:\\dev\\java
     *        </pre>
     */
    public static void main(String[] args) {

        try {
            LookAndFeelUtil.setPreferredLAF();
        }
        catch (Exception e) {
            logger_.error("Error setting the preferred look and feel.", e);
        }

        DirMon dirMon = new DirMon();
        dirMon.setVisible(true);
        
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            
            // Read in file containing list of directories to mointor
            if (arg.startsWith("@")) {

                File propsFile = new File(arg.substring(1));

                Properties props = new Properties();
                FileInputStream fis = null;
                
                try {
                    fis = new FileInputStream(propsFile);
                    props.load(fis);
                }
                catch (FileNotFoundException fnfe) {
                    logger_.error("File not found: " + propsFile, fnfe);
                }
                catch (IOException ioe) {
                    logger_.error("IOException : " + propsFile, ioe);
                }
                finally {
                    org.apache.commons.io.IOUtils.closeQuietly(fis);
                }
                
                int numDirs = PropertiesUtil.getInteger(props, "dirmon.num", 0);
                logger_.debug("Number of dirs in props file = " + numDirs);
                
                for (int j = 1; j <= numDirs; j++) {
                    String nameProp = "dirmon." + j + ".name";
                    String dirProp  = "dirmon." + j + ".dir";
                    String name = props.getProperty(nameProp).trim();
                    String dir = props.getProperty(dirProp).trim();
                    addMonitor(dirMon, dir, name);
                }
            }
            
            // Pring help
            else if (arg.startsWith("-h") || arg.startsWith("--help") || arg.startsWith("-?") || arg.startsWith("/?")) {
                System.out.println("Usage: dirmon [<dir> | @<properties file>]");
                System.exit(0);
            }
            
            // Monitor an individual directory passed as an arg on the command line
            else {
                addMonitor(dirMon, arg, arg);
            }
        }
    }

    
    /**
     * @param dirMon
     * @param dirPath
     */
    private static void addMonitor(DirMon dirMon, String dirPath, String name) {
        File dir = new File(dirPath);
        
        if (dir.isDirectory() && dir.canRead()) {
            dirMon.new MonitorDirectoryAction().monitor(name, dir);
        }
        else {
            logger_.error("'" + dirPath + "' is not a valid directory");
        }
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public DirMon() {
        super("Directory Monitor");
        buildView();
    }

    // -------------------------------------------------------------------------
    // Private
    // -------------------------------------------------------------------------
    
    private void buildView() {

        setIconImage(ResourceUtil.getResourceAsImage(
            "/toolbox/util/ui/images/Toolbox.png"));
        
        consoleView_ = new ConsoleView();
        allEventsTableView_ = new EventTableView("Activity");
        systemTrayUpdater_ = new SystemTrayUpdater(this);
        desktopNotifier_ = new DesktopNotifier();
        
        tabbedPane_ = new JSmartTabbedPane(true);
        tabbedPane_.addSmartTabbedPaneListener(this);
        tabbedPane_.addTab("Console", consoleView_);
        tabbedPane_.addTab("All Events", allEventsTableView_);
        
        JPanel inputPanel = new JPanel(new FlowLayout());
        dirField_ = new JSmartTextField(40);
        
        new FileAutoCompleter(dirField_);
        
        delayField_ = new JSmartTextField(DEFAULT_DELAY_MINUTES + "", 4);
        monitorButton_ = new JSmartButton(new MonitorDirectoryAction());
        dirChooserButton_ = new JSmartButton(new PickDirectoryAction());

        inputPanel.add(new JSmartLabel("Directory"));
        inputPanel.add(dirField_);
        inputPanel.add(dirChooserButton_);
        inputPanel.add(monitorButton_);
        inputPanel.add(new JSmartLabel("Delay"));
        inputPanel.add(delayField_);
        inputPanel.add(new JSmartLabel("minutes"));
        
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(BorderLayout.CENTER, tabbedPane_);
        contentPane.add(BorderLayout.NORTH, inputPanel);

        SwingUtil.setSizeAsDesktopPercentage(this, 50, 50); 
        SwingUtil.centerWindow(this);
    }

    // -------------------------------------------------------------------------
    // SmartTabbedPaneListener Interface
    // -------------------------------------------------------------------------
    
    /**
     * When a tab is closed, deregister the console and event table views from
     * the directory monitor associated with that tab.
     * 
     * who is responsible..
     * 
     * when the tab is closed, the service should destroy itself.
     * 
     * who should listen for the tab closing and who should tell the service to
     * destroy and clean up after itself?
     * 
     * @see toolbox.util.ui.tabbedpane.SmartTabbedPaneListener#tabClosing(toolbox.util.ui.tabbedpane.JSmartTabbedPane, int)
     */
    public void tabClosing(JSmartTabbedPane tabbedPane, int tabIndex) {
        
        Component source = tabbedPane.getComponentAt(tabIndex);
        
        if (source instanceof SingleMonitorView) {
            SingleMonitorView singleView = (SingleMonitorView) source;
            DirectoryMonitor monitor = singleView.getMonitor();
            monitor.destroy();
        }
    }
    
    // -------------------------------------------------------------------------
    // MonitorDirectoryAction
    // -------------------------------------------------------------------------

    /**
     * Action to create and start a directory monitor for the currently 
     * entered directory.
     */
    public class MonitorDirectoryAction extends SmartAction {

        public MonitorDirectoryAction() {
            super("Monitor", true, true, null);
        }

        
        /*
         * @see toolbox.util.ui.SmartAction#runAction(java.awt.event.ActionEvent)
         */
        public void runAction(ActionEvent e) throws Exception {
            File f = new File(dirField_.getText().trim());

            if (f.isDirectory()) {
                monitor(f.getAbsolutePath(), f);
            }
            else {
                JSmartOptionPane.showMessageDialog(
                    DirMon.this, "Not a valid directory");
            }
        }

        
        /**
         * Callable when not initiated by a GUI event. Subdirs are monitored
         * by default.
         * 
         * @param f Directory to monitor.
         */
        public void monitor(String displayName, File f) {
            DirectoryMonitor dm = new DirectoryMonitor(f, displayName, true);
            String delayValue = delayField_.getText().trim();
            
            // seconds
            if (delayValue.endsWith("s")) {
                delayValue = delayValue.substring(0, delayValue.length() - 1);
                dm.setDelay(Integer.parseInt(delayValue) * 1000);
            }
            
            // minutes
            else {
                dm.setDelay(Integer.parseInt(delayValue) * 1000 * 60);
            }
            
            // Events for the directory monitor to recognize
            dm.addRecognizer(new DropUselessEventsRecognizer(new FileCreatedRecognizer(dm)));
            dm.addRecognizer(new DropUselessEventsRecognizer(new FileDeletedRecognizer(dm)));
            dm.addRecognizer(new DropUselessEventsRecognizer(new FileChangedRecognizer(dm)));
            
            // Components interested in directory monitor events
            dm.addDirectoryMonitorListener(systemTrayUpdater_);
            dm.addDirectoryMonitorListener(desktopNotifier_);
            dm.addDirectoryMonitorListener(consoleView_);
            dm.addDirectoryMonitorListener(allEventsTableView_);
            SingleMonitorView singleView = new SingleMonitorView(dm);
            
            dm.start();
            
            // TODO: Fix me
            // has to be after service is started cuz ServiceView (which
            // is a aggregated by SingleMonitorView) requires that the
            // service state be something
            
            
            //String dirName = dm.getName(); 
            //dm.getMonitoredDirectories().iterator().next().toString();
            
            tabbedPane_.insertTab(
                dm.getName(),                            // friendly name as tab name
                null,
                singleView,
                dm.getRootDirectory().getAbsolutePath(), // full dir name as tooltip
                0);                                      // insert as first tab
        };
    }

    // -------------------------------------------------------------------------
    // PickDirectoryAction
    // -------------------------------------------------------------------------
    
    /**
     * Allows user to pick a source directory through the file chooser instead 
     * of typing one in.
     */
    class PickDirectoryAction extends SmartAction {

        PickDirectoryAction() {
            super("...", true, false, null);
        }


        /*
         * @see toolbox.util.ui.SmartAction#runAction(java.awt.event.ActionEvent)
         */
        public void runAction(ActionEvent e) throws Exception {
            
            if (dirChooser_ ==  null) {
                dirChooser_ = new JSmartFileChooser();
                dirChooser_.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            }

            if (dirChooser_.showDialog(DirMon.this, "Select Directory") == 
                JFileChooser.APPROVE_OPTION) {
                
                dirField_.setText(
                    dirChooser_.getSelectedFile().getCanonicalPath());
            }
        }
    }    
}