package toolbox.util.decompiler;

import java.io.File;

import org.apache.log4j.Logger;

import toolbox.util.StreamUtil;

/**
 * Decompiler bridge to the windows only JAD decompiler.
 * 
 * @see toolbox.util.decompiler.DecompilerFactory 
 */
public class JadDecompiler extends AbstractDecompiler
{
    private static final Logger logger_ = Logger.getLogger(JadDecompiler.class);
    
    //--------------------------------------------------------------------------
    // Constructors
    //--------------------------------------------------------------------------

    /**
     * Creates a JadDecompiler.
     */
    public JadDecompiler()
    {
        super("Jad");
    }
    
    //--------------------------------------------------------------------------
    // Decompiler Interface
    //--------------------------------------------------------------------------
    
    /**
     * @see toolbox.util.decompiler.Decompiler#decompile(java.io.File)
     */
    public String decompile(File classFile) throws DecompilerException 
    {
        String javaSource = null;
        
        try
        {
            String cmdLine = 
                "jad -space -o -ff -s java -p -& \"" + 
                    classFile.getCanonicalPath() + "\"";
            
            Process process = Runtime.getRuntime().exec(cmdLine);
            
            javaSource = StreamUtil.asString(process.getInputStream());
            
        }
        catch (Exception e)
        {
            throw new DecompilerException(e);
        }
        
        return javaSource;
    }
    
    
    /**
     * @see toolbox.util.decompiler.Decompiler#decompile(
     *      java.lang.String, java.lang.String)
     */
    public String decompile(String className, String classPath)
        throws DecompilerException
    {
        throw new IllegalArgumentException("Not supported");
    }
}