package toolbox.util.xslfo;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import com.adobe.acrobat.Viewer;

import org.apache.fop.apps.Fop;
import org.apache.log4j.Logger;
import org.jedit.syntax.SyntaxStyle;
import org.jedit.syntax.TextAreaDefaults;
import org.jedit.syntax.Token;
import org.jedit.syntax.XMLTokenMarker;

import toolbox.jedit.JEditPopupMenu;
import toolbox.jedit.JEditTextArea;
import toolbox.util.ClassUtil;
import toolbox.util.ExceptionUtil;
import toolbox.util.FileUtil;
import toolbox.util.StringUtil;
import toolbox.util.SwingUtil;
import toolbox.util.XMLUtil;
import toolbox.util.io.StringInputStream;
import toolbox.util.ui.JFileExplorer;
import toolbox.util.ui.JFileExplorerAdapter;
import toolbox.util.ui.flippane.JFlipPane;
import toolbox.util.ui.plugin.IPlugin;
import toolbox.util.ui.plugin.IStatusBar;
import toolbox.util.ui.plugin.PluginWorkspace;
import toolbox.util.ui.plugin.WorkspaceAction;

/**
 * XSLFO Plugin is a simple GUI interface to edit, process and view transformed
 * XSL-FO documents.
 * <p>
 * Features:
 * <p>
 * <ul>
 * <li>user selectable FO implementation (Apache FOP or RenderX XEP)
 * <li>user selectable output format (PDF or Postscript)
 * <li>user selectable viewer  (Embedded PDF viewer or can launch Acrobat)
 * <li>XML editor is capable of syntax-hiliting and formatting the XML for
 *     increased legibility
 * </ul>
 */ 
public class XSLFOPlugin extends JPanel implements IPlugin
{
    /*  
     * TODO: Create XMLDefaults ala JavaDefaults for JEditTextArea and refactor
     */

    private static final Logger logger_ = 
        Logger.getLogger(XSLFOPlugin.class);

    /** Prefix passed to sub components when asked to save their preferences */
    private static final String PREFS_PREFIX =  
        ClassUtil.stripPackage(XSLFOPlugin.class.getName()).toLowerCase();
        
    /** Flip panel that houses the file explorer */
    private JFlipPane flipPane_;    
    
    /** Shared status bar with plugin host */
    private IStatusBar statusBar_;
    
    /** XML text work area */
    private JEditTextArea xmlArea_;

    /** Default settings for XML text area */
    private TextAreaDefaults defaults_;    
    
    /** XML output pane */
    private JPanel outputPanel_;
    
    /** Embedded PDF viewer component */
    private Viewer viewer_;    

    /** File explorer to load xml from files directly */
    private JFileExplorer explorer_;

    /** Full Path to acrobat reader executable */
    private String acrobatPath_;

    /** Apache XSLFO implementation = FOP */
    private FOProcessor fopProcessor_;
    
    /** RenderX XSLFO implementation = XEP */
    private FOProcessor xepProcessor_;

    //--------------------------------------------------------------------------
    //  Constructors
    //--------------------------------------------------------------------------

    /**
     * Default Constructor
     */
    public XSLFOPlugin()
    {
    }

    //--------------------------------------------------------------------------
    // Private
    //--------------------------------------------------------------------------

    /**
     * Customizes the configuration of the text area
     */
    protected void initTextArea()
    {
        defaults_ = TextAreaDefaults.getDefaults();
        
        defaults_.editable = true;
        defaults_.caretVisible = true;
        defaults_.caretBlinks = false;
        defaults_.blockCaret = true;
        //defaults.electricScroll=int??
        
        //public int cols;
        defaults_.rows = 5;
        defaults_.styles = getSyntaxStyles();
        //public Color caretColor;
        //public Color selectionColor;
        //public Color lineHighlightColor;
        //public boolean lineHighlight;
        //public Color bracketHighlightColor;
        //public boolean bracketHighlight;
        //public Color eolMarkerColor;
        defaults_.eolMarkers=false;
        defaults_.paintInvalid=false;
        defaults_.popup = new JEditPopupMenu();
    }

