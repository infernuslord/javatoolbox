package toolbox.jtail.config.exml;

import java.awt.Font;
import java.io.IOException;

import org.apache.log4j.Logger;

import electric.xml.Element;

import toolbox.jtail.config.ITailPaneConfig;
import toolbox.util.collections.AsMap;
    
/**
 * TailConfig is a data object that captures the configuration of a given tail 
 * instance with the ability to marshal itself to and from XML format. 
 */
public class TailPaneConfig implements ITailPaneConfig, XMLConstants
{
    private static final Logger logger_ =
        Logger.getLogger(TailPaneConfig.class);
    

    private String  filename_;
    private boolean autoScroll_;
    private boolean showLineNumbers_;
    private boolean antiAlias_;
    private Font    font_;
    private String  filter_;
    private String  cutExpression_;

    //--------------------------------------------------------------------------
    //  Constructors
    //--------------------------------------------------------------------------
    
    /**
     * Default constructor
     */
    public TailPaneConfig()
    {
    }


    /**
     * Creates TailConfig with given parameters
     * 
     * @param  file             File to tail
     * @param  autoScroll       Turn on autoscroll
     * @param  showLineNumbers  Shows line numbers in output
     * @param  antiAlias        Antialias text in output area
     * @param  font             Font of display text area
     * @param  filter           Optional filter (regular expression) 
     *                          for weeding out junk            
     */
    public TailPaneConfig(String file, boolean autoScroll, 
        boolean showLineNumbers, boolean antiAlias, Font font, String filter)
    {
        setFilename(file);
        setAutoScroll(autoScroll);
        setShowLineNumbers(showLineNumbers);
        setAntiAlias(antiAlias);
        setFont(font);
        setRegularExpression(filter);
    }

    //--------------------------------------------------------------------------
    //  Public
    //--------------------------------------------------------------------------
    
    /**
     * Unmarshals an XML element representing a TailConfig object
     * 
     * @param   tail  Element representing a TailPaneConfig
     * @return  Fully populated TailPaneConfig
     * @throws  IOException on IO error
     */
    public static TailPaneConfig unmarshal(Element tail) throws IOException 
    {
        TailPaneConfig config = new TailPaneConfig();
        
        // Handle tail element
        config.setFilename(tail.getAttribute(ATTR_FILE));
        
        // Optional autoscroll
        if (tail.getAttribute(ATTR_AUTOSCROLL) != null)
            config.setAutoScroll(new Boolean(
                tail.getAttribute(ATTR_AUTOSCROLL)).booleanValue());
        else
            config.setAutoScroll(DEFAULT_AUTOSCROLL);
                
        // Optional show line numbers
        if (tail.getAttribute(ATTR_LINENUMBERS) != null)
            config.setShowLineNumbers(new Boolean(
                tail.getAttribute(ATTR_LINENUMBERS)).booleanValue());
        else
            config.setShowLineNumbers(DEFAULT_LINENUMBERS);
        
        // Optional antiAlias attribute
        String antiAlias = tail.getAttribute(ATTR_ANTIALIAS);
        if (antiAlias != null)
            config.setAntiAlias(new Boolean(antiAlias).booleanValue());
        else
            config.setAntiAlias(DEFAULT_ANTIALIAS);

        
        // Handle optional font element    
        Element fontNode = tail.getElement(ELEMENT_FONT);
        
        if (fontNode != null)
        {
            String family = fontNode.getAttribute(ATTR_FAMILY);
            int style = Integer.parseInt(fontNode.getAttribute(ATTR_STYLE));
            int size = Integer.parseInt(fontNode.getAttribute(ATTR_SIZE));
            config.setFont(new Font(family, style, size));
        }
        else
        {
            // NOTE: what is the default font?
            config.setFont(null);    
        }
        
        // Handle optional filter element
        Element filterNode = tail.getElement(ELEMENT_FILTER);
        
        if (filterNode != null)
        {
            config.setRegularExpression(filterNode.getTextString());

            // NOTE: support negate and case
        }
        else
        {
            config.setRegularExpression(DEFAULT_REGEX);
        }
            
        return config;
    }


