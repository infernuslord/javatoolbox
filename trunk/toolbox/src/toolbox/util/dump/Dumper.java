package toolbox.util.dump;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Stack;

import org.apache.log4j.Logger;
import org.apache.regexp.RESyntaxException;

import toolbox.util.ClassUtil;
import toolbox.util.StringUtil;

/**
 * Dumper is a simple but useful utility used to dump a graph of java objects
 * to an ASCII tree like structure in a format that is easily recognizable. It 
 * is great for acquainting oneself with unfamiliar code or as a helpful
 * debugging aid to find out just what all may hanging around your object tree.
 * 
 * @see BasicDumpFormatter 
 */
public class Dumper
{
    private static final Logger logger_ = Logger.getLogger(Dumper.class);
    
    // TODO: Fix dangling tail
    // TODO: Add option to have refences to already traversed objects generate
    //       unique labels to they can be referenced. (ugly but useful)
    // TODO: Allow option to leave out nulls
    // TODO: Add interface to support custom dumpers for specific types of objs
        
    //--------------------------------------------------------------------------
    // Constants 
    //--------------------------------------------------------------------------
    
    /**
     * Trunk character of the ascii tree.
     */    
    private static final String BAR = "|   ";
    
    /**
     * Junction character of the ascii tree.
     */
    private static final String JUNCTION = "+";
    
    /**
     * Arm character of the ascii tree.
     */
    private static final String ARM = "---";

    /** 
     * Max length of right hand value. 
     */
    private static final int MAX_PRESENTABLE_LENGTH = 100;

    //--------------------------------------------------------------------------
    // Fields
    //--------------------------------------------------------------------------
    
    /** 
     * Maximum depth to traverse into the object graph. 
     */
    private int maxDepth_;
        
    /** 
     * Caches objects which have already been traversed. 
     */
    private ObjectCache cache_;
    
    /** 
     * Dump formatter and configuration. 
     */
    private DumpFormatter formatter_;

    //--------------------------------------------------------------------------
    //  Constructors
    //--------------------------------------------------------------------------
    
    /**
	 * Creates a Dumper with the default BasicDumpFormatter.
	 */
    private Dumper()
    {
        this(new BasicDumpFormatter());
    }

    
    /**
	 * Creates a Dumper with the given formatter.
	 * 
	 * @param formatter Dump formatting and output criteria.
	 */
    public Dumper(DumpFormatter formatter)
    {
        formatter_ = formatter;
        maxDepth_  = Integer.MAX_VALUE;
        cache_     = new ObjectCache();
    }

    //--------------------------------------------------------------------------
    //  Public (Static) 
    //--------------------------------------------------------------------------
    
    /**
	 * Traverses an object graph and dumps it to a string using a tree
	 * structure.
	 * 
	 * @param obj Object to dump.
	 * @return Object dumped to string.
	 */
    public static String dump(Object obj)
    {
        return dump(obj, Integer.MAX_VALUE);
    }

    
    /**
	 * Traverses an object graph and dumps it to a string using a tree
	 * structure with the given maximum depth of traversal from the root
	 * object.
	 * 
	 * @param obj Object to dump.
	 * @param maxDepth Max number of levels to recurse down into the obj.
	 * @return Object dumped as a tree.
	 */
    public static String dump(Object obj, int maxDepth)
    {
        String result = null;
        
        try
        {
            result = dump(obj, maxDepth, new BasicDumpFormatter() );
        }
        catch (RESyntaxException re)
        {
            logger_.error("dump", re);
        }
        finally
        {
            
        }
        
        return result;
    }

    
    /**
	 * Traverses an object graph and dumps it to a string using a tree
	 * structure with the given maximum depth of traversal from the root object
	 * and formater.
	 * 
	 * @param obj Object to dump.
	 * @param maxDepth Max number of levels to recurse down into the obj.
	 * @param formatter Dump formatter.
	 * @return Object dumped as a tree.
	 */
    public static String dump(Object obj, int maxDepth, DumpFormatter formatter)
    {
        Dumper dumper = new Dumper(formatter);
        dumper.setMaxDepth(maxDepth);
        String dump = dumper.nonStaticDumpObject(obj);
        return dump;        
    }

    //--------------------------------------------------------------------------
    //  Public (Instance)
    //--------------------------------------------------------------------------

    /**
     * Returns the maxDepth.
     * 
     * @return Max number of levels to recurse from the root object.
     */
    public int getMaxDepth()
    {
        return maxDepth_;
    }

    
    /**
     * Sets the maxDepth.
     * 
     * @param maxDepth Max number of levels to recurse from the root object.
     */
    public void setMaxDepth(int maxDepth)
    {
        maxDepth_ = maxDepth;
    }

