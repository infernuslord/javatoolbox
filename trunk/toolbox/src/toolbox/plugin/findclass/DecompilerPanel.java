package toolbox.plugin.findclass;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.io.File;
import java.net.URL;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JToolBar;

import org.apache.log4j.Logger;

import org.jedit.syntax.JavaTokenMarker;
import org.jedit.syntax.TextAreaDefaults;

import toolbox.jedit.JEditPopupMenu;
import toolbox.jedit.JEditTextArea;
import toolbox.jedit.JavaDefaults;
import toolbox.util.ClassUtil;
import toolbox.util.ExceptionUtil;
import toolbox.util.FileUtil;
import toolbox.util.FontUtil;
import toolbox.util.StreamUtil;
import toolbox.util.decompiler.Decompiler;
import toolbox.util.decompiler.DecompilerException;
import toolbox.util.decompiler.DecompilerFactory;
import toolbox.util.ui.ImageCache;
import toolbox.util.ui.JHeaderPanel;
import toolbox.util.ui.JSmartComboBox;
import toolbox.util.ui.tabbedpane.JSmartTabbedPane;
import toolbox.workspace.WorkspaceAction;

/**
 * Decompiler Panel facilitates decompiling of class files that are 
 * identified by the JFindClass plugin.
 * <p>
 * Supports:
 * <ul>
 *   <li>User selectable decompiler.
 *   <li>Decompiled files are stacked on a tabbed pane.
 * </ul>
 * 
 * @see toolbox.plugin.findclass.FindClassPlugin
 */
public class DecompilerPanel extends JHeaderPanel
{
    private static final Logger logger_ = 
        Logger.getLogger(DecompilerPanel.class);

    //--------------------------------------------------------------------------
    // Fields
    //--------------------------------------------------------------------------
    
    /**
     * Allows user to choose which decompiler to use for decompiling.
     */
    private JSmartComboBox decompilerCombo_;
    
    /**
     * Tab panel for decompiled source. One tab per class. 
     */
    private JSmartTabbedPane tabbedPane_;
    
    /**
     * Reference to the enclosing plugin's results table that is passed in at
     * time of construction. The table is the source for the location and FQCN
     * for the class to decompile.
     */
    private JTable resultTable_;

    //--------------------------------------------------------------------------
    // Constructors
    //--------------------------------------------------------------------------
    
    /**
     * Creates a DecompilerPanel.
     * 
     * @param resultTable Table containing list of class locations and names.
     */
    public DecompilerPanel(JTable resultTable)
    {
        super("Decompiler");
        resultTable_ = resultTable;
        buildView();
    }

    //--------------------------------------------------------------------------
    // Protected
    //--------------------------------------------------------------------------

    /**
     * Constructs the user interface.
     */
    protected void buildView()
    {
        Decompiler[] decompilers = null;
        
        try
        {
            decompilers = DecompilerFactory.createAll(); 
        }
        catch (DecompilerException de)
        {
            ExceptionUtil.handleUI(de, logger_);
        }

        JPanel content = new JPanel(new BorderLayout());

        decompilerCombo_ = new JSmartComboBox(decompilers);
        
        JButton decompile = createButton(
            ImageCache.getIcon(ImageCache.IMAGE_SPANNER),
            "Decompile",
            new DecompileAction());
        
        JToolBar tb = createToolBar();
        tb.add(decompilerCombo_);
        tb.add(decompile);
        setToolBar(tb);
        tabbedPane_ = new JSmartTabbedPane(true);
        content.add(tabbedPane_, BorderLayout.CENTER);
        setContent(content);
    }
    
    
    /**
     * Adds a tab to the tabbed pane. The class name is used as the tab title
     * and the source is embedded in a text area for viewing.
     * 
     * @param clazz Class associated with the tab.
     * @param source Source code.
     */
    protected void addTab(String clazz, String source)
    {
        TextAreaDefaults defaults = new JavaDefaults();
        
        JEditTextArea sourceArea = new JEditTextArea(
            new JavaTokenMarker(), defaults);
        
        sourceArea.getPainter().setFont(FontUtil.getPreferredMonoFont());

        // Hack for circular reference in popup menu            
        ((JEditPopupMenu) defaults.popup).setTextArea(sourceArea);
        ((JEditPopupMenu) defaults.popup).buildView();
        
        sourceArea.setText(source);
        sourceArea.setCaretPosition(0);
        tabbedPane_.addTab(clazz, sourceArea);
        tabbedPane_.setSelectedComponent(sourceArea);
    }
    
    //--------------------------------------------------------------------------
    // DecompileAction
    //--------------------------------------------------------------------------
        
    /**
     * Action to decompile the currently selected class file.
     */
    class DecompileAction extends WorkspaceAction
    {
        /**
         * Creates a DecompileAction.
         */
        DecompileAction()
        {
            // TODO: Re-integrate the status bar
            super("Decompile", true, null, null /*statusBar_*/);
            putValue(MNEMONIC_KEY, new Integer('D'));    
            putValue(SHORT_DESCRIPTION, "Decompiles the selected class");
        }
        
        
        /**
         * @see toolbox.util.ui.SmartAction#runAction(
         *      java.awt.event.ActionEvent)
         */
        public void runAction(ActionEvent e) throws Exception
        {
            int idx = resultTable_.getSelectedRow();
            
            if (idx >= 0)
            {
                // Jar or directory path
                String location = (String) 
                    resultTable_.getModel().getValueAt(
                        idx, ResultsTableModel.COL_SOURCE); 
                    
                // FQN of class
                String clazz  = (String) 
                    resultTable_.getModel().getValueAt(
                        idx, ResultsTableModel.COL_CLASS);
            
                location = location.trim();
                clazz  = clazz.trim();
                
                Decompiler d = (Decompiler) decompilerCombo_.getSelectedItem();

                String source = null;
                
                try
                {
                    source = d.decompile(clazz, location);
                }
                catch (IllegalArgumentException iae)
                {
                    if (ClassUtil.isArchive(location))
                    {   
                        // Build up a jar url pointing to the class 
                        // file inside the jar
                        StringBuffer sb = new StringBuffer();
                        sb.append("jar:file:");
                        sb.append(new File(location).getCanonicalPath());
                        sb.append("!/");
                        sb.append(clazz.replace('.', '/'));
                        sb.append(".class");
                    
                        // jar:file:/c:/crap/my.jar!/com/company/MyClass.class"
                        logger_.debug("JAR URL=" + sb);
                    
                        // TODO: Also extract all anonymous/innerclasses
                        
                        URL url = new URL(sb.toString());
                        
                        logger_.debug("URL as file=" + url.getFile());
                        logger_.debug("Class file len=" + 
                            url.openConnection().getContentLength());
                    
                        // Extract the class file as an array of bytes
                        byte[] bytecode = StreamUtil.toBytes(
                            url.openConnection().getInputStream());
                    
                        // Write out class file to the temp dir on disk 
                        // (least common denominator so that all decompilers 
                        // can get to it.
                        File tempClassFile = 
                            new File(FileUtil.getTempDir(), 
                                ClassUtil.stripPackage(clazz) + ".class");
                        
                        FileUtil.setFileContents(
                            tempClassFile.getCanonicalPath(), bytecode, false);
                    
                        // Do some decompiling...
                        source = d.decompile(tempClassFile);
                        
                        // Cleanup
                        FileUtil.delete(tempClassFile.getCanonicalPath());
                    }
                    else
                    {                   
                        source = "Not supported";
                    }
                }

                addTab(clazz, source);
            }           
        }
    }
}