package toolbox.util;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import toolbox.util.io.WrappingWriter;

/**
 * Most commonly used String utility methods are already available in
 * {@link org.apache.commons.lang.StringUtils}. For those that aren't, this is
 * where you'll find them.
 */
public final class StringUtil
{
    private static final Logger logger_ = Logger.getLogger(StringUtil.class);
    
    //--------------------------------------------------------------------------
    // Constants
    //--------------------------------------------------------------------------
    
    /** 
     * Line break. 
     */
    public static final String BR = StringUtils.repeat("=", 80);
    
    /** 
     * New line.
     */
    public static final String NL = "\n"; 
    //System.getProperty("line.separator");
    
    /** 
     * Break with newline. 
     */
    public static final String BRNL = BR + NL;

    /**
     * Default wrap length is 80.
     */
    public static final int DEFAULT_WRAP_LENGTH = 80;
    
    /**
     * Default indentation character is a space.
     */
    public static final String DEFAULT_INDENT_CHAR = " ";
    
    /**
     * Default indentation length is 2 characters.
     */
    public static final int DEFAULT_INDENT_LENGTH = 2;
    
    /**
     * Clover private constructor workaround
     */
    static { new StringUtil(); }
    
    //--------------------------------------------------------------------------
    // Constructors
    //--------------------------------------------------------------------------
    
    /**
     * Private constructor.
     */
    private StringUtil()
    { 
    }
    
    //--------------------------------------------------------------------------
    // Left
    //--------------------------------------------------------------------------
    
    /**
     * Left justifies a string representing an integer with a given width. The
     * num is converted to a string which is padded with space characters on
     * the right to the given width. If the string representing the integer is
     * greater than width then the string is returned. A space is used by
     * default as the padding character.
     * 
     * @param num Number to format.
     * @param width Width of string.
     * @return Left justified string.
     */
    public static String left(int num, int width)
    {
        return left(Integer.toString(num), width);
    }

    
    /**
     * Left justifies a string within the given width. The string is padded
     * with space characters on the right to given width. Strings longer than
     * the width are returned unaltered.
     * 
     * @param str String to left.
     * @param width Max width.
     * @return Left justified string.
     */
    public static String left(String str, int width)
    {
        return left(str, width, ' ');
    }

    
    /**
     * Left justifies a string to a given width using a pad character. Strings
     * longer than the width are returned unaltered.
     * 
     * @param str String to justify.
     * @param width Width of resulting screen.
     * @param padChar Character to use for padding.
     * @return String of length width containing the given string on the left.
     */
    public static String left(String str, int width, char padChar)
    {
        return left(str, width, padChar, false);
    }

    
    /**
     * Left justifies a string to a given width using a pad character. Strings
     * longer than the width are returned unaltered if 'trunc' is false; else
     * they are truncated.
     * 
     * @param str String to justify.
     * @param width Width of resulting screen.
     * @param padChar Character to use for padding.
     * @param doTruncate If true, truncate; if false, leave unaltered.
     * @return String of length width containing the given string on the left.
     */
    public static String left(
        String str,
        int width,
        char padChar,
        boolean doTruncate)
    {
        final int strLen = str.length();
        String justStr = str;

        if (strLen > width)
        {
            if (doTruncate)
                justStr = str.substring(0, width);
        }
        else if (strLen < width)
        {
            StringBuffer bf = new StringBuffer(width);
            bf.append(str);
            
            for (int idx = strLen; idx < width; ++idx)
                bf.append(padChar);
            
            justStr = bf.toString();
        }

        return justStr;
    }

    //--------------------------------------------------------------------------
    // Right
    //--------------------------------------------------------------------------
    
    /**
     * Right justifies a string representing an integer within the given width.
     * The num is converted to a string which is padded with space characters
     * on the left to the given width. If the string representing the integer
     * is greater than width then the unaltered string is returned.
     * 
     * @param num Number to right justify.
     * @param width Max width.
     * @return Right justified string.
     */
    public static String right(int num, int width)
    {
        return right(Integer.toString(num), width);
    }


