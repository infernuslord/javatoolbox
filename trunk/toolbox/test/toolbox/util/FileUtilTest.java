package toolbox.util;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;
import junit.textui.TestRunner;

import org.apache.commons.collections.ListUtils;
import org.apache.commons.collections.TransformerUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.lang.math.RandomUtils;
import org.apache.log4j.Logger;

import toolbox.tree.Tree;

/**
 * Unit test for {@link toolbox.util.FileUtil}.
 */
public class FileUtilTest extends TestCase
{
    private static final Logger logger_ = Logger.getLogger(FileUtilTest.class);

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
        TestRunner.run(FileUtilTest.class);
    }

    //--------------------------------------------------------------------------
    // Unit Tests
    //--------------------------------------------------------------------------
    
    /**
     * Tests the getTempDir() method. 
     * 
     * @throws Exception on error
     */    
    public void testGetTempDir() throws Exception
    {
        logger_.info("Running testGetTempDir...");
        
        String tempDir = FileUtil.getTempDir().getCanonicalPath();
        File file = new File(tempDir);
        assertTrue("temp is not a directory", file.isDirectory());
        logger_.info("Passed: Temp dir = " + tempDir);
    }

    //--------------------------------------------------------------------------
    // generateTempFilename()
    //--------------------------------------------------------------------------
    
    /**
     * Tests the generateTempFilename() method. 
     * 
     * @throws Exception on error
     */    
    public void testGenerateTempFilename() throws Exception
    {
        logger_.info("Running testGenerateTempFilename...");
        
        // Generate temp file name
        String tempFile = FileUtil.createTempFilename();
        assertNotNull("temp filename is null", tempFile);
        
        // Use temp file name to create a file
        FileUtil.setFileContents(tempFile, "this is a temp file", false);
        FileUtil.getFileContents(tempFile); 
        File file = new File(tempFile);
        file.delete();
        
        logger_.info("Passed: Created temp file " + tempFile);
    }

    
    /**
     * Tests the generateTempFilename(File forDir) method. 
     * 
     * @throws Exception on error
     */    
    public void testGenerateTempFilenameForDir() throws Exception
    {
        logger_.info("Running testGenerateTempFilenameForDir...");
        
        // Generate temp file name in temp directory
        String tempFile = FileUtil.createTempFilename(FileUtil.getTempDir());
        assertNotNull("temp filename is null", tempFile);
        
        // Use temp file name to create a file
        FileUtil.setFileContents(tempFile, "this is a temp file", false);
        FileUtil.getFileContents(tempFile); 
        File file = new File(tempFile);
        file.delete();
        
        logger_.info("Passed: Created temp file " + tempFile);
    }

    //--------------------------------------------------------------------------
    // cleanDir()
    //--------------------------------------------------------------------------
    
    /**
     * Tests cleanDir() for cleaning the contents of a single directory.
     * 
     * @throws Exception on error
     */
    public void testCleanDir() throws Exception
    {
        logger_.info("Running testCleanDirFailure...");
        
        int numFiles = 10;
        
        // Create a directory
        File dir = FileUtil.createTempDir();
        
        // Populate with files
        for (int i = 0; i < numFiles; i++)
        {
            String filename = i + ".txt";
            File file = new File(dir, filename);
            FileUtil.setFileContents(file, "testing..", false);
        }

        // Verify test files created
        String[] before = dir.list();
        logger_.info("Contents before: " + ArrayUtil.toString(before));
        assertEquals("Dir " + dir + " should have files",
            numFiles, before.length); 

        try
        {   
            // Clean dir and verify no files are left
            FileUtil.cleanDir(dir);
            String[] after = dir.list();
            logger_.info("Contents after: " + ArrayUtil.toString(after));
            assertEquals("No files should be left in " + dir, 0, after.length);
        }
        finally
        {
            FileUtil.removeDir(dir);
        }
    }
    
    
    /**
     * Tests cleanDir() for failure by passing a file instead of a directory.
     * 
     * @throws Exception on error
     */
    public void testCleanDirFailure1() throws Exception
    {
        logger_.info("Running testCleanDirFailure1...");
        
        // Create a file
        String file = FileUtil.createTempFilename();
        FileUtil.setFileContents(file, "hello", false);
        File f = new File(file);
        
        try
        {
            FileUtil.cleanDir(f);
            fail("Should have failed on a file, not a directory");
        }
        catch (IllegalArgumentException e)
        {
            logger_.info("Passed: " + e);
        }
        finally
        {
            f.delete();    
        }
    }
    
    
    /**
     * Tests cleanDir() for failure by passing in a non-existant directory.
     * 
     * @throws Exception on error
     */
    public void testCleanDirFailure2() throws Exception
    {
        logger_.info("Running testCleanDirFailure2...");
        
        // Create a bogus dir name
        String dir = FileUtil.createTempFilename();
        
        try
        {
            FileUtil.cleanDir(new File(dir));
            fail("Should have failed on a non-existant directory");
        }
        catch (IllegalArgumentException e)
        {
            logger_.info("Passed: " + e);
        }
    }
    
    //--------------------------------------------------------------------------
    // getFileContents()
    //--------------------------------------------------------------------------
    
    /**
     * Tests getFileContents().
     * 
     * @throws Exception on error
     */
    public void testGetFileContents() throws Exception
    {
        logger_.info("Running testGetFileContents...");
        
        // Create a file
        String file = FileUtil.createTempFilename();
        String contents = "blah blah blah";
        FileUtil.setFileContents(file, contents, false);
        
        // Read it back in
        String currentContents = FileUtil.getFileContents(file);
        
        // Compare
        assertEquals("contents should be equals", contents, currentContents);
        
        // Clean up
        new File(file).delete();
    }

    
    /**
     * Tests getFileContents() for a large file (500k).
     * 
     * @throws Exception on error
     */
    public void testGetFileContentsLargeFile() throws Exception
    {
        logger_.info("Running testGetFileContentsLargeFile...");
        
        // Half meg file
        int fileSize = 500000;
        
        // Create a file
        String file = FileUtil.createTempFilename();
        StringBuffer contents = new StringBuffer();
        
        for (int i = 0; i < fileSize; i++)
            contents.append(RandomUtil.nextAlpha());
        
        FileUtil.setFileContents(file, contents.toString(), false);
        
        // Read it back in
        String currentContents = FileUtil.getFileContents(file);
        
        // Compare
        assertEquals("contents should be equals", 
            contents.toString(), currentContents);
        
        logger_.info("Passed: " + file + " length " + currentContents.length());
        
        // Clean up
        File reread = new File(file);        
        reread.delete();
    }

    //--------------------------------------------------------------------------
    // getFileAsBytes()
    //--------------------------------------------------------------------------
    
    /**
     * Tests getFileAsBytes().
     * 
     * @throws Exception on error
     */
    public void testGetFileAsBytes() throws Exception
    {
        logger_.info("Running testGetFileAsBytes...");
        
        String file = FileUtil.createTempFilename();
        
        try
        {
            String contents = "blah blah blah";
            FileUtil.setFileContents(file, contents, false);
            byte[] currentContents = FileUtil.getFileAsBytes(file);
            
            assertEquals("File contents should be equal", contents, 
                new String(currentContents));
        }
        finally
        {
            FileUtil.delete(file);
        }
    }

    //--------------------------------------------------------------------------
    // setFileContents()
    //--------------------------------------------------------------------------

    /**
     * Tests setFileContents().
     * 
     * @throws Exception on error
     */
    public void testSetFileContents() throws Exception
    {
        logger_.info("Running testSetFileContents...");
        
        // Create a file
        String file = FileUtil.createTempFilename();
        String contents = "blah blah blah";
        FileUtil.setFileContents(file, contents, false);
        
        // Read it back in
        File reread = new File(file);
        String currentContents = FileUtil.getFileContents(file);
        
        // Compare
        assertEquals("contents should be equals", contents, currentContents);
        logger_.info("Passed: setFileContents on " + file);
        
        reread.delete();
    }

    
    /**
     * Tests setFileContents().
     * 
     * @throws Exception on error
     */
    public void testSetFileContentsBytes() throws Exception
    {
        logger_.info("Running testSetFileContentsBytes...");
        
        // Create a file
        String file = FileUtil.createTempFilename();
        byte[] contents = "blah blah blah".getBytes();
        FileUtil.setFileContents(file, contents, false);
        
        // Read it back in
        byte[] currentContents = FileUtil.getFileAsBytes(file);
        
        // Compare
        assertEquals("contents should be equals", 
            new String(contents), 
                new String(currentContents));
                
        new File(file).delete();
    }

    
    /**
     * Tests setFileContents(File).
     * 
     * @throws Exception on error
     */
    public void testSetFileContents2() throws Exception
    {
        logger_.info("Running testSetFileContents2...");
        
        // Create a file
        String file = FileUtil.createTempFilename();
        String contents = "blah blah blah";
        FileUtil.setFileContents(new File(file), contents, false);
        
        // Read it back in
        File reread = new File(file);
        String currentContents = FileUtil.getFileContents(file);
        
        // Compare
        assertEquals("contents should be equals", contents, currentContents);
        logger_.info("Passed: setFileContents2 on " + file);
        
        // Clean up
        reread.delete();
    }
    
    //--------------------------------------------------------------------------
    // moveFile()
    //--------------------------------------------------------------------------
    
    /**
     * Tests moveFile() for simple case.
     * 
     * @throws Exception on error
     * 
     * <pre>
     * 
     * tmpdir
     *   |
     *   +-sourcedir
     *   |    | 
     *   |    +-file.txt <== move from
     *   |
     *   |
     *   +-destdir       <== move to
     * 
     * 
     * </pre>
     */
    public void testMoveFile() throws Exception
    {
        logger_.info("Running testMoveFile..."); 
        
        // Make src dir
        String srcDirName = FileUtil.createTempFilename();
        File   srcDir     = new File(srcDirName);
        srcDir.mkdir();
        
        // Make dest dir
        String destDirName = FileUtil.createTempFilename();
        File   destDir     = new File(destDirName);
        destDir.mkdir();
 
        try
        {
            // Make src file
            String srcFilename = FileUtil.createTempFilename(srcDir);
            File   srcFile     = new File(srcFilename);
            String srcContents =  "test file for move";
            FileUtil.setFileContents(srcFilename, srcContents, false);
    
            // Take snapshot before file move
            String[] beforeMoveSrc = srcDir.list();
            String[] beforeMoveDest = destDir.list();
            
            logger_.info(
                "Before move:  src=" + ArrayUtil.toString(beforeMoveSrc));
                
            logger_.info(
                "Before move: dest=" + ArrayUtil.toString(beforeMoveDest));
            
            assertEquals(
                "should be one file in src dir", 1, beforeMoveSrc.length);
                
            assertEquals(
                "should be zero files in dest dir", 0, beforeMoveDest.length);
    
            // Move file
            FileUtil.moveFile(srcFile, destDir);
    
            // Take snapshot again
            String[] afterMoveSrc = srcDir.list();
            String[] afterMoveDest = destDir.list();
    
            logger_.info(
                "After move:  src=" + ArrayUtil.toString(afterMoveSrc));
                
            logger_.info(
                "After move: dest=" + ArrayUtil.toString(afterMoveDest));
                    
            assertEquals(
                "should be zero files in src dir", 0, afterMoveSrc.length);
                
            assertEquals(
                "should be one file in dest dir", 1, afterMoveDest.length);
    
            // Compare contents of moved file
            String destContents = 
                FileUtil.getFileContents(
                    destDir.listFiles()[0].getAbsolutePath());
                
            assertEquals("contents of moved file should be the same", 
                srcContents, destContents);
                
            logger_.info("Passed: moveFile");
        }
        finally
        {
            // Cleanup
            
            FileUtil.cleanDir(destDir);
            srcDir.delete();
            destDir.delete();
        }
    }

    //--------------------------------------------------------------------------
    // trailWithSeparator()
    //--------------------------------------------------------------------------
    
    /**
     * Tests trailWithSeparator() with the separator missing.
     */
    public void testTrailWithSeparatorMissing()
    {
        logger_.info("Running testTrailWithSeparatorMissing...");
        
        String path = File.separator + "java";
        String trailed = FileUtil.trailWithSeparator(path);
        assertEquals(path + File.separator, trailed);
    }    
    
    
    /**
     * Tests trailWithSeparator() with separator already there.
     */
    public void testTrailWithSeparatorAlreadyExists()
    {
        logger_.info("Running testTrailWithSeparatorAlreadyExists...");
        
        String path = File.separator + "java" + File.separator;
        String trailed = FileUtil.trailWithSeparator(path);
        assertEquals(path, trailed);
    }    
    
    //--------------------------------------------------------------------------
    // delete()
    //--------------------------------------------------------------------------
    
    /**
     * Tests delete(File).
     * 
     * @throws Exception on error
     */
    public void testDeleteByFile() throws Exception
    {
        logger_.info("Running testDeleteByFile...");
        
        String file = FileUtil.createTempFilename();
        FileUtil.setFileContents(file, "test data", false);
        File f = new File(file);
        assertTrue(f.exists());
        FileUtil.delete(f);
        assertTrue(!f.exists());
    }
    
    
    /**
     * Tests delete(String).
     * 
     * @throws Exception on error
     */
    public void testDeleteByFilename() throws Exception
    {
        logger_.info("Running testDeleteByFilename...");
        
        String file = FileUtil.createTempFilename();
        FileUtil.setFileContents(file, "test data", false);
        File f = new File(file);
        assertTrue(f.exists());
        FileUtil.delete(file);
        assertTrue(!f.exists());
    }
    
    
    /**
     * Tests delete() on a non-existant file.
     * 
     * @throws Exception on error
     */
    public void testDeleteBogusFile() throws Exception
    {
        logger_.info("Running testDeleteBogusFile...");
        
        try
        {
            File f = new File("some_bogus_file");
            FileUtil.delete(f);
            assertNotNull("Success");
        }
        catch (Exception e)
        {
            // Delete should be quiet no matter what so we don't expect an
            // exception to be thrown on a non-existant file.
            fail("Delete of non-existant file should not generate an error.");
        }
        
        try
        {
            FileUtil.delete("some_bogus_file");
            assertNotNull("Success");
        }
        catch (Exception e)
        {
            // Delete should be quiet no matter what so we don't expect an
            // exception to be thrown on a non-existant file.
            fail("Delete of non-existant file should not generate an error.");
        }
    }
    
    
    /**
     * Tests delete() passed a null object.
     * 
     * @throws Exception on error
     */
    public void testDeleteNull() throws Exception
    {
        logger_.info("Running testDeleteNull...");
        
        try
        {
            FileUtil.delete((File) null);
            assertNotNull("Success");
        }
        catch (Exception e)
        {
            fail("Delete error for null should be suppressed.");   
        }
        
        try
        {
            FileUtil.delete((String) null);
            assertNotNull("Success");
        }
        catch (Exception e)
        {
            fail("Delete error for null should be suppressed.");   
        }
    }

    //--------------------------------------------------------------------------
    // getExtension()
    //--------------------------------------------------------------------------
    
    /**
     * Tests getExtension(File)
     */
    public void testGetExtensionByFile()
    {
        logger_.info("Running testGetExtensionByFile...");
        
        File t = FileUtil.getTempDir();
        assertEquals("txt", FileUtil.getExtension(new File(t, "file.txt")));
        assertEquals("txt", FileUtil.getExtension(new File(t, "file.ame.txt")));
        assertEquals("txt", FileUtil.getExtension(new File(t, ".txt")));
        assertEquals("", FileUtil.getExtension(new File(t, "noextension")));
        assertEquals("", FileUtil.getExtension(new File(t, "")));
        assertEquals("", FileUtil.getExtension(new File(t, "file.txt.")));
    }
    
    //--------------------------------------------------------------------------
    // Misc
    //--------------------------------------------------------------------------
    
    /**
     * Tests matchPlatformSeparator().
     */
    public void testMatchPlatformSeparator()
    {
        logger_.info("Running testMatchPlatformSeparator...");
        
        String match = File.separator + "a" + File.separator + "b";
        assertEquals(match, FileUtil.matchPlatformSeparator("\\a\\b"));
        assertEquals(match, FileUtil.matchPlatformSeparator("/a/b"));
    }
    
    
    /**
     * Tests dropExtension().
     */
    public void testDropExtension()
    {
        logger_.info("Running testDropExtension...");
 
        assertEquals("foobar", FileUtil.dropExtension("foobar"));
        assertEquals("foobar", FileUtil.dropExtension("foobar.txt"));
        assertEquals("foobar.old", FileUtil.dropExtension("foobar.old.txt"));
    }
    
    
    /**
     * Tests createTempDir().
     * 
     * @throws Exception on error
     */
    public void testCreateTempDir() throws Exception
    {
        logger_.info("Running testCreateTempDir...");
        
        // Create temp dir
        // Get list of dirs in the system temp dir
        // Make sure created temp dir shows up in the list.
        
        File tempDir = FileUtil.createTempDir();
        
        try
        {
            File[] fdirs = 
                FileUtil.getTempDir().listFiles(
                    (FileFilter) DirectoryFileFilter.INSTANCE);
            
            List names = ListUtils.transformedList(new ArrayList(), 
                TransformerUtils.invokerTransformer("getName"));

            ArrayUtil.drainTo(fdirs, names);
            //logger_.debug("Dirs = " + ArrayUtil.toString(names.toArray()));
            assertTrue(ArrayUtil.contains(names.toArray(), tempDir.getName()));
        }
        finally
        {
            FileUtils.forceDelete(tempDir);
        }
    }
    
    
    /**
     * Tests findFiles().
     * 
     * @throws Exception on error
     */
    public void testFindFiles() throws Exception
    {
        //logger_.info("Running testFindFiles...");
        
        // TODO: write me
        
        //assertTrue(true);
    }
    
    
    /**
     * Tests getLargestFile()
     * 
     * @throws Exception on error
     */
    public void testGetLargestFile() throws Exception
    {
        logger_.info("Running testGetLargestFile...");
        
        File dir = FileUtil.createTempDir();
        int maxFiles = 20;
        
        try
        {
            int maxLen = 0;
            
            for (int i = 0; i < maxFiles; i++)
            {
                File file = FileUtil.createTempFile(dir);
                int len = RandomUtil.nextInt(1, 5000);
                
                if (len > maxLen)
                    maxLen = len;
                
                String content = RandomUtil.nextString(len);
                FileUtil.setFileContents(file, content, false);
            }
            
            File largest = FileUtil.getLargestFile(dir.listFiles());
            assertEquals(maxLen, largest.length());
        }
        finally
        {
            FileUtil.removeDir(dir);
        }
    }

    
    /**
     * Tests getLargestFile() for parameters null and empty array. 
     * 
     * @throws Exception on error.
     */
    public void testGetLargestFileNegative() throws Exception
    {
        logger_.info("Running testGetLargestFileNegative...");
        assertNull(FileUtil.getLargestFile(new File[0]));
        assertNull(FileUtil.getLargestFile(null));
    }
    
    
    /**
     * Tests getLongestFilename()
     * 
     * @throws Exception on error
     */
    public void testGetLongestFilename() throws Exception
    {
        logger_.info("Running testGetLongestFilename...");
        
        File longest = FileUtil.getLongestFilename(new File[] 
        {
            new File("a"),
            new File("xyz"),
            new File("ij"),
            new File("")
        });
        
        assertEquals("xyz", longest.getName());
    }

    
    /**
     * Tests getLongestFilename() for parameters null and empty array. 
     * 
     * @throws Exception on error.
     */
    public void testGetLongestFilenameNegative() throws Exception
    {
        logger_.info("Running testGetLongestFilenameNegative...");
        assertNull(FileUtil.getLongestFilename(new File[0]));
        assertNull(FileUtil.getLongestFilename(null));
    }
    
    
    
    /**
     * Tests removeDir()
     * 
     * @throws Exception on error.
     */
    public void testRemoveDir() throws Exception
    {
        logger_.info("Running testRemoveDir...");
        
        File root = FileUtil.createTempDir();
        File current = root;
        
        for (int i = 0; i < 10; i++)
        {    
            File newDir = new File(current, "dir-" + 
                RandomUtil.nextString(10).toLowerCase());
            
            if (!newDir.mkdir())
                break;
            
            FileUtil.setFileContents(
                newDir.getAbsolutePath() + File.separator + "file-" + 
                RandomUtils.nextInt(50),
                RandomUtil.nextString(RandomUtils.nextInt(5000)).toLowerCase(), 
                false);
            
            current = newDir;
        }
        
        Tree before = new Tree(root, true, true);
        before.showTree();
        
        FileUtil.removeDir(root);
        
        assertTrue(!root.exists());
    }


    /**
     * Tests removeDir() failure cases.
     * 
     * @throws Exception on error.
     */
    public void testRemoveDirFailure() throws Exception
    {
        logger_.info("Running testRemoveDirFailure...");
        
        FileUtil.removeDir(null);

        // TODO: Readonly files.
        //       Files current in use/locked.
    }
    
    
    /**
     * Tests getInfo()
     */
    public void testGetInfo() throws Exception
    {
        logger_.info("Running testGetFileInfo...");
        String s = FileUtil.getInfo(new File(System.getProperty("user.dir")));
        logger_.info(StringUtil.banner(s));
        assertNotNull(s);
    }
}