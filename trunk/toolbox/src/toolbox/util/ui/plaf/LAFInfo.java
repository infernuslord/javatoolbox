package toolbox.util.ui.plaf;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import junit.framework.Assert;

import nu.xom.Attribute;
import nu.xom.Element;
import nu.xom.Elements;

import org.apache.log4j.Logger;

import toolbox.util.collections.AsMap;
import toolbox.workspace.IPreferenced;

/**
 * LAFInfo is used to capture additional info about a look and feel instance.
 * For example a theme name or a theme file. Class name would be
 * LookAndFeelInfo if it was not for UIManager.LookAndFeelInfo.
 */
public class LAFInfo implements IPreferenced
{
    private static final Logger logger_ = Logger.getLogger(LAFInfo.class);
    
    //--------------------------------------------------------------------------
    // Constants
    //--------------------------------------------------------------------------

    // Node and attribute names used for persistence to XML.
    public static final String NODE_LOOKANDFEEL  = "LookAndFeel";
    public static final String   ATTR_NAME       = "name";
    public static final String   ATTR_CLASS      = "class";
    public static final String   ATTR_ACTION     = "action";
    public static final String   NODE_THEME      = "Theme";
    public static final String     NODE_PROPERTY = "Property";
    public static final String     ATTR_VALUE    = "value";
    
    /**
     * This property is embedded in LookAndFeel.getUIDefaults() so that LAFInfo
     * can be accessed easily without any interatction with LookAndFeelUtil.
     */
    public static final String PROP_HIDDEN_KEY = "lookandfeel.info";
    
    //--------------------------------------------------------------------------
    // Fields
    //--------------------------------------------------------------------------
    
    /**
     * Name of the look and feel.
     */
    private String name_;
    
    /**
     * Fully qualified class name of the look and feel.
     */
    private String className_;
    
    /**
     * Fully qualified class name of the activation class for the look and feel.
     */
    private String action_;
    
    /**
     * Map of arbitrary name/value pairs specific to the look and feel.
     */
    private Map props_;

    //--------------------------------------------------------------------------
    // Constructors
    //--------------------------------------------------------------------------
    
    /**
     * Creates a LAFInfo.
     */
    public LAFInfo() 
    {
        // Map has to be sorted since order of keys is not enforced in XML
        props_ = new TreeMap();
    }
    
    
    /**
     * Creates a LAFInfo.
     *
     * @param lookAndFeelNode
     * @throws Exception
     */
    public LAFInfo(Element lookAndFeelNode) throws Exception
    {
        this();
        applyPrefs(lookAndFeelNode);
    }
    
    //--------------------------------------------------------------------------
    // Public
    //--------------------------------------------------------------------------
    
    /**
     * Sets the name of the look and feel.
     * 
     * @param name Name of the look and feel.
     */
    public void setName(String name)
    {
        name_ = name;
    }

    
    /**
	 * Returns the name of the look and feel in a form suitable for a menu or
	 * other presentation.
	 * 
	 * @return String
	 */
    public String getName()
    {
        return name_;
    }


    /**
	 * Returns the name of the class that implements this look and feel.
	 * 
	 * @return String
	 */
    public String getClassName()
    {
        return className_;
    }

    
    /**
     * Sets the class name of the look and feel.
     * 
     * @param className FQCN class of the look and feel.
     */
    public void setClassName(String className)
    {
        className_ = className;
    }

    
    /**
     * Returns the action.
     *
     * @return String
     */
    public String getAction()
    {
        return action_;
    }

    
    /**
     * Sets the action.
     *
     * @param action The action to set.
     */
    public void setAction(String action)
    {
        action_ = action;
    }
    
    
    /**
     * Returns the value of the property with the given name.
     * 
     * @param name
     * @return
     */
    public String getProperty(String name)
    {
        return (String) props_.get(name);
    }
    
    
    /**
     * Returns the look and feel properties map.
     * 
     * @return Map
     */
    public Map getProperties()
    {
        return props_;
    }
    
    
    //--------------------------------------------------------------------------
    // IPreferenced Interface
    //--------------------------------------------------------------------------
    
    /**
     * @see toolbox.workspace.IPreferenced#applyPrefs(nu.xom.Element)
     */
    public void applyPrefs(Element lookAndFeelNode) throws Exception
    {
        Assert.assertEquals(
                "Wrong element",
                NODE_LOOKANDFEEL, 
                lookAndFeelNode.getLocalName());
        
        setName(lookAndFeelNode.getAttributeValue(ATTR_NAME));
        setClassName(lookAndFeelNode.getAttributeValue(ATTR_CLASS));
        setAction(lookAndFeelNode.getAttributeValue(ATTR_ACTION));
        
        Elements props = lookAndFeelNode.getChildElements(NODE_PROPERTY);
        
        for (int i=0; i<props.size(); i++)
        {
            Element prop = props.get(i);
            
            props_.put(
                prop.getAttributeValue(ATTR_NAME), 
                prop.getAttributeValue(ATTR_VALUE));
        }
    }
    
    
    /**
     * @see toolbox.workspace.IPreferenced#savePrefs(nu.xom.Element)
     */
    public void savePrefs(Element prefs) throws Exception
    {
        Element laf = new Element(NODE_LOOKANDFEEL);
        laf.addAttribute(new Attribute(ATTR_NAME, getName()));
        laf.addAttribute(new Attribute(ATTR_CLASS, getClassName()));
        laf.addAttribute(new Attribute(ATTR_ACTION, getAction()));
        
        for (Iterator i = props_.entrySet().iterator(); i.hasNext(); )
        {
            Map.Entry entry = (Map.Entry) i.next();
            Element prop = new Element(NODE_PROPERTY);
            
            prop.addAttribute(
                new Attribute(ATTR_NAME, (String) entry.getKey()));
            
            prop.addAttribute(
                new Attribute(ATTR_VALUE, (String) entry.getValue()));
            
            laf.appendChild(prop);
        }
        
        prefs.appendChild(laf);
    }
    
    //--------------------------------------------------------------------------
    // Overrides java.lang.Object
    //--------------------------------------------------------------------------
    
    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj)
    {
        if (obj == this)
            return true;

        if (obj == null || !obj.getClass().equals(getClass()))
            return false;

        return toString().equals(obj.toString());
    }
    
      
    /**
	 * Returns a string that displays and identifies this object's properties.
	 * 
	 * @return String
	 */
    public String toString()
    {
        return AsMap.of(this).toString();
        
        //    StringBuffer sb = new StringBuffer();
        //    sb.append(getName());
        //    sb.append(getClassName());
        //    sb.append(getAction());
        //    
        //    for (Iterator i = props_.keySet().iterator(); i.hasNext(); )
        //    {
        //        String key = (String) i.next();
        //        String value = (String) props_.get(key);
        //        
        //        sb.append(key);
        //        sb.append(value);
        //    }
        //    
        //    return sb.toString();
    }
    
    
    /**
     * @see java.lang.Object#hashCode()
     */
    public int hashCode()
    {
        return toString().hashCode();
    }
}