package toolbox.util.io;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;

import org.apache.log4j.Logger;
import org.apache.regexp.RE;
import org.apache.regexp.RESyntaxException;

/**
 * A {@link java.io.Reader} that applies a regular expression to each line read 
 * from a Reader. If the line does not match the regular expression, it is 
 * discarded from the stream.
 * <p>
 * <b>Example:</b>
 * <pre class="snippet">
 * StringReader sr = new StringReader("a \n ab \n abc");
 * 
 * RegexFilterReader rfr = 
 *     new RegexFilterReader(
 *          sr,      // Reader to filter
 *          ".*b.*", // Regular expression
 *          false);  // Ignore case
 * 
 * String line = null;
 * 
 * // prints out matching lines ab and abc
 * while ((line = rfr.readLine()) != null)
 *     System.out.println("Line matched: " + line);
 * </pre>
 * 
 * @see java.io.LineNumberReader
 * @see java.io.Reader
 * @see org.apache.regexp.RE
 */
public class RegexFilterReader extends LineNumberReader
{
    private static final Logger logger_ = 
        Logger.getLogger(RegexFilterReader.class);
    
    //--------------------------------------------------------------------------
    // Constants 
    //--------------------------------------------------------------------------
    
    /** 
     * Default regular expression matches all if one is not specified. 
     */    
    private static final String DEFAULT_MATCH = ".*";

    //--------------------------------------------------------------------------
    // Fields 
    //--------------------------------------------------------------------------
    
    /** 
     * Regular expression as a string. 
     */
    private String strRegExp_;
    
    /** 
     * Flag to match case. 
     */
    private boolean matchCase_;

    /** 
     * Inverse match flag. 
     */
    private boolean matchInverse_;
    
    /** 
     * Regular expression. 
     */
    private RE regExp_;

    //--------------------------------------------------------------------------
    // Constructors
    //--------------------------------------------------------------------------
        
    /**
     * Creates a RegexFilterReader.
     * 
     * @param in Reader to filter.
     */
    public RegexFilterReader(Reader in)
    {
        this(in, DEFAULT_MATCH, false);
    }

    
    /**
     * Creates a RegexFilterReader.
     * 
     * @param in Reader to filter.
     * @param regExp Regular expression to match.
     * @param matchCase Set to true to observe case sensetivity.
     */
    public RegexFilterReader(Reader in, String regExp, boolean matchCase)
    {
        this(in, regExp, matchCase, false);
    }

    
    /**
     * Creates a RegexFilterReader.
     * 
     * @param in Reader to filter.
     * @param regExp Regular expression to match.
     * @param matchCase Set to true to observe case sensetivity.
     * @param matchInverse Match all lines that do not satisty the regular
     *        expression.
     */
    public RegexFilterReader(Reader in, String regExp, boolean matchCase, 
        boolean matchInverse)
    {
        super(in);
        strRegExp_    = regExp;
        matchCase_    = matchCase;
        matchInverse_ = matchInverse;

        try
        {
            regExp_ = new RE(strRegExp_);
            
            if (!matchCase_)
                regExp_.setMatchFlags(RE.MATCH_CASEINDEPENDENT);        
        }
        catch (RESyntaxException e)
        {
            logger_.error("constructor", e);
        }
    }

    //--------------------------------------------------------------------------
    // Overrides java.io.LineNumberReader
    //--------------------------------------------------------------------------
        
    /**
     * Reads the next line that matches the regular expression.
     * 
     * @return String that matches regular expression or null if the end of the
     *         stream has been reached.
     * @throws IOException on error.
     */
    public String readLine() throws IOException
    {
        String  line = null;
        
        while (true)
        {
            line = super.readLine();
            
            if (line == null)
                return null;
                
            boolean matches = regExp_.match(line);

            if (matches && !matchInverse_)
                return line;
            else if (!matches && matchInverse_)
                return line;
        }
    }
}