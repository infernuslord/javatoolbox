package toolbox.jtail;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileNotFoundException;
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

import org.apache.log4j.Logger;
import org.apache.regexp.RESyntaxException;

import toolbox.jtail.config.ITailPaneConfig;
import toolbox.jtail.filter.CutLineFilter;
import toolbox.jtail.filter.ILineFilter;
import toolbox.jtail.filter.LineNumberDecorator;
import toolbox.jtail.filter.RegexLineFilter;
import toolbox.tail.Tail;
import toolbox.tail.TailAdapter;
import toolbox.util.StringUtil;
import toolbox.util.SwingUtil;
import toolbox.util.concurrent.BatchingQueueReader;
import toolbox.util.concurrent.BlockingQueue;
import toolbox.util.concurrent.IBatchingQueueListener;
import toolbox.util.ui.JSmartOptionPane;
import toolbox.util.ui.JSmartTextArea;

/**
 * Tail pane
 */
public class TailPane extends JPanel
{
    /** Logger **/
    private static final Logger logger_ = 
        Logger.getLogger(TailPane.class);
    
    private JButton        clearButton_;
    private JButton        pauseButton_;
    private JButton        startButton_;
    private JButton        closeButton_;
    
    private JCheckBox      autoScrollBox_;
    private JCheckBox      lineNumbersBox_;
    
    private JTextField     regexField_;
    private JTextField     cutField_;

    private BlockingQueue       queue_;
    private BatchingQueueReader queueReader_;
    private TailQueueListener   queueListener_;
    
    private List filters_ = new ArrayList();
    
    // Decorators and filters 
    private RegexLineFilter     regexFilter_;
    private CutLineFilter       cutFilter_;
    private LineNumberDecorator lineNumberDecorator_;
    
    /** 
     * Output for tail 
     */
    private JSmartTextArea tailArea_;

    /** 
     * The tailer 
     */    
    private Tail tail_;

    /** 
     * Configuration 
     */
    private ITailPaneConfig config_;
    
    
    //--------------------------------------------------------------------------
    //  Constructors
    //--------------------------------------------------------------------------
        
    /** 
     * Creates a TAilPane with the given configuration
     * 
     * @param   config  TailConfig
     * @throws  FileNotFoundException if file not found
     */
    public TailPane(ITailPaneConfig config) throws FileNotFoundException
    {
        buildView();
        buildFilters();        
        setConfiguration(config);                
        init();
    }
    
    //--------------------------------------------------------------------------
    //  Private
    //--------------------------------------------------------------------------
    
    /**
     * Initializes the tail
     * 
     * @param  file  File to tail
     * @throws FileNotFoundException
     */
    protected void init() throws FileNotFoundException
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
        
        queue_          = new BlockingQueue();
        queueListener_  = new TailQueueListener();        
        queueReader_    = new BatchingQueueReader(queue_);        
        queueReader_.addBatchQueueListener(queueListener_);
        Thread queueReaderThread = new Thread(queueReader_);
        queueReaderThread.start();
        
        // Setup tail
        tail_ = new Tail();
        tail_.addTailListener(new TailListener());
        tail_.setTailFile(config_.getFilename());

