package toolbox.util.concurrent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import toolbox.util.ThreadUtil;

/**
 * Reads as much content off a queue as possible (batch mode) and delivers in a 
 * single call to IBatchingQueueListener.nextBatch().
 */
public class BatchingQueueReader
{
    //--------------------------------------------------------------------------
    // Fields
    //--------------------------------------------------------------------------
    
    /** 
     * Queue to read elements from. 
     */
    private BlockingQueue queue_;
    
    /** 
     * Started flag. 
     */
    private boolean started_;
    
    /** 
     * Queue Listeners. 
     */
    private List listeners_;    

    /** 
     * Batch thread. 
     */
    private Thread worker_;
    
    /** 
     * Friendly name assigned to the batch thread. 
     */
    private String name_;
    
    //--------------------------------------------------------------------------
    // Constructors
    //--------------------------------------------------------------------------
    
    /**
     * Creates a BatchingQueueReader.
     * 
     * @param queue Queue to read in batch mode from.
     */
    public BatchingQueueReader(BlockingQueue queue)
    {
        this(queue, "BatchingQueueReader");
    }

    
    /**
     * Creates a BatchingQueueReader.
     * 
     * @param queue Queue to read in batch mode from.
     * @param name Friendly name assigned to the batch thread.
     */
    public BatchingQueueReader(BlockingQueue queue, String name)
    {
        queue_ = queue;
        name_ = name;
        started_ = false;
        listeners_ = new ArrayList();
    }

    //--------------------------------------------------------------------------
    // Public
    //--------------------------------------------------------------------------
    
    /**
     * Stops the reader.
     * 
     * @throws IllegalStateException if the reader has already been stopped.
     */
    public synchronized void stop() throws IllegalStateException
    {
        if (started_)
        {
            started_  = false;
            ThreadUtil.stop(worker_, 2000);
        }
        else
        {
            throw new IllegalStateException(
                "BatchingQueueReader has already been stopped.");    
        }
    }

    
    /**
     * Starts the reader.
     * 
     * @throws IllegalStateException if the reader is already started.
     */
    public synchronized void start() throws IllegalStateException
    {
        if (!started_)
        {
            started_  = true;
            worker_ = new Thread(new Worker(), name_);
            worker_.start(); 
        }
        else
        {
            throw new IllegalStateException(
                "BatchingQueueReader has already been started.");
        }
    }

    //--------------------------------------------------------------------------
    // Event Notification
    //--------------------------------------------------------------------------
 
    /**
     * Fires notification of new batch of elements available.
     * 
     * @param elements New elements available.
     */
    protected synchronized void fireNextBatch(Object[] elements)
    {
        Iterator i = listeners_.iterator();
        
        while (i.hasNext())
        {
            IBatchingQueueListener listener = 
                (IBatchingQueueListener) i.next();
                
            listener.nextBatch(elements);
        }    
    }
    
    
    /**
     * Adds a listener. 
     * 
     * @param listener Listener to add.
     */   
    public synchronized void addBatchingQueueListener(
        IBatchingQueueListener listener)
    {
        listeners_.add(listener);
    }
 
    
    /**
     * Removes a listener.
     * 
     * @param listener Listener to remove.
     */
    public synchronized void removeBatchingQueueListener(
        IBatchingQueueListener listener)
    {
        listeners_.remove(listener);
    }
    
    //--------------------------------------------------------------------------
    // Inner Classes
    //--------------------------------------------------------------------------

    /**
     * Reads objects off the queue in as large blocks as possible and notifies
     * listeners that the next batch is available.
     */    
    class Worker implements Runnable
    {
        /**
         * @see java.lang.Runnable#run()
         */
        public void run()
        {
            //logger_.debug(method + "Batching queue reader started!");
                    
            while (started_)
            {
                try
                {
                    Object first = queue_.pull();
                    
                    int size = queue_.size();
                    
                    if (size > 0)
                    {
                        // Create array with one extra slot for the first
                        Object[] objs = new Object[size + 1];

                        // Place first elemnt in array
                        objs[0] = first;

                        // Read the rest from the queue
                        for (int i = 1; i <= size; i++)
                            objs[i] = queue_.pull();

                        fireNextBatch(objs);
                    }
                    else
                    {
                        fireNextBatch(new Object[] {first});
                    }
                }
                catch (InterruptedException e)
                {
                    break;
                }
            }
        }
    }
}