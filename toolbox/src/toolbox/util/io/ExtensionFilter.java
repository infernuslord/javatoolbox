package toolbox.util.io;

import java.io.File;
import java.io.FilenameFilter;

import org.apache.log4j.Category;

/**
 * Filters files based on the file's extension
 */
public class ExtensionFilter implements FilenameFilter
{
    /** Logger **/
    private static final Category logger_ = 
        Category.getInstance(ExtensionFilter.class);
        
    /** Extension to filter on **/
    private String extension;
    
    /**
     * Creates an Extension filter with the given file extension
     * 
     * @param  fileExtension   The file extension to filter on
     */   
    public ExtensionFilter(String fileExtension)
    {
        /* add a dot just in case */
        if(!fileExtension.startsWith("."))
            fileExtension = "." + fileExtension;
        extension = fileExtension;
    }
    
    /**
     * Filter out a files by extension
     * 
     * @param    dir   Directory file is contained in
     * @param    name  Name of file
     * @return   True if the file matches the extension, false otherwise
     */
    public boolean accept(File dir,String name)
    {
        boolean b = name.toLowerCase().endsWith(extension.toLowerCase());
        
        //if (b)
        //    logger_.debug("Accepted " + name);
        
        return b;
    }
}