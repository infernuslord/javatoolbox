package toolbox.util.concurrent;

import junit.framework.TestCase;
import junit.textui.TestRunner;

import org.apache.log4j.Logger;

import toolbox.util.ArrayUtil;

/**
 * Unit test for BatchingQueueReader.
 */
public class BatchingQueueReaderTest extends TestCase
{
    private static final Logger logger_ = 
        Logger.getLogger(BatchingQueueReaderTest.class);

    //--------------------------------------------------------------------------
    // Fields
    //--------------------------------------------------------------------------
    
    /**
     * Queue to read from in batch mode.
     */
    private BlockingQueue qout_;

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
        TestRunner.run(BatchingQueueReaderTest.class);
    }

    //--------------------------------------------------------------------------
    // Overrides TestCase
    //--------------------------------------------------------------------------
    
    /**
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception
    {
        qout_ = new BlockingQueue();        
    }
    
    //--------------------------------------------------------------------------
    //  Unit Tests
    //--------------------------------------------------------------------------
    
    /**
     * Test notification.
     * 
     * @throws Exception on error.
     */
    public void testNextBatch() throws Exception
    {
        logger_.info("Running testNextBatch...");
        
        // Create queue and stuff with > 1 objects
        BlockingQueue qin = new BlockingQueue();
        qin.push("one");
        qin.push("two");
        qin.push("three");
        
        // Attaching batching reader and start
        BatchingQueueReader reader = new BatchingQueueReader(qin);
        IBatchingQueueListener listener = new TestQueueListener();
        reader.addBatchingQueueListener(listener);
        reader.start();


        // Pulling from the queue should return all elements as a single array
        Object[] batch = (Object[]) qout_.pull();
        
        // Test returned elements equal to pushed elements
        assertEquals(3, batch.length);
        assertEquals(batch[0], "one");
        assertEquals(batch[1], "two");
        assertEquals(batch[2], "three");
        
        reader.removeBatchingQueueListener(listener);
        reader.stop();
    }

    
    /**
     * Test start() and stop().
     * 
     * @throws Exception on error.
     */
    public void testStartStop() throws Exception
    {
        logger_.info("Running testStartStop...");
        
        BlockingQueue q = new BlockingQueue();
        q.push("one");
        q.push("two");
        q.push("three");
        
        BatchingQueueReader bqr = new BatchingQueueReader(q);
        IBatchingQueueListener queueListener = new TestQueueListener();
        bqr.addBatchingQueueListener(queueListener);
        bqr.start();
        
        logger_.info("Queue reader started...");
        
        Object[] batch = (Object[]) qout_.pull();
        
        logger_.info("Pulled next batch from queue...");
        
        assertEquals(batch[0], "one");
        assertEquals(batch[1], "two");
        assertEquals(batch[2], "three");
        
        logger_.info("About to stop ...");
        
        bqr.stop();
        
        assertTrue(q.size() == 0);
        
        logger_.info("About to restart...");
        
        bqr.start();
        
        logger_.info("About to stop again...");
        
        bqr.stop();
        
        assertTrue(q.size() == 0);        
        
        q.push("three");
        q.push("four");
        q.push("five");
        
        logger_.info(q);
        
        logger_.info("About to start 3rd time...");
        
        bqr.start();
        
        logger_.info(q);        
        
        Object[] batch2 = (Object[]) qout_.pull();
        
        assertEquals(batch2[0], "three");
        assertEquals(batch2[1], "four");
        assertEquals(batch2[2], "five");
        
        bqr.removeBatchingQueueListener(queueListener);
        bqr.stop();
    }

    
    /**
     * Test start() if called twice.
     * 
     * @throws Exception on error.
     */
    public void testStartTwice() throws Exception
    {
        logger_.info("Running testStartTwice...");
        
        BatchingQueueReader r = new BatchingQueueReader(new BlockingQueue());
        r.start();
        
        try
        {
            r.start();
            fail("Reader should be able to start if already running.");
        }
        catch (IllegalStateException ise)
        {
            assertTrue(true);
        }      
        finally
        {
            r.stop();  
        }
    }

    
    /**
     * Test stop() if called twice.
     * 
     * @throws Exception on error.
     */
    public void testStopTwice() throws Exception
    {
        logger_.info("Running testStopTwice...");
        
        BatchingQueueReader r = new BatchingQueueReader(new BlockingQueue());
        r.start();
        r.stop();
        
        try
        {
            r.stop();
            fail("Reader should not be able to be stopped twice");
        }
        catch (IllegalStateException ise)
        {
            assertTrue(true);
        }        
    }
    
    //--------------------------------------------------------------------------
    // Helper Classes
    //--------------------------------------------------------------------------
    
    /**
     * Listens for next batch off the queue.
     */
    class TestQueueListener implements IBatchingQueueListener
    {
        /**
         * @see toolbox.util.concurrent.IBatchingQueueListener#nextBatch(
         *      java.lang.Object[])
         */
        public void nextBatch(Object[] elements)
        {
            logger_.info("nextBatch: " + ArrayUtil.toString(elements));
            qout_.push(elements);
        }
    }
}