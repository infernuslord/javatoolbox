package toolbox.findclass;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Utility that finds all occurences of a given class in the 
 * CLASSPATH and the current directory 
 */
public class Main 
{ 
    private String      classToFind = "";          
    private String[]    classpath;
    private boolean     wildCard = false;
    private Vector      classFileList = new Vector();
    private String      fileSeparator = System.getProperty("file.separator");

    /* File filters */
    private FilenameFilter jarFilter       = new ExtensionFilter(".jar");
    private FilenameFilter zipFilter       = new ExtensionFilter(".zip");
    private FilenameFilter classFilter     = new ExtensionFilter(".class");
    private FilenameFilter archiveFilter   = new CompositeFilter(jarFilter, zipFilter);
    private FilenameFilter directoryFilter = new DirectoryFilter();
    
    /**
     * FindClass entry point
     * 
     * @param   args[0]  Name of class/class fragment to search for
     */
    public static void main(String args[])
    {
        
        if (args.length == 1) 
        { 
            Main f = new Main(args[0], true);
        }
        else 
        { 
            // print usage
            System.out.println();
            System.out.println("Searches for all occurrences of a class in the following places:");
            System.out.println("  1. Directories in the CLASSPATH");
            System.out.println("  2. Archives (zip & jar) in the CLASSPATH");
            System.out.println("  3. Archives in the current directory");
            System.out.println();
            System.out.println("The class name is assumed to be a case-insensetive wildcard and");
            System.out.println("will match any string in a fully qualified class name.");
            System.out.println();
            System.out.println("Usage  : java toolbox.findclass.Main <class name>");
            System.out.println();
            System.out.println("Example: Find the class java.lang.Object");
            System.out.println("         java toolbox.findclass.Main java.lang.Object");
            System.out.println();
            System.out.println("Example: Find all classes which contain the string 'String'");         
            System.out.println("         java toolbox.findclass.Main String");
        }
    }

    /**
     * Constructor
     * 
     * @param   classToFind     the name of class to find
     * @param   wildcard        turns on wildcard search
     */
    public Main(String classToFind, boolean wildCard) 
    { 
        setWildCard( wildCard );
        setClassToFind( classToFind );

        /* build list of archives and dirs to search */        
        List searchList = new ArrayList();
        searchList.addAll(getClassPathTargets());
        searchList.addAll(getArchiveTargets());

		/* convert search list to an array */
        String dirs[] = (String[])searchList.toArray(new String[0]);
        
        /* yee haw! */
        setClassPath(dirs);
        findClass(getClassToFind());
    }

    /**
     * Filters files based on the files extension
     */
    private class ExtensionFilter implements FilenameFilter
    {
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
    private class CompositeFilter implements FilenameFilter
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
    private class DirectoryFilter implements FilenameFilter
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
        return findFilesRecursively(".", archiveFilter);        
    }

    /**
     * Finds files recursively from a given starting directory using the
     * passed in filter as selection criteria.
     * 
     * @param    startDir    Start directory for the search
     * @param    filter      Filename filter criteria
     * @return   List of files that match the filter from the start dir
     */    
    public List findFilesRecursively(String startingDir, FilenameFilter filter)
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
            String[] dirs  = f.list(directoryFilter);
                        
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
            String[] c = getClassPath();

            for (int i=0; i< c.length; i++) 
            { 
                if (isArchive( c[i]))
                    findInArchive(c[i]);
                else
                    findInPath(c[i]);
            }
        }
        catch (Exception e) 
        { 
            System.out.println("Exception: " + e.toString());
            e.printStackTrace();
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
            System.out.println("*** Could not find or open " + jarName + "!!!! ***");
            return;
        }

        for (Enumeration e = zf.entries(); e.hasMoreElements();) 
        { 
            ZipEntry ze = (ZipEntry) e.nextElement();

            if (!ze.isDirectory() &&  ze.getName().endsWith(".class" )) 
            { 
                String name = ze.getName().replace('/', '.');
                name = name.substring(0, name.length() - ".class".length());

                //System.out.println("Converted=" + name + "]");

                if (!useWildCard()) 
                { 
                    if (name.equals(getClassToFind()))
                        classFound(getClassToFind(),jarName);
                }
                else 
                {
                    // case insensetive substring match
                    if (name.toUpperCase().indexOf(getClassToFind().toUpperCase()) != -1 )
                        classFound(name,jarName);
                }
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

        if (!useWildCard()) 
        { 
            /* exact search */
            char c = fileSeparator.charAt(0);
            String s = getClassToFind().replace('.', c );
            pathName += s;
            pathName += ".class";

            File f = new File(pathName);
            if (f.exists())
                classFound(getClassToFind(), pathName);
        }
        else  
        { 
            /* wildcard search */
            List classFiles = findFilesRecursively(pathName, classFilter);
            
            for(Iterator i = classFiles.iterator(); i.hasNext(); )
            {
                String fileMixed = (String)i.next();
                String fileLower = fileMixed.toLowerCase();
                String findMixed = getClassToFind();
                String findLower = findMixed.toLowerCase();
                
                if (fileLower.indexOf(findLower) >= 0)
                    classFound(fileMixed, findMixed);
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
     * Accessor for the classpath
     * 
     * @return  array of entries contained in the classpath
     */
    public String[] getClassPath() 
    { 
        return classpath;
    }
    
    /**
     * Accessor for the name of the class to find
     * 
     * @return  the name of the class to find
     */
    public String getClassToFind() 
    { 
        return classToFind;
    }
    
    /**
     * Determines whether a given file is a java archive
     * 
     * @param   s   absolute name of the java archive
     * @return      true if a valid archive, false otherwise
     */
    boolean isArchive(String s) 
    { 
        s = s.toUpperCase();
        if (s.endsWith(".JAR") || s.endsWith(".ZIP"))
            return true;
        else
            return false;
    }
   
    /**
     * Mutator for the classpath
     * 
     * @param   s   array of entries in the classpath
     */
    public void setClassPath( String[] s ) 
    { 
        classpath = s;
    }
    
    /**
     * Mutator for the name of the class to find
     * 
     * @param   s   the name of the class to find
     */
    public void setClassToFind( String s ) 
    { 
        classToFind = s;
    }
    
    /**
     * Mutator for the wildcard flag
     * 
     * @param   b     turns wildcard searh on
     */
    public void setWildCard( boolean b ) 
    { 
        wildCard = b;
    }
    
    /**
     * Accessor for the wildcard flag
     * 
     * @return  true if wildcard search turned on, false otherwise
     */
    public boolean useWildCard() 
    { 
        return wildCard;
    }
}
