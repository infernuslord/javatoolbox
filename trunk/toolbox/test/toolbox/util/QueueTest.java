package toolbox.util;

import junit.framework.TestCase;
import junit.textui.TestRunner;

import org.apache.log4j.Logger;

import toolbox.util.Queue;

/**
 * Unit test for {@link toolbox.util.Queue}.
 */
public class QueueTest extends TestCase
{
    private static final Logger logger_ = Logger.getLogger(QueueTest.class);

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
        TestRunner.run(QueueTest.class);
    }

    //--------------------------------------------------------------------------
    // Unit Tests
    //--------------------------------------------------------------------------

    /**
     * Tests the default constructor.
     */
    public void testConstructor()
    {
        logger_.info("Running testConstructor...");
        
        Queue q = new Queue();
        assertNotNull(q);
    }
    
    
    /**
     * Tests enqueue()
     */    
    public void testEnqueue()
    {
        logger_.info("Running testEnqueue...");
        
        String token = "blah";
        Queue q = new Queue();
        q.enqueue(token);

        assertEquals(1, q.size());
        assertTrue(!q.isEmpty());
        assertEquals(token, q.dequeue());
    }
    
    
    /**
     * Tests dequeue()
     */    
    public void testDequeue()
    {
        logger_.info("Running testDequeue...");
        
        String token = "blah";
        Queue q = new Queue();
        q.enqueue(token);

        assertEquals(token, q.dequeue());
        assertEquals(0, q.size());
        assertTrue(q.isEmpty());
    }

    
    /**
     * Tests dequeue() when empty.
     */    
    public void testDequeueWhenEmpty()
    {
        logger_.info("Running testDequeueWhenEmpty...");
        
        Queue q = new Queue();
        assertNull(q.dequeue());
    }

    
    /**
     * Tests peek()
     */
    public void testPeek()
    {
        logger_.info("Running testPeek...");
        
        String token = "blah";
        Queue q = new Queue();
        q.enqueue(token);

        assertEquals(token, q.peek());
        assertEquals(1, q.size());
        assertTrue(!q.isEmpty());
    }    
    
    
    /**
     * Tests peek() when empty.
     */    
    public void testPeekWhenEmpty()
    {
        logger_.info("Running testPeekWhenEmpty...");
        
        Queue q = new Queue();
        assertNull(q.peek());
    }

    
    
    /**
     * Tests isEmpty()
     */
    public void testIsEmpty()
    {
        logger_.info("Running testIsEmpty...");
        
        String token = "blah";
        Queue q = new Queue();
        q.enqueue(token);

        assertTrue(!q.isEmpty());
        q.dequeue();
        assertTrue(q.isEmpty());
    }    

    
    /**
     * Tests size()
     */
    public void testSize()
    {
        logger_.info("Running testSize...");
        
        Queue q = new Queue();
        
        assertEquals(0, q.size());
        
        q.enqueue("one");
        
        assertEquals(1, q.size());
        
        for (int i = 0; i < 10; i++)
            q.enqueue(i + "");
            
        assertEquals(11, q.size());
        
        for (int i = 0; i < 11; i++)
            q.dequeue();
            
        assertEquals(0, q.size());
    }
}