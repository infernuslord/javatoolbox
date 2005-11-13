package toolbox.util.dirmon.recognizer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.Assert;
import toolbox.util.CollectionUtil;
import toolbox.util.dirmon.DirSnapshot;
import toolbox.util.dirmon.DirectoryMonitor;
import toolbox.util.dirmon.DirectoryMonitorEvent;
import toolbox.util.dirmon.FileSnapshot;
import toolbox.util.dirmon.IFileActivityRecognizer;

/**
 * An activity that is capable of recognizing when new files are added to a
 * directory.
 */
public class FileCreatedRecognizer implements IFileActivityRecognizer {

    private DirectoryMonitor monitor_;
    
    // --------------------------------------------------------------------------
    // Constructors
    // --------------------------------------------------------------------------

    public FileCreatedRecognizer(DirectoryMonitor monitor) {
        monitor_ = monitor;
    }

    // --------------------------------------------------------------------------
    // IFileActivityRecognizer Interface
    // --------------------------------------------------------------------------

    /*
     * @see toolbox.util.file.activity.IFileActivityRecognizer#getRecognizedEvents(toolbox.util.file.snapshot.DirSnapshot, toolbox.util.file.snapshot.DirSnapshot)
     */
    public List getRecognizedEvents(DirSnapshot before, DirSnapshot after) {
        
        Assert.assertTrue(before.getDirectory().equals(after.getDirectory()));
        
        List createdFileEvents = new ArrayList();
        Map beforeFileSnapshots = before.getFileSnapshots();
        Map afterFileSnapshots = after.getFileSnapshots();
        
        Set beforeFileKeys = beforeFileSnapshots.keySet();
        Set afterFileKeys = afterFileSnapshots.keySet();
        
        Collection allFileKeys = 
            CollectionUtil.union(beforeFileKeys, afterFileKeys);
        
        Collection createdFileKeys =
            CollectionUtil.difference(allFileKeys, beforeFileKeys);
        
        for (Iterator i = createdFileKeys.iterator(); i.hasNext();) {
            String fileKey = (String) i.next();

            DirectoryMonitorEvent event = 
                new DirectoryMonitorEvent(
                    DirectoryMonitorEvent.TYPE_CREATED,
                    monitor_, 
                    (FileSnapshot) null, 
                    (FileSnapshot) after.getFileSnapshots().get(fileKey));
            
            createdFileEvents.add(event);
        }
        
        return createdFileEvents;
    }
}