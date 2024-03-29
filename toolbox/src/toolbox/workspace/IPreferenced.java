package toolbox.workspace;

import nu.xom.Element;

/**
 * IPreferenced provides a simple interface for saving and restoring
 * preferences to and from an XML document.
 */
public interface IPreferenced
{
    /**
     * Reads preferences from the prefs DOM and applies them to the preferenced 
     * target.
     * 
     * @param prefs Document object model from which to read preferences. 
     * @throws PreferencedException on error.
     */
    void applyPrefs(Element prefs) throws PreferencedException;
    
    
    /**
     * Saves preferences to the prefs DOM.
     *  
     * @param prefs Document object model to persist preferences to.
     * @throws PreferencedException on error.
     */
    void savePrefs(Element prefs) throws PreferencedException;
}