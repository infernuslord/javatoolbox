package toolbox.util.reflect;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Hashtable;

/**
 * NumberParamPattern
 */
public class NumberParamPattern extends ParamPattern
{
    private static Hashtable PrimitiveNumbers_;
    private static Hashtable WrapperNumbers_;
    private Method convertMethod_;

    // STATIC INITIALIZER
    static
    {
        PrimitiveNumbers_ = new Hashtable(20);
        WrapperNumbers_ = new Hashtable(20);

        Method[] methods = NumberParamPattern.class.getDeclaredMethods();

        try
        {
            for (int i = 0; i < methods.length; i++)
                if (Modifier.isStatic(methods[i].getModifiers())
                    && methods[i].getName().startsWith("to"))
                {
                    Class c = Class.forName("java.lang."
                        + methods[i].getName().substring(2));

                    WrapperNumbers_.put(c, methods[i]);

                    PrimitiveNumbers_.put(c.getField("TYPE").get(null),
                        methods[i]);
                }
        }
        catch (Exception ex)
        {
            ; // Ignore
        }
    }

    static
    {
        ParamPattern.register(new NumberParamPattern());
    }


    // CONSTRUCTORS

    /**
     * Creates a new NumberParamPattern object.
     */
    public NumberParamPattern()
    {
    }


    /**
     * Creates a new NumberParamPattern object.
     * 
     * @param paramType DOCUMENT ME!
     */
    public NumberParamPattern(Class paramType)
    {
        super(paramType);
        initializeConvertMethod();
    }


    // PARAMPATTERN METHODS

    /**
     * DOCUMENT ME!
     * 
     * @param aClass DOCUMENT ME!
     * @return DOCUMENT ME!
     */
    protected boolean isApplicable(Class aClass)
    {
        return Number.class.isAssignableFrom(aClass)
            || PrimitiveNumbers_.get(aClass) != null;
    }


    /**
     * DOCUMENT ME!
     * 
     * @param aClass DOCUMENT ME!
     * @return DOCUMENT ME!
     */
    protected int getFactor(Class aClass)
    {
        return MATCH_CONVERT;
    }


    /**
     * DOCUMENT ME!
     * 
     * @param object DOCUMENT ME!
     * @return DOCUMENT ME!
     */
    protected Object advancedConvert(Object object)
    {
        if (object instanceof Number)
            return invoke(object);
        else
            return super.advancedConvert(object);
    }


    // SUPPORT METHODS

    /**
     * DOCUMENT ME!
     * 
     * @param object DOCUMENT ME!
     * @return DOCUMENT ME!
     */
    protected Object invoke(Object object)
    {
        try
        {
            return convertMethod_.invoke(null, new Object[]{object});
        }
        catch (Exception ex)
        {
            throw new ClassCastException(ex.getMessage());
        }
    }


    /**
     * DOCUMENT ME!
     */
    protected void initializeConvertMethod()
    {
        convertMethod_ = (Method) (Number.class
            .isAssignableFrom(getParamType()) ? WrapperNumbers_
            .get(getParamType()) : PrimitiveNumbers_.get(getParamType()));
    }


    // CONVERSION METHODS

    /**
     * DOCUMENT ME!
     * 
     * @param num DOCUMENT ME!
     * @return DOCUMENT ME!
     */
    public static Object toInteger(Number num)
    {
        return new Integer(num.intValue());
    }


    /**
     * DOCUMENT ME!
     * 
     * @param num DOCUMENT ME!
     * @return DOCUMENT ME!
     */
    public static Object toShort(Number num)
    {
        return new Short(num.shortValue());
    }


    /**
     * DOCUMENT ME!
     * 
     * @param num DOCUMENT ME!
     * @return DOCUMENT ME!
     */
    public static Object toLong(Number num)
    {
        return new Long(num.longValue());
    }


    /**
     * DOCUMENT ME!
     * 
     * @param num DOCUMENT ME!
     * @return DOCUMENT ME!
     */
    public static Object toFloat(Number num)
    {
        return new Float(num.floatValue());
    }


    /**
     * DOCUMENT ME!
     * 
     * @param num DOCUMENT ME!
     * @return DOCUMENT ME!
     */
    public static Object toDouble(Number num)
    {
        return new Double(num.doubleValue());
    }


    /**
     * DOCUMENT ME!
     * 
     * @param num DOCUMENT ME!
     * @return DOCUMENT ME!
     */
    public static Object toByte(Number num)
    {
        return new Byte(num.byteValue());
    }
}