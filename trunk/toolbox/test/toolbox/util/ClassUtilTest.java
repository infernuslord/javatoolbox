package toolbox.util;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;

import javassist.ClassPool;
import javassist.CtClass;

import junit.framework.TestCase;
import junit.textui.TestRunner;

import org.apache.log4j.Logger;

import toolbox.log4j.SmartLogger;

/**
 * Unit test for {@link toolbox.util.ClassUtil}.
 */
public class ClassUtilTest extends TestCase
{
    private static final Logger logger_ = Logger.getLogger(ClassUtilTest.class);

    //--------------------------------------------------------------------------
    // Main
    //--------------------------------------------------------------------------
        
    /**
     * Entrypoint.
     * 
     * @param args None recognized.
     */
    public static void main(String[] args)
    {
        TestRunner.run(ClassUtilTest.class);
    }

    //--------------------------------------------------------------------------
    // Unit Tests
    //--------------------------------------------------------------------------
    
    /**
     * Tests getClassesInPackage() for a package in a file system directory.
     */
    public void testGetClassesInPackageDirectory()
    {
        assertTrue(true);
        
        // TODO: Revisit testcase 
        
        // Have to revisit because no classes files are hanging out in
        // a directory on the classpath when the test is run from outside
        // eclipse
        
        //logger_.info("Running testGetClassesInPackageDirectory...");
        
        //String[] classes = ClassUtil.getClassesInPackage("toolbox.util");
        //logger_.info("\n" + ArrayUtil.toString(classes, true));
        //assertTrue(StringUtil.class.getName() + " not found in package", 
        //    ArrayUtil.contains(classes, StringUtil.class.getName()));
    }
    
    
    /**
     * Tests getClassesInPackage() for a package in a jar file.
     */
    public void testGetClassesInPackageArchive()
    {
        logger_.info("Running testGetClassesInPackageArchive...");
        
        String[] classes = ClassUtil.getClassesInPackage("junit.textui");
        
        //logger_.info("\n"+ArrayUtil.toString(classes, true));
        
        assertTrue(TestRunner.class.getName() + " not found in package", 
            ArrayUtil.contains(classes, TestRunner.class.getName()));
    }
    
    
    /**
     * Tests getClassesInPackage() for a package in the boot classpath.
     */
    public void testGetClassesInPackageBoot()
    {
        logger_.info("Running testGetClassesInPackageBoot...");
        
        String[] classes = ClassUtil.getClassesInPackage("java.lang");
        
        //logger_.info("\n" + ArrayUtil.toString(classes, true));
        
        assertTrue(String.class.getName() + " not found in package", 
            ArrayUtil.contains(classes, String.class.getName()));
    }
    
    
    /**
     * Tests getPackagesInClasspath(). 
     */
    public void testGetPackagesInClasspath()
    {   
        logger_.info("Running testGetPackagesInClasspath...");
        
        String[] packages = ClassUtil.getPackagesInClasspath();
        SmartLogger.info(logger_, ArrayUtil.toString(packages, true));
        assertTrue("tgpic1", ArrayUtil.contains(packages, "java.lang"));
        assertTrue("tgpic3", ArrayUtil.contains(packages, "junit.textui"));
    }
    
    
    /**
     * Tests getClasspath()
     */
    public void testGetClasspath()
    {
        logger_.info("Running testGetClasspath...");
        
        String classpath = ClassUtil.getClasspath();
        assertNotNull("cp null", classpath);
        String[] paths = StringUtil.tokenize(classpath, File.pathSeparator);
        SmartLogger.info(logger_, ArrayUtil.toString(paths, true));
    }
    
    
    /**
     * Positive tests isArchive()
     */
    public void testIsArchive()
    {
        logger_.info("Running testIsArchive...");
        
        // Positive tests
        assertTrue("testIsArchive 1", ClassUtil.isArchive("a.jar"));
        assertTrue("testIsArchive 2", ClassUtil.isArchive("b.zip"));
        assertTrue("testIsArchive 3", ClassUtil.isArchive("c.ear"));
        assertTrue("testIsArchive 4", ClassUtil.isArchive("d.war"));
        assertTrue("testIsArchive 5", ClassUtil.isArchive("AFD.JAR"));
        assertTrue("testIsArchive 6", ClassUtil.isArchive("ASDF.ZIP"));
        assertTrue("testIsArchive 7", ClassUtil.isArchive("XYZ.EAR"));
        assertTrue("testIsArchive 8", ClassUtil.isArchive("ABC.WAR"));
        assertTrue("testIsArchive 9", ClassUtil.isArchive("a.b.jar"));
        assertTrue("testIsArchive 10", ClassUtil.isArchive("C.D.ZIP"));
        assertTrue("testIsArchive 11", ClassUtil.isArchive("a.b.ear"));
        assertTrue("testIsArchive 12", ClassUtil.isArchive("C.D.war"));
    }

    
    /**
     * Negative tests isArchive()
     */
    public void testIsArchiveNegative()
    {
        logger_.info("Running testIsArchiveNegative...");

        // Negative tests        
        assertTrue("testIsArchiveFailure 1", !ClassUtil.isArchive(""));
        assertTrue("testIsArchiveFailure 2", !ClassUtil.isArchive(" "));
        assertTrue("testIsArchiveFailure 3", !ClassUtil.isArchive("jar"));
        assertTrue("testIsArchiveFailure 4", !ClassUtil.isArchive("ZIP"));
        assertTrue("testIsArchiveFailure 5", !ClassUtil.isArchive("ear"));
        assertTrue("testIsArchiveFailure 6", !ClassUtil.isArchive("WaR"));        
        assertTrue("testIsArchiveFailure 7", !ClassUtil.isArchive("df.jarx"));
        assertTrue("testIsArchiveFailure 8", !ClassUtil.isArchive("df.zipo"));
        assertTrue("testIsArchiveFailure 9", !ClassUtil.isArchive("df.earx"));
        assertTrue("testIsArchiveFailure 10", !ClassUtil.isArchive("df.waro"));
    }

    
    /**
     * Positive tests isClassFile()
     */ 
    public void testIsClassFile()
    {
        logger_.info("Running testIsClassFile...");
        
        // Positive tests
        assertTrue(ClassUtil.isClassFile("a.class"));
        assertTrue(ClassUtil.isClassFile("A.CLASS"));
        assertTrue(ClassUtil.isClassFile("asdfasf.class"));
        assertTrue(ClassUtil.isClassFile("abc$123.class")); 
        assertTrue(ClassUtil.isClassFile("abc_123.CLASS"));
    }
    
    
    /**
     * Negative tests for isClassFile()
     */ 
    public void testIsClassFileNegative()
    {
        logger_.info("Running testIsClassFileNegative...");
        
        // Negative tests        
        assertTrue(!ClassUtil.isArchive(".class"));
        assertTrue(!ClassUtil.isArchive("class "));
        assertTrue(!ClassUtil.isArchive(".class."));
        assertTrue(!ClassUtil.isArchive("a.b.class.x"));        
        assertTrue(!ClassUtil.isArchive("X.CLASS.X"));
    }
    
    
    /**
     * Tests packageToPath()
     */
    public void testPackageToPath()
    {
        logger_.info("Running testPackageToPath...");
        
        assertEquals("a", ClassUtil.packageToPath("a"));

        assertEquals("a" + File.separatorChar + "b" , 
            ClassUtil.packageToPath("a.b"));
                
        assertEquals("a" + File.separatorChar + 
                     "b" + File.separatorChar +
                     "c" + File.separatorChar + 
                     "d", ClassUtil.packageToPath("a.b.c.d"));
    }

    
    /**
     * Tests pathToPackage()
     */
    public void testPathToPackage()
    {
        logger_.info("Running testPathToPackage...");
        
        assertEquals("", ClassUtil.pathToPackage(""));
        assertEquals("  ", ClassUtil.pathToPackage("  "));
        assertEquals("", ClassUtil.pathToPackage("\\"));
        assertEquals("", ClassUtil.pathToPackage("/"));
        assertEquals("a", ClassUtil.pathToPackage("a"));
        assertEquals("a.b", ClassUtil.pathToPackage("a\\b"));
        assertEquals("a.b", ClassUtil.pathToPackage("a/b"));
        assertEquals("a.b", ClassUtil.pathToPackage("/a/b/"));
        assertEquals("a.b", ClassUtil.pathToPackage("\\a\\b\\"));
        
        String s = File.separator;
        
        assertEquals(
            "a.b.c.d", 
            ClassUtil.pathToPackage("a" + s + "b" + s + "c" + s + "d"));
    }

    
    /**
     * Tests getMatchingClasses() for null, zero, one, and > 1 as input.
     */
    public void testGetMatchingClasses()
    {
        logger_.info("Running testGetMatchingClasses...");
        
        // Null
        assertEquals(0, ClassUtil.getMatchingClasses(null).length);
        
        // Zero
        assertEquals(0, ClassUtil.getMatchingClasses(new Object[0]).length);
        
        // One
        Class[] one = ClassUtil.getMatchingClasses(new Object[] {"whoopee"});
        assertEquals(1, one.length);
        assertEquals(String.class, one[0]);
        
        // Many
        Object[] objs = 
            new Object[] {"whoopee", new Integer(3), new ArrayList()};
        
        Class[] many = ClassUtil.getMatchingClasses(objs);
        assertEquals(3, many.length);
        assertEquals(String.class, many[0]);
        assertEquals(Integer.class, many[1]);
        assertEquals(ArrayList.class, many[2]);
    }
    
    
    /**
     * Tests getClassLocation() from a jar file.
     */
    public void testGetClassLocationFromJar() throws Exception
    {
        logger_.info("Running testGetClassLocationFromJar...");
        
        // Try a system class
        URL loc1 = ClassUtil.getClassLocation(Object.class);
        logger_.info("Class Location = " + loc1);
        assertNotNull(loc1);
        assertTrue(loc1.toString().indexOf("rt.jar") >= 0);
        assertTrue(loc1.toString().indexOf("Object.class") >= 0);
    }
    
    
    /**
     * Tests getClassLocation() from the file system
     */
    public void testGetClassLocationFromFileSystem() throws Exception
    {
        logger_.info("Running testGetClassLocationFromFileSystem...");
        
        // Try a toolbox class
        URL loc2 = ClassUtil.getClassLocation(ClassUtil.class);    
        logger_.info("Class Location = " + loc2);
        assertNotNull(loc2);
        assertTrue(loc2.toString().indexOf("toolbox") >= 0);
        assertTrue(loc2.toString().indexOf("ClassUtil.class") >= 0);
    }
    
    
    /**
     * Tests getClassLocation() from a dynamically created class file.
     */
    public void testGetClassLocationFromDynamicClass() throws Exception
    {
        logger_.info("Running testGetClassLocationFromDynamicClass...");
        
        CtClass c = ClassPool.getDefault().makeClass("DynoClass");
        Class dynoClass = c.toClass();
            
        // Try a dynamically created class
        URL loc3 = ClassUtil.getClassLocation(dynoClass);    
        logger_.info("Class Location = " + loc3);
        assertNull(loc3);
    }
}