package toolbox.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.log4j.Category;

/**
 * Utility class for thread related stuff
 */
public final class ThreadUtil
{
	/** Logger **/
	private static final Category logger = 
        Category.getInstance(ThreadUtil.class);
		
    /**
     * Prevent construction
     */ 
    private ThreadUtil()
    {
    }
    
    /**
     * Sleep without all the try/catch business
     * 
     * @param	millis	Milliseconds for the current thread to sleep
     */
    public static void sleep(int millis)
    {
    	try
    	{
    		Thread.currentThread().sleep(millis);
    	}
    	catch(Exception e)
    	{
  			logger.error(e);  		
    	}
    }
    
    /**
     * Runs a specific method in a thread asynchronously
     * 
     * @param   target      Object which contains method to run
     * @param   methodName  Name of the method to execute
     * @param   params      Method arguments
     * @return  Thread that method is/was executed on
     */
    public static Thread runInThread(Object target, String methodName, Object[] params)
    {
        /**
         * Runs a method on a given object in a thread
         */        
        class MethodRunner implements Runnable
        {
            private String method;
            private Object target;
            private Object[] params;

            /**
             * Creates a MethodRunner
             * 
             * @param  newTarget   Target object of method invocation
             * @param  newMethod   Method name on target object
             * @param  newParams   List of parameters to the method
             */
            public MethodRunner(Object newTarget, String newMethod, Object[] newParams)
            {
                target = newTarget;
                method = newMethod;                    
                params = newParams;
            }
            
            /**
             * Executes the method provided at time of construction
             */
            public void run()
            {
                try
                {   
                    /* convert array of parameters into array of classes */
                    Class[] paramTypes = new Class[params.length];
                    for(int i=0; i<params.length; i++)
                        paramTypes[i] = params[i].getClass();
                    
                    /* get method and invoke */
                    Method m = target.getClass().getMethod(method, paramTypes);                    
                    m.invoke(target, params);    
                }
                catch (NoSuchMethodException nsme)
                {
                    logger.error(nsme);
                }        
                catch (IllegalAccessException iae)
                {
                    logger.error(iae);
                }
                catch (InvocationTargetException ite)
                {
                    logger.error(ite);
                }
            }
        }

        /* create thread with a MethodRunner and start */
        Runnable runnable = new MethodRunner(target, methodName, params);
        Thread thread = new Thread(runnable);
        thread.start();
        return thread;
    }
}

