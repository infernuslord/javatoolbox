package toolbox.util.dump;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Stack;

import junit.framework.TestCase;
import junit.textui.TestRunner;

import org.apache.log4j.Logger;

import toolbox.util.StringUtil;

/**
 * Unit test for Dumper.
 */
public class DumperTest extends TestCase
{
    private static final Logger logger_ = Logger.getLogger(DumperTest.class);

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
        TestRunner.run(DumperTest.class);
    }

    //--------------------------------------------------------------------------
    //  Unit Tests
    //--------------------------------------------------------------------------
    
    /**
     * Tests an empty class.
     */
    public void testDumpEmptyObject()
    {
        logger_.info("Running testDumpEmptyObject...");
        logger_.debug(StringUtil.addBars(Dumper.dump(new EmptyClass())));
    }

    
    /**
     * Tests a class with simple fields. 
     */
    public void testDumpSimpleObject()
    {
        logger_.info("Running testDumpSimpleObject...");
        logger_.debug(StringUtil.addBars(Dumper.dump(new Address())));
    }

    
    /**
     * Tests object dumper for a simple test case.
     */
    public void testDumpNestedObject()
    {
        logger_.info("Running testDumpNestedObject...");
        logger_.debug(StringUtil.addBars(Dumper.dump(new Employee())));
    }
    
    
    /**
     * Tests dump of JFrame.
     */
    public void testDumpJFrame()
    {
        logger_.info("Running testDumpJFrame...");
        
        // Too much output
        //logger_.debug(StringUtil.addBars(Dumper.dump(new JPanel())));
    }
    
    
    /**
     * Tests dump of duplicate objects existing in the object graph.
     */
    public void testDumpMultipleReferences()
    {
        logger_.info("Running testDumpMultipleReferences...");
        logger_.debug(
            StringUtil.addBars(Dumper.dump(new MultipleReferences())));        
    }
    
    
    /** 
     * Tests dump of object with a constraining maxDepth.
     */
    public void testDumpMaxDepth()
    { 
        logger_.info("Running testDumpMaxDepth...");

        Employee emp = new Employee();
        
        logger_.debug(
            StringUtil.NL + StringUtil.BRNL + "Max depth = 1" + 
            StringUtil.NL + StringUtil.BRNL +
            StringUtil.addBars(Dumper.dump(emp, 1)));
         
        logger_.debug(
            StringUtil.NL + StringUtil.BRNL + "Max depth = 2" + 
            StringUtil.NL + StringUtil.BRNL +
            StringUtil.addBars(Dumper.dump(emp, 2)));        

        logger_.debug(
            StringUtil.NL + StringUtil.BRNL + "Max depth = 3" + 
            StringUtil.NL + StringUtil.BRNL +
            StringUtil.addBars(Dumper.dump(emp, 3)));        
    }
    
    
    /**
     * Tests dumping of collection classes.
     */    
    public void testDumpCollection()
    {
        logger_.info("Running testDumpCollection...");
        
        class CollectionDump
        {
            private Collection collection_;
            private Collection clone_;
                        
            /**
             * Creates a CollectionDump.
             */
            public CollectionDump()
            {
                collection_ = new ArrayList();
                collection_.add("one");
                collection_.add(new Country());
                collection_.add(new Employee());
                clone_ = collection_;
            }

            
            /**
             * Creates a CollectionDump.
             * 
             * @param c Collection
             */
            public CollectionDump(Collection c)
            {
                collection_ = c;
                clone_ = collection_;
            }
        }
        
        logger_.debug(StringUtil.addBars(Dumper.dump(new CollectionDump())));
    }

    
    /**
     * Tests dump(obj, depth, formatter). Creates a new formatter and adds
     * lastName_ to the list of fields to be excluded. Verifies lastName_ does
     * not show up in the dump!
     * 
     * @throws Exception on error
     */ 
    public void testDumpObjectDepthFormatter() throws Exception
    {
        logger_.info("Running testDumpObjectDepthFormatter...");
        
        Employee emp = new Employee();
        BasicDumpFormatter formatter = new BasicDumpFormatter();
        formatter.excludeFields("lastName_");
        String dump = Dumper.dump(emp, 10, formatter);
        logger_.debug(StringUtil.addBars(dump));
        assertTrue(dump.indexOf("lastName_") < 0);        
    }
     
    
    /**
     * Tests legacy version. 
     */   
    public void testLegacy()
    {
        logger_.info("Running testLegacy...");
        String dump = Dumper.dump(new D());
        logger_.debug(StringUtil.addBars(dump));
    }
            
    //--------------------------------------------------------------------------
    // EmptyClass
    //--------------------------------------------------------------------------
            
    /**
     * EmptyClass
     */
    class EmptyClass
    {
    }
    
    //--------------------------------------------------------------------------
    // Employee
    //--------------------------------------------------------------------------
    
    /**
     * Employee
     */            
    class Employee
    {
        private String  firstName_;
        private String  lastName_;
        private int     ssn_;
        private float   salary_;
        private Address address_;
        private Status  status_;
            
        /**
         * Creates a Employee.
         */
        public Employee()
        {
            firstName_ = "Daffy";
            lastName_  = "Duck";
            ssn_       = 232323;
            salary_    = 567.89f;
            
            address_ = new Address();
            status_  = new Status();
        }
    }    
    
    //--------------------------------------------------------------------------
    // Address
    //--------------------------------------------------------------------------
    
    /**
     * Address
     */
    class Address
    {
        private String street_;
        private String city_;
        private String state_;
        private String zipCode_;
        private Country country_;
                
        /**
         * Creates a Address.
         */
        public Address()
        {
            street_ = "1010 Main St";
            city_ = "Dallas";
            state_ = "TX";
            zipCode_ = "76123";
            country_ = new Country();
        }
    }

    //--------------------------------------------------------------------------
    // Country
    //--------------------------------------------------------------------------
    
    /**
     * Country
     */
    public class Country
    {
        private String country_ = "USA";
    }
    
    //--------------------------------------------------------------------------
    // Status
    //--------------------------------------------------------------------------
    
    /**
     * Status 
     */
    public class Status
    {
        private boolean citizen_ = true;
        private String  status_  = null; //"Naturalized";
    }
    
    //--------------------------------------------------------------------------
    // MultipleReferences
    //--------------------------------------------------------------------------
    
    /**
     * MultipleReferences
     */
    public class MultipleReferences
    {
        private Address address_   = new Address();
        private Address reference_ = address_; 
    }

    //--------------------------------------------------------------------------
    // Legacy Test Objects
    //--------------------------------------------------------------------------
    
    /**
     * A
     */
    class A
    {
        private int anAVariable;
        private int xDeclaredInAandB = 5;
    }
    
    /**
     * B
     */
    class B extends A
    {
        protected transient int aBVariable;
        private int xDeclaredInAandB = 8;
    }

    /**
     * C
     */
    class C extends B
    {
        volatile boolean aCVariable = false;
    }

    /**
     * D
     */
    class D extends C
    {
        //  protected Object aDVariable = new Object();
        //  private String myString2 = "aaaa\nbbbb\n\n\ncccc\n\n";
        //  Object differentType = new C();
        //private volatile static Integer synchInteger = new Integer(9);
        protected transient Fun fun = new MegaFun();
        //  static D loop = new D();
        public Stack stack = new Stack();
        //  public javax.swing.JLabel jLabel= new javax.swing.JLabel();
    }

    /**
     * Fun
     */
    class Fun extends Object
    {
        private Integer funInteger = new Integer(13);
    }

    /**
     * MegaFun
     */
    class MegaFun extends Fun
    {
        private Integer megaFunInteger = new Integer(14);
        
        /**
         * @see java.lang.Object#toString()
         */
        public String toString()
        {
            return "this is what you get from calling \"toString()\" !!!";
        }
    }

    /**
     * TestObject2
     */
    class TestObject2
    {

        public int jInt = 5;
        private transient float jFloat = 6;
        public Integer jInteger = new Integer(7);
        private Double jDouble = new Double(8);

        public D d = new D();
        //public static final TestObject2 to2 = new TestObject2();

    }
}