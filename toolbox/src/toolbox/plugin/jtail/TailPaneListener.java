package toolbox.plugin.jtail;

/**
 * Notification interface for events generated by a TailPane.
 */
public interface TailPaneListener
{
    /**
     * Notification of new data available.
     * 
     * @param tailPane Tailpane.
     */
    void newDataAvailable(TailPane tailPane);
    
    
    /**
     * Notification that an existing tail was aggregated with a new tail.
     * 
     * @param tailPane TailPane that houses the aggregate tails.
     */
    void tailAggregated(TailPane tailPane);
}