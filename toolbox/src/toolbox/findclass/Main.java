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
import toolbox.util.StringUtil;

/**
 * Utility that finds all occurences of a given class in the 
 * CLASSPATH, current directory, and archives (recursively)
 */
public class Main 
{ 
    /** Logger **/
    private static final Category logger_ = Category.getInstance(Main.class);
    
    private String      classToFind_ = "";          
    private String[]    classpath_;
    private boolean     useRegExp_ = false;
    private List        classFileList = new ArrayList();
    private String      fileSeparator = System.getProperty("file.separator");

    /* File filters */
    private FilenameFilter jarFilter_       = new ExtensionFilter(".jar");
    private FilenameFilter zipFilter_       = new ExtensionFilter(".zip");
    private FilenameFilter classFilter_     = new ExtensionFilter(".class");
    private FilenameFilter archiveFilter_   = new CompositeFilter(jarFilter_, zipFilter_);
    private FilenameFilter directoryFilter_ = new DirectoryFilter();

    /* if this system property is set to anything, then debug output will be generated */    
    private static final String debugProp_ = "findclass.debug";

    /** regular expression matcher **/
    private RE regExp_;
        
    /**
     * FindClass entry point
     * 
     * @param   args[0]  Name of class/class fragment to search for
     */
    public static void main(String args[])
    {
        /* init log4j */
        BasicConfigurator.configure();
        
        if (System.getProperty(debugProp_) == null)
            Category.getDefaultHierarchy().disableDebug();
        
        String regExpFlag = "-re";
        String classToFind;
        boolean useRegExp;
        
        if (args.length == 1) 
        {
            classToFind = args[0];
            useRegExp = false;
            
            new Main(classToFind, useRegExp);
        }
        else if (args.length == 2)
        {
            if (args[0].equals(regExpFlag))
            {
                useRegExp = true;
                classToFind = args[1];
                new Main(classToFind, useRegExp);                
            }
            else if (args[1].equals(regExpFlag))
            {
                useRegExp = true;
                classToFind = args[0];                   
                new Main(classToFind, useRegExp);                
            }
            else
                printUsage();
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
        System.out.println("Usage  :                                                             ");
        System.out.println("  java toolbox.findclass.Main <class name> | [-re <regular expression>]");
        
        /*
        System.out.println("Example: To find the class java.lang.Object");
        System.out.println("         java toolbox.findclass.Main java.lang.Object");
        System.out.println();
        System.out.println("Example: Find all classes which contain the string 'String'");         
        System.out.println("         java toolbox.findclass.Main String");
        */
    }

    /**
     * Constructor
     * 
     * @param   classToFind     the name of class to find
     * @param   useRegExp       turns on regular expression search
     */
    public Main(String classToFind, boolean useRegExp) 
    { 
        /* set arguments */
        useRegExp_   = useRegExp;
        classToFind_ = classToFind;

        try
        {
            /* setup regexp based on flag */                    
            regExp_ = new RE(classToFind_);
            if (!useRegExp_)
                regExp_.setMatchFlags(RE.MATCH_CASEINDEPENDENT);        
        }
        catch (RESyntaxException e)
        {
            logger_.error("constructor", e);
        }

        /* build list of archives and dirs to search */        
        List searchList = new ArrayList();
        searchList.addAll(getClassPathTargets());
        searchList.addAll(getArchiveTargets());

		/* convert search list to an array */
        classpath_ = (String[])searchList.toArray(new String[0]);
        
        if (logger_.isDebugEnabled())
        {
            logger_.debug("Search targets");
            logger_.debug("==============================");
            
            for(int i=0; i < classpath_.length; i++)
                logger_.debug(classpath_[i]);

            logger_.debug("==============================");                
        }
        /* yee haw! */
        findClass(classToFind_);
    }

    /**
     * Filters files based on the files extension
     */
    protected class ExtensionFilter implements FilenameFilter
    {
        /** Extension to filter on **/
        private String extension;
        
        /**
         * Creates an Extension filter with the given file extension
         * 
         * @param  fileException   The file extension to filter on
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
            return name.toLowerCase().endsWith(extension.toLowerCase());
        }
    }

    /**
     * Composite file filter. Matches up to two filters in an OR fashion
     */
    protected class CompositeFilter implements FilenameFilter
    {
        private FilenameFilter firstFilter;
        private FilenameFilter secondFilter;
        
        /**
         * Creates a filter that is the composite of two filters
         * 
         * @param  filterOne   First filter
         * @param  filterTwo   Second filter
         */   
        public CompositeFilter(FilenameFilter filterOne, FilenameFilter filterTwo)
        {
            firstFilter = filterOne;
            secondFilter = filterTwo;
        }
        
        /**
         * Filter as a composite  
         * 
         * @param    dir   Directory file is contained in
         * @param    name  Name of file
         * @return   True if the file matches at least one of two filter,
         *            false otherwise.
         */
        public boolean accept(File dir,String name)
        {
            return firstFilter.accept(dir, name) || 
                   secondFilter.accept(dir, name);
        }
    }

    /**
     * Filters directories
     */
    protected class DirectoryFilter implements FilenameFilter
    {
        /**
         * Filter out directories
         * 
         * @param    dir   Directory file is contained in
         * @param    name  Name of file
         * @return   True if the file matches the extension, false otherwise
         */
        public boolean accept(File dir,String name)
        {
            File f = new File(dir, name);
            return f.isDirectory();
        }
    }
    
    /**
     * Retrieves all search targets (archives and directories) on the classpath
     *
     * @return  Array of file/directory strings
     */
    protected List getClassPathTargets()
    {
        List targets = new ArrayList();
        
        /* get classpath */
        String c = System.getProperty("java.class.path");
        
        /* tokenize */  
        StringTokenizer t = 
            new StringTokenizer(c, System.getProperty("path.separator"), false);
                
        /* iterate and add to search list */    
        while (t.hasMoreTokens())
            targets.add(t.nextToken());
            
        return targets;
    }
    
    /**
     * Retrieves a list of all archive targets to search
     * starting from the current directory and all directories
     * contained with it recursively.
     * 
     * @return Array of strings to archive file locations
     */
    protected List getArchiveTargets()
    {
        return findFilesRecursively(".", archiveFilter_);        
    }

    /**
     * Finds files recursively from a given starting directory using the
     * passed in filter as selection criteria.
     * 
     * @param    startDir    Start directory for the search
     * @param    filter      Filename filter criteria
     * @return   List of files that match the filter from the start dir
     */    
    protected List findFilesRecursively(String startingDir, FilenameFilter filter)
    {
        File f = new File(startingDir);
        ArrayList basket = new ArrayList(20);

        if (f.exists() && f.isDirectory()) 
        { 
            /* smack a trailing / on the start dir */
            if (!startingDir.endsWith(fileSeparator))
                startingDir += fileSeparator;
            
            /* process files */
            String[] files = f.list(filter);
            
            for (int i=0; i<files.length; i++) 
            { 
                File current = new File(f, files[i]);
                basket.add(startingDir + files[i]);
            }
            
            /* process directories */
            String[] dirs  = f.list(directoryFilter_);
                        
            for(int i=0; i<dirs.length; i++)
            {
                List subBasket = findFilesRecursively(startingDir + dirs[i], filter);
                basket.addAll(subBasket);
            }
        }
        
        return basket;
    }
    
    /**
     * Finds given class and prints out results to console
     * 
     * @param   classname   the name of the class to find
     */
    protected void findClass(String className) 
    { 
        try 
        { 
            for (int i=0; i< classpath_.length; i++) 
            { 
                if (isArchive(classpath_[i]))
                    findInArchive(classpath_[i]);
                else
                    findInPath(classpath_[i]);
            }
        }
        catch (Exception e) 
        { 
            logger_.error("findclass", e);
        }
    }
    
    /**
     * Finds class in a given jar file
     * 
     * @param   jarName     the name of the jar file to search
     */
    protected void findInArchive(String jarName) throws Exception 
    { 
        ZipFile zf = null;

        try 
        { 
            zf = new ZipFile(jarName);
        }
        catch (Exception e) 
        { 
            System.out.println("Error: Could not locate " + jarName + ".");
            return;
        }
        
        for (Enumeration e = zf.entries(); e.hasMoreElements();) 
        { 
            ZipEntry ze = (ZipEntry) e.nextElement();

            if (!ze.isDirectory() &&  ze.getName().endsWith(".class" )) 
            { 
                String name = ze.getName().replace('/', '.');
                name = name.substring(0, name.length() - ".class".length());

                if (regExp_.match(name))
                    classFound(name, jarName);
            }
        }
        zf.close();
    }
    
    /**
     * Finds class in a given directory and subdirs
     * 
     * @param   pathName    the absolute name of the directory to search
     */    
    protected void findInPath(String pathName) 
    { 
        /* tack a slash on the end */
        if (!pathName.endsWith( fileSeparator ))
            pathName += fileSeparator;

        /*
        if (!useRegExp_) 
        { 
            // exact search 
            char c = fileSeparator.charAt(0);
            String s = classToFind_.replace('.', c );
            pathName += s;
            pathName += ".class";

            File f = new File(pathName);
            if (f.exists())
                classFound(classToFind_, pathName);
        }
        else  
        */
        
        { 
            /* regular expression search */
            List classFiles = findFilesRecursively(pathName, classFilter_);
            
            for(Iterator i = classFiles.iterator(); i.hasNext(); )
            {
                String fileName = (String)i.next();
                String dotted= fileName.replace(File.separatorChar, '.');
                dotted = StringUtil.truncate(dotted, fileName.length() - ".class".length());
                
                logger_.debug("file=" + dotted);
                
                if (regExp_.match(dotted))
                    classFound(fileName, classToFind_);
            }
        }
    }
    
    /**
     * Called when a class is found by the various search methods
     *
     * @param  clazz        Class that was found
     * @param  clazzSource  Where the class was found (dir, zip, etc)
     */
    protected void classFound(String clazz, String clazzSource)
    {
    	System.out.println(clazzSource + " => " + clazz);	
    }
    
    /**
     * Determines whether a given file is a java archive
     * 
     * @param   s   absolute name of the java archive
     * @return      true if a valid archive, false otherwise
     */
    protected boolean isArchive(String s) 
    { 
        s = s.toUpperCase();
        if (s.endsWith(".JAR") || s.endsWith(".ZIP"))
            return true;
        else
            return false;
    }
}
