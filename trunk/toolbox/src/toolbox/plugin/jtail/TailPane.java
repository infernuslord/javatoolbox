package toolbox.jtail;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;
import org.apache.log4j.TTCCLayout;
import org.apache.log4j.WriterAppender;
import org.apache.regexp.RESyntaxException;

import toolbox.jtail.config.ITailPaneConfig;
import toolbox.jtail.filter.CutLineFilter;
import toolbox.jtail.filter.ILineFilter;
import toolbox.jtail.filter.LineNumberDecorator;
import toolbox.jtail.filter.RegexLineFilter;
import toolbox.tail.Tail;
import toolbox.tail.TailAdapter;
import toolbox.util.ExceptionUtil;
import toolbox.util.StringUtil;
import toolbox.util.SwingUtil;
import toolbox.util.concurrent.BatchingQueueReader;
import toolbox.util.concurrent.BlockingQueue;
import toolbox.util.concurrent.IBatchingQueueListener;
import toolbox.util.io.NullWriter;
import toolbox.util.ui.JSmartTextArea;
import toolbox.util.ui.plugin.IStatusBar;

/**
 * A UI component that serves as the view for the tailing of a single file. 
 */
public class TailPane extends JPanel
{
    /*
     * TODO: Figure why anti alias select from font chooser dialog does not get
     *       applied/persisted to the tailpane!
     * TODO: Color code keywords
     * TODO: Color code time lapse delays
     * TODO: Move button panel to its own flippane
     * TODO: Add option to tail the whole file from the beginning
     * TODO: Create filter that will accept a beanshell script 
     */
     
    private static final Logger logger_ = 
        Logger.getLogger(TailPane.class);
    
    /** Special tail type for System.out */
    public static final String LOG_SYSTEM_OUT = "[System.out]";
    
    /** Specital tail type for Log4J */
    public static final String LOG_LOG4J = "[Log4J]";

    /** Start button text for dual action button start/stop */
    private static final String MODE_START = "Start";
    
    /** Stop button text for dual action button start/stop */
    private static final String MODE_STOP  = "Stop";

    /** Reference to workspace status bar */
    private IStatusBar statusBar_;
    
    /** Tail output is appended into this text area */
    private JSmartTextArea tailArea_;
    
    /** Clears the output text area */
    private JButton clearButton_;
    
    /** Dual action button that handles pause/unpause of tail */
    private JButton pauseButton_;
    
    /** Dual action button that handles start/stop of tail */
    private JButton startButton_;
    
    /** Closes the tail (also triggers adding the tail to the recent menu) */
    private JButton closeButton_;
    
    /** Checkbox to toggle auto scrolling of the output text area */
    private JCheckBox autoScrollBox_;
    
    /** Checkbox to toggles the inclusion of lines numbers in the tail output*/
    private JCheckBox lineNumbersBox_;
    
    /** Regular expression filter field that includes matching lines */ 
    private JTextField regexField_;
    
    /** Cut expression filter field that chops columns from a line */
    private JTextField cutField_;

    /** Lines are places in this queue for the UI component to pick up from */
    private BlockingQueue queue_;
    
    /** Optimization to read lines from the queue in batch instead of one'zies*/ 
    private BatchingQueueReader queueReader_;
    
    /** Listener for queue events */
    private TailQueueListener queueListener_;
    
    /** List of filters that are applied to each line */
    private List filters_ = new ArrayList();
    
    /** Filter that includes lines matching a regular expression */ 
    private RegexLineFilter regexFilter_;
    
    /** Filter that cuts columns from a line */
    private CutLineFilter cutFilter_;
    
    /** Filter that adds a line number to the beginning of each line */
    private LineNumberDecorator lineNumberDecorator_;

    /** The non-UI tail component */    
    private Tail tail_;

    /** Tail configuration */
    private ITailPaneConfig config_;
    
    //--------------------------------------------------------------------------
    //  Constructors
    //--------------------------------------------------------------------------
        
    /** 
     * Creates a TailPane with the given configuration
     * 
     * @param   config      Details of the tail configuration
     * @param   statusBar   Status bar
     * @throws  IOException if an IO error occurs
     * @throws  FileNotFoundException if file not found
     */
    public TailPane(ITailPaneConfig config, IStatusBar statusBar) 
        throws IOException, FileNotFoundException
    {
        statusBar_ = statusBar;
        buildView(config);
        buildFilters();        
        setConfiguration(config);                
        
        // Start tail through action so button states are OK
        if (config_.isAutoStart())
            startButton_.getAction().actionPerformed(
                new ActionEvent(this, 0, "Start"));
        
    }
    
    //--------------------------------------------------------------------------
    // Protected
    //--------------------------------------------------------------------------
    
