package toolbox.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Counter that wraps around given a finite range [a..b] and also emits events
 * when wrapping around.
 */
public class RollingCounter
{
    //--------------------------------------------------------------------------
    // Fields
    //--------------------------------------------------------------------------
    
    /**
     * Start of range.
     */
    private int start_;

    /**
     * End of range.
     */
    private int end_;

    /**
     * Current position in range.
     */
    private int cnt_;

    /**
     * List of counter listeners.
     */
    private List listeners_;
    
    //--------------------------------------------------------------------------
    // Constructors
    //--------------------------------------------------------------------------

    /**
     * Creates a counter that wraps around.
     * 
     * @param start Starting value of range.
     * @param end Ending value of range.
     * @param initialValue Initial value of counter. 
     */        
    public RollingCounter(int start, int end, int initialValue)
    {
        start_     = start;
        end_       = end;
        cnt_       = initialValue;
        listeners_ = new ArrayList();
    }

    //--------------------------------------------------------------------------
    // Public
    //--------------------------------------------------------------------------
    
    /**
     * Increments the counter by 1.
     *
     * @return Counter value after increment.
     */
    public int increment()
    {   
        if (cnt_ == end_)
            fireBeforeRoll();
        
        ++cnt_;

        if (cnt_ > end_)
            cnt_ = start_;

        if (cnt_ == start_)
            fireAfterRoll();            
            
        return cnt_;
    }

    
    /**
     * Returns the current counter value.
     * 
     * @return int
     */
    public int getCount()
    {
        return cnt_;
    }


    /**
     * Return true if the counter is at the end of the range.
     * 
     * @return boolean
     */
    public boolean isAtEnd()
    {
        return (cnt_ == end_);
    }

    
    /**
     * Returns true if the counter is at the beginning of the range.
     * 
     * @return boolean
     */
    public boolean isAtStart()
    {
        return (cnt_ == start_);
    }

    //--------------------------------------------------------------------------
    // Event Support
    //--------------------------------------------------------------------------
    
    /**
     * Fires event for before a roll occurs.
     */
    public void fireBeforeRoll()
    {
        for (int i = 0; i < listeners_.size(); i++)
             ((IRollingCounterListener) listeners_.get(i)).beforeRoll(this);
    }

    
    /**
     * Fires event for after a roll occurs.
     */
    public void fireAfterRoll()
    {
        for (int i = 0; i < listeners_.size(); i++)
            ((IRollingCounterListener) listeners_.get(i)).afterRoll(this);
    }

    
    /**
     * Adds a listener.
     *
     * @param listener Listener to add.
     */
    public void addRollingCounterListener(IRollingCounterListener listener)
    {
        listeners_.add(listener);
    }

    //--------------------------------------------------------------------------
    // Overridden from java.lang.Object
    //--------------------------------------------------------------------------
    
    /**
     * Returns debug friendly dump of this object.
     * 
     * @return String
     */
    public String toString()
    {
        return "{cnt=" + cnt_ + ", start=" + start_ + ", end=" + end_ + "}";
    }
    
    //--------------------------------------------------------------------------
    // Interfaces
    //--------------------------------------------------------------------------
    
    /**
     * Interface for notification of rolling events.
     */    
    public interface IRollingCounterListener
    {
        /**
         * Called before a roll is about to happen.
         * 
         * @param rc Counter. 
         */
        void beforeRoll(RollingCounter rc);
        
        
        /**
         * Called after a roll has happened.
         * 
         * @param rc Counter.
         */
        void afterRoll(RollingCounter rc);
    }
}

