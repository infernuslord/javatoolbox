package toolbox.util.ui.flippane;

/**
 * Listener interface for notifications provided by JFlipPane.
 * 
 * @see toolbox.util.ui.flippane.JFlipPane
 */
public interface FlipPaneListener
{
    /**
     * Called when a flippane is collapsed.
     * 
     * @param flipPane Flip pane that was collapsed.
     */
    void collapsed(JFlipPane flipPane);
    
    
    /**
     * Called when a flippane is expanded.
     * 
     * @param flipPane Flip pane that was expanded.
     */
    void expanded(JFlipPane flipPane);
}