    /**
     * Customizes the colors used for syntax hiliting the xml
     * 
     * @return  Syntax styles
     */
    protected static SyntaxStyle[] getSyntaxStyles()
    {
        SyntaxStyle[] styles = new SyntaxStyle[Token.ID_COUNT];

        styles[Token.COMMENT1] =
            new SyntaxStyle(Color.red.darker(), false, false);
            
        styles[Token.COMMENT2] =
            new SyntaxStyle(new Color(0x990033), false, false);
            
        styles[Token.KEYWORD1] =
            new SyntaxStyle(Color.blue.darker(), false, false);
            
        styles[Token.KEYWORD2] =
            new SyntaxStyle(Color.blue.darker(), false, false);
            
        styles[Token.KEYWORD3] =
            new SyntaxStyle(new Color(0x009600), false, false);
            
        styles[Token.LITERAL1] =
            new SyntaxStyle(Color.green.darker() /*new Color(0x650099)*/
                , false, false);
                
        styles[Token.LITERAL2] =
            new SyntaxStyle(new Color(0x650099), false, false);
            
        styles[Token.LABEL] =
            new SyntaxStyle(new Color(0x990033), false, false);
            
        styles[Token.OPERATOR] =
            new SyntaxStyle(Color.blue.darker(), false, false);
            
        styles[Token.INVALID] = 
            new SyntaxStyle(Color.red, false, false);

        return styles;
    }
    
    /**
     * Builds the GUI
     */
    protected void buildView()
    {
        initTextArea();
        xmlArea_ = new JEditTextArea(new XMLTokenMarker(), defaults_);
        ((JEditPopupMenu) defaults_.popup).setTextArea(xmlArea_);
        ((JEditPopupMenu) defaults_.popup).buildView();
        
        setLayout(new BorderLayout());
        explorer_ = new JFileExplorer(false);
        explorer_.addJFileExplorerListener(new FileSelectionListener());
                
        flipPane_ = new JFlipPane(JFlipPane.LEFT);
        flipPane_.addFlipper("File Explorer", explorer_);

        outputPanel_ = new JPanel(new BorderLayout());
        
        JSplitPane splitPane = 
            new JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                xmlArea_,
                outputPanel_);
        
        JPanel buttonPane = new JPanel(new FlowLayout());
        buttonPane.add(new JButton(new FormatAction()));
        buttonPane.add(new JButton(new FOPAWTAction()));
        buttonPane.add(new JButton(new FOPRenderAction()));
        buttonPane.add(new JButton(new FOPLaunchAction()));
        buttonPane.add(new JButton(new FOPExportToPDFAction()));
        buttonPane.add(new JButton(new FOPExportToPostscriptAction()));
        buttonPane.add(new JButton(new XEPRenderAction()));
        buttonPane.add(new JButton(new XEPLaunchAction()));
        
