package toolbox.findclass;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Category;
import org.apache.regexp.RE;
import org.apache.regexp.RESyntaxException;
import toolbox.util.ArrayUtil;
import toolbox.util.StringUtil;

/**
 * Utility that finds all occurences of a given class in the 
 * CLASSPATH, current directory, and archives (recursively)
 */
public class Main 
{ 
    /** Logger **/
    private static final Category logger_ = Category.getInstance(Main.class);
    
    /** command line flag to specify case sensetivity **/
    private static final String caseSensetiveFlag_ = "-cs";
    
    /**
     * FindClass entry point
     * 
     * @param   args[]  Args
     */
    public static void main(String args[])
    {
        /* init log4j */
        BasicConfigurator.configure();
        
        FindClass finder = new FindClass();
        String classToFind;
        boolean ignoreCase;

        /* handle args */        
        if (args.length == 1) 
        {
            classToFind = args[0];
            ignoreCase  = true;
            
            finder.findClass(classToFind, ignoreCase);
        }
        else if (args.length == 2)
        { 
            switch (ArrayUtil.indexOf(args, caseSensetiveFlag_))
            {
                case -1: printUsage(); 
                         break;
                
                case  0: classToFind = args[1]; 
                         ignoreCase = false;
                         finder.findClass(classToFind, ignoreCase);                
                
                case  1: classToFind = args[0]; 
                         ignoreCase = false;
                         finder.findClass(classToFind, ignoreCase);                
                         break;
            }
        }
        else 
            printUsage();
    }

    /**
     * Prints program usage
     */
    private static void printUsage()
    {
        System.out.println("FindClass searches for all occurrences of a class in");
        System.out.println("your classpath and archives visible from the current");
        System.out.println("directory.");
        System.out.println();
        System.out.println("Usage  : java toolbox.findclass.Main -cs <regular expression>");
        System.out.println("Options: -cs => Case sensetive search");
    }
}
