package toolbox.util;

import java.io.*;
import java.util.*;
import java.text.*;
import java.net.*;

/**
 * Array util class
 */
public class ArrayUtil
{

    /**
     * Prevent construction
     */
    private ArrayUtil()
    {
    }

    /**
     *    Initializes an array of double with a given value
     */
    public static double[] init(double[] d, double value)
    {
        for (int i = 0; i < d.length; d[i++] = value);
        return d;
    }

    /**
     *    Initializes an array of ints with a given value
     */
    public static int[] init(int[] d, int value)
    {
        for (int i = 0; i < d.length; d[i++] = value);
        return d;
    }

    /**
     * Returns subset of a given double array
     *
     * @param    array       The array to get subset of
     * @param    startIndex  The starting index (inclusive)
     * @param    endIndex    The ending index (inclusive)
     * @return               Subset of array
     */
    public static double[] subset(double[] array, int startIndex, int endIndex)
    {

        int len = array.length;

        if (len == 0)
            return new double[0];

        /* do bounds checking */
        Assert.isTrue(
            startIndex <= endIndex,
            "Start index " + startIndex + " must be <= end index of " + endIndex);
        Assert.isTrue(
            endIndex <= len,
            "End index " + endIndex + " must be <= array length of " + len);

        /* copy array */
        int subLen = (endIndex - startIndex) + 1;
        double[] sub = new double[subLen];
        int s = 0;
        for (int i = startIndex; i <= endIndex;)
            sub[s++] = array[i++];
        return sub;
    }

    /**
     * Converts an array of doubles to a string
     */
    public static String toString(double[] array)
    {

        Double[] wrapper = new Double[array.length];
        for (int i = 0; i < array.length; i++)
            wrapper[i] = new Double(array[i]);
        return toString(wrapper);
    }

    /**
     * Converts an array of ints to a string
     */
    public static String toString(int[] array)
    {

        Integer[] wrapper = new Integer[array.length];
        for (int i = 0; i < array.length; i++)
            wrapper[i] = new Integer(array[i]);
        return toString(wrapper);
    }

    /**
     * Converts an object array into a comma delimited single string of each elements toString()
     *
     * @param    array        Array of objects to stringify
     * @return   String of comma delimited array elements toString()
     */
    public static String toString(Object[] array)
    {

        StringBuffer sb = new StringBuffer("[" + array.length + "]{");

        switch (array.length)
        {

            case 0 :

                break;

            case 1 :

                sb.append(array[0].toString());
                break;

            default :

                for (int i = 0; i < array.length - 1; i++)
                {
                    if (i != 0)
                        sb.append(", ");
                    sb.append(array[i].toString());
                }
                sb.append(", ");
                sb.append(array[array.length - 1].toString());
                break;

        }

        sb.append("}");
        return sb.toString();
    }

    /**
     * Converts an object array into a comma delimited single string of each elements toString()
     *
     * @param     array        Array of objects to stringify
     * @return    String of comma delimited array elements toString()
     */
    public static String toString(Object[] array, boolean onePerLine)
    {

        StringBuffer sb = new StringBuffer("[" + array.length + "]{");

        if (array.length > 0)
        {

            for (int i = 0; i < array.length - 1; i++)
            {
                if (i != 0)
                {
                    sb.append(", ");
                }

                if (onePerLine)
                    sb.append("\n");

                sb.append(array[i].toString());
            }
            sb.append(", ");
            if (onePerLine)
                sb.append("\n");
            sb.append(array[array.length - 1].toString());
        }

        sb.append("}");
        return sb.toString();
    }
}