    /**
     * Right justifies a string to the given width using spaces. If the string
     * is longer than the width then the string is returned unaltered.
     * 
     * @param str String to right justify.
     * @param width Width of justified string.
     * @return Right justified string.
     */
    public static String right(String str, int width)
    {
        return right(str, width, ' ');
    }

    
    /**
     * Right justifies a string to the given width and pad character. If the
     * string is longer than the width, the string is returned unalteded.
     * 
     * @param str String to right justify.
     * @param width Width of justified string.
     * @param padChar Pad character.
     * @return Right justified string.
     */
    public static String right(String str, int width, char padChar)
    {
        return right(str, width, padChar, false);
    }

    
    /**
     * Right justifies a string to a given width using a pad character. If the
     * string is longer than the width and doTruncate is false, then the string
     * is returned unaltered otherwise the string is truncated to the width
     * specified.
     * 
     * @param str String to justify.
     * @param width Width of resulting screen.
     * @param padChar Character to use for padding.
     * @param doTruncate If true, truncate; if false, leave unaltered.
     * @return String of length width containing the given string on the left.
     */
    public static String right(
        String str,
        int width,
        char padChar,
        boolean doTruncate)
    {
        int strLen = str.length();
        String justStr = str;

        if (strLen > width)
        {
            if (doTruncate)
                justStr = str.substring(0, width);
        }
        else if (strLen < width)
        {
            StringBuffer bf = new StringBuffer(width);
            
            for (int idx = strLen; idx < width; ++idx)
                bf.append(padChar);
            
            bf.append(str);
            justStr = bf.toString();
        }

        return justStr;
    }

    //--------------------------------------------------------------------------
    // Wrap
    //--------------------------------------------------------------------------
    
    /**
     * Wraps a string to a default width of 80.
     * 
     * @param s String to wrap.
     * @return Wrapped string.
     */    
    public static String wrap(String s)
    {
        return wrap(s, DEFAULT_WRAP_LENGTH);
    }
    
    
    /**
     * Wraps a string to a default width of 80. The beginnning of line and end
     * of line are decorated with brackets to create a box effect if the border
     * flag is set.
     * 
     * <pre>
     *  [some text here] [more text here]
       </pre>
     * 
     * @param s String to wrap.
     * @param border True to enclose wrapped text in brackets.
     * @return Wrapped string with box decoration.
     */
    public static String wrap(String s, boolean border)
    {
        return wrap(s, DEFAULT_WRAP_LENGTH, border);
    }
    
    
    /**
     * Wraps a string to a given width. The beginning of line and end of line
     * are decorated with brackets to create a box effect if the border flag is
     * set.
     * 
     * @param s String to wrap.
     * @param width Width to wrap the string.
     * @param border Should the wrapped text be decorated with a border?
     * @return Wrapped string.
     */
    public static String wrap(String s, int width, boolean border)
    {
        if (border)
            return wrap(s, width, "[", "]");    
        else
            return wrap(s, width, "", "");
    }

    
    /**
     * Wraps a string to a given width.
     * 
     * @param s String to wrap.
     * @param width Width to wrap the string.
     * @return Wrapped string.
     */
    public static String wrap(String s, int width)
    {
        return wrap(s, width, "", "");    
    }

    
    /**
     * Wraps a string to the specified criteria.
     * 
     * @param s String to wrap.
     * @param width Width to wrap the string.
     * @param prefix Prefix before each line.
     * @param suffix Suffix after each line.
     * @return Wrapped string.
     */
    public static String wrap(String s, int width, String prefix, 
        String suffix)
    {
        String wrapped = null;
        
        try
        {
            StringWriter sw = new StringWriter();
            WrappingWriter w = new WrappingWriter(sw, width, prefix, suffix);
            w.write(s);
            w.close();
            wrapped = sw.toString();
        }
        catch (IOException e)
        {
            logger_.error(e);
        }
        
        return wrapped;
    }
    
