package toolbox.util.thread;

/**
 * IThreadable is the interface implemented by all requests that must be 
 * delivered using a particular threading strategy. Runnable interface was not 
 * used because it did not provide the needed return value.
 */
public interface IThreadable
{
    /**
     * Performs the processing of the request and returns the result or null 
     * if no result.
     * 
     * @return Object
     */
    Object run();
}