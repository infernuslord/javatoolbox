package toolbox.plugin.xslfo;

import java.awt.BorderLayout;
import java.awt.Color;
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

import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import com.adobe.acrobat.Viewer;

import nu.xom.Element;

import org.apache.commons.lang.StringUtils;
import org.apache.fop.apps.Fop;
import org.apache.log4j.Logger;

import org.jedit.syntax.SyntaxStyle;
import org.jedit.syntax.TextAreaDefaults;
import org.jedit.syntax.Token;
import org.jedit.syntax.XMLTokenMarker;

import toolbox.jedit.JEditPopupMenu;
import toolbox.jedit.JEditTextArea;
import toolbox.util.ExceptionUtil;
import toolbox.util.FileUtil;
import toolbox.util.SwingUtil;
import toolbox.util.XMLUtil;
import toolbox.util.XOMUtil;
import toolbox.util.io.StringInputStream;
import toolbox.util.ui.JSmartButton;
import toolbox.util.ui.JSmartSplitPane;
import toolbox.util.ui.explorer.FileExplorerAdapter;
import toolbox.util.ui.explorer.JFileExplorer;
import toolbox.util.ui.flippane.JFlipPane;
import toolbox.workspace.IPlugin;
import toolbox.workspace.IStatusBar;
import toolbox.workspace.PluginWorkspace;
import toolbox.workspace.WorkspaceAction;

/**
 * XSLFO Plugin is a simple GUI interface to edit, process and view transformed
 * XSL-FO documents.
 * <p>
 * Features:
 * <p>
 * <ul>
 *   <li>Selectable XSL-FO implementation (Apache FOP or RenderX XEP)
 *   <li>Selectable output format (PDF or Postscript)
 *   <li>Selectable viewer  (Embedded PDF viewer or launched Acrobat Viewer)
 *   <li>XML editor with syntax-hiliting
 *   <li>XML formatter
 * </ul>
 */ 
public class XSLFOPlugin extends JPanel implements IPlugin
{
    // TODO: Create XMLDefaults ala JavaDefaults for JEditTextArea and refactor

    private static final Logger logger_ = Logger.getLogger(XSLFOPlugin.class);

    //--------------------------------------------------------------------------
    // XML Constants
    //--------------------------------------------------------------------------
    
    /** 
     * Root preferences node for this plugin.
     */
    private static final String NODE_XSLFO_PLUGIN = "XSLFOPlugin";
    
    /**
     * Node for PDFViewer preferences.
     */
    private static final String NODE_PDF_VIEWER = "PDFViewer";
    
    //--------------------------------------------------------------------------
    // Fields
    //--------------------------------------------------------------------------
    
    /** 
     * Flip panel that houses the file explorer. 
     */
    private JFlipPane flipPane_;    
    
    /** 
     * Shared status bar with plugin host.
     */
    private IStatusBar statusBar_;
    
    /** 
     * XML text work area. 
     */
    private JEditTextArea xmlArea_;

    /** 
     * Default settings for XML text area. 
     */
    private TextAreaDefaults defaults_;    
    
    /** 
     * XML output pane. 
     */
    private JPanel outputPanel_;
    
    /** 
     * Embedded PDF viewer component. 
     */
    private Viewer viewer_;    

    /** 
     * File explorer used to open XML files. 
     */
    private JFileExplorer explorer_;

    /** 
     * Full Path to acrobat reader executable. 
     */
    private String pdfViewerPath_;

    /** 
     * Apache XSLFO implementation = FOP. 
     */
    private FOProcessor fopProcessor_;
    
    /** 
     * RenderX XSLFO implementation = XEP.
     */
    private FOProcessor xepProcessor_;

    /**
     * Splitter between the xmlArea and the outputPanel.
     */
    private JSmartSplitPane splitPane_;
    
    //--------------------------------------------------------------------------
    // Constructors
    //--------------------------------------------------------------------------

    /**
     * Creates an XSLFOPlugin.
     */
    public XSLFOPlugin()
    {
    }

    //--------------------------------------------------------------------------
    // Protected
    //--------------------------------------------------------------------------