    //--------------------------------------------------------------------------
    // Indent
    //--------------------------------------------------------------------------

    /**
     * Returns an indented string using the default indentation string (two
     * spaces).
     * 
     * @param s String to indent.
     * @return If the input string is non-null, the indented string.  
     *         If the input string is null, the default indent string.
     */   
    public static String indent(String s) {
        return indent(s, DEFAULT_INDENT_LENGTH);
    }


    /**
     * Returns an indented string using a space character repeated the given 
     * number of times. 
     * 
     * @param s String to indent.
     * @param numChars The number of times to repeat the default indent 
     *        character.
     * @return If the input string is non-null, the indented string.  
     *         If the input string is null, indentString.
     */   
    public static String indent(String s, int numChars) {
        return indent(s, numChars, DEFAULT_INDENT_CHAR);
    }

    
    /**
     * Returns an indented string.
     * 
     * @param s String to indent.
     * @param numChars The number of times to repeat the indentChar.
     * @param indentChar The character to use for indentation.
     * @return If the input string is non-null, the indented string.  
     *         If the input string is null, indentString.
     */   
    public static String indent(String s, int numChars, String indentChar) {
        return indent(s, StringUtils.repeat(indentChar, numChars));
    }
 
 
    /**
     * Returns an indented string.
     * 
     * @param s String to indent.
     * @param indentString String to use as the indentation.
     * @return If the input string is non-null, the indented string. If the
     *         input string is null, indentString.
     */
    public static String indent(String s, String indentString)
    {
        // Null short circuit
        if (s == null)
            return indentString;

        // Single line short circuit
        if (!StringUtil.isMultiline(s))
            return indentString + s;

        // Result buffer
        StringBuffer sb = new StringBuffer();

        // Tokenize preserving new lines (contiguous newlines would otherwise
        // be lost).
        StringTokenizer st = new StringTokenizer(s, "\n", true);

        // Flag to recognize consecutive newline chars
        boolean prevWasNewline = false;

        // Flag for the first line of the string
        boolean isFirst = true;

        while (st.hasMoreTokens())
        {
            String token = st.nextToken();

            if (token.equals("\n"))
            {
                if (prevWasNewline || isFirst)
                    sb.append(indentString + token);
                else
                    sb.append(token);
                prevWasNewline = true;
            }
            else
            {
                sb.append(indentString + token);
                prevWasNewline = false;
            }

            isFirst = false;
        }

        return sb.toString();
    }    

    //--------------------------------------------------------------------------
    // Misc
    //--------------------------------------------------------------------------
    
