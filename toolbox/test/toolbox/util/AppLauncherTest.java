package toolbox.util;

import java.io.File;

import junit.framework.TestCase;
import junit.textui.TestRunner;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;


public class AppLauncherTest extends TestCase {

    public static final Logger logger = Logger.getLogger(AppLauncherTest.class);
    
    
    public static void main(String[] args) {
        TestRunner.run(AppLauncherTest.class);
    }
    
    
    public void testLaunch() throws Exception {
        logger.info("Running testLaunch...");
        
        File tempDir = FileUtil.createTempDir();
        File tempFile = new File(tempDir, "AppLauncherTestFile.txt");
        
        try {
            FileUtils.writeStringToFile(tempFile, "This is the content of " + tempFile.getAbsolutePath(), "UTF8");
            AppLauncher.launch(tempFile.getAbsolutePath());
        }
        finally {
            FileUtils.deleteDirectory(tempDir);
        }
    }

    
    public void testLaunchVsd() throws Exception {
        logger.info("Running testLaunchVsd...");
        File tempFile = new File("M:\\x1700_aas_services_dynamic\\bservices\\Pricing\\design\\PricingServiceUMLDiagrams.vsd");
        AppLauncher.launch(tempFile.getAbsolutePath());
    }
}
