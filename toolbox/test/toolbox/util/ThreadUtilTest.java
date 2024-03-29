package toolbox.util;

import java.io.PrintWriter;
import java.io.Writer;

import junit.framework.TestCase;
import junit.textui.TestRunner;

import org.apache.log4j.Logger;

import toolbox.util.ArrayUtil;
import toolbox.util.ElapsedTime;
import toolbox.util.ThreadUtil;

/**
 * Unit test for {@link toolbox.util.ThreadUtil}.
 */
public class ThreadUtilTest extends TestCase
{
    private static final Logger logger_ = 
        Logger.getLogger(ThreadUtilTest.class);

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
        TestRunner.run(ThreadUtilTest.class);
    }
    
    //--------------------------------------------------------------------------
    // Unit Tests
    //--------------------------------------------------------------------------
    
    /**
     * Tests sleep()
     */
    public void testSleep() 
    {
        logger_.info("Running testSleep...");
        
        // Not much to test
        ThreadUtil.sleep(500);
    }
    
    
    /**
     * Tests join(Thread)
     */
    public void testJoinThread()
    {
        logger_.info("Running testJoinThread...");
        
        Thread t = new Thread(new DelayedRunner(2000));
        t.start();
        ThreadUtil.join(t);
        
        // Negative
        ThreadUtil.join(null);
    }

    
    /**
     * Tests join(Thread, millis)
     */
    public void testJoinThreadTimed()
    {
        logger_.info("Running testJoinThreadTimed...");
        
        Thread t = new Thread(new DelayedRunner(4000));
        t.start();
        ThreadUtil.join(t, 1000);
        assertTrue("Thread should still be alive", t.isAlive());
        
        // Negative
        ThreadUtil.join(null, 1000);
    }

    
    /**
     * Tests join()
     */
    public void testJoin()
    {
        logger_.info("Running testJoin...");
        
        ThreadUtil.join(1000);
    }

    
    /**
     * Tests run() on a method with no args.
     * 
     * @throws Exception on error.
     */
    public void testRunSimple() throws Exception
    {
        logger_.info("Running testRunSimple...");
        
        Tester target = new Tester();
        ThreadUtil.run(target, "pingSimple", null).join();
        assertTrue("ping was not executed", target.pingSimpleCalled_);
    }

    
    /**
     * Tests run() on a method with a single arg.
     * 
     * @throws Exception on error.
     */
    public void testRunOneArg() throws Exception
    {
        logger_.info("Running testRunOneArg...");
        
        Tester target = new Tester();
        ThreadUtil.run(target, "pingOneArg", "hello").join();
        assertTrue("ping was not executed", target.pingOneArgCalled_);
    }

    
    /**
     * Tests run() on a method with simple args and arrays.
     * 
     * @throws Exception on error.
     */
    public void testRunArgs() throws Exception
    {
        logger_.info("Running testRunArgs...");
        
        Tester target = new Tester();
        
        // Call method with args
        Object[] params = new Object[] 
        {
            "string1",
            new String[] {"element1", "element2"}
        };
       
        ThreadUtil.run(target, "pingArgs", params).join();
        assertTrue("pingArgs was not executed", target.pingArgsCalled_);
    }
 
    
    /**
     * Tests run() on a method with complex arg types.
     * 
     * @throws Exception on error.
     */
    public void testRunComplex() throws Exception
    {
        logger_.info("Running testRunComplex...");
        
        Tester target = new Tester();
        
        Writer writer = new PrintWriter(System.out);
        
        ThreadUtil.run(target, "pingComplex", 
            new Object[] 
            {  
                writer, 
                new Integer(10), 
                new Integer(1000), 
                "hello from testRunComplex()"
            }).join();
            
        assertTrue("pingComplex was not executed", target.pingComplexCalled_);
    } 

    
    /**
     * Tests run() on a method with ???
     * 
     * @throws Exception on error.
     */
    public void testRunAssignable() throws Exception
    {
        logger_.info("Running testRunAssignable...");
        
        Tester target = null; 
        
        Writer writer = new PrintWriter(System.out);
        PrintWriter pw = new PrintWriter(System.out);
        
        target = new Tester();
        ThreadUtil.run(target, "pingAssignable", writer).join();
        assertTrue("pingAssignable was not executed for w", 
                target.pingAssignableCalled_);
        
        target = new Tester();
        ThreadUtil.run(target, "pingAssignable", pw).join();
        assertTrue("pingAssignable was not executed for pw", 
                target.pingAssignableCalled_);
        
    } 

    
    /**
     * Tests run() on a method with primitive args.
     * 
     * @throws Exception on error.
     */
    public void testRunPrimitive() throws Exception
    {
        logger_.info("Running testRunPrimitive...");
        
        Tester target = new Tester();
               
        ThreadUtil.run(target, "pingPrimitive", 
            new Object[] 
            { 
                new Integer(10), 
                //new Character('y'),
                new Boolean(true),
                new Long(99L),
                new Float(100.3f)
            }).join();
            
        assertTrue("pingPrimitive was not executed", 
            target.pingPrimitiveCalled_);
    } 

    
    /**
     * Tests run() with a bogus method.
     * 
     * @throws Exception on error.
     */
    public void testRunBogusMethod() throws Exception
    {
        logger_.info("Running testRunBogusMethod...");
        
        Tester target = new Tester();
        
        try
        {
            ThreadUtil.run(target, "bogusMethod", null).join();
            fail("Should have throw exception on bogus method");
        }
        catch (IllegalArgumentException iea)
        {
            ; // Behaves as expected
        }
    } 

    
    /**
     * Tests run() with a bogus parameter.
     * 
     * @throws Exception on error.
     */
    public void testRunBogusParameter() throws Exception
    {
        logger_.info("Running testRunBogusParameter...");
        
        Tester target = new Tester();
        
        try
        {
            ThreadUtil.run(target, "pingSimple", "bogusParam").join();
            fail("Should have throw exception on bogus parameter");
        }
        catch (IllegalArgumentException iea)
        {
            ; // Behaves as expected
        }
    } 

    
    /**
     * Tests run() with a bogus target.
     * 
     * @throws Exception on error.
     */
    public void testRunBogusTarget() throws Exception
    {
        logger_.info("Running testRunBogusTarget...");
        
        try
        {
            ThreadUtil.run("bogusTarget", "dontMatter", "dontMatter").join();
            fail("Should have throw exception on bogus target");
        }
        catch (IllegalArgumentException iea)
        {
            ; // Behaves as expected            
        }
    } 

    
    /**
     * Tests run() using the full signature with Class[].
     * 
     * @throws Exception on error.
     */
    public void testRunWithClassesIncluded() throws Exception
    {
        logger_.info("Running testRunWithClassesIncluded...");
        
        Tester target = new Tester();
        
        ThreadUtil.run(
            target, 
            "pingOneArg", 
            new Object[] {"hello"}, 
            new Class[] {String.class}).join();
            
        assertTrue("ping was not executed", target.pingOneArgCalled_);
    }
 
    
    /**
     * Tests from an inner class.
     * 
     * @throws Exception on error.
     */
    public void testInner() throws Exception
    {
        logger_.info("Running testInner...");
        
        Tester target = new Tester();
        target.testFromInnerClass();
    }  

    
    /**
     * Tests stop(). 
     * 
     * @throws Exception on error
     */
    public void testStop() throws Exception
    {
        logger_.info("Running testStop...");
        
        ElapsedTime et = new ElapsedTime();
        Thread t = new Thread(new DelayedRunner(10000));
        ThreadUtil.stop(t);
        et.setEndTime();
        assertTrue(!t.isAlive());
        logger_.debug(et);
    }

    
    /**
     * Tests stop() with a timeout. 
     * 
     * @throws Exception on error
     */
    public void testStopTimeout() throws Exception
    {
        logger_.info("Running testStopTimeout...");
        
        ElapsedTime et = new ElapsedTime();
        Thread t = new Thread(new DelayedRunner(10000));
        ThreadUtil.stop(t, 5000);
        et.setEndTime();
        assertTrue(!t.isAlive());
        logger_.debug(et);
    }

    
    /**
     * Tests toString(). 
     * 
     * @throws Exception on error.
     */
    public void testToString() throws Exception
    {
        logger_.info("Running testToString...");
        
        Thread t = new Thread(new DelayedRunner(100));
        logger_.debug("toString() before start()\n" + ThreadUtil.toString(t));
        t.start();
        logger_.debug("toString() after start()\n" + ThreadUtil.toString(t));
        t.join();
        logger_.debug("toString() after join()\n" + ThreadUtil.toString(t));
    }

    //--------------------------------------------------------------------------
    //  Inner Classes
    //--------------------------------------------------------------------------
     
    /**
     * Test class for testRun().
     */   
    public class Tester
    {
        private boolean pingAssignableCalled_;
        private boolean pingSimpleCalled_;
        private boolean pingOneArgCalled_;
        private boolean pingArgsCalled_;
        private boolean pingComplexCalled_;
        private boolean pingPrimitiveCalled_;
        private boolean pingInnerCalled_;
               
        /**
         * Creates a new Tester. 
         */
        public Tester()
        {
            pingSimpleCalled_    = false;
            pingOneArgCalled_    = false;
            pingArgsCalled_      = false;
            pingComplexCalled_   = false;
            pingPrimitiveCalled_ = false;
            pingInnerCalled_     = false;
        }
        
        
        /**
         * Simplest method - no args. 
         */
        public void pingSimple()
        {
            pingSimpleCalled_ = true;
            logger_.debug("Called ping()");
        }
        
        
        /**
         * Method with a single arg.
         * 
         * @param str String.
         */
        public void pingOneArg(String str)
        {
            pingOneArgCalled_ = true;
            logger_.debug("Called pingOneArg(" + str + ", " + str + ")");
        }
        
        
        /**
         * Method with args of complex type.
         * 
         * @param str String.
         * @param strArray String array.
         */
        public void pingArgs(String str, String[] strArray)
        {
            pingArgsCalled_ = true;
            logger_.debug("Called pingArgs(" + str + ", " + 
                ArrayUtil.toString(strArray, false) + ")");
        }
 
        
        /**
         * Test method with complex args.
         * 
         * @param pw Writer.
         * @param i Integer.
         * @param i2 Integer.
         * @param s String.

         */       
        public void pingComplex(Writer pw,  Integer i, Integer i2, String s)
        {
            pingComplexCalled_ = true;
            logger_.debug("Called write delayed with " + 
                pw + i + " " + i2 + " " + s);
        }
        
        
        /**
         * Test method method with primitive args.
         * 
         * @param a int.
         * @param b boolean.
         * @param l long.
         * @param f float.
         */
        public void pingPrimitive(int a, /*char c,*/ boolean b, long l, float f)
        {
          
            pingPrimitiveCalled_ = true;
            
            logger_.debug("Called pingPrimitive with " + 
                a + " " + b + " " + l + " " + f);
        }
        
        
        /**
         * Test method which will accept Writer/PrintWriter.
         * 
         * @param w Writer.
         */
        public void pingAssignable(Writer w)
        {
            logger_.debug(w.getClass().getName());
            pingAssignableCalled_ = true;        
        }
        
        
        /**
         * Simplest method - no args. 
         */
        public void pingInner()
        {
            pingInnerCalled_ = true;
            logger_.debug("Called pingInner()");
        }
        
        
        /**
         * Test calling a method from in innerclass on itself.
         * 
         * @throws Exception on error.
         */
        public void testFromInnerClass() throws Exception
        {
            ThreadUtil.run(Tester.this, "pingInner", null).join();
            assertTrue("pingInner() not called", pingInnerCalled_);
        }
        
        
        /**
         * Returns the pingArgsCalled.
         * 
         * @return boolean.
         */
        public boolean isPingArgsCalled()
        {
            return pingArgsCalled_;
        }

        
        /**
         * Returns the pingComplexCalled.
         * 
         * @return boolean
         */
        public boolean isPingComplexCalled()
        {
            return pingComplexCalled_;
        }

        
        /**
         * Returns the pingInnerCalled.
         * 
         * @return boolean
         */
        public boolean isPingInnerCalled()
        {
            return pingInnerCalled_;
        }

        
        /**
         * Returns the pingPrimitiveCalled.
         * 
         * @return boolean
         */
        public boolean isPingPrimitiveCalled()
        {
            return pingPrimitiveCalled_;
        }

        
        /**
         * Returns the pingSimpleCalled.
         * 
         * @return boolean
         */
        public boolean isPingSimpleCalled()
        {
            return pingSimpleCalled_;
        }

        
        /**
         * Sets the pingArgsCalled.
         * 
         * @param pingArgsCalled The pingArgsCalled to set.
         */
        public void setPingArgsCalled(boolean pingArgsCalled)
        {
            this.pingArgsCalled_ = pingArgsCalled;
        }

        
        /**
         * Sets the pingComplexCalled.
         * 
         * @param pingComplexCalled The pingComplexCalled to set.
         */
        public void setPingComplexCalled(boolean pingComplexCalled)
        {
            this.pingComplexCalled_ = pingComplexCalled;
        }

        
        /**
         * Sets the pingInnerCalled.
         * 
         * @param pingInnerCalled The pingInnerCalled to set.
         */
        public void setPingInnerCalled(boolean pingInnerCalled)
        {
            this.pingInnerCalled_ = pingInnerCalled;
        }

        
        /**
         * Sets the pingPrimitiveCalled.
         * 
         * @param pingPrimitiveCalled The pingPrimitiveCalled to set.
         */
        public void setPingPrimitiveCalled(boolean pingPrimitiveCalled)
        {
            this.pingPrimitiveCalled_ = pingPrimitiveCalled;
        }

        
        /**
         * Sets the pingSimpleCalled.
         * 
         * @param pingSimpleCalled The pingSimpleCalled to set.
         */
        public void setPingSimpleCalled(boolean pingSimpleCalled)
        {
            this.pingSimpleCalled_ = pingSimpleCalled;
        }
    }

    //--------------------------------------------------------------------------
    // DelayedRunner 
    //--------------------------------------------------------------------------
    
    /**
     * Delays execution for a number of milliseconds.
     */    
    class DelayedRunner implements Runnable
    {
        private int delay_;
        
        /**
         * Creates a DelayedRunner
         * 
         * @param delay Delay in milliseconds.
         */
        public DelayedRunner(int delay)
        {
            delay_ = delay;           
        }
     
        
        /**
         * @see java.lang.Runnable#run()
         */
        public void run()
        {
            ThreadUtil.sleep(delay_);
        }
    }
}