package toolbox.util.concurrent;

import java.util.ArrayList;

import org.apache.log4j.Logger;

/**
 * Queue that blocks on calls to pull() until an element is available
 */
public class BlockingQueue
{
    /** Logger **/
    private static final Logger logger_ =
        Logger.getLogger(BlockingQueue.class);
        
    private ArrayList   queue_      = null;
    private Semaphore   semaphore_  = null;
    private Mutex       mutex_      = new Mutex();

    //--------------------------------------------------------------------------
    // Constuctors
    //--------------------------------------------------------------------------
    
    /**
     * Default constructor
     */
    public BlockingQueue()
    {
        semaphore_ = new Semaphore(0);
        queue_ = new ArrayList(50);
    }

    //--------------------------------------------------------------------------
    // Public
    //--------------------------------------------------------------------------
        
    /**
     * Pulls element off the queue. Blocks until an element is available if the
     * queue is empty
     * 
     * @return  Next element
     * @throws  InterruptedException on error
     */
    public Object pull() throws InterruptedException
    {
        try
        {
            try
            {
                semaphore_.acquire();
                mutex_.acquire();
                Object obj = queue_.remove(0);

                return obj;
            }
            finally
            {
                mutex_.release();
            }
        }
        catch (InterruptedException e)
        {
            throw e;
        }
    }


    /**
     * Pushes an element onto the queue.
     * 
     * @param  obj  Object to push onto the queue
     * @throws InterruptedException on error
     */
    public void push(Object obj) throws InterruptedException
    {
        try
        {
            mutex_.acquire();
            queue_.add(obj);
            semaphore_.release();
        }
        catch (Exception e)
        {
            logger_.error(e);
        }
        finally
        {
            mutex_.release();
        }
    }


    /**
     * Returns the size of the queue
     * 
     * @return Queue size
     */
    public int size()
    {
        int size = 0;
        try
        {
            mutex_.acquire();
            size = queue_.size();
        }
        catch (Exception e)
        {
            logger_.error(e);
        }
        finally
        {
            mutex_.release();
        }

        return size;
    }
}