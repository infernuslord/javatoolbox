package toolbox.util.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Comparator;
import java.util.Date;

import org.apache.commons.io.IOUtils;

/**
 * A {@link java.io.File} comparator that can compare files by a number of file
 * attributes. Comparison by the following characteristics is supported:
 * <p>
 * <ul>
 *   <li>File name (case insensetive)
 *   <li>File size
 *   <li>File timestamp
 *   <li>File contents
 * </ul>
 */
public class FileComparator implements Comparator
{
    //--------------------------------------------------------------------------
    // Constants
    //--------------------------------------------------------------------------
    
    /**
     * Compare by file name (case insensetive).
     */
    private static final int COMPARE_NAME_ = 1;
    
    /**
     * Compare by file size.
     */
    private static final int COMPARE_SIZE_ = 2;
    
    /**
     * Compare by file timestamp.
     */
    private static final int COMPARE_DATE_ = 3;

    /**
     * Compare by file contents.
     */
    private static final int COMPARE_CONTENTS_ = 4;
    
    
    public static final FileComparator COMPARE_NAME = 
        new FileComparator(COMPARE_NAME_);
    
    public static final FileComparator COMPARE_SIZE = 
        new FileComparator(COMPARE_SIZE_);
    
    public static final FileComparator COMPARE_DATE = 
        new FileComparator(COMPARE_DATE_);
    
    public static final FileComparator COMPARE_CONTENTS = 
        new FileComparator(COMPARE_CONTENTS_);
    
    
    //--------------------------------------------------------------------------
    // Fields
    //--------------------------------------------------------------------------
    
    /**
     * Field to compare by.
     */    
    private int compareBy_;
    
    //--------------------------------------------------------------------------
    // Constructors
    //--------------------------------------------------------------------------

    /**
     * Creates a FileComparator.
     * 
     * @param compareBy Method by which to compare the files. 
     *        Use FileComparator.COMPARE_[NAME|SIZE|DATE|CONTENTS]
     */
    private FileComparator(int compareBy)
    {
        compareBy_ = compareBy;
    }
    
    //--------------------------------------------------------------------------
    // Comparator Interface
    //--------------------------------------------------------------------------

    /**
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    public int compare(Object a, Object b)
    {
        int result = Integer.MAX_VALUE;
        
        File fileA = (File) a;
        File fileB = (File) b;
        
        switch (compareBy_)
        {
            case COMPARE_NAME_ :
             
                result = fileA.getName().compareToIgnoreCase(fileB.getName());
                break;
            
                
            case COMPARE_SIZE_ :
            
                long sizeA = fileA.length();
                long sizeB = fileB.length();
                
                if (sizeA == sizeB)
                    result = 0;
                else if (sizeA > sizeB)
                    result = 1;
                else
                    result = -1;
                break;

                
            case COMPARE_DATE_ :
                
                Date dateA = new Date(fileA.lastModified());
                Date dateB = new Date(fileB.lastModified());
                result = dateA.compareTo(dateB);
                break;

                
            case COMPARE_CONTENTS_ :
                
                result = compareByContents(fileA, fileB);
                break;
                
                
            default:
            
                throw new IllegalArgumentException(
                    "Invalid compare option: " + compareBy_); 
        }
        
        return result;
    }

    //--------------------------------------------------------------------------
    // Protected
    //--------------------------------------------------------------------------
    
    /**
     * Does a byte by byte comparison of the two files.
     * 
     * @param f1 File object 1.
     * @param f2 File object 2.
     * @return int
     */
    protected int compareByContents(File f1, File f2)
    {
        if ((f1 == f2) || (f1 != null && f1.equals(f2)))
            return 0;
        
        if (f1 == null)
            return -1;
        
        if (f2 == null)
            return 1;

        if (!f1.exists() || !f1.isFile())
            throw new IllegalArgumentException(
                "File '" + f1 + "' does not exist");
        
        if (!f2.exists() || !f2.isFile())
            throw new IllegalArgumentException(
                "File '" + f2 + "' does not exist");

        if (f1.length() != f2.length())
            return (int) (f1.length() - f2.length());

        FileInputStream is1 = null;
        FileInputStream is2 = null;
        
        try
        {
            is1 = new FileInputStream(f1);
            is2 = new FileInputStream(f2);

            int b1 = -2;
            int b2 = -2;
            
            try
            {
                while ((b1 = is1.read()) != -1 && (b2 = is2.read()) != -1)
                {
                    if (b1 != b2)
                        return b1 - b2;
                    
                    b1 = b2 = -2;
                }
            }
            catch (IOException io)
            {
                return b1 == -2 ? -1 : 1;
            }
            finally
            {
                IOUtils.closeQuietly(is1);
                IOUtils.closeQuietly(is2);
            }
            
            return 0;
        }
        catch (FileNotFoundException fnf)
        {
            return is1 == null ? -1 : 1;
        }
    }
}