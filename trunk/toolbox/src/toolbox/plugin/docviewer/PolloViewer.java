package toolbox.plugin.docviewer;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JScrollPane;

import org.apache.log4j.Logger;

import org.outerj.pollo.xmleditor.XmlEditor;
import org.outerj.pollo.xmleditor.displayspec.GenericDisplaySpecification;
import org.outerj.pollo.xmleditor.model.XmlModel;

import toolbox.util.ArrayUtil;
import toolbox.util.ExceptionUtil;
import toolbox.util.FileUtil;
import toolbox.util.FontUtil;

/**
 * XML document viewer that uses Pollo for rendering the document.
 */
public class PolloViewer implements DocumentViewer
{
    private static final Logger logger_ = Logger.getLogger(PolloViewer.class);
    
    //--------------------------------------------------------------------------
    // Fields
    //--------------------------------------------------------------------------
    
    /**
     * XML UI widget.
     */
    private XmlEditor editor_;
    
    /**
     * Scrollpane wrapping the XmlEditor.
     */
    private JScrollPane scroller_;
    
    //--------------------------------------------------------------------------
    // Protected 
    //--------------------------------------------------------------------------

    /**
     * Lazy loads the UI component for faster plugin loading.
     */
    protected void lazyLoad()
    {
        if (editor_ == null) 
        {    
            try
            {
                GenericDisplaySpecification displaySpec = 
                    new GenericDisplaySpecification();
                
                HashMap initMap = new HashMap();
                initMap.put("use-random-colors", "true");
                //init.put("fixed-color", "0xffeeff");
                //init.put("background-color", null);
                initMap.put("treetype", "pollo");
                
                displaySpec.init(initMap);
                editor_ = new XmlEditor(null, displaySpec, -1);
                editor_.setAntialiasing(true);
                editor_.setCharacterDataFont(FontUtil.getPreferredMonoFont());
                editor_.setElementNameFont(FontUtil.getPreferredMonoFont());
                scroller_ = new JScrollPane(editor_);                
            }
            catch (Exception pe)
            {
                ExceptionUtil.handleUI(pe, logger_);
            }
        }
    }
    
    //--------------------------------------------------------------------------
    // DocumentViewer Interface 
    //--------------------------------------------------------------------------
    
    /**
     * @see toolbox.plugin.docviewer.DocumentViewer#startup(java.util.Map)
     */
    public void startup(Map init) throws DocumentViewerException
    {
    }
    
    
    /**
     * @see toolbox.plugin.docviewer.DocumentViewer#getName()
     */
    public String getName()
    {
        return "Pollo";
    }

    
    /**
     * @see toolbox.plugin.docviewer.DocumentViewer#getComponent()
     */
    public JComponent getComponent()
    {
        lazyLoad();
        return scroller_;
    }
    
    
    /**
     * @see toolbox.plugin.docviewer.DocumentViewer#getViewableFileTypes()
     */
    public String[] getViewableFileTypes()
    {
        return new String[] {"xml", "xsl", "xslt", "fo", "jnlp"};
    }
    
    
    /**
     * @see toolbox.plugin.docviewer.DocumentViewer#canView(java.io.File)
     */
    public boolean canView(File file)
    {
        return ArrayUtil.contains(
                getViewableFileTypes(), 
                FileUtil.getExtension(file));
    }

    
    /**
     * @see toolbox.plugin.docviewer.DocumentViewer#view(java.io.File)
     */
    public void view(File file) throws DocumentViewerException
    {
        lazyLoad();
        
        try
        {
            XmlModel model = new XmlModel(0);
            model.readFromResource(file);
            editor_.setXmlModel(model);
            editor_.validate();
            editor_.repaint();
        }
        catch (Exception e)
        {
            throw new DocumentViewerException(e);
        }
    }
    
    
    /**
     * @see toolbox.plugin.docviewer.DocumentViewer#view(java.io.InputStream)
     */
    public void view(InputStream is) throws DocumentViewerException
    {
    }

    
    /**
     * @see toolbox.plugin.docviewer.DocumentViewer#shutdown()
     */
    public void shutdown()
    {
        scroller_ = null;
        editor_ = null;
    }
}