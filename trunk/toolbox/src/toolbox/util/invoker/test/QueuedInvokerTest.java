package toolbox.util.invoker.test;

import java.io.StringWriter;
import java.io.Writer;

import junit.framework.TestCase;
import junit.textui.TestRunner;

import org.apache.log4j.Appender;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;
import org.apache.log4j.WriterAppender;

import toolbox.util.ElapsedTime;
import toolbox.util.StringUtil;
import toolbox.util.ThreadUtil;
import toolbox.util.invoker.Invoker;
import toolbox.util.invoker.QueuedInvoker;

/**
 * Unit test for QueuedInvoker.
 */
public class QueuedInvokerTest extends TestCase
{
    private static final Logger logger_ =
        Logger.getLogger(QueuedInvokerTest.class);
        
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
        TestRunner.run(QueuedInvokerTest.class);
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

        
        int queueSize = 5;
        int delay = 2000;
        Invoker invoker = new QueuedInvoker();

        TimedRunner invokables[] = new TimedRunner[queueSize];

        try
        {
            for (int i = 0; i < queueSize; i++)
            {
                invokables[i] = new TimedRunner(delay);
                ElapsedTime time = new ElapsedTime();
                invoker.invoke(invokables[i]);
                time.setEndTime();
            }
    
            Thread.sleep((delay * queueSize) + 1000);
    
            for (int i = 0; i < invokables.length; i++)
            {
                assertTrue("Method was not invoked", 
                    invokables[i].wasInvoked());
    
                // Compare current begin timestamp to previous end timestamp to 
                // verify the invokables were executed in sequence.
                if (i >= 1)
                {
                    long begin = invokables[i].getBegin().getTime();
                    long end = invokables[i - 1].getEnd().getTime();
    
                    assertTrue(
                        "Time sequence mismatch: current.begin: "
                            + begin
                            + "  previous.end: "
                            + end,
                        end <= begin);
                }
            }
        }
        finally
        {
            invoker.shutdown();
        }
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

        int queueSize = 5;
        int delay = 2000;
        Invoker invoker = new QueuedInvoker();

        TimedRunner invokables[] = new TimedRunner[queueSize];

        try
        {
            for (int i = 0; i < queueSize; i++)
            {
                invokables[i] = new TimedRunner(delay);
                ElapsedTime time = new ElapsedTime();
                invoker.invoke(invokables[i], "run", null);
                time.setEndTime();
            }
    
            Thread.sleep((delay * queueSize) + 1000);
    
            for (int i = 0; i < invokables.length; i++)
            {
                assertTrue("Method was not invoked", 
                    invokables[i].wasInvoked());
    
                // Compare current begin timestamp to previous end timestamp to
                //  verify invokables were executed in sequence.
                if (i >= 1)
                {
                    long begin = invokables[i].getBegin().getTime();
                    long end = invokables[i - 1].getEnd().getTime();
    
                    assertTrue(
                        "Time sequence mismatch: current.begin: "
                            + begin
                            + "  previous.end: "
                            + end,
                        end <= begin);
                }
            }
        }
        finally
        {
            invoker.shutdown();
        }
    }

    
    /**
     * Stress tests invoke()
     * 
     * @throws Exception on error.
     */
    public void testInvokeStressTest() throws Exception
    {
        logger_.info("Running testInvokeStressTest...");

        int delay = 1;
        int numIterations = 1000;

        QueuedInvoker invoker = new QueuedInvoker();
        TimedRunner[] invokables = new TimedRunner[numIterations];

        try
        {
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
            for (int i = 0; i < invokables.length; i++)
            {
                assertTrue("Method was not invoked",
                    invokables[i].wasInvoked());

                // Compare current begin timestamp to previous end timestamp to
                // verify invokables where execute in sequence.
                if (i >= 1)
                {
                    long begin = invokables[i].getBegin().getTime();
                    long end = invokables[i - 1].getEnd().getTime();

                    assertTrue(
                        "Time sequence mismatch: current.begin: "
                            + begin
                            + "  previous.end: "
                            + end,
                        end <= begin);
                }
            }
        }
        finally
        {
            invoker.shutdown();
        }
    }

    
    /**
     * Tests shutdown() while there are still pending pending invocations in
     * the queue.
     *
     * @throws Exception on error.
     */
    public void testShutdownPremature() throws Exception
    {
        logger_.info("Running testShutdownPremature...");

        int queueSize = 5;
        int delay = 1000;
        QueuedInvoker invoker = new QueuedInvoker();

        try
        {
            for (int i = 0; i < queueSize; i++)
                invoker.invoke(new TimedRunner(delay));
        }
        finally
        {
            invoker.shutdown();
        }
    }
    
    
    /**
     * Tests invoke() failure.
     *
     * @throws Exception on error.
     */
    public void testInvokeFailure() throws Exception
    {
        logger_.info("Running testInvokeFailure...");

        //
        // We have to hook into the logger of the QueuedInvoker to
        // capture the error message that should be generated.
        //
        Logger l = Logger.getLogger(QueuedInvoker.class.getName());
        Writer sw = new StringWriter();
        Appender appender = new WriterAppender(new SimpleLayout(), sw);
        l.addAppender(appender);

        //
        // Let it rip...
        //
        Invoker invoker = new QueuedInvoker();
        invoker.invoke(new String(""), "invalid_method", null);
        
        //
        // Needs time to run
        //
        ThreadUtil.sleep(2000);
        
        //
        // Make sure the error message contains the name of the bogus method
        //
        String error = sw.toString();
        logger_.debug(StringUtil.addBars(error));
        assertTrue(error.indexOf("invalid_method") >= 0);
        
        //
        // Cleanup
        //
        l.removeAppender(appender);
    }
}