        add(BorderLayout.WEST, flipPane_);
        add(BorderLayout.CENTER, splitPane);
        add(BorderLayout.SOUTH, buttonPane);
    }

    /**
     * Launches Adobe Acrobat Reader on the given file
     * 
     * @param outfile
     */
    private void viewPDFExternal(String outfile) throws IOException
    {
        if (StringUtil.isNullOrEmpty(acrobatPath_))
        {
            try
            {
                JFileChooser jfc = new JFileChooser();
                
                if (jfc.showOpenDialog(
                        SwingUtil.getFrameAncestor(this)) == 
                            JFileChooser.APPROVE_OPTION) 
                {
                    acrobatPath_ = jfc.getSelectedFile().getCanonicalPath();
                }
            }
            catch (FileNotFoundException fnfe)
            {
                ExceptionUtil.handleUI(fnfe, logger_);
            }
            catch (IOException ioe)
            {
                ExceptionUtil.handleUI(ioe, logger_);
            }
        }
        
        try
        {
            logger_.info("Executing: " + acrobatPath_ + " " + outfile);
            Runtime.getRuntime().exec(acrobatPath_ + " " + outfile);
        }
        catch (Exception ex)
        {
            ExceptionUtil.handleUI(ex, logger_);
            acrobatPath_ = null;
        }
    }

    /**
     * Views a PDF using an embedded java pdf viewer
     * 
     * @param file
     */
    private void viewPDFEmbedded(File file) throws Exception
    {
        viewPDFEmbedded(new FileInputStream(file));
    }

    /**
     * Views a PDF using an embedded java pdf viewer
     * 
     * @param inputStream
     * @throws Exception
     */
    private void viewPDFEmbedded(InputStream inputStream) throws Exception
    {
        if (viewer_ == null)
        {
            viewer_ = new Viewer();
            outputPanel_.add(BorderLayout.CENTER, viewer_);
            viewer_.activate();
        }
        
        viewer_.setDocumentInputStream(inputStream);        
        
        viewer_.execMenuItem("FitVisibleWidth");
    }

    /**
     * @return  Apache FOP processor
     */
    protected FOProcessor getFOP()
    {
        if (fopProcessor_ == null)
        {
            fopProcessor_ = FOProcessorFactory.createProcessor(
                FOProcessorFactory.FO_IMPL_APACHE);
              
            fopProcessor_.initialize(new Properties());
        }
        
        return fopProcessor_;
    }

    /**
     * @return  RenderX XEP processor
     */
    protected FOProcessor getXEP()
    {
        if (xepProcessor_ == null)
        {
            xepProcessor_ = FOProcessorFactory.createProcessor(
                FOProcessorFactory.FO_IMPL_RENDERX);
              
            xepProcessor_.initialize(new Properties());
        }
        
        return xepProcessor_;
    }

    //--------------------------------------------------------------------------
    //  IPlugin Interface
    //--------------------------------------------------------------------------
    
    public String getName()
    {
        return "XSL-FO";
    }

    public Component getComponent()
    {
        return this;
    }

    public String getDescription()
    {
        return "Transforms valid XSL-FO to either PDF or Postscript using " + 
               "Apache FOP or RenderX XEP.";
    }

    public void startup(Map params)
    {
        if (params != null)
            statusBar_= (IStatusBar) params.get(PluginWorkspace.PROP_STATUSBAR);
        
        buildView();
    }

    public void savePrefs(Properties prefs)
    {
        explorer_.savePrefs(prefs, PREFS_PREFIX);
        flipPane_.savePrefs(prefs, PREFS_PREFIX);
        
        if (!StringUtil.isNullOrEmpty(acrobatPath_))
            prefs.setProperty("xslfoplugin.acrobat.path", acrobatPath_);
    }

    public void applyPrefs(Properties prefs)
    {
        explorer_.applyPrefs(prefs, PREFS_PREFIX);
        flipPane_.applyPrefs(prefs, PREFS_PREFIX);
        
        acrobatPath_ = prefs.getProperty("xslfoplugin.acrobat.path", null);
    }

    public void shutdown()
    {
        if (viewer_ != null)
            viewer_.deactivate();
    }    
    
    //--------------------------------------------------------------------------
    //  Inner Classes
    //--------------------------------------------------------------------------
    
    /**
     * Populates file that is double clicked on in the text area
     */
    private class FileSelectionListener extends JFileExplorerAdapter
    {
        public void fileDoubleClicked(String file)
        {
            try
            {
                xmlArea_.setText(FileUtil.getFileContents(file));
            }
            catch (FileNotFoundException fnfe)
            {
                ExceptionUtil.handleUI(fnfe, logger_);
            }
            catch (IOException ioe)
            {
                ExceptionUtil.handleUI(ioe, logger_);
            }
        }
    }
    
    //--------------------------------------------------------------------------
    //  Actions
    //--------------------------------------------------------------------------
    
    /**
     * Formats the XML with correct indentation and spacing
     */
    private class FormatAction extends WorkspaceAction 
    {
        public FormatAction()
        {
            super("Format", false, null, null);
        }
    
        public void runAction(ActionEvent e) throws Exception
        {
            xmlArea_.setText(XMLUtil.format(xmlArea_.getText()));
        }
    }
    
    /**
     * Launches FOP AWT viewer
     */
    private class FOPAWTAction extends WorkspaceAction
    {
        public FOPAWTAction()
        {
            super("Launch with FOP AWT", false, null, null);
        }
        
        public void runAction(ActionEvent e) throws Exception
        {
            String xml = xmlArea_.getText();
            String foFile  = FileUtil.getTempFilename() + ".xml";
            FileUtil.setFileContents(foFile, xml, false);
            Fop.main(new String[] { foFile, "-awt"});
        }
    }
    
    /**
     * Renders the XSLFO and views using the internal PDF viewer
     */
    private class FOPRenderAction extends WorkspaceAction
    {
        public FOPRenderAction()
        {
            super("Render with FOP", true, XSLFOPlugin.this, statusBar_);
        }
        
        public void runAction(ActionEvent e) throws Exception
        {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            StringInputStream input = new StringInputStream(xmlArea_.getText());
            
            getFOP().renderPDF(input, output);
            
            viewPDFEmbedded(new ByteArrayInputStream(output.toByteArray()));
        }
    }

    /**
     * Uses FOP formatter and views externally as a PDF
     */
    private class FOPLaunchAction extends WorkspaceAction
    {
        public FOPLaunchAction()
        {
            super("Launch with FOP", true, XSLFOPlugin.this, statusBar_);
        }
        
        public void runAction(ActionEvent e) throws Exception
        {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            StringInputStream input = new StringInputStream(xmlArea_.getText());
            
            getFOP().renderPDF(input, output);
            byte[] pdfBytes = output.toByteArray();
            
            String pdfFile = FileUtil.getTempFilename() + ".pdf";
            FileUtil.setFileContents(pdfFile, pdfBytes, false);
            viewPDFExternal(pdfFile);
        }
    }    
    
    /**
     * Saves generated PDF to file
     */
    private class FOPExportToPDFAction extends WorkspaceAction
    {
        private File lastDir_;
        
        public FOPExportToPDFAction()
        {
            super("Export to PDF..", false, null, null);
        }
        
        public void runAction(ActionEvent e) throws Exception
        {
            JFileChooser chooser = null;
            
            if (lastDir_ == null)
                chooser = new JFileChooser();
            else
                chooser = new JFileChooser(lastDir_);

            if (chooser.showSaveDialog(null) ==  JFileChooser.APPROVE_OPTION) 
            {
                String saveFile = chooser.getSelectedFile().getCanonicalPath();
                
                logger_.debug("Save file=" + saveFile);
                
                FileOutputStream output = new FileOutputStream(saveFile);
                StringInputStream i = new StringInputStream(xmlArea_.getText());
                getFOP().renderPDF(i, output);
                output.close();
            }
            
            lastDir_ = chooser.getCurrentDirectory();
        }
    }
    
    /**
     * Uses XEP formatter and views as PDF
     */
    private class XEPRenderAction extends WorkspaceAction
    {
        public XEPRenderAction()
        {
            super("Render with XEP", true, XSLFOPlugin.this, statusBar_);
        }
        
        public void runAction(ActionEvent e) throws Exception
        {
            InputStream input = new StringInputStream(xmlArea_.getText());
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            getXEP().renderPDF(input, output);
            viewPDFEmbedded(new ByteArrayInputStream(output.toByteArray()));
        }
    }
    
    /**
     * Uses XEP formatter and views externally as a PDF
     */
    private class XEPLaunchAction extends WorkspaceAction
    {
        public XEPLaunchAction()
        {
            super("Launch with XEP", true, XSLFOPlugin.this, statusBar_);
        }
        
        public void runAction(ActionEvent e) throws Exception
        {
            InputStream input = new StringInputStream(xmlArea_.getText());
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            getXEP().renderPDF(input, output);
             
            String pdfFile = FileUtil.getTempFilename() + ".pdf";
            FileUtil.setFileContents(pdfFile, output.toByteArray(), false);
            viewPDFExternal(pdfFile);
        }
    }    
    
    /**
     * Exports XSL-FO to a Postscript file
     */
    private class FOPExportToPostscriptAction extends WorkspaceAction
    {
        private File lastDir_;
        
        public FOPExportToPostscriptAction()
        {
            super("Export to Postscript..", false, null, null);
        }
        
        public void runAction(ActionEvent e) throws Exception
        {
            JFileChooser chooser = null;
            
            if (lastDir_ == null)
                chooser = new JFileChooser();
            else
                chooser = new JFileChooser(lastDir_);

            if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) 
            {
                String saveFile = 
                    chooser.getSelectedFile().getCanonicalPath();
                
                logger_.debug("Save file=" + saveFile);
                
                FileOutputStream fos = new FileOutputStream(saveFile);
                
                getFOP().renderPostscript(
                    new StringInputStream(xmlArea_.getText()), fos);
            }
            
            lastDir_ = chooser.getCurrentDirectory();
        }
    }
}