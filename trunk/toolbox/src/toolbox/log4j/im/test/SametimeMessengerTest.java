package toolbox.log4j.im.test;

import java.util.Properties;

import junit.framework.TestCase;
import junit.textui.TestRunner;

import org.apache.log4j.Logger;
import org.apache.log4j.helpers.LogLog;
import org.apache.log4j.xml.DOMConfigurator;

import toolbox.junit.StandaloneTestCase;
import toolbox.log4j.im.InstantMessenger;
import toolbox.log4j.im.SametimeMessenger;
import toolbox.util.ResourceUtil;
import toolbox.util.ThreadUtil;
import toolbox.util.XMLUtil;

/**
 * Unit test for SametimeMessenger.
 */
public class SametimeMessengerTest
    extends TestCase
    implements StandaloneTestCase
{
    private static final Logger logger_ =
        Logger.getLogger(SametimeMessengerTest.class);

    //--------------------------------------------------------------------------
    // Main
    //--------------------------------------------------------------------------

    /**
     * Entrypoint.
     * 
     * @param args None recognized.
     */
    public static void main(String[] args)
    {
        TestRunner.run(SametimeMessengerTest.class);
    }

    //--------------------------------------------------------------------------
    // Unit Tests
    //--------------------------------------------------------------------------

    /**
     * Tests full lifecycle of typical usage.
     * 
     * @throws Exception on error.
     */
    public void testLifeCycle() throws Exception
    {
        LogLog.setInternalDebugging(true);

        logger_.info("Running testLifeCycle...");

        InstantMessenger messenger = new SametimeMessenger();

        logger_.debug("Before init...");
        Properties props = new Properties();
        props.setProperty(InstantMessenger.PROP_THROTTLE, 500 + "");
        messenger.initialize(props);

        logger_.debug("Before login...");
        messenger.login("supahfuzzy", "techno");

        logger_.debug("Before send...");
        messenger.send(
            "analogue",
            "Hello from the "
                + getClass().getName()
                + ".testLifeCycle unit test.");
        ThreadUtil.sleep(10000);

        logger_.debug("Before logout...");
        messenger.logout();

        logger_.debug("Before shutdown...");
        messenger.shutdown();

        logger_.debug("All done!");
    }

    
    /**
     * Tests sending a whole slew of messages.
     * 
     * @throws Exception on error.
     */
    public void xtestSendMany() throws Exception
    {
        logger_.info("Running testSendMany...");

        InstantMessenger messenger = new SametimeMessenger();
        Properties props = new Properties();
        props.setProperty(InstantMessenger.PROP_THROTTLE, 500 + "");

        messenger.login("supahfuzzy", "techno");

        for (int i = 0; i < 100; i++)
        {
            ThreadUtil.sleep(300); // throttle
            messenger.send(
                "analogue",
                "This is message number " + (i + 1) + " of 100");
        }

        messenger.logout();
        messenger.shutdown();
    }

    
    /**
     * Tests that the configuration in the xml file is read and executed
     * correctly.
     * 
     * @throws Exception on error.
     */
    public void xtestConfigByXML() throws Exception
    {
        logger_.info("Running testConfigByXML...");

        String xmlConfig =
            ResourceUtil.getResourceAsString(
                "/toolbox/log4j/im/test/SametimeMessengerTest.xml");

        //BasicConfigurator.resetConfiguration();
        DOMConfigurator.configure(XMLUtil.toElement(xmlConfig));

        Logger logger = Logger.getLogger("testXMLInit_logger");

        logger.debug("debug");
        logger.info("info");
        logger.warn("warn");
        logger.error("error");

        ThreadUtil.sleep(2000);

        Logger.getRootLogger().removeAppender("testXMLInit_appender");
    }
}