    /**
     * Customizes the configuration of the text area.
     */
    protected void initTextArea()
    {
        defaults_ = TextAreaDefaults.getDefaults();
        
        defaults_.editable = true;
        defaults_.caretVisible = true;
        defaults_.caretBlinks = false;
        defaults_.blockCaret = true;
        defaults_.electricScroll = 3;
        
        //public int cols;
        //defaults_.rows = 5;
        defaults_.styles = getSyntaxStyles();
        //public Color caretColor;
        //public Color selectionColor;
        //public Color lineHighlightColor;
        //public boolean lineHighlight;
        //public Color bracketHighlightColor;
        //public boolean bracketHighlight;
        //public Color eolMarkerColor;
        defaults_.eolMarkers = false;
        defaults_.paintInvalid = false;
        defaults_.popup = new JEditPopupMenu();
    }


    /**
     * Customizes the colors used for syntax hiliting the xml.
     * 
     * @return Syntax styles
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
     * Builds the GUI.
     */
    protected void buildView()
    {
        initTextArea();
        xmlArea_ = new JEditTextArea(new XMLTokenMarker(), defaults_);
        ((JEditPopupMenu) defaults_.popup).setTextArea(xmlArea_);
        ((JEditPopupMenu) defaults_.popup).buildView();
        
        setLayout(new BorderLayout());
        explorer_ = new JFileExplorer(false);
        explorer_.addFileExplorerListener(new FileSelectionListener());
        
        flipPane_ = new JFlipPane(JFlipPane.LEFT);
        flipPane_.addFlipper(JFileExplorer.ICON, "File Explorer", explorer_);
        outputPanel_ = new JPanel(new BorderLayout());
        
        splitPane_ = 
            new JSmartSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                xmlArea_,
                outputPanel_);
        
        JPanel buttonPane = new JPanel(new FlowLayout());
        buttonPane.add(new JSmartButton(new FormatAction()));
        buttonPane.add(new JSmartButton(new FOPAWTAction()));
        buttonPane.add(new JSmartButton(new FOPRenderAction()));
        buttonPane.add(new JSmartButton(new FOPLaunchAction()));
        buttonPane.add(new JSmartButton(new FOPExportToPDFAction()));
        buttonPane.add(new JSmartButton(new FOPExportToPostscriptAction()));
        buttonPane.add(new JSmartButton(new XEPRenderAction()));
        buttonPane.add(new JSmartButton(new XEPLaunchAction()));
        