    /**
     * Initializes the tail
     * 
     * @param  file  File to tail
     * @throws FileNotFoundException if the file to tail is non-existant
     * @throws IOException if problems occur tailed System.out
     */
    protected void init() throws IOException, FileNotFoundException
    {
        //
        //                      Queue 
        //                        ^
        //                        |  
        //                        |   pops lines from (individually)
        //                        |
        //                BatchingQueueReader
        //                        |
        //                        |
        //                        |   provides lines to (batched)
        //                        |
        //                        v
        //                TailQueueListener
        //
        
        String file = config_.getFilename();
        
        queue_          = new BlockingQueue();
        queueListener_  = new TailQueueListener();        
        queueReader_    = new BatchingQueueReader(
                              queue_,file + "-BatchingQueueReader");
                                        
        queueReader_.addBatchingQueueListener(queueListener_);
        queueReader_.start();
        
        // Setup tail
        tail_ = new Tail();
        tail_.addTailListener(new TailListener());
         
        if (file.equals(LOG_SYSTEM_OUT))
        {
            PipedOutputStream pos = new PipedOutputStream();
            PipedInputStream  pis = new PipedInputStream(pos);
            PrintStream ps = new PrintStream(pos, true);
            System.setOut(ps);
            tail_.follow(new InputStreamReader(pis), new NullWriter(), file);
            logger_.debug("Tailing System.out...");
        }
        else if (file.equals(LOG_LOG4J))
        {
            PipedOutputStream pos = new PipedOutputStream();
            PipedInputStream  pis = new PipedInputStream(pos);
            WriterAppender appender = new WriterAppender(new TTCCLayout(), pos);
            appender.setImmediateFlush(true);
            appender.setThreshold(Priority.DEBUG);
            appender.setName("toolbox-stream-appender");
            LogManager.getLogger("toolbox").addAppender(appender);
            tail_.follow(new InputStreamReader(pis), new NullWriter(), file);
            logger_.debug("Tailing Log4J...");                
        }
        else
        {
            tail_.follow(new File(file), new NullWriter());
        }
    }
    