    /**
     * Return the given list as a debug string.
     * 
     * @param list List to convert to a string.
     * @return List converted to a string.
     */
    public static String toString(List list)
    {
        StringBuffer buf = new StringBuffer(20);
        Iterator iter = list.iterator();
        int index = 0;

        while (iter.hasNext())
            buf.append(" [").append(index++).append("] = ").append(iter.next());

        return buf.toString();
    }
    
    
    /**
     * Generates a numbered ruler underneath a string for character counting 
     * purposes. For example:
     * <pre>
     * 
     * Input : getStringRuler("abcdef") 
     * Output: abcdef
     *         123456
     * 
     * Input : getStringRuler("this is long") 
     * Output: this is long
     *         123456789012
     *                  111 <- read upwards
     * </pre> 
     * 
     * @param s String to generate a ruler for.
     * @return String containing the original string with the ruler appended.
     */
    public static String getStringRuler(String s)
    {
        StringBuffer sb = new StringBuffer();
        sb.append(s);
        sb.append("\n");
        
        int len      = s.length();
        int maxLines = (len + "").length();
        
        for (int i = 0; i < maxLines; i++)
        {
            for (int j = 1; j <= len; j++)
            {
                String num = left(j + "", maxLines);
                sb.append(num.charAt(i));
            }

            if (i != maxLines - 1)
                sb.append("\n");
        }

        return sb.toString();
    }
    
    
    /**
     * Truncates a string to the given length. If s.length() <= n, returns s.
     * Else, returns the first n characters of s.
     * 
     * @param s String to truncate.
     * @param n Length to truncate to.
     * @return Truncated string.
     */
    public static String truncate(String s, int n)
    {
        if (s.length() <= n)
            return s;
        else
            return s.substring(0, n);    
    }  

    
    /**
     * Returns array of individual tokens from a string. <br>
     * Note, this differs from commons-lang split() in that split supports a set
     * of single character delimiters specified as a single string and tokenize
     * supports a single delimiter that can have a length > 1.
     * 
     * @param s String to tokenize.
     * @param delimiter Delimiter used to separate tokens.
     * @return Array of string tokens.
     */  
    public static String[] tokenize(String s, String delimiter)
    {
        return tokenize(s, delimiter, false);
    }

    
    /**
     * Returns an arry of individual tokens from a string.
     * 
     * @param s String to tokenize.
     * @param delimiter Delimiter used for separate tokens.
     * @param saveDelims Counts the delimiter as a token.
     * @return Array of string tokens.
     */  
    public static String[] tokenize(
        String s, 
        String delimiter, 
        boolean saveDelims)
    {
        StringTokenizer st = new StringTokenizer(s, delimiter, saveDelims);
        String[] tokens = new String[st.countTokens()];   
        for (int i = 0; st.hasMoreTokens(); tokens[i++] = st.nextToken());
        return tokens;
    }

    
    /**
     * Determines if a string is multiline (contains one or more carriage
     * returns).
     * 
     * @param s String to inspect.
     * @return True if a multiline string, false otherwise.
     */
    public static boolean isMultiline(String s)
    {
        return s.indexOf("\n") >= 0;
    }
    
    
    /**
     * Retrieves the nth line from a string.
     * 
     * @param s Multiline string.
     * @param lineNumber Line number to retrieve. First line starts at 0.
     * @return Line contents at the given lineNumber.
     */
    public static String getLine(String s, int lineNumber)
    {
        LineNumberReader lnr = new LineNumberReader(new StringReader(s));
        String lineString = null;
        
        try
        {
            while (lnr.getLineNumber() <= lineNumber)
            {
                lineString = lnr.readLine();
                 
                if (lineString == null)
                    return null;
            }
        }
        catch (IOException ioe)
        {
            logger_.error("getLine", ioe);
        }
        finally
        {
            IOUtils.closeQuietly(lnr);
        }
        
        return lineString;
    }

    
    /**
     * Converts a string into a boolean value.
     * 
     * @param s String containing boolean value.
     * @return Boolean equivalent of the string.
     */
    public static boolean toBoolean(String s)
    {
        return new Boolean(s).booleanValue();        
    }
    
    
    /**
     * Encloses a string in horizontal bars for easier identification when
     * printing out. Example:
     * <pre>
     * 
     * =========================================================================
     * Your text goes here
     * =========================================================================
     * 
     * </pre>
     * 
     * @param s String to enclose in bars.
     * @return String
     */
    public static String addBars(String s)
    {
        StringBuffer sb = new StringBuffer();
        String bar = StringUtils.repeat("=", 80);
        sb.append("\n");
        sb.append(" ");
        sb.append(bar);
        sb.append("\n");

        String[] lines = StringUtil.tokenize(s, "\n");
        
        for (int i = 0; i < lines.length - 1; i++)
            sb.append("|").append(lines[i]).append("\n");
        
        if (lines.length > 0)
            sb.append("|").append(lines[lines.length - 1]);
        
        sb.append("\n");
        sb.append(" ");
        sb.append(bar);
        sb.append("\n");
        
        return sb.toString();
    }
    
    public static String banner(String s)
    {
        return addBars(s);
    }
}