    /**
     * Marshals from Java object representation to XML representation.
     * <pre>
     * 
     * Tail
     *   |
     *   +--Font
     *   |
     *   +--Filter
     * 
     * </pre>
     * 
     * @return  Tail XML node
     * @throws  IOException on IO error
     */
    public Element marshal()  throws IOException 
    {
        // Tail element
        Element tail = new Element(ELEMENT_TAIL);
        tail.setAttribute(ATTR_FILE, getFilename());
        tail.setAttribute(ATTR_AUTOSCROLL, isAutoScroll() + "");
        tail.setAttribute(ATTR_LINENUMBERS, isShowLineNumbers() + "");
        tail.setAttribute(ATTR_ANTIALIAS, isAntiAlias() + "");
        
        // Font element    
        Element font = new Element(ELEMENT_FONT);
        font.setAttribute(ATTR_FAMILY, getFont().getFamily());
        font.setAttribute(ATTR_STYLE, getFont().getStyle() + "");
        font.setAttribute(ATTR_SIZE, getFont().getSize() + "");            
        
        // Filter element
        Element filter = new Element(ELEMENT_FILTER);
        filter.setText(getRegularExpression());
        
        // Add child nodes to tail
        tail.addElement(font);
        tail.addElement(filter);
        
        return tail;
    }


    /**
     * @return String representation
     */
    public String toString()
    {
        return AsMap.of(this).toString();
    }


    //--------------------------------------------------------------------------
    //  Interface ITailPaneConfig
    //--------------------------------------------------------------------------


    /**
     * Returns the autoScroll.
     * 
     * @return boolean
     */
    public boolean isAutoScroll()
    {
        return autoScroll_;
    }


    /**
     * Returns the filename.
     * 
     * @return String
     */
    public String getFilename()
    {
        return filename_;
    }
    

    /**
     * Returns the showLineNumbers.
     * 
     * @return boolean
     */
    public boolean isShowLineNumbers()
    {
        return showLineNumbers_;
    }


    /**
     * Sets the autoScroll.
     * 
     * @param autoScroll The autoScroll to set
     */
    public void setAutoScroll(boolean autoScroll)
    {
        autoScroll_ = autoScroll;
    }


    /**
     * Sets the filename.
     * 
     * @param filename The filename to set
     */
    public void setFilename(String filename)
    {
        filename_ = filename;
    }


    /**
     * Sets the showLineNumbers.
     * 
     * @param showLineNumbers The showLineNumbers to set
     */
    public void setShowLineNumbers(boolean showLineNumbers)
    {
        showLineNumbers_ = showLineNumbers;
    }
    
    
    /**
     * Returns the font.
     * 
     * @return Font
     */
    public Font getFont()
    {
        return font_;
    }


    /**
     * Sets the font.
     * 
     * @param font The font to set
     */
    public void setFont(Font font)
    {
        font_ = font;
    }
    
    
    /**
     * Returns the filter.
     * 
     * @return String
     */
    public String getRegularExpression()
    {
        return filter_;
    }


    /**
     * Sets the filter.
     * 
     * @param filter The filter to set
     */
    public void setRegularExpression(String filter)
    {
        filter_ = filter;
    }
    
    /**
     * Accessor for the antialias flag
     * 
     * @return  True if antialias is on, false otherwise
     */
    public boolean isAntiAlias()
    {
        return antiAlias_;
    }
    
    
    /**
     * Mutator for the antialias flag
     * 
     * @param  b  True to turn antialias on, false otherwise
     */
    public void setAntiAlias(boolean b)
    {
        antiAlias_ = b;
    }
    
    /**
     * @see toolbox.jtail.config.ITailPaneConfig#getCutExpression()
     */
    public String getCutExpression()
    {
        return cutExpression_;
    }
    
    /**
     * @see toolbox.jtail.config.ITailPaneConfig#setCutExpression(String)
     */
    public void setCutExpression(String cutExpression)
    {
        cutExpression_ = cutExpression;
    }
    
    /**
     * @see toolbox.jtail.config.ITailPaneConfig#isAutoStart()
     */
    public boolean isAutoStart()
    {
        return false;
    }

    /**
     * @see toolbox.jtail.config.ITailPaneConfig#setAutoStart(boolean)
     */
    public void setAutoStart(boolean autoStart)
    {
        // Do something later
    }
}