    /**
     * Builds the GUI
     */    
    protected void buildView(ITailPaneConfig config)
    {
        tailArea_ = new JSmartTextArea("");
        tailArea_.setFont(SwingUtil.getPreferredMonoFont());
        
        clearButton_    = new JButton(tailArea_.new ClearAction());
        pauseButton_    = new JButton(new PauseUnpauseAction());
        
        String startMode =  
            config.isAutoStart() ? MODE_START : MODE_STOP;
        
        startButton_    = new JButton(new StartStopAction(startMode));
        closeButton_    = new JButton(new CloseAction());
        autoScrollBox_  = new JCheckBox(new AutoScrollAction());
        lineNumbersBox_ = new JCheckBox(new ShowLineNumbersAction());
        regexField_     = new JTextField(5);
        cutField_       = new JTextField(5);

        //regexField_.addKeyListener(new RegexKeyListener());
        //cutField_.addKeyListener(new CutKeyListener());        
        
        regexField_.addActionListener(new RegexActionListener());
        cutField_.addActionListener(new CutActionListener());
        
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(startButton_);
        buttonPanel.add(pauseButton_);
        buttonPanel.add(clearButton_);
        buttonPanel.add(closeButton_);
        buttonPanel.add(autoScrollBox_);
        buttonPanel.add(lineNumbersBox_);
        buttonPanel.add(new JLabel("Include filter"));
        buttonPanel.add(regexField_);
        buttonPanel.add(new JLabel("Cut"));
        buttonPanel.add(cutField_);
        
        setLayout(new BorderLayout());
        add(new JScrollPane(tailArea_), BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }
 
    /**
     * Sets up appropriate filters based on configuration
     */
    protected void buildFilters()
    {
        try
        {
            // Filter based on regular expression            
            regexFilter_ = new RegexLineFilter();
            regexFilter_.setEnabled(false);
            filters_.add(regexFilter_);
        }
        catch (RESyntaxException re)
        {
            logger_.error("buildFilters", re);
        }
        
        // Cut filter
        cutFilter_ = new CutLineFilter();
        cutFilter_.setEnabled(false);
        filters_.add(cutFilter_);
        
        // Show line numbers
        lineNumberDecorator_ = new LineNumberDecorator();
        lineNumberDecorator_.setEnabled(false);
        filters_.add(lineNumberDecorator_);
    }

    /**
     * Returns regular expression filter 
     * 
     * @return Filter text
     */
    protected String getRegularExpression()
    {
        return regexField_.getText().trim();
    }
        
    /**
     * Sets the filter text
     * 
     * @param  filter  Filter text as a regular expression
     */
    protected void setRegularExpression(String filter)
    {
        try
        {  
            regexFilter_.setEnabled(true);            
            regexField_.setText(filter);
            regexFilter_.setRegularExpression(filter);
        }
        catch (RESyntaxException res)
        {
            logger_.info("Invalid regular expression: " + filter);
        }
    }

    /**
     * Returns the cut expression
     * 
     * @return Cut expression
     */
    protected String getCutExpression()
    {
        return cutField_.getText().trim();
    }
    
    /**
     * Sets the cut text
     * 
     * @param  cut  Cut text. Example: 1-10 cuts columns one through ten.
     */
    protected void setCutExpression(String cut)
    {
        try
        {
            cutFilter_.setCut(cut);
            cutFilter_.setEnabled(true);        
            cutField_.setText(cut);            
        }
        catch (IllegalArgumentException e)
        {
            logger_.info("Invalid cut expression: " + cut);
        }
    }

    //--------------------------------------------------------------------------
    // Public
    //--------------------------------------------------------------------------
    
    /**
     * Sets the configuration
     * 
     * @param  config  Tail configuration
     */
    public void setConfiguration(ITailPaneConfig config)
    {
        config_ = config;

        autoScrollBox_.setSelected(config_.isAutoScroll());
        tailArea_.setAutoScroll(config_.isAutoScroll());
        
        boolean lineNumbers = config_.isShowLineNumbers();
        lineNumbersBox_.setSelected(lineNumbers);
        lineNumberDecorator_.setEnabled(lineNumbers);
        
        tailArea_.setFont(config_.getFont());
        tailArea_.setAntiAlias(config.isAntiAlias());
        
        setRegularExpression(config_.getRegularExpression());
        setCutExpression(config_.getCutExpression());
    }

    /**
     * Gets the configuration
     * 
     * @return TailConfig
     */
    public ITailPaneConfig getConfiguration()
    {
        // Make sure configuration up to date
        config_.setAutoScroll(autoScrollBox_.isSelected());
        config_.setShowLineNumbers(lineNumbersBox_.isSelected());
        config_.setFont(tailArea_.getFont());
        config_.setAntiAlias(tailArea_.isAntiAlias());
        config_.setRegularExpression(getRegularExpression());
        config_.setCutExpression(getCutExpression());
        
        config_.setAutoStart(
            startButton_.getText().equals(MODE_START) ? false : true);
            
        return config_;
    }    

    /**
     * Returns the close button
     * 
     * @return Close button
     */
    public JButton getCloseButton()
    {
        return closeButton_;
    }

    //--------------------------------------------------------------------------
    //  Interfaces
    //--------------------------------------------------------------------------
    
    /**
     * Interface to listen to the tail pane
     */
    public interface ITailPaneListener
    {
        /**
         * Notification of new data available
         * 
         * @param  tailPane  Tailpane
         */
        public void newDataAvailable(TailPane tailPane);
    }

    //--------------------------------------------------------------------------
    //  Inner Classes
    //--------------------------------------------------------------------------
    
    /**
     * Listener for tail
     */
    class TailListener extends TailAdapter
    {
        /**
         * Called when next line of input is available. When a new line is
         * available, just push it on the shared queue.
         * 
         * @param  line  Next line read
         */
        public void nextLine(Tail tail, String line)
        {
            try
            {
                queue_.push(line);
            }
            catch (InterruptedException ie)
            {
                ExceptionUtil.handleUI(ie, logger_);
            }
        }
        
        /*
         * @see toolbox.tail.TailAdapter#tailReattached(toolbox.tail.Tail)
         */
        public void tailReattached(Tail tail)
        {
            statusBar_.setStatus(
                "Tail reattached to " + tail.getFile().getName());
        }
    }
    
    /**
     * Pops groups of messages off the queue (as many as can be read without 
     * waiting) and consolidates before sending then to the textarea
     */
    class TailQueueListener implements IBatchingQueueListener
    {
        //----------------------------------------------------------------------
        //  IBatchingQueueListener Interface
        //----------------------------------------------------------------------
       
        public void nextBatch(Object[] objs)
        {
            // Iterate over each line delivered            
            for (int i=0; i<objs.length; i++)
            {
                String line = (String)objs[i];

                // Apply filters
                for (Iterator e = filters_.iterator(); e.hasNext(); )
                {
                    ILineFilter filter = (ILineFilter) e.next();
                    line = filter.filter(line);
                }

                if (line != null)
                    tailArea_.append(line + "\n");                 
            }
        }
    }

    /**
     * Listens for changes in the regular expression (user must press enter) and
     * applies the new regular expression accordingly.
     */    
    class RegexActionListener implements ActionListener
    {
        public void actionPerformed(ActionEvent e)
        {
            String s = getRegularExpression();
            
            if (StringUtil.isNullOrEmpty(s))
                regexFilter_.setEnabled(false);
            else
            {
                setRegularExpression(getRegularExpression());
                statusBar_.setStatus("Filtering on regular expression: " + s);
            }
        }
    }

    /**
     * Listens for changes in the cut expression (user must press enter) and
     * applies the new cut expression accordingly.
     */    
    class CutActionListener implements ActionListener
    {
        public void actionPerformed(ActionEvent e)
        {
            String s = getCutExpression();
            
            if (StringUtil.isNullOrEmpty(s))
                cutFilter_.setEnabled(false);
            else
                setCutExpression(getCutExpression());
        }
    }

    //--------------------------------------------------------------------------
    //  Actions
    //--------------------------------------------------------------------------
    
    /**
     * Starts/stops the tail
     */
    class StartStopAction extends AbstractAction
    {
        private String mode_;
            
        StartStopAction(String mode)
        {
            super(mode);
            mode_ = mode;
            putValue(MNEMONIC_KEY, new Integer('S'));
            putValue(SHORT_DESCRIPTION, "Starts/Stops the tail");
        }
    
        public void actionPerformed(ActionEvent e)
        { 
            //
            // Start
            //
            if (mode_.equals(MODE_START))
            {
                try
                {
                    init();
                    tail_.start();
                    mode_ = MODE_STOP;
                    putValue(Action.NAME, mode_);
                    pauseButton_.setEnabled(true);
                    //queueListener_.resetLines();
                    statusBar_.setStatus(
                        "Started tail for " + config_.getFilename());
                }
                catch (IOException ioe)
                {
                    logger_.error("StartStopAction", ioe);
                }
            }
            
            //
            // Stop
            //
            else
            {
                tail_.stop();
                queueReader_.stop();
                mode_ = MODE_START;
                putValue(Action.NAME, mode_);                
                pauseButton_.setEnabled(false);
                statusBar_.setStatus(
                    "Stopped tail for " + config_.getFilename());
            }
        }
    }

    /**
     * Pauses/unpauses the tail
     */
    class PauseUnpauseAction extends AbstractAction
    {
        private static final String MODE_PAUSE   = "Pause";
        private static final String MODE_UNPAUSE = "Unpause";
            
        PauseUnpauseAction()
        {
            super(MODE_PAUSE);
            putValue(MNEMONIC_KEY, new Integer('P'));
            putValue(SHORT_DESCRIPTION, "Pause/Unpauses the tail");
        }
    
        public void actionPerformed(ActionEvent e)
        { 
            if (tail_.isPaused())
            {
                tail_.unpause();
                putValue(Action.NAME, MODE_PAUSE);
                statusBar_.setStatus(
                    "Unpaused tail for " + config_.getFilename());              
            }
            else
            {
                tail_.pause();
                putValue(Action.NAME, MODE_UNPAUSE);
                statusBar_.setStatus(
                    "Paused tail for " + config_.getFilename());
            }
        }
    }

    /**
     * Closes the tail pane
     */
    class CloseAction extends AbstractAction
    {
        CloseAction()
        {
            super("Close");
            putValue(MNEMONIC_KEY, new Integer('e'));
            putValue(SHORT_DESCRIPTION, "Closes the tail pane");
        }
    
        public void actionPerformed(ActionEvent e)
        { 
            if (tail_.isPaused())
                tail_.unpause();
                
            if (tail_.isAlive())
                tail_.stop();
                
            statusBar_.setStatus("Closed tail for " + config_.getFilename());
        }
    }

    /**
     * Toggles autoscroll of the output text area
     */
    class AutoScrollAction extends AbstractAction
    {
        AutoScrollAction()
        {
            super("Autoscroll");
            putValue(MNEMONIC_KEY, new Integer('a'));
            putValue(SHORT_DESCRIPTION, "Toggles autoscroll");
        }
    
        public void actionPerformed(ActionEvent e)
        { 
            tailArea_.setAutoScroll(autoScrollBox_.isSelected()); 
        }
    }

    /**
     * Toggles line numbers in the output area
     */
    class ShowLineNumbersAction extends AbstractAction
    {
        ShowLineNumbersAction()
        {
            super("Line numbers");
            putValue(MNEMONIC_KEY, new Integer('L'));
            putValue(SHORT_DESCRIPTION, 
                "Toggles display of line numbers in the output");
        }
    
        public void actionPerformed(ActionEvent e)
        { 
            lineNumberDecorator_.setEnabled(!lineNumberDecorator_.isEnabled());
        }
    }
}