package toolbox.launcher;

import java.util.HashMap;

import toolbox.util.ArrayUtil;

/**
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 */
public class Main
{
    
    private static HashMap programMap;
    
    static
    {
        /* map program names to class names */
        programMap = new HashMap(10);
        
        programMap.put("findclass",     "toolbox.findclass.Main");
        programMap.put("showclasspath", "toolbox.showclasspath.Main");
        programMap.put("showpath",      "toolbox.showpath.Main");
        programMap.put("jsourceview",   "toolbox.jsourceview.JSourceView");
        programMap.put("jtail",         "toolbox.jtail.Main");
        programMap.put("tail",          "toolbox.tail.Main");
        programMap.put("jtcptunnel",    "toolbox.tcptunnel.JTcpTunnel");
        programMap.put("tcptunnel",     "toolbox.tcptunnel.TcpTunnel");
    }
    
    /**
     * Entrypoint 
     */
    public static void main(String[] args)
    {
        Main launcher = new Main(args);
    }
    
    /**
     * Constructor for Main.
     */
    public Main()
    {
        super();
    }
    
    /**
     * Arg constructor 
     */
    public Main(String[] args)
    {
        switch (args.length)
        {
            case 0 :
                printUsage();    
                break;
                
            default :
                launch(ArrayUtil.s
                break;
        }
        
    }
    
    /**
     * Prints launcher usage
     */
    public void printUsage()
    {
        System.out.println(
            "Usage: java -jar toolbox.jar [program] [args]          \n" +
            "                                                       \n" +
            "       where program is:                               \n" +
            "                                                       \n" +
            "       findclass     => find a java class file         \n" + 
            "       showclasspath => show detailed classpath info   \n" +
            "       showpath      => show detailed path info        \n" +
            "       jsourceview   => java code counter              \n" +
            "       jtail         => java tailer (GUI)              \n" +
            "       tail          => java tail   (console)          \n" +
            "       jtcptunnel    => tcp tunnel  (GUI)              \n" +
            "       tcptunnel     => tcp tunnel  (console)          \n" );
    }
    
}