        // Start tail through action so button states are OK        
        startButton_.getAction().actionPerformed(
            new ActionEvent(this, 0, "Start"));
    }

    
    /**
     * Builds the GUI
     */    
    protected void buildView()
    {
        tailArea_ = new JSmartTextArea("");
        tailArea_.setFont(SwingUtil.getPreferredMonoFont());
        //tailArea_.setDoubleBuffered(false);
        
        clearButton_    = new JButton(new ClearAction());
        pauseButton_    = new JButton(new PauseUnpauseAction());
        startButton_    = new JButton(new StartStopAction());
        closeButton_    = new JButton(new CloseAction());
        autoScrollBox_  = new JCheckBox(new AutoScrollAction());
        lineNumbersBox_ = new JCheckBox(new ShowLineNumbersAction());
        regexField_    = new JTextField(5);
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
        buttonPanel.add(new JLabel("Filter"));
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
     * @return Cut text
     */
    protected String getCutExpression()
    {
        return cutField_.getText().trim();
    }
    
    
    /**
     * Sets the cut text
     * 
     * @param  cit  Cut text. Example: 1-10 cuts columns one through ten.
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
        return config_;
    }    


    /**
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
    private class TailListener extends TailAdapter
    {
        /**
         * Called when next line of input is available. When a new line is
         * available, just push it on the shared queue.
         * 
         * @param  line  Next line read
         */
        public void nextLine(String line)
        {
            try
            {
                queue_.push(line);
            }
            catch (InterruptedException ie)
            {
                JSmartOptionPane.showExceptionMessageDialog(null, ie);
            }
        }
    }
    
    /**
     * Pops groups of messages off the queue (as many as can be read without 
     * waiting) and consolidates before sending then to the textarea
     */
    private class TailQueueListener implements IBatchingQueueListener
    {
        //----------------------------------------------------------------------
        //  IBatchingQueueListener Interface
        //----------------------------------------------------------------------
       
        /**
         * Adds next batch of lines from the queue to the output area
         * in a one shot dilly-o
         *
         * @param  objs  Next batch of lines
         */
        public void nextBatch(Object[] objs)
        {
            String method = "[nxtBat] ";
            
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
     * Enabled dynamic filtering based on regex as it is typed
     */    
    public class RegexActionListener implements ActionListener
    {
        /**
         * @param e  ActionEvent
         */
        public void actionPerformed(ActionEvent e)
        {
            String s = getRegularExpression();
            
            if (StringUtil.isNullOrEmpty(s))
                regexFilter_.setEnabled(false);
            else
                setRegularExpression(getRegularExpression());
        }
    }


    /**
     * Enabled dynamic filtering based on regex as it is typed
     */    
    public class CutActionListener implements ActionListener
    {
        /**
         * @param e  ActionEvent
         */
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
    private class StartStopAction extends AbstractAction
    {
        private static final String MODE_START = "Start";
        private static final String MODE_STOP  = "Stop";
        
        private String mode_ = MODE_START;
            
        /**
         * Default constructor
         */
        public StartStopAction()
        {
            super(MODE_START);
            putValue(MNEMONIC_KEY, new Integer('S'));
            putValue(SHORT_DESCRIPTION, "Starts/Stops the tail");
        }
    
        /**
         * Stops the tail if it is alraedy alive and then starts the tail
         * 
         * @param  e    ActionEvent
         */
        public void actionPerformed(ActionEvent e)
        { 
            String method = "[actPrf] ";
            
            if (mode_.equals(MODE_START))
            {
                try
                {
                    tail_.start();
                    mode_ = MODE_STOP;
                    putValue(Action.NAME, mode_);
                    pauseButton_.setEnabled(true);
                    //queueListener_.resetLines();
                    logger_.debug(method + "Started tail: " + tail_.getFile());
                }
                catch(FileNotFoundException fnfe)
                {
                    logger_.error(method, fnfe);
                }
            }
            else
            {
                tail_.stop();
                mode_ = MODE_START;
                putValue(Action.NAME, mode_);                
                pauseButton_.setEnabled(false);
                logger_.debug(method + "Stopped tail: " + tail_.getFile());
            }
        }
    }


    /**
     * Pauses/unpauses the tail
     */
    private class PauseUnpauseAction extends AbstractAction
    {
        private static final String MODE_PAUSE   = "Pause";
        private static final String MODE_UNPAUSE = "Unpause";
            
        /**
         * Default constructor
         */
        public PauseUnpauseAction()
        {
            super(MODE_PAUSE);
            putValue(MNEMONIC_KEY, new Integer('P'));
            putValue(SHORT_DESCRIPTION, "Pause/Unpauses the tail");
        }

    
        /**
         * Pauses/unpauses the tail based on the current state
         * 
         * @param  e    ActionEvent
         */
        public void actionPerformed(ActionEvent e)
        { 
            if (tail_.isPaused())
            {
                tail_.unpause();
                putValue(Action.NAME, MODE_PAUSE);                
            }
            else
            {
                tail_.pause();
                putValue(Action.NAME, MODE_UNPAUSE);
            }
        }
    }
    

    /**
     * Closes the tail pane
     */
    private class CloseAction extends AbstractAction
    {
        /**
         * Default constructor
         */
        public CloseAction()
        {
            super("Close");
            putValue(MNEMONIC_KEY, new Integer('e'));
            putValue(SHORT_DESCRIPTION, "Closes the tail pane");
        }
    
        /**
         * Closes the tail pane
         * 
         * @param  e    ActionEvent
         */
        public void actionPerformed(ActionEvent e)
        { 
            if (tail_.isPaused())
                tail_.unpause();
                
            if (tail_.isAlive())
                tail_.stop();
        }
    }


    /**
     * Clears the output area
     */
    private class ClearAction extends AbstractAction
    {
        /**
         * Default constructor
         */
        public ClearAction()
        {
            super("Clear");
            putValue(MNEMONIC_KEY, new Integer('r'));
            putValue(SHORT_DESCRIPTION, "Clears the output area");
        }
    
        /**
         * Clears the output area
         * 
         * @param  e    ActionEvent
         */
        public void actionPerformed(ActionEvent e)
        { 
            tailArea_.setText("");
        }
    }

    
    /**
     * Toggles autoscroll of the output text area
     */
    private class AutoScrollAction extends AbstractAction
    {
        /**
         * Default constructor
         */
        public AutoScrollAction()
        {
            super("Autoscroll");
            putValue(MNEMONIC_KEY, new Integer('a'));
            putValue(SHORT_DESCRIPTION, "Toggles autoscroll");
        }
    
        /**
         * Toggle autoscroll
         * 
         * @param  e    ActionEvent
         */
        public void actionPerformed(ActionEvent e)
        { 
            tailArea_.setAutoScroll(autoScrollBox_.isSelected()); 
        }
    }


    /**
     * Toggles line numbers in the output area
     */
    private class ShowLineNumbersAction extends AbstractAction
    {
        /**
         * Default constructor
         */
        public ShowLineNumbersAction()
        {
            super("Line numbers");
            putValue(MNEMONIC_KEY, new Integer('L'));
            putValue(SHORT_DESCRIPTION, 
                "Toggles display of line numbers in the output");
        }
    
        /**
         * Toggle line numbers
         * 
         * @param  e    ActionEvent
         */
        public void actionPerformed(ActionEvent e)
        { 
            lineNumberDecorator_.setEnabled(!lineNumberDecorator_.isEnabled());
        }
    }
}
