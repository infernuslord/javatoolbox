package toolbox.util;

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.swing.JFrame;

import org.apache.log4j.Logger;

import toolbox.util.ui.JSmartOptionPane;

/**
 * Exception utilities.
 */
public final class ExceptionUtil
{
    //--------------------------------------------------------------------------
    // Constructors
    //--------------------------------------------------------------------------

    /**
     * Prevent construction of this static singleton.
     */
    private ExceptionUtil()
    {
    }

    //--------------------------------------------------------------------------
    // Public
    //--------------------------------------------------------------------------

    /**
     * Converts a Throwable's stack trace to a string.
     *
     * @param t Throwable to extrace stack trace from.
     * @return Stack trace of the throwable as a string.
     */
    public static String getStackTrace(Throwable t) 
    {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
    
    
    /**
     * Generic Error handler for exceptions generated on a client side UI. 
     * Logs the error to the given logger and displays the error via a dialog 
     * box to the user.
     * 
     * @param t Exception causing Error.
     * @param c Logger to log to.
     */
    public static void handleUI(Throwable t, Logger c)
    {
        c.error(t.getMessage(), t);
        JFrame frame = new JFrame();
        JSmartOptionPane.showExceptionMessageDialog(frame, t);
        frame.dispose();
    }
}