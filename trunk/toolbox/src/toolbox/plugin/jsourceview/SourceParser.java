package toolbox.plugin.jsourceview;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import org.apache.log4j.Logger;

import toolbox.util.ElapsedTime;
import toolbox.util.FileUtil;

/**
 * Pops files off of the work queue and parses them to gather stats.
 */
class SourceParser implements Runnable
{
    private static final Logger logger_ = Logger.getLogger(SourceParser.class);
    
    //--------------------------------------------------------------------------
    // Fields 
    //--------------------------------------------------------------------------
    
    /**
     * Logical parent.
     */
    private final JSourceView sourceView_;
    
    /**
     * Cancel flag.
     */
    private boolean cancel_;

    /**
     * Statistics totals.
     */
    private FileStats totals_;
    
    //--------------------------------------------------------------------------
    // Constructors
    //--------------------------------------------------------------------------
    
    /**
     * Creates a SourceParser.
     * 
     * @param view Parent view.
     */
    SourceParser(JSourceView view)
    {
        sourceView_ = view;
    }
    
    //--------------------------------------------------------------------------
    // Public
    //--------------------------------------------------------------------------

    /** 
     * Cancels the parsing activity.
     */
    public void cancel()
    {
        cancel_ = true;
        // REMOVE: sourceView_.getTableSorter().setEnabled(true);
        sourceView_.setParseStatus("Search canceled!");            
    }

    //--------------------------------------------------------------------------
    // Runnable Interface
    //--------------------------------------------------------------------------

    /**
     * Pops source code off the work queue and parses collecting stats.
     */
    public void run()
    {
        ElapsedTime elapsed = new ElapsedTime();
        totals_ = new FileStats();
        int fileCount = 0;
        StatsCollector statsCollector = new StatsCollector();
        
        while (!sourceView_.getWorkQueue().isEmpty() || 
                sourceView_.scanDirThread_.isAlive()) 
        {
            if (cancel_)
                break;
                
            // Pop file of the queue
            String filename = (String) sourceView_.getWorkQueue().dequeue();
            
            if (filename != null)
            {
                sourceView_.setParseStatus(
                    "Parsing [" + 
                    sourceView_.getWorkQueue().size() + 
                    "] " + 
                    filename + " ...");
                 
                // Parse file and add to totals
                FileStats fileStats = null;
                
                try
                {
                    fileStats = statsCollector.getStats(filename);
                    totals_.add(fileStats);
                    ++fileCount;

                    // Create table row data and append                    
                    Object tableRow[] = 
                        new Object[JSourceView.COL_NAMES.length];
                    
                    tableRow[0] = new Integer(fileCount);
                    tableRow[1] = FileUtil.stripFile(filename);
                    tableRow[2] = FileUtil.stripPath(filename);
                    tableRow[3] = new Integer(fileStats.getCodeLines());
                    tableRow[4] = new Integer(fileStats.getCommentLines());
                    tableRow[5] = new Integer(fileStats.getBlankLines());
                    tableRow[6] = new Integer(fileStats.getThrownOutLines());
                    tableRow[7] = new Integer(fileStats.getTotalLines());
                    tableRow[8] = new Integer(fileStats.getPercent());
                
                    sourceView_.getTableModel().addRow(tableRow);
                    
                }
                catch (Exception e)
                {
                    logger_.error("run", e);
                }
            }
        }
    
        NumberFormat df = DecimalFormat.getIntegerInstance();
        
        sourceView_.setParseStatus(
            "[Total " + df.format(totals_.getTotalLines()) + "]  " +
            "[Code " + df.format(totals_.getCodeLines()) + "]  " +
            "[Comments " + df.format(totals_.getCommentLines()) + "]  " +
            "[Empty " + df.format(totals_.getBlankLines()) + "]  " +
            "[Thrown out " + df.format(totals_.getThrownOutLines()) + "]  " + 
            "[Percent code vs comments " + df.format(totals_.getPercent()) + 
            "%]"); 
        
        sourceView_.setScanStatus("Done parsing.");
        sourceView_.goButton_.setText(JSourceView.LABEL_GO);
        
        // Turn the sorter back on
        // REMOVE: sourceView_.getTableSorter().setEnabled(true);
        
        elapsed.setEndTime();
        sourceView_.setScanStatus("Elapsed time: " + elapsed.toString());
    }

    
    /**
     * Returns the statistics totals.
     * 
     * @return FileStats
     */
    public FileStats getTotals()
    {
        return totals_;
    }
}