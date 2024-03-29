package toolbox.plugin.jsourceview;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import toolbox.util.ResourceUtil;

/**
 * Collects source code statistics.
 */
public class StatsCollector
{
    //--------------------------------------------------------------------------
    // Fields 
    //--------------------------------------------------------------------------
    
    /** 
     * List of collectors that will analyze the source code. 
     */
    private List collectors_;
    
    /** 
     * Current line of source code being analyzed. 
     */        
    private String line_;
            
    //--------------------------------------------------------------------------
    // Constructors
    //--------------------------------------------------------------------------
    
    /**
     * Creates a StatsCollector.
     */
    public StatsCollector()
    {
        buildCollectors();
    }
    
    //--------------------------------------------------------------------------
    // Public
    //--------------------------------------------------------------------------

    /**
     * Scans a given file and returns the source code statistics.
     * 
     * @param filename Name of the file.
     * @return FileStats
     * @throws IOException on I/O error.
     */
    public FileStats getStats(String filename) throws IOException
    {
        Reader reader = 
            new InputStreamReader(ResourceUtil.getResource(filename));
            
        FileStats stats = getStats(reader);
        reader.close();
        return stats; 
    }
    
    
    /**
     * Scans a given file and generates source code statistics.
     * 
     * @param rdr Source of source code.
     * @return FileStats
     * @throws IOException on I/O error.
     */
    public FileStats getStats(Reader rdr) throws IOException
    {
        FileStats stats = new FileStats();
        BufferedReader reader;
        
        if (rdr instanceof BufferedReader)
            reader = (BufferedReader) rdr;
        else
            reader = new BufferedReader(rdr);
        
        while ((line_ = reader.readLine()) != null) 
        {
            // Nuke tabs and leading/trailing spaces                
            line_ = line_.replace('\t', ' ').trim();

            for (Iterator i = collectors_.iterator(); i.hasNext();)
            {
                CodeCollector collector = (CodeCollector) i.next();
                
                boolean done = collector.identify(stats, line_);
                
                if (done)
                    break;
            }
        }
        
        return stats;
    }
    
    //--------------------------------------------------------------------------
    // Protected
    //--------------------------------------------------------------------------
    
    /**
     * Builds a pipeline of statistics collectors for the source code to be
     * run against. 
     */
    protected void buildCollectors()
    {
        collectors_ = new ArrayList();
        collectors_.add(new TotalCollector());        
        collectors_.add(new BlankCollector());
        collectors_.add(new BraceCollector());
        
        // Causes side effect with the scanner in realcode collector
        // collectors_.add(new CommentBeginEndCollector());
        
        collectors_.add(new ImportCollector());
        collectors_.add(new RealCodeCollector());
    }
    
    //--------------------------------------------------------------------------
    // Interfaces
    //--------------------------------------------------------------------------

    /**
     * A CodeCollector identifies the category in which a line of source code
     * belongs.
     */    
    interface CodeCollector
    {
        /**
         * Identify a category to which a line of source code belongs
         * 
         * @param stats Increments category counter here.
         * @param line Line of source code.
         * @return True if the line was categorized, false otherwise.
         */
        boolean identify(FileStats stats, String line);
    }
    
    //--------------------------------------------------------------------------
    // ImportCollector
    //--------------------------------------------------------------------------
    
    /**
     * Collector which identifies import statements and classifies them as
     * "throw out" lines of code.
     */
    class ImportCollector implements CodeCollector
    {
        /*
         * @see toolbox.plugin.jsourceview.StatsCollector.CodeCollector#identify(toolbox.plugin.jsourceview.FileStats, java.lang.String)
         */
        public boolean identify(FileStats stats, String line)
        {
            boolean done = false;
            
            if (line.startsWith("import "))
            {
                stats.incrementThrownOutLines();
                done = true;
            }
                
            return done;
        }
    }

    //--------------------------------------------------------------------------
    // BraceCollector
    //--------------------------------------------------------------------------
    
    /**
     * Collector which identifies braces that occupy an entire line and 
     * classifies the line as "thrown out".
     */    
    class BraceCollector implements CodeCollector
    {
        /*
         * @see toolbox.plugin.jsourceview.StatsCollector.CodeCollector#identify(toolbox.plugin.jsourceview.FileStats, java.lang.String)
         */
        public boolean identify(FileStats stats, String line)
        {
            boolean done = false;
            
            if (line.equals("{") || line.equals("}"))
            {
                stats.incrementThrownOutLines();
                done = true;
            }
            
            return done;
        }
    }
    
    //--------------------------------------------------------------------------
    // BlankCollector
    //--------------------------------------------------------------------------
    
    /**
     * Collector which identifies empty lines of code also considered 
     * whitespace.
     */    
    class BlankCollector implements CodeCollector
    {
        /*
         * @see toolbox.plugin.jsourceview.StatsCollector.CodeCollector#identify(toolbox.plugin.jsourceview.FileStats, java.lang.String)
         */
        public boolean identify(FileStats stats, String line)
        {
            boolean done = false;
            
            if (line.length() == 0)
            {
                stats.incrementBlankLines();
                done = true;
            }
            
            return done;
        }
    }

    //--------------------------------------------------------------------------
    // TotalCollector
    //--------------------------------------------------------------------------

    /**
     * Collector which counts every line of source code regardless of its
     * categorization.
     */
    class TotalCollector implements CodeCollector
    {
        /*
         * @see toolbox.plugin.jsourceview.StatsCollector.CodeCollector#identify(toolbox.plugin.jsourceview.FileStats, java.lang.String)
         */
        public boolean identify(FileStats stats, String line)
        {
            stats.incrementTotalLines();
            return false;
        }
    }

    //--------------------------------------------------------------------------
    // RealCodeCollector
    //--------------------------------------------------------------------------

    /**
     * Collector which identifies "real" lines of source code.
     */
    class RealCodeCollector implements CodeCollector
    {
        /**
         * Status of the line.
         */
        private LineStatus  status_  = new LineStatus();
        
        /**
         * The line scanner.
         */
        private LineScanner scanner_ = new LineScanner();
        
        /*
         * @see toolbox.plugin.jsourceview.StatsCollector.CodeCollector#identify(toolbox.plugin.jsourceview.FileStats, java.lang.String)
         */
        public boolean identify(FileStats stats, String line)
        {
            scanner_.setLine(line);
            Machine.scanLine(scanner_, status_);
                        
            if (status_.isRealCode())
            {
                stats.incrementCodeLines();
                //logger_.debug("Real code: " + line);
            }
            else
            {
                stats.incrementCommentLines();
                //logger_.debug("Comment code: " + line);
            }
                
            return true;
        }
    }
    
    //--------------------------------------------------------------------------
    // CommentBeginEndCollector
    //--------------------------------------------------------------------------
    
    /**
     * Collector which identifies starting an ending tags for comments
     * occupying a line all by themselves. These lines are categorized as
     * "thrown out".
     */
    class CommentBeginEndCollector implements CodeCollector
    {
        /*
         * @see toolbox.plugin.jsourceview.StatsCollector.CodeCollector#identify(toolbox.plugin.jsourceview.FileStats, java.lang.String)
         */
        public boolean identify(FileStats stats, String line)
        {
            boolean done = false;
            
            if (line.equals("/*") || line.equals("/**") || line.equals("*/"))
            {
                stats.incrementThrownOutLines();
                done = true;
            }
            
            return done;
        }
    }
}