        add(BorderLayout.WEST, flipPane_);
        add(BorderLayout.CENTER, splitPane_);
        add(BorderLayout.SOUTH, buttonPane);
    }


    /**
     * Launches Adobe Acrobat Reader on the given file.
     * 
     * @param outfile File to open in PDF viewer
     */
    protected void viewPDFExternal(String outfile)
    {
        if (StringUtils.isEmpty(pdfViewerPath_))
        {
            try
            {
                JFileChooser jfc = new JFileChooser();
                
                if (jfc.showOpenDialog(
                        SwingUtil.getFrameAncestor(this)) == 
                            JFileChooser.APPROVE_OPTION) 
                {
                    pdfViewerPath_ = jfc.getSelectedFile().getCanonicalPath();
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
            logger_.info("Executing: " + pdfViewerPath_ + " " + outfile);
            Runtime.getRuntime().exec(pdfViewerPath_ + " " + outfile);
        }
        catch (Exception ex)
        {
            ExceptionUtil.handleUI(ex, logger_);
            pdfViewerPath_ = null;
        }
    }


    /**
     * Views a PDF using an embedded java pdf viewer.
     * 
     * @param file File to view with the embedded PDF viewer
     * @throws Exception on error
     */
    protected void viewPDFEmbedded(File file) throws Exception
    {
        viewPDFEmbedded(new FileInputStream(file));
    }


    /**
     * Views a PDF using an embedded java pdf viewer.
     * 
     * @param inputStream Stream to read PDF bytes from
     * @throws Exception on error
     */
    protected void viewPDFEmbedded(InputStream inputStream) throws Exception
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
     * Returns the Apache FOP processor.
     * 
     * @return FOProcessor
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
     * Returns the RenderX XEP processor.
     * 
     * @return FOProcessor
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
    // IPlugin Interface
    //--------------------------------------------------------------------------

    /**
     * @see toolbox.workspace.IPlugin#getPluginName()
     */
    public String getPluginName()
    {
        return "XSL-FO";
    }


    /**
     * @see toolbox.workspace.IPlugin#getComponent()
     */
    public JComponent getComponent()
    {
        return this;
    }


    /**
     * @see toolbox.workspace.IPlugin#getDescription()
     */
    public String getDescription()
    {
        return "Transforms valid XSL-FO to either PDF or Postscript using " + 
               "Apache FOP or RenderX XEP.";
    }


    /**
     * @see toolbox.workspace.IPlugin#startup(java.util.Map)
     */
    public void startup(Map params)
    {
        if (params != null)
            statusBar_ = (IStatusBar) 
                params.get(PluginWorkspace.KEY_STATUSBAR);
        
        buildView();
    }


    /**
     * @see toolbox.workspace.IPlugin#shutdown()
     */
    public void shutdown()
    {
        if (viewer_ != null)
            viewer_.deactivate();
    }    

    //--------------------------------------------------------------------------
    // IPreferenced Interface
    //--------------------------------------------------------------------------
    
    /**
     * @see toolbox.workspace.IPreferenced#applyPrefs(nu.xom.Element)
     */
    public void applyPrefs(Element prefs) throws Exception
    {
        Element root = 
            XOMUtil.getFirstChildElement(
                prefs, NODE_XSLFO_PLUGIN, new Element(NODE_XSLFO_PLUGIN));    
        
        explorer_.applyPrefs(root);
        flipPane_.applyPrefs(root);
        xmlArea_.applyPrefs(root);
        splitPane_.applyPrefs(root);
            
        pdfViewerPath_ = XOMUtil.getString(
            root.getFirstChildElement(NODE_PDF_VIEWER), null);
    }


    /**
     * Saves preferences in following XML structure.
     * <pre>
     * 
     * Parent
     * |
     * +--XSLFOPlugin
     *    |
     *    +--PDFViewer
     *    |
     *    +--JFileExplorer
     *    |
     *    +--JFlipPane
     *    |
     *    '--JEditTextArea 
     * 
     * </pre>
     * 
     * @see toolbox.workspace.IPreferenced#savePrefs(nu.xom.Element)
     */
    public void savePrefs(Element prefs)
    {
        Element root = new Element(NODE_XSLFO_PLUGIN);
        
        explorer_.savePrefs(root);
        flipPane_.savePrefs(root);
        xmlArea_.savePrefs(root);
        splitPane_.savePrefs(root);
        
        if (!StringUtils.isEmpty(pdfViewerPath_))
        {
            Element pdf = new Element(NODE_PDF_VIEWER);
            pdf.appendChild(pdfViewerPath_);
            root.appendChild(pdf);
        }
        
        XOMUtil.insertOrReplace(prefs, root);
    }

    //--------------------------------------------------------------------------
    // FileSelectionListener
    //--------------------------------------------------------------------------
    
    /**
     * Populates file that is double clicked on in the text area.
     */
    class FileSelectionListener extends FileExplorerAdapter
    {
        /**
         * @see toolbox.util.ui.explorer.FileExplorerListener#fileDoubleClicked(
         *      java.lang.String)
         */
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
    // FormatAction
    //--------------------------------------------------------------------------
    
    /**
     * Formats the XML with correct indentation and spacing.
     */
    class FormatAction extends WorkspaceAction 
    {
        /**
         * Creates a FormatAction. 
         */
        FormatAction()
        {
            super("Format", false, null, null);
        }
    
        
        /**
         * @see toolbox.util.ui.SmartAction#runAction(
         *      java.awt.event.ActionEvent)
         */
        public void runAction(ActionEvent e) throws Exception
        {
            xmlArea_.setText(XMLUtil.format(xmlArea_.getText()));
        }
    }

    //--------------------------------------------------------------------------
    // FOPAWTAction
    //--------------------------------------------------------------------------
    
    /**
     * Launches FOP AWT viewer.
     */
    class FOPAWTAction extends WorkspaceAction
    {
        /**
         * Creates a FOPAWTAction. 
         */
        FOPAWTAction()
        {
            super("Launch with FOP AWT", false, null, null);
        }
        
        
        /**
         * @see toolbox.util.ui.SmartAction#runAction(
         *      java.awt.event.ActionEvent)
         */
        public void runAction(ActionEvent e) throws Exception
        {
            String xml = xmlArea_.getText();
            String foFile  = FileUtil.createTempFilename() + ".xml";
            FileUtil.setFileContents(foFile, xml, false);
            Fop.main(new String[] {foFile, "-awt"});
        }
    }
    
    //--------------------------------------------------------------------------
    // FOPRenderAction
    //--------------------------------------------------------------------------
    
    /**
     * Renders the XSLFO and views using the internal PDF viewer.
     */
    class FOPRenderAction extends WorkspaceAction
    {
        /**
         * Creates a FOPRenderAction. 
         */
        FOPRenderAction()
        {
            super("Render with FOP", true, XSLFOPlugin.this, statusBar_);
        }
        
        
        /**
         * @see toolbox.util.ui.SmartAction#runAction(
         *      java.awt.event.ActionEvent)
         */
        public void runAction(ActionEvent e) throws Exception
        {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            StringInputStream input = new StringInputStream(xmlArea_.getText());
            
            getFOP().renderPDF(input, output);
            
            viewPDFEmbedded(new ByteArrayInputStream(output.toByteArray()));
        }
    }

    //--------------------------------------------------------------------------
    // FOPLaunchAction
    //--------------------------------------------------------------------------
    
    /**
     * Uses FOP formatter and views externally as a PDF.
     */
    class FOPLaunchAction extends WorkspaceAction
    {
        /**
         * Creates a FOPLaunchAction. 
         */
        FOPLaunchAction()
        {
            super("Launch with FOP", true, XSLFOPlugin.this, statusBar_);
        }
        
        
        /**
         * @see toolbox.util.ui.SmartAction#runAction(
         *      java.awt.event.ActionEvent)
         */
        public void runAction(ActionEvent e) throws Exception
        {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            StringInputStream input = new StringInputStream(xmlArea_.getText());
            
            getFOP().renderPDF(input, output);
            byte[] pdfBytes = output.toByteArray();
            
            String pdfFile = FileUtil.createTempFilename() + ".pdf";
            FileUtil.setFileContents(pdfFile, pdfBytes, false);
            viewPDFExternal(pdfFile);
        }
    }    
    
    //--------------------------------------------------------------------------
    // FOPExportToPDFAction
    //--------------------------------------------------------------------------
    
    /**
     * Saves generated PDF to file.
     */
    class FOPExportToPDFAction extends WorkspaceAction
    {
        private File lastDir_;
        
        /**
         * Creates a FOPExportToPDFAction. 
         */
        FOPExportToPDFAction()
        {
            super("Export to PDF..", false, null, null);
        }
        
        
        /**
         * @see toolbox.util.ui.SmartAction#runAction(
         *      java.awt.event.ActionEvent)
         */
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
    
    //--------------------------------------------------------------------------
    // XEPRenderAction
    //--------------------------------------------------------------------------
    
    /**
     * Uses XEP formatter and views as PDF.
     */
    class XEPRenderAction extends WorkspaceAction
    {
        /**
         * Creates a XEPRenderAction. 
         */
        XEPRenderAction()
        {
            super("Render with XEP", true, XSLFOPlugin.this, statusBar_);
        }
        
        
        /**
         * @see toolbox.util.ui.SmartAction#runAction(
         *      java.awt.event.ActionEvent)
         */
        public void runAction(ActionEvent e) throws Exception
        {
            InputStream input = new StringInputStream(xmlArea_.getText());
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            getXEP().renderPDF(input, output);
            viewPDFEmbedded(new ByteArrayInputStream(output.toByteArray()));
        }
    }
    
    //--------------------------------------------------------------------------
    // XEPLaunchAction
    //--------------------------------------------------------------------------
    
    /**
     * Uses XEP formatter and views externally as a PDF.
     */
    class XEPLaunchAction extends WorkspaceAction
    {
        /**
         * Creates a XEPLaunchAction. 
         */
        XEPLaunchAction()
        {
            super("Launch with XEP", true, XSLFOPlugin.this, statusBar_);
        }
        
        
        /**
         * @see toolbox.util.ui.SmartAction#runAction(
         *      java.awt.event.ActionEvent)
         */
        public void runAction(ActionEvent e) throws Exception
        {
            InputStream input = new StringInputStream(xmlArea_.getText());
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            getXEP().renderPDF(input, output);
             
            String pdfFile = FileUtil.createTempFilename() + ".pdf";
            FileUtil.setFileContents(pdfFile, output.toByteArray(), false);
            viewPDFExternal(pdfFile);
        }
    }    
    
    //--------------------------------------------------------------------------
    // FOPExportToPostcriptAction
    //--------------------------------------------------------------------------
    
    /**
     * Exports XSL-FO to a Postscript file.
     */
    class FOPExportToPostscriptAction extends WorkspaceAction
    {
        private File lastDir_;
        
        /**
         * Creates a FOPExportToPostscriptAction. 
         */
        FOPExportToPostscriptAction()
        {
            super("Export to Postscript..", false, null, null);
        }
        
        
        /**
         * @see toolbox.util.ui.SmartAction#runAction(
         *      java.awt.event.ActionEvent)
         */
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