package toolbox.graph;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import toolbox.junit.testcase.UITestCase;
import toolbox.util.RandomUtil;

/**
 * AbstractGraphTest is a base class for testing the graphing subsystem.
 */
public abstract class AbstractGraphTest extends UITestCase
{
    private static final Logger logger_ = 
        Logger.getLogger(AbstractGraphTest.class);

    //--------------------------------------------------------------------------
    // Abstract
    //--------------------------------------------------------------------------
    
    /**
     * Returns the graph library implementation to test in the subclass.
     * 
     * @return GraphLib
     */
    public abstract GraphLib getGraphLib();
    
    //--------------------------------------------------------------------------
    // Unit Tests
    //--------------------------------------------------------------------------
    
    public void testGraph1() throws Exception
    {
        logger_.info("Running testGraph1...");
        
        GraphLib factory = getGraphLib();
        Graph graph = factory.createGraph();
        
        Vertex sun = factory.createVertex(graph, "sun");
        graph.addVertex(sun);
        
        Vertex moon = factory.createVertex(graph, "moon");
        graph.addVertex(moon);
        
        Edge edge = factory.createEdge(sun, moon);
        graph.addEdge(edge);
        
        GraphView view = factory.createView(graph);
        
        launchInDialog(view.getComponent());
    }
    
    
    public void testGraph2() throws Exception
    {
        logger_.info("Running testGraph2...");
        
        GraphLib lib = getGraphLib();
        Graph graph = lib.createGraph();
        
        List nodes = new ArrayList();
        
        for (int i = 0; i < 50; i++)
        {
            Vertex v = lib.createVertex(graph, i + 1 + "");
            nodes.add(v);
            graph.addVertex(v);
        }
        
        for (int i = 0; i < 50; i++)
        {
            Vertex from = (Vertex) nodes.get(i);
            Vertex to = (Vertex) RandomUtil.nextElement(nodes);
            Edge edge = lib.createEdge(from, to);
            graph.addEdge(edge);
        }
        
        GraphView view = lib.createView(graph);
        
        launchInDialog(view.getComponent());
    }
}