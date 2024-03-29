package toolbox.plugin.jdbc.action;

import java.awt.event.ActionEvent;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;

import toolbox.plugin.jdbc.QueryPlugin;
import toolbox.util.JDBCSession;
import toolbox.util.JDBCUtil;

/**
 * Queries the DB metadata and dumps a list of the tables.
 * 
 * @see toolbox.plugin.jdbc.QueryPlugin
 */
public class ListTablesAction extends BaseAction
{
    //--------------------------------------------------------------------------
    // Constructors
    //--------------------------------------------------------------------------
    
    /**
     * Creates a ListTablesAction.
     * 
     * @param plugin Parent plugin.
     */
    public ListTablesAction(QueryPlugin plugin)
    {
        super(plugin, "List Tables", true, null, plugin.getStatusBar());
    }

    //--------------------------------------------------------------------------
    // SmartAction Abstract Methods 
    //--------------------------------------------------------------------------
    
    /**
     * @see toolbox.util.ui.SmartAction#runAction(
     *      java.awt.event.ActionEvent)
     */
    public void runAction(ActionEvent e) throws Exception
    {
        Connection conn = null;
        ResultSet rs = null;
        String session = getPlugin().getCurrentProfile().getProfileName();
        
        try
        {
            conn = JDBCSession.getConnection(session);
            DatabaseMetaData meta = conn.getMetaData();
            rs = meta.getTables(null, null, null, null);
            String tables = JDBCUtil.format(rs);
            getPlugin().getResultsArea().append(tables);
        }
        finally
        {
            JDBCUtil.close(rs);
            JDBCUtil.releaseConnection(conn);
        }
    }
}