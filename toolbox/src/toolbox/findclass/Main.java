package toolbox.findclass;

import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import toolbox.util.ArrayUtil;

/**
 * Utility that finds all occurences of a given class in the 
 * CLASSPATH and the current directory 
 */
public class Main { 
    
    String      _classToFind = "";          
    String[]    _classPath;
    boolean     _wildCard = false;
    Vector      _classFileList = new Vector();
    String      _fileSep = System.getProperty("file.separator");

    /**
     * FindClass entry point
     * 
     * @param   args    list of command line arguments
     */
    public static void main(String args[]) {
        
        if (args.length == 1) { 
            // exact search
            Main f = new Main(args[0], true);
        }
//        else if(args.length == 2  && args[0].equals("-wc")) { 
//            // wildcard search
//            Main f = new Main(args[1], true);
//        }
        else { 
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
    Main(String classToFind, boolean wildCard) { 
        
        setWildCard( wildCard );
        setClassToFind( classToFind );
        
        List searchList = new ArrayList();
        
        /* get classpath */
        String c = 
        	System.getProperty("java.class.path");
        
        /* tokenize */	
        StringTokenizer t = 
        	new StringTokenizer(c, System.getProperty("path.separator"), false);
        
        /* iterate and add to search list */	
        while (t.hasMoreTokens())
            searchList.add(t.nextToken());
        
        /* get archives in current dir */
        File currDir = new File(".");
        File[] archives = currDir.listFiles();
        
        /* pick out the archives */
        for(int i=0; i<archives.length; i++)
        {
        	File f = archives[i];
        	String lower = f.getName().toLowerCase();
        	if(f.isFile() && (lower.endsWith(".zip") || lower.endsWith(".jar")))
        		searchList.add(f.getName());
        }

		/* convert search list to an array */
        String dirs[] = (String[])searchList.toArray(new String[0]);
        
        //System.out.println(ArrayUtil.toString(dirs));
        
        /* yee haw! */
        setClassPath(dirs);
        findClass(getClassToFind());
    }
    
    /**
     * Finds all class files that that exist in a given directory and
     * subdirectorys
     * 
     * @param   pathName    the absolute name of the path to search
     */
    void findAllClassFilesRecursively(String pathName) { 

        //System.out.println( "Recursively searching : " + pathName );

        File f = new File(pathName);

        if (f.exists() && f.isDirectory()) { 
            String[] files = f.list();

            for (int i=0; i<files.length; i++) { 
                File currentFile = new File(f, files[i]);
                if (files[i].toLowerCase().endsWith(".class") && !currentFile.isDirectory()) { 
                    if (!pathName.endsWith(_fileSep))
                        pathName += _fileSep;
                    _classFileList.addElement(pathName + files[i]);
                }
                else if (currentFile.isDirectory())
                    findAllClassFilesRecursively(currentFile.getPath());
            }
        }
    }
    
    /**
     * Finds given class and prints out results to console
     * 
     * @param   classname   the name of the class to find
     */
    void findClass(String className) { 
        
        try { 
            String[] c = getClassPath();

            for (int i=0; i< c.length; i++) { 
                //System.out.println("Searching " + c[i] + "..." );

                if (isArchive( c[i]))
                    findInArchive(c[i]);
                else
                    findInPath(c[i]);
            }
        }
        catch (Exception e) { 
            System.out.println("Exception: " + e.toString());
            e.printStackTrace();
        }
    }
    
    /**
     * Finds class in a given jar file
     * 
     * @param   jarName     the name of the jar file to search
     */
    void findInArchive(String jarName) throws Exception { 
        
        ZipFile zf = null;

        try { 
            zf = new ZipFile(jarName);
        }
        catch (Exception e) { 
            System.out.println("*** Could not find or open " + jarName + "!!!! ***");
            return;
        }

        for (Enumeration e = zf.entries(); e.hasMoreElements();) { 
            ZipEntry ze = (ZipEntry) e.nextElement();

            if (!ze.isDirectory() &&  ze.getName().endsWith(".class" )) { 
                String name = ze.getName().replace('/', '.');
                name = name.substring(0, name.length() - ".class".length());

                //System.out.println("Converted=" + name + "]");

                if (!useWildCard()) { 
                    if (name.equals(getClassToFind()))
                        classFound(getClassToFind(),jarName);
                }
                else {
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
    void findInPath(String pathName) { 

        /*
         * TODO: spatel
         * Once a class is found in a given directory, read in class file and
         * parse bytecode to verify that the package name matches the location of the file in
         * the directory
         */
        if (!pathName.endsWith( _fileSep ))
            pathName += _fileSep;

        if (!useWildCard()) { 
            char c = _fileSep.charAt(0);
            String s = getClassToFind().replace('.', c );
            pathName += s;
            pathName += ".class";

            //System.out.println("Looking for " + pathName );

            File f = new File(pathName);

            if (f.exists())
                classFound(getClassToFind(), pathName);
        }
        else { 
            //System.out.println("Searching path " + pathName + " in classpath");
            _classFileList.removeAllElements();
            findAllClassFilesRecursively( pathName );

            for (Enumeration e = _classFileList.elements(); e.hasMoreElements(); ) { 
                String file = (String) e.nextElement();
                if (file.indexOf( getClassToFind() ) >= 0)
                    classFound(getClassToFind(),file);
            }

        }
    }
    
    /**
     * Called when a class is found by the various search methods
     *
     * @param  clazz        Class that was found
     * @param  clazzSource  Where the class was found (dir, zip, etc)
     */
    private void classFound(String clazz, String clazzSource)
    {
    	System.out.println(clazzSource + " => " + clazz);	
    }
    
    /**
     * Accessor for the classpath
     * 
     * @return  array of entries contained in the classpath
     */
    public String[] getClassPath() { 
        return _classPath;
    }
    
    /**
     * Accessor for the name of the class to find
     * 
     * @return  the name of the class to find
     */
    public String getClassToFind() { 
        return _classToFind;
    }
    
    /**
     * Determines whether a given file is a java archive
     * 
     * @param   s   absolute name of the java archive
     * @return      true if a valid archive, false otherwise
     */
    boolean isArchive(String s) { 
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
    public void setClassPath( String[] s ) { 
        _classPath = s;
    }
    
    /**
     * Mutator for the name of the class to find
     * 
     * @param   s   the name of the class to find
     */
    public void setClassToFind( String s ) { 
        _classToFind = s;
    }
    
    /**
     * Mutator for the wildcard flag
     * 
     * @param   b     turns wildcard searh on
     */
    public void setWildCard( boolean b ) { 
        _wildCard = b;
    }
    
    /**
     * Accessor for the wildcard flag
     * 
     * @return  true if wildcard search turned on, false otherwise
     */
    public boolean useWildCard() { 
        return _wildCard;
    }
}
