package toolbox.plugin.jdbc.action;

import java.awt.event.ActionEvent;

import org.apache.log4j.Logger;

import toolbox.plugin.jdbc.QueryPlugin;
import toolbox.util.db.SQLFormatter;

/**
 * Runs the query selected from the SQL history popup menu.
 * 
 * @see toolbox.plugin.jdbc.QueryPlugin
 */
public class ExecutePriorAction extends BaseAction
{
    private static final Logger logger_ = 
        Logger.getLogger(ExecutePriorAction.class);
    
    //--------------------------------------------------------------------------
    // Fields
    //--------------------------------------------------------------------------
    
    /**
     * SQL statement to execute.
     */
    private String sql_;

    //--------------------------------------------------------------------------
    // Constructors
    //--------------------------------------------------------------------------
    
    /**
     * Creates a ExecutePriorAction.
     *
     * @param plugin Query plugin.
     * @param sql SQL to execute.
     * @throws Exception
     */
    public ExecutePriorAction(QueryPlugin plugin, String sql)
    {
        super(plugin, "Execute prior", true, null, plugin.getStatusBar());
        sql_ = sql;
        SQLFormatter sf = new SQLFormatter();
        sf.setIndent(2);
        sf.setNewLine("<br>");
        
        try
        {
            sql = sf.format(sql);
        }
        catch (Exception e)
        {
            logger_.error(e);
        }
        
        sql = "<html>" + sql + "</html>";
        putValue(NAME, sql);
        putValue(SHORT_DESCRIPTION, "Executes the SQL statement");
    }

    //--------------------------------------------------------------------------
    // SmartAction Abstract Methods
    //--------------------------------------------------------------------------
    
    /**
     * @see toolbox.util.ui.SmartAction#runAction(java.awt.event.ActionEvent)
     */
    public void runAction(ActionEvent e)
    {
        getPlugin().getSQLEditor().setText(sql_);
        new ExecuteAllAction(getPlugin()).actionPerformed(e);
    }
}