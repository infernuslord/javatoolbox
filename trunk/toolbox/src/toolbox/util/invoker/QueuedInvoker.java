package toolbox.util.invoker;

import org.apache.commons.beanutils.MethodUtils;
import org.apache.log4j.Logger;

import toolbox.util.ThreadUtil;
import toolbox.util.concurrent.BlockingQueue;

/**
 * Invoker that queues up invocations requests on a queue and executes them in
 * a serial manner. The invoke() method does not wait for the execution to 
 * complete. Instead, invoke() returns immediately after the request is placed
 * on the queue. The size the queue is not bounded (maybe later).
 */
public class QueuedInvoker implements Invoker
{
    private static final Logger logger_ = 
        Logger.getLogger(QueuedInvoker.class);
    
    /** 
     * Queue of Runnables waiting to be invoked 
     */
    private BlockingQueue queue_;
    
    /**
     * Thread that pulls Runnables off of the queue_
     */
    private Thread puller_;

    //--------------------------------------------------------------------------
    // Constructors
    //--------------------------------------------------------------------------

    /**
     * Creates a queued invoker with no delay between invocations
     */
    public QueuedInvoker()
    {
        this(0);
    }    

    /**
     * Creates a queued invoker
     * 
     * @param  millis  Delay in milliseconds between invocations
     */
    public QueuedInvoker(final long millis)
    {
        queue_ = new BlockingQueue();

        // Creates the consumer thread and starts it    
        puller_ = new Thread(new Runnable()
        {
            public void run()
            {
                boolean shutdown = false;
                
                while (!shutdown)
                {
                    try
                    {
                        Runnable r = (Runnable) queue_.pull();
                        r.run();
                        Thread.sleep(millis);
                    }
                    catch (InterruptedException ie)
                    {
                        shutdown = true;
                        logger_.debug("Thread " + puller_.getName() + 
                            " was interrupted(). Shutting down...");
                    }
                }
            }
        }, Thread.currentThread().getName() + "->QueuedInvoker");

        // Start the consumer
        puller_.start();
    }

    //--------------------------------------------------------------------------
    // Invoker Interface
    //--------------------------------------------------------------------------

    public void invoke(Runnable invokable) throws Exception
    {
        queue_.push(invokable);
    }

    public void invoke(        
        final Object target, 
        final String method, 
        final Object[] params)
        throws Exception
    {
        invoke(new Runnable()
        {
            public void run()
            {
                try
                {
                    MethodUtils.invokeMethod(target, method, params);
                }
                catch (Exception e)
                {
                    logger_.error("run", e);
                }
            }
        });
    }
    
    public void shutdown() throws Exception
    {
        ThreadUtil.stop(puller_);
    }
}
