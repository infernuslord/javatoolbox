package toolbox.jtail.exml;

/**
 * XML configuration constants
 */
public interface XMLConstants
{
    // JTail XML element
    public static final String ELEMENT_JTAIL = "JTail";
    public static final String ATTR_HEIGHT   = "height";
    public static final String ATTR_WIDTH    = "width";
    public static final String ATTR_X        = "x";
    public static final String ATTR_Y        = "y";
    public static final String ATTR_DIR      = "dir";
        
    // Defaults XML element
    public static final String ELEMENT_DEFAULTS = "Defaults";

    // Tail XML element
    public static final String ELEMENT_TAIL     = "Tail";
    public static final String ATTR_FILE        = "file";
    public static final String ATTR_AUTOSCROLL  = "autoScroll";
    public static final String ATTR_LINENUMBERS = "showLineNumbers";
    public static final String ATTR_ANTIALIAS   = "antiAlias";
    
    // Font XML element
    public static final String ELEMENT_FONT     = "Font";
    public static final String ATTR_FAMILY      = "family";
    public static final String ATTR_STYLE       = "style";        
    public static final String ATTR_SIZE        = "size";

    // Filter XML element
    public static final String ELEMENT_FILTER = "Filter";
    public static final String ATTR_NEGATE    = "negate";
    public static final String ATTR_MATCH_CASE = "matchCase";
}