    //--------------------------------------------------------------------------
    // Protected
    //--------------------------------------------------------------------------
    
    /**
	 * Non-static method to allow access to instance variables which in turn is
	 * required for multiple simultaneous threads without interference.
	 * 
	 * @param obj Object to dump.
	 * @return Stringified dump of object.
	 */
    protected String nonStaticDumpObject(Object obj)
    {
        StringBuffer buffer = new StringBuffer();
        
        try
        {
            if (obj == null)
            {
                buffer.append("null");
                return buffer.toString();
            }
            
            // Print out the root object
            cache_.put(obj);
            buffer.append(obj.getClass().getName());
            buffer.append(StringUtil.NL);
            
            dump(obj, buffer, "");
        }
        catch (Throwable ex)
        {
            logger_.error("dump", ex);
            logger_.error(buffer.toString());
        }
        
        return buffer.toString();
    }

    
    /**
	 * Traverses an object and its children to generate a tree like structure
	 * of the object graphs and the values of its elements.
	 * 
	 * @param obj Object to dump.
	 * @param buffer Dump buffer.
	 * @param depth Recursion depth.
	 * @throws IllegalAccessException if attribute/method not accessible.
	 */
    protected void dump(Object obj, StringBuffer buffer, String depth) 
        throws IllegalAccessException
    {
        if (obj != null)
        {
            // Go back in the class hierarchy and find the first one that's 
            // not Object
            Class cl = obj.getClass();
            Stack stack = new Stack();
            Class superClass = cl;
            
            do
            {
                stack.push(superClass);
            }
            while ((superClass = superClass.getSuperclass()) != null);
            
            while (!stack.isEmpty())
            {
                Class c = (Class) stack.pop();
                
                // This is different from reachedMaxDepth!
                if (depth.length()/4 <= maxDepth_)           
                    dump(c, obj, buffer, depth);
            }
        }
    }

    
    /**
	 * Dumps an array or collection class.
	 * 
	 * @param obj Object to dump.
	 * @param buffer Dump buffer.
	 * @param depth Recursion depth.
	 * @throws IllegalAccessExceptio.n if attribute/method not accessible.
	 */
    protected void dump(
        Field arrayField, 
        Object[] array, 
        StringBuffer buffer, 
        String depth) 
        throws IllegalAccessException
    {
        cache_.put(array);
        
        // Iterator over array, dumping the value at each index
        
        for (int i=0; i<array.length; i++)
        {
            buffer.append(makeBranch(depth));
                        
            buffer.append(
                formatter_.formatFieldName(arrayField.getName()) + "["+i+"]");
                
            buffer.append(" = ");
            buffer.append(makePresentable(array[i]));
            buffer.append(StringUtil.NL);
            dump(array[i], buffer, depth + BAR);
        }
    }

    
    /**
	 * Calls itself recursively. Gets all attributes of obj and dumps relevant
	 * data.
	 * 
	 * @param clazz Class representing level in class hierarchy.
	 * @param obj Object to dump.
	 * @param buffer Dump buffer.
	 * @param depth Recursion depth.
	 * @throws IllegalAccessException on illegal access.
	 */    
    protected void dump(
        Class clazz,  
        Object obj, 
        StringBuffer buffer,
        String depth) 
        throws IllegalAccessException
    {
        if (!formatter_.shouldInclude(clazz))
            return;
        
        // Get list of classses fields
        Field[] fields = clazz.getDeclaredFields();
        
        if (formatter_.sortFields())
            Arrays.sort(fields, new FieldNameComparator());
        
        for (int i = 0; i < fields.length; i++)
        {
            Field field = fields[i];
            field.setAccessible(true);

            // Skip if field is excluded             
            if (formatter_.shouldInclude(field))
            {    
                Class  type  = field.getType();
                Object value = field.get(obj);
    
                // Update cache            
                if ((value != null) && (!cache_.contains(value)))
                    cache_.put(value , field );
    
                buffer.append(makeBranch(depth));
                buffer.append(formatter_.formatFieldName(field.getName()));
                buffer.append(" = ");
    
                if (value == null)
                {
                    buffer.append("null");
                    buffer.append(StringUtil.NL);
                }
                else
                {
                    if ((!type.isPrimitive()) && 
                        (value != null) &&
                        (cache_.hasTraversed(value)))
                    {
                        buffer.append("[Visited] ");
                        
                        buffer.append(formatter_.formatFieldName(
                            cache_.getInfo(value).getField().getName()));
                            
                        buffer.append(StringUtil.NL);
                    }
                    else if (Collection.class.isAssignableFrom(type))
                    {
                        checkTraversed(value);
                        
                        Collection c = (Collection) value;
                        
                        buffer.append(
                            ClassUtil.stripPackage(value.getClass().getName()));
                            
                        buffer.append("[" + c.size() + "]"); 
                        buffer.append(StringUtil.NL);
                        
                        if (!reachedMaxDepth(depth))
                            dump(field, c.toArray(), buffer, depth + BAR);
                    }
                    else
                    {
                        checkTraversed(value);
                        buffer.append(makePresentable(value));
                        appendInheritance(type, buffer);
                        buffer.append(StringUtil.NL);
                        
                        if (!reachedMaxDepth(depth))                    
                            dump(type, value, buffer, depth + BAR);
                    }
                }
            }
        }
    }

    
    /**
	 * Determines if we're reached the maxnumber of levels specified to
	 * traverse down the object hierarchy.
	 * 
	 * @param Current indentation string (or depth).
	 * @return True if we should not go down any deeper, false otherwise.
	 */
    protected boolean reachedMaxDepth(String depth)
    {
        return !((depth + BAR).length()/4 < maxDepth_);        
    }

    
    /**
	 * Check if the object has already been traversed, if not flip the bit
	 * cause we're about to traverse it!
	 * 
	 * @param value Object to check for traversal.
	 */
    protected void checkTraversed(Object value)
    {
        ObjectInfo objInfo = cache_.getInfo(value);

        if (objInfo != null)
            objInfo.setTraversed(true);
    }

    
    /**
	 * Convenience method to Create an ASCII tree branch.
	 * 
	 * @param depth Current indentation string.
	 * @return ASCII branch.
	 */
    protected String makeBranch(String depth)
    {
        StringBuffer sb = new StringBuffer();
           
        sb.append(depth);
        sb.append(BAR);
        sb.append(StringUtil.NL);
            
        sb.append(depth);
        sb.append(JUNCTION);
        sb.append(ARM);

        return sb.toString();
    }

    
    /**
	 * Makes an object's value presentable in a concise manner. If certain
	 * conditions are true, then substitutes are used in order to simplify
	 * readability.
	 * 
	 * @param obj Object to make presentable.
	 * @return Object's value is a presentable string.
	 */
    protected String makePresentable(Object obj)
    {
        String result = null;
        
        if (obj == null)
        {
            result = "null";
        }
        else
        {
            String toString = obj.toString();
            String name = obj.getClass().getName();
            String stripped = ClassUtil.stripPackage(name);
            
            if (toString == null)
            {
                result = "null";
            }
            else if (StringUtil.isNullOrBlank(toString))
            {
                result = stripped;
            }
            else if (StringUtil.isMultiline(toString))
            {
                result = toString.substring(0, toString.indexOf("\n"));
                
                if (result.length() > MAX_PRESENTABLE_LENGTH)
                    result = result.substring(0, MAX_PRESENTABLE_LENGTH-3) + 
                        "...";
                
            }
            else if (toString.startsWith(name))
            {
                result = "[" + stripped + "]";
            }
            else
            {
                result = toString;
                
                if (result.length() > MAX_PRESENTABLE_LENGTH)
                    result = result.substring(0, MAX_PRESENTABLE_LENGTH-3) + 
                        "...";
            }
        }
        
        return result;
    }

    
    /**
	 * Appends the given classes inheritance tree to the dump buffer.
	 * 
	 * @param clazz Class for which to print the tree.
	 * @param buffer Dump buffer.
	 */
    protected void appendInheritance(Class clazz, StringBuffer buffer)
    {
        if (formatter_.showInheritance())
        {
            if (clazz.isPrimitive())
            {
                ; // NOOP
            }
            else
            {
                Class currentClass = clazz;
                buffer.append(" [");
                
                do
                {
                    String className = currentClass.getName();
                    buffer.append(formatter_.formatClassName(className));

                    if (!(currentClass == Object.class))
                        buffer.append(" -> ");
                }
                while ((currentClass = currentClass.getSuperclass()) != null);
                
                buffer.append("] ");
            }
        }
    }

    //--------------------------------------------------------------------------
    // FieldNameComparator
    //--------------------------------------------------------------------------
    
    /** 
     * Comparator for field names.
     */
    class FieldNameComparator implements Comparator
    {
        /**
         * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
         */
        public int compare(Object o1, Object o2)
        {
            String name1 = ((Field)o1).getName();
            String name2 = ((Field)o2).getName();
            return name1.compareTo(name2);
        }
    }
}