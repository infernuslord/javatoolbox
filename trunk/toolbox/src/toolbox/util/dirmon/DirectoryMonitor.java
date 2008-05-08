package toolbox.util.dirmon;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.buffer.BoundedFifoBuffer;
import org.apache.log4j.Logger;

import toolbox.org.apache.commons.io.find.FileFinder;
import toolbox.org.apache.commons.io.find.FindEvent;
import toolbox.org.apache.commons.io.find.FindListener;
import toolbox.org.apache.commons.io.find.Finder;
import toolbox.util.ArrayUtil;
import toolbox.util.ElapsedTime;
import toolbox.util.ThreadUtil;
import toolbox.util.dirmon.event.FileEvent;
import toolbox.util.dirmon.event.StatusEvent;
import toolbox.util.service.Destroyable;
import toolbox.util.service.Nameable;
import toolbox.util.service.ObservableService;
import toolbox.util.service.ServiceException;
import toolbox.util.service.ServiceListener;
import toolbox.util.service.ServiceNotifier;
import toolbox.util.service.ServiceState;
import toolbox.util.service.ServiceTransition;
import toolbox.util.service.ServiceUtil;
import toolbox.util.service.Startable;
import toolbox.util.service.Suspendable;
import toolbox.util.statemachine.StateMachine;

/**
 * Monitors a directory for file activity based on a configurable selection 
 * criteria. Interested parties can register for notification of activity by 
 * implementing the {@link toolbox.util.dirmon.IDirectoryMonitorListener} 
 * interface.
 * <p>
 * <b>Example:</b>
 * <pre class="snippet">
 * 
 * // Monitor the current directory
 * DirectoryMonitor dm = new DirectoryMonitor(new File("."));
 * 
 * // Lets listen for newly created files
 * dm.addRecognizer(new FileCreatedRecognizer());
 * 
 * // Register a listener
 * dm.addDirectoryMonitorListener(new IDirectoryMonitorListener() {
 * 
 *     public void directoryActivity(FileEvent event)
 *         throws Exception {
 *         System.out.println("File created: " + event);  
 *      }
 *  });
 *
 * // Starts the monitor asynchronously
 * dm.start();
 *
 * // Directory now being monitored...any file activity in the directory will
 * // be reported via the IDirectoryListener interface.
 *
 * // All done..shutdown 
 * dm.stop();
 * dm.destroy();
 * </pre>
 */
public class DirectoryMonitor 
    implements Startable, Suspendable, Destroyable, ObservableService , Nameable {
    
    private static Logger logger_ =  Logger.getLogger(DirectoryMonitor.class);

    //--------------------------------------------------------------------------
    // Constants 
    //--------------------------------------------------------------------------
    
    private static final int DEFAULT_DELAY = 60000;       // 60 seconds
    private static final int DEFAULT_PER_DIR_DELAY = 100; // 100 milliseconds

    //--------------------------------------------------------------------------
    // Fields 
    //--------------------------------------------------------------------------
    
    /** 
     * List of {@link IDirectoryMonitorListener}s interested in events
     * generated by this DirectoryMonitor. 
     */
    private List listeners_;
    
    /** 
     * Delay interval in millis used to check for new activity. 
     */
    private int perScanDelay_;

    /**
     * Delay between each directory being processed in list of directories.
     */
    private int perDirectoryDelay_;

    /** 
     * List of directories to monitor for file activity if recursive monitoring
     * is turned on.
     * 
     * @see java.io.File
     */
    private List monitoredDirectories_;
   
    /**
     * The root directory to monitor.
     */
    private File rootDirectory_;
    
    /** 
     * Thread that interested listeners are notified on. 
     */
    private Thread monitor_;
    
    /**
     * Represents the service lifecycle of this DirectoryMonitor.
     */
    private StateMachine stateMachine_;

    /**
     * Delegate convenience class that aids in the notification of 
     * {@link toolbox.util.service.Service} state changes.
     */
    private ServiceNotifier notifier_;
    
    /**
     * Key = String which is File.getAbsolutePath() for a directory
     * Value = DirSnapshot
     */
    private Map dirSnapshots_;
    
    /**
     * List of recognizers that are able to discern file differences given
     * two directory snapshots.
     * 
     * @see IFileActivityRecognizer
     */
    private List recognizers_;

    /**
     * Friendly name of this directory monitor.
     */
    private String name_;

    /**
     * Flag to specify that the monitor should also recursively monitor all
     * subdirectories.
     */
    private boolean recurse_;

    //--------------------------------------------------------------------------
    // Constructors
    //--------------------------------------------------------------------------
    
    /**
     * Creates a DirectoryMonitor for the given directory. Subdirectories are
     * not monitored and the friendly name is set to the directory's absolute
     * path.
     * 
     * @param dir Directory to monitor.
     */
    public DirectoryMonitor(File dir) {
        this(dir, false);
    }

    
    /**
     * Creates a DirectoryMonitor for the given directory and sets the friendly
     * name to the directory's absolute path.
     * 
     * @param dir Directory to monitor.
     * @param subdirs Set to true to also monitor subdirectories.
     */
    public DirectoryMonitor(File dir, boolean subdirs) {
        this(dir, dir.getAbsolutePath(), false);
    }

    
    /**
     * Creates a DirectoryMonitor for the given directory.
     *
     * @param dir Directory to monitor.
     * @param friendlyName Display friendly name for the directory monitor.
     * @param subdirs Set to true to also monitor subdirectories.
     */
    public DirectoryMonitor(File dir, String friendlyName, boolean subdirs) {
        
        setDelay(DEFAULT_DELAY);
        perDirectoryDelay_ = DEFAULT_PER_DIR_DELAY;
        dirSnapshots_ = new HashMap();
        recognizers_ = new ArrayList();
        stateMachine_ = ServiceUtil.createStateMachine(this);
        notifier_ = new ServiceNotifier(this);
        monitoredDirectories_ = new ArrayList();
        listeners_ = new ArrayList();
        setName(friendlyName);
        recurse_ = subdirs;
        rootDirectory_ = dir;
        
//        fireStatusChanged(
//            new StatusEvent(
//                StatusEvent.TYPE_START_DISCOVERY, 
//                this, 
//                "Started discovery"));
//
//        if (subdirs) {
//            // Find all subdirs of the starting dir
//            FileFinder finder = new FileFinder();
//            Map findOptions = new HashMap();
//            findOptions.put(Finder.TYPE, "d");
//            
//            logger_.debug("Finding all subdirs of " + dir + "...");
//            
//            File[] subdirectories = finder.find(dir, findOptions);
//            
//            logger_.debug("Found " + subdirectories.length + " subdirs total!");
//            logger_.debug(ArrayUtil.toString(subdirectories, true));
//            
//            for (int i = 0; i < subdirectories.length; i++) 
//                addDirectory(subdirectories[i]);
//        }
//        else {
//            addDirectory(dir);
//        }
//        
//        fireStatusChanged(
//            new StatusEvent(
//                StatusEvent.TYPE_END_DISCOVERY, 
//                this, 
//                "Ended discovery"));
        
    }
    
    //--------------------------------------------------------------------------
    // Startable Interface
    //--------------------------------------------------------------------------
    
    /**
     * Starts execution of this directory monitor.
     * 
     * @throws IllegalStateException if monitor is already running.
     */
    public void start() throws IllegalStateException {
        stateMachine_.checkTransition(ServiceTransition.START);
        
        if (monitor_ != null && monitor_.isAlive())
            throw new IllegalStateException(
                "The directory monitor for " 
                + getName()
                + "already running.");
        
        monitor_ = 
            new Thread(new DirectoryScanner(),
                "DirectoryMonitor[" 
                + getName() 
                + "]");
                        
        monitor_.start();
        stateMachine_.transition(ServiceTransition.START);
        notifier_.fireServiceStateChanged();
    }

    
    /**
     * Requests termination of this monitor. Does not block on termination
     * nor does it guarantee termination.
     * 
     * @see toolbox.util.service.Startable#stop()
     */
    public void stop() throws ServiceException {
        stateMachine_.checkTransition(ServiceTransition.STOP);
        
        try {
            logger_.debug("Stopping directory monitor..");
            stateMachine_.transition(ServiceTransition.STOP);

            // wait at most 10 secs for monitor to shutdown
            monitor_.join(1000);
            
            if (monitor_.isAlive()) {
                logger_.debug("Monitor did not die gracefully. Interrupting...");
                monitor_.interrupt();
            }
            
            logger_.debug("Waiting for monitor to die...");
            monitor_.join();
            
            logger_.debug("Monitor stopped!");
            monitor_ = null;
            
            notifier_.fireServiceStateChanged();
        }
        catch (InterruptedException e) {
            throw new ServiceException(e);
        }
    }

    
    /*
     * @see toolbox.util.service.Startable#isRunning()
     */
    public boolean isRunning() {
        return getState() == ServiceState.RUNNING;
    }
    
    // -------------------------------------------------------------------------
    // Suspendable Interface
    // -------------------------------------------------------------------------
    
    /*
     * @see toolbox.util.service.Suspendable#suspend()
     */
    public void suspend() throws IllegalStateException, ServiceException {
        stateMachine_.checkTransition(ServiceTransition.SUSPEND);
        logger_.debug("Suspending directory monitor..");
        stateMachine_.transition(ServiceTransition.SUSPEND);
        notifier_.fireServiceStateChanged();
    }
    
    /*
     * @see toolbox.util.service.Suspendable#resume()
     */
    public void resume() throws IllegalStateException, ServiceException {
        stateMachine_.checkTransition(ServiceTransition.RESUME);
        logger_.debug("Resuming directory monitor..");
        stateMachine_.transition(ServiceTransition.RESUME);
        synchronized(monitor_) {
            monitor_.notify();
        }
        notifier_.fireServiceStateChanged();
    }
    
    /*
     * @see toolbox.util.service.Suspendable#isSuspended()
     */
    public boolean isSuspended() {
        return getState() == ServiceState.SUSPENDED;
    }
    
    //--------------------------------------------------------------------------
    // Destroyable Interface
    //--------------------------------------------------------------------------
    
    /**
     * When destroyed, remove all associated resources including listeners.
     * 
     * @see toolbox.util.service.Destroyable#destroy()
     */
    public void destroy() throws IllegalStateException, ServiceException {
        
        // TODO:
        // Make the service easily support transition to destroy..refactor
        // later to make a feature of a state machine decorator to run the
        // service through a given set of states.
        
        if (getState() == ServiceState.DESTROYED) {
            logger_.warn("Service " + this + " already destroyed.");
        }
        else if (getState() == ServiceState.RUNNING) {
            stop();
            destroy();
        }
        else if (getState() == ServiceState.SUSPENDED) {
            resume();
            stop();
            destroy();
        }
        else {
            stateMachine_.checkTransition(ServiceTransition.DESTROY);
            logger_.debug("Destroying directory monitor..");
            stateMachine_.transition(ServiceTransition.DESTROY);
            listeners_.clear();
            notifier_.fireServiceStateChanged();
        }
    }
    
    /*
     * @see toolbox.util.service.Destroyable#isDestroyed()
     */
    public boolean isDestroyed() {
        return getState() == ServiceState.DESTROYED;
    }
    
    // --------------------------------------------------------------------------
    // ObservableService Interface
    // --------------------------------------------------------------------------

    /*
     * @see toolbox.util.service.ObservableService#addServiceListener(toolbox.util.service.ServiceListener)
     */
    public void addServiceListener(ServiceListener listener){
        notifier_.addServiceListener(listener);
    }


    /*
     * @see toolbox.util.service.ObservableService#removeServiceListener(toolbox.util.service.ServiceListener)
     */
    public void removeServiceListener(ServiceListener listener){
        notifier_.removeServiceListener(listener);
    }

    //--------------------------------------------------------------------------
    // Service Interface
    //--------------------------------------------------------------------------
    
    /*
     * @see toolbox.util.service.Service#getState()
     */
    public ServiceState getState() {
        return (ServiceState) stateMachine_.getState();
    }
    
    // -------------------------------------------------------------------------
    // Nameable Interface
    // -------------------------------------------------------------------------

    public String getName() {
        return name_;
    }
    
    public void setName(String name) {
        name_ = name;
    }
    
    //--------------------------------------------------------------------------
    // Public
    //--------------------------------------------------------------------------

    /**
     * @return Root directory being monitored.
     */
    public File getRootDirectory() {
        return rootDirectory_;
    }

    
    /**
     * @return List of directories being monitored.
     */
    public List getMonitoredDirectories() {
        return monitoredDirectories_;
    }

    
    /**
     * Returns the polling delay in milliseconds.
     *
     * @return int
     */
    public int getDelay() {
        return perScanDelay_;
    }

    
    /**
     * Sets the polling delay in milliseconds.
     *
     * @param newDelay Delay
     */
    public void setDelay(int newDelay) {
        perScanDelay_ = newDelay;
    }
   
    
    /**
     * Adds an activity to monitor.
     * 
     * @param r Recognizer to add.
     */
    public void addRecognizer(IFileActivityRecognizer r) {
        recognizers_.add(r);
    }

    
    /**
     * Removes an activity from the list of monitored activities.
     * 
     * @param r Recognizer to remove.
     */
    public void removeRecognizer(IFileActivityRecognizer r) {
        recognizers_.remove(r);
    }

    // -------------------------------------------------------------------------
    // Public but for internal usage only 
    // -------------------------------------------------------------------------
    
    /**
     * @param directory Additional directory to monitor. Not for public usage
     */
    public void internalAddDirectory(File directory) {
        monitoredDirectories_.add(directory);
    }
    
    
    /**
     * @param directory Additional directory to remove from monitoring. Not for
     *        public usage.
     */
    public void internalRemoveDirectory(File directory) {
        monitoredDirectories_.remove(directory);
    }

    //--------------------------------------------------------------------------
    // Event Notification Support
    //--------------------------------------------------------------------------

    /**
     * Do not use this method even though it is public. 
     * FileCreationFinishedRecognizer needs visibility to this method to fire
     * events that don't originate from {@link 
     * IFileActivityRecognizer#getRecognizedEvents(DirSnapshot, DirSnapshot)}.
     * In particular, {@link 
     * toolbox.util.dirmon.recognizer.FileCreationFinishedRecognizer}.
     */
    public void fireDirectoryActivity(FileEvent event)
        throws Exception {

        for (Iterator i = listeners_.iterator(); i.hasNext();) {
            IDirectoryMonitorListener dirListener = 
                (IDirectoryMonitorListener) i.next();
            dirListener.directoryActivity(event);
        }
    }

    
    protected void fireStatusChanged(StatusEvent event) {

        for (Iterator i = listeners_.iterator(); i.hasNext();) {
            
            try {
                IDirectoryMonitorListener dirListener = 
                    (IDirectoryMonitorListener) i.next();
                dirListener.statusChanged(event);
            }
            catch (Exception e) {
                logger_.error("Error delivering status changed event", e);
            }
        }
    }


    /**
     * Removes a listener from the list that is notified each time a file
     * becomes available.
     * 
     * @param listener Listener to remove from the notification list.
     */
    public void removeDirectoryMonitorListener(
        IDirectoryMonitorListener listener) {
        listeners_.remove(listener);
    }

    
    /**
     * Adds a listener to the list that's notified each time a new file is 
     * available.
     *
     * @param listener Listener to add to notification list.
     */
    public void addDirectoryMonitorListener(
        IDirectoryMonitorListener listener) {
        listeners_.add(listener);
    }
    
    //--------------------------------------------------------------------------
    // DirectoryScanner
    //--------------------------------------------------------------------------
    
    class DirectoryScanner implements Runnable {
        
        public void run() {
            setup();
            boolean first = true;
            Throttler throttler = new Throttler();
            
            // Check termination flag
            while (isRunning() || isSuspended()) {
                
                try {
                    if (!isSuspended()) {
                        
                        logger_.debug((first ? "First" : "Update")
                            + " scan started for "
                            + monitoredDirectories_.size() + " directories.");

                        fireStatusChanged(new StatusEvent(
                            StatusEvent.TYPE_START_SCAN, 
                            DirectoryMonitor.this,
                            "Started scan"));

                        for (Iterator di = monitoredDirectories_.iterator(); di.hasNext() && isRunning();) {
                            File dir = (File) di.next();
                            scanDirectory(dir);
                        }

                        fireStatusChanged(new StatusEvent(
                            StatusEvent.TYPE_END_SCAN, 
                            DirectoryMonitor.this,
                            "Ended scan"));
                    }
                    else {
                        logger_.debug("Skipping scan due to suspension...");
                    }

                    if (!first) {
                        synchronized (monitor_) {
                            try {
                                if (isSuspended()) {
                                    // Wait indefinitely until resumed
                                    logger_.debug("Suspending gracefully..");
                                    monitor_.wait();
                                    logger_.debug("Woken up from suspended..");
                                }
                                else {
                                    logger_.trace("Waiting " + getDelay()
                                        + "ms until next scan...");

                                    // Wait until delay expires
                                    monitor_.wait(getDelay());
                                    logger_
                                        .debug("Woken up from suspended or delay expired");
                                }
                            }
                            catch (InterruptedException e) {
                                logger_.debug("Monitor thread interrupted!");
                            }
                        }
                    }
                    else {
                        first = false;
                    }
                }
                catch (Exception e) {
                    logger_.error("Error in directory scanning loop", e);
                    throttler.poke();
                }
            }
            
            logger_.debug("Exiting thread run()");
        }

        
        private void setup() {
            
            fireStatusChanged(
                new StatusEvent(
                    StatusEvent.TYPE_START_DISCOVERY, 
                    DirectoryMonitor.this, 
                    "Started discovery"));

            File baseDir = getRootDirectory();
            
            if (recurse_) {
                // Find all subdirs of the starting dir
                FileFinder finder = new FileFinder();
                
                Map findOptions = new HashMap();
                
                // only scan for non hidden directories
                findOptions.put(Finder.TYPE, "d");
                findOptions.put(Finder.HIDDEN, "false");
                
                logger_.debug("Finding all subdirs of " + baseDir + "...");
                
                finder.addFindListener(new FindListener() {
                    
                    public void directoryFinished(FindEvent findEvent) {
                    }
                    
                    public void directoryStarted(FindEvent findEvent) {
                        String dir = findEvent.getDirectory().getAbsolutePath();
                        if (dir != null)
                            Thread.currentThread().setName(dir);
                    }
                    
                    public void fileFound(FindEvent findEvent) {
                    }
                });
                    
                File[] subdirectories = finder.find(baseDir, findOptions);
                logger_.debug("Found " + subdirectories.length + " subdirs total!");
                logger_.debug(ArrayUtil.toString(subdirectories, true));
                for (int i = 0; i < subdirectories.length; i++) 
                    internalAddDirectory(subdirectories[i]);
            }
            else {
                internalAddDirectory(baseDir);
            }
            
            fireStatusChanged(
                new StatusEvent(
                    StatusEvent.TYPE_END_DISCOVERY, 
                    DirectoryMonitor.this, 
                    "Ended discovery"));

            if (logger_.isDebugEnabled())
                for (Iterator i = recognizers_.iterator(); i.hasNext();)
                    logger_.debug("Recognizer registered: " + i.next());
        }

        private void scanDirectory(File dir){
            String dirKey = dir.getAbsolutePath();
            DirSnapshot beforeDirSnapshot = (DirSnapshot) dirSnapshots_.get(dirKey);

            if (beforeDirSnapshot == null) {
                dirSnapshots_.put(dirKey, new DirSnapshot(dir));
            }
            else {
                DirSnapshot afterDirSnapshot = new DirSnapshot(dir);

                for (Iterator i = recognizers_.iterator(); i.hasNext() && isRunning();) {
                    IFileActivityRecognizer recognizer = (IFileActivityRecognizer) i.next();
                    List recognizedEvents = recognizer.getRecognizedEvents(beforeDirSnapshot, afterDirSnapshot);

                    for (Iterator r = recognizedEvents.iterator(); r.hasNext();) {

                        try {
                            FileEvent event = (FileEvent) r.next();
                            fireDirectoryActivity(event);
                        }
                        catch (Exception e) {
                            logger_.error("ActivityRunner.run", e);
                        }
                    }
                    ThreadUtil.sleep(perDirectoryDelay_);
                }
                // Update the snapshot to the latest
                dirSnapshots_.put(dirKey, afterDirSnapshot);
            }
        }
    }
    
    //--------------------------------------------------------------------------
    // Throttler
    //--------------------------------------------------------------------------
    
    class Throttler {

        BoundedFifoBuffer events = new BoundedFifoBuffer(10);
        
        public void poke() {
            Date d = new Date();
            events.add(d);
            if (shouldThrottle())
                throttle();
        }
        
        public boolean shouldThrottle() {
            Date now = new Date();
            int counter = 0;
            
            // if there have been more than 10 pokes in the last minute then throttle
            for (Iterator i = events.iterator(); i.hasNext(); ) {
                Date poke = (Date) i.next();
                
                ElapsedTime range = new ElapsedTime(poke, now);
                
                if (range.getTotalMillis() < 60000)
                    counter++;
                
                if (counter == 10) {
                    events.clear();
                    return true;
                }
            }
            
            return false;
        }
        
        public void throttle() {
            logger_.debug("Throttling for 60 secs...");
            ThreadUtil.sleep(60000);
        }
    }
}