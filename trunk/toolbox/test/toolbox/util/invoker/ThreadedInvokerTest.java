package toolbox.util.invoker;

import junit.framework.TestCase;
import junit.textui.TestRunner;

import org.apache.log4j.Logger;

import toolbox.util.ElapsedTime;
import toolbox.util.ThreadUtil;

/**
 * Unit test for ThreadedInvoker.
 * 
 * @see toolbox.util.invoker.ThreadedInvoker
 */
public class ThreadedInvokerTest extends TestCase
{
    private static final Logger logger_ =
        Logger.getLogger(ThreadedInvokerTest.class);

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
        TestRunner.run(ThreadedInvokerTest.class);
    }

    //--------------------------------------------------------------------------
    // Unit Tests
    //--------------------------------------------------------------------------

    /**
     * Tests that a call to invoke(Runnable) method returns before the amount of
     * time that it takes to execute the method.
     * 
     * @throws Exception on error.
     */
    public void testInvokeRunnable() throws Exception
    {
        logger_.info("Running testInvokeRunnable...");

        int delay = 2000;
        Invoker invoker = new ThreadedInvoker();
        TimedRunner invokable = new TimedRunner(delay);
        ElapsedTime time = new ElapsedTime();
        invoker.invoke(invokable);
        time.setEndTime();
        invoker.destroy();
        
        assertTrue(
            "Method did not execute in expected amount of time",
            time.getTotalMillis() < delay);

        Thread.sleep(delay + 1000); // 1 sec for invocation overhead

        assertTrue("Method was not invoked", invokable.wasInvoked());
    }

    
    /**
     * Tests that a call to invoke(Object, Method, Object[]) returns before the 
     * amount of time that it takes to execute the method.
     * 
     * @throws Exception on error.
     */
    public void testInvokeReflectively() throws Exception
    {
        logger_.info("Running testInvokeReflectively...");

        int delay = 2000;
        Invoker invoker = new ThreadedInvoker();
        TimedRunner invokable = new TimedRunner(delay);
        ElapsedTime time = new ElapsedTime();
        invoker.invoke(invokable, "run", null);
        time.setEndTime();
        invoker.destroy();
        
        assertTrue(
            "Method did not execute in expected amount of time",
            time.getTotalMillis() < delay);

        Thread.sleep(delay + 1000); // 1 sec for invocation overhead

        assertTrue("Method was not invoked", invokable.wasInvoked());
    }

    
    /**
     * Stress tests invoke().
     * 
     * @throws Exception on error.
     */
    public void testInvokeStressTest() throws Exception
    {
        logger_.info("Running testInvokeStressTest...");

        int delay = 1;
        int numIterations = 1000;

        Invoker invoker = new ThreadedInvoker();
        TimedRunner[] invokables = new TimedRunner[numIterations];

        // Stress    
        for (int i = 0; i < numIterations; i++)
        {
            invokables[i] = new TimedRunner(delay);
            invoker.invoke(invokables[i]);
        }

        // Wait for everything to finish + overhead.. 
        // would be nice to have invoker.join()
        Thread.sleep((numIterations * delay) + 1000);

        // Verify
        for (int i = 0; i < numIterations; i++)
            assertTrue("Method was not invoked for iteration " + i,
                invokables[i].wasInvoked());
        
        invoker.destroy();
    }
    
    
    /**
     * Test exception thrown during invocation.
     * 
     * @throws Exception on error.
     */
    public void testInvokeThrowsException() throws Exception
    {
        logger_.info("Running testInvokeThrowsException...");
        
        RuntimeException re = 
            new IllegalArgumentException("This exception is OK!");
        
        re.setStackTrace(new StackTraceElement[0]);
        ThrowsExceptionRunner ex = new ThrowsExceptionRunner(re, 1000);
        Invoker invoker = new ThreadedInvoker();
        invoker.invoke(ex, "run", null);
        ThreadUtil.sleep(2000);
        invoker.destroy();
    }
}