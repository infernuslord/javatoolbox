package toolbox.plugin.jtail;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JPanel;

import toolbox.util.ui.JSmartButton;
import toolbox.util.ui.explorer.JFileExplorer;

/**
 * Custom file explorer panel for JTail. Just adds a "Tail" button to the
 * bottom of the panel.
 */
public class FileSelectionView extends JPanel
{
    //--------------------------------------------------------------------------
    // Fields
    //--------------------------------------------------------------------------

    /**
     * File explorer used to select the file to tail.
     */    
    private JFileExplorer fileExplorer_;
    
    /**
     * Button that will tail the currently selected file in the file explorer.
     */
    private JButton tailButton_;
    
    /**
     * Button that will tail the currently selected file and aggregate the 
     * output with the currently active TailView.
     */
    private JButton aggregateButton_;
    
    //--------------------------------------------------------------------------
    //  Constructors
    //--------------------------------------------------------------------------
    
    /**
     * Creates a FileSelectionView.
     */
    public FileSelectionView()
    {
        this(null);
    }
    
    
    /**
     * Creates a FileSelectionView with the given directory selected.
     *
     * @param dir Directory to select by default.
     */
    public FileSelectionView(String dir)
    {
        super(new BorderLayout(), false);
        buildView();
        
        if (dir != null)
            fileExplorer_.selectFolder(dir);        
    }
    
    //--------------------------------------------------------------------------
    // Private
    //--------------------------------------------------------------------------
    
    /**
     * Builds the GUI.
     */
    protected void buildView()
    {
        // File explorer         
        fileExplorer_ = new JFileExplorer(false);
        add(fileExplorer_, BorderLayout.CENTER);
        
        // Button panel    
        JPanel buttonPanel = new JPanel(new FlowLayout());
        tailButton_ = new JSmartButton("Tail");
        aggregateButton_ = new JSmartButton("Aggregate");
        buttonPanel.add(tailButton_);
        buttonPanel.add(aggregateButton_);
        add(buttonPanel, BorderLayout.SOUTH);
    }
    
    //--------------------------------------------------------------------------
    //  Accessors/Mutators
    //--------------------------------------------------------------------------
        
    /**
     * Returns the file explorer.
     * 
     * @return JFileExplorer
     */
    public JFileExplorer getFileExplorer()
    {
        return fileExplorer_;
    }

    
    /**
     * Returns the tail button.
     * 
     * @return JButton
     */
    public JButton getTailButton()
    {
        return tailButton_;
    }

    
    /**
     * Returns the aggregate button.
     * 
     * @return JButton
     */
    public JButton getAggregateButton()
    {
        return aggregateButton_;
    }
}