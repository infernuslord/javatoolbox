package toolbox.util.service;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.swing.Action;
import javax.swing.JPanel;

import org.apache.commons.collections.MapUtils;
import org.apache.log4j.Logger;

import toolbox.util.statemachine.StateMachine;
import toolbox.util.ui.JSmartButton;
import toolbox.util.ui.JSmartLabel;
import toolbox.util.ui.SmartAction;

/**
 * ServiceView is a user interface component that presents a view on an object
 * that implements the Service interface. Features include:
 * <ul>
 *  <li>Initialize, start, stop, suspend, resume, and destroy a Service
 *  <li>Displays the current state of the Service.
 * </ul>
 */
public class ServiceView2 extends JPanel
{
    private static final Logger logger_ = Logger.getLogger(ServiceView2.class);

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------

    /**
     * Service thats services as the model for this view.
     */
    private ObservableService service_;

    /**
     * Maps an action's name to its corresponding action.
     */
    private Map actions_;

    /**
     * Internal listener for this service.
     */
    private ServiceListener myServiceListener_;

    /**
     * Label that displays the current state of serivce associated with this 
     * view. 
     */
    private JSmartLabel currentStateLabel_;

    private StateMachine machine_;
    
    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Creates a ServiceView.
     * 
     * @param service Service that acts as the model for this view.
     */
    public ServiceView2(ObservableService service, StateMachine machine) {
        machine_ = machine;
        myServiceListener_ = new MyServiceListener();
        currentStateLabel_ = new JSmartLabel();
        setService(service);
        actions_ = new HashMap(4);
        buildView();
        enforce();
    }


    // -------------------------------------------------------------------------
    // Public
    // -------------------------------------------------------------------------

    /**
     * Returns the service.
     * 
     * @return Service
     */
    public ObservableService getService(){
        return service_;
    }


    /**
     * Sets the value of service.
     * 
     * @param service The service to set.
     */
    public void setService(ObservableService service){
        // Remove the old one if one exists
        if (service_ != null)
            service_.removeServiceListener(myServiceListener_);

        service_ = service;

        // Transfer the listener to the newly appointed service.
        service_.addServiceListener(myServiceListener_);
        
        currentStateLabel_.setText(service_.getState().toString());
    }


    // -------------------------------------------------------------------------
    // Protected
    // -------------------------------------------------------------------------

    /**
     * Builds the GUI.
     */
    protected void buildView(){
        
        setLayout(new FlowLayout());

        if (service_ instanceof Initializable) {
            actions_.put(ServiceTransition.INITIALIZE, new InitializeAction());

            add(new JSmartButton(
                (Action) actions_.get(ServiceTransition.INITIALIZE)));
        }

        if (service_ instanceof Startable) {
            actions_.put(ServiceTransition.START, new StartAction());
            actions_.put(ServiceTransition.STOP, new StopAction());

            add(new JSmartButton((Action) 
                actions_.get(ServiceTransition.START)));
            
            add(new JSmartButton((Action) 
                actions_.get(ServiceTransition.STOP)));
        }

        if (service_ instanceof Suspendable) {
            actions_.put(ServiceTransition.SUSPEND, new SuspendAction());
            actions_.put(ServiceTransition.RESUME, new ResumeAction());

            add(new JSmartButton((Action) 
                actions_.get(ServiceTransition.SUSPEND)));

            add(new JSmartButton((Action) 
                actions_.get(ServiceTransition.RESUME)));
        }

        if (service_ instanceof Destroyable) {
            actions_.put(ServiceTransition.DESTROY, new DestroyAction());

            add(new JSmartButton((Action) 
                actions_.get(ServiceTransition.DESTROY)));
        }
        
        add(currentStateLabel_);
    }

    
    public void enforce() {
        
        Set transitions = actions_.keySet();
        
        for (Iterator i = transitions.iterator(); i.hasNext(); ) {
            ServiceTransition t = (ServiceTransition) i.next();
            Action action = (Action) actions_.get(t);
            action.setEnabled(machine_.canTransition(t));
        }
    }
    
    // --------------------------------------------------------------------------
    // MyServicelistener
    // --------------------------------------------------------------------------

    /**
     * The internal listener is only interested in the INITIALIZED state so that
     * the initial state of the buttons can be set.
     */
    class MyServiceListener implements ServiceListener{
        
        /*
         * @see toolbox.util.service.ServiceListener#serviceStateChanged(toolbox.util.service.Service)
         */
        public void serviceStateChanged(Service service) 
            throws ServiceException {

            logger_.debug("Service state changed to " + service.getState());
            
            currentStateLabel_.setText(service.getState().toString());
            
            enforce();
            
//            ServiceState current = service.getState();
//
//            logger_.debug("New state = " + service.getState());
//
//            if (service instanceof ObservableService) {
//                
//                ObservableService obs = (ObservableService) service;
//                StateMachine sm = obs.getStateMachine();
//                List transitions = sm.getTransitionsFrom(current);
//
//                for (Iterator iter = actions_.entrySet().iterator(); 
//                    iter.hasNext();) {
//                    
//                    Map.Entry entry = (Map.Entry) iter.next();
//                    ServiceTransition tran = (ServiceTransition) entry.getKey();
//                    Action action = (Action) entry.getValue();
//                    action.setEnabled(transitions.contains(tran));
//                }
//                
//                currentStateLabel_.setText(sm.getState().toString());
//            }
//            else {
//                
//            }
        }
    }

    // -------------------------------------------------------------------------
    // InitializeAction
    // -------------------------------------------------------------------------

    /**
     * Initializes the service.
     */
    class InitializeAction extends SmartAction{
        
        /**
         * Creates an InitializeAction.
         */
        public InitializeAction(){
            super("Init", true, false, null);
        }


        /*
         * @see toolbox.util.ui.SmartAction#runAction(java.awt.event.ActionEvent)
         */
        public void runAction(ActionEvent e) throws Exception{
            ((Initializable) service_).initialize(MapUtils.EMPTY_MAP);
        }
    }

    // -------------------------------------------------------------------------
    // StartAction
    // -------------------------------------------------------------------------

    /**
     * StartAction starts the attached service and sets the state of the buttons
     * according to the next available set of state transitions.
     */
    class StartAction extends SmartAction{
        
        /**
         * Creates a StartAction.
         */
        public StartAction(){
            super("Start", true, false, null);
        }


        /*
         * @see toolbox.util.ui.SmartAction#runAction(java.awt.event.ActionEvent)
         */
        public void runAction(ActionEvent e) throws Exception{
            ((Startable) service_).start();
        }
    }

    // -------------------------------------------------------------------------
    // StopAction
    // -------------------------------------------------------------------------

    /**
     * StopAction stops the attached service and sets the state of the buttons
     * according to the next available set of state transitions.
     */
    class StopAction extends SmartAction{
        
        /**
         * Creates a StopAction.
         */
        public StopAction(){
            super("Stop", true, false, null);
        }


        /*
         * @see toolbox.util.ui.SmartAction#runAction(java.awt.event.ActionEvent)
         */
        public void runAction(ActionEvent e) throws Exception{
            ((Startable) service_).stop();
        }
    }

    // -------------------------------------------------------------------------
    // SuspendAction
    // -------------------------------------------------------------------------

    /**
     * SuspendAction suspends the attached service and sets the state of the
     * buttons according to the next available set of state transitions.
     */
    class SuspendAction extends SmartAction{
        
        /**
         * Creates a SuspendAction.
         */
        public SuspendAction(){
            super("Suspend", true, false, null);
        }


        /*
         * @see toolbox.util.ui.SmartAction#runAction(java.awt.event.ActionEvent)
         */
        public void runAction(ActionEvent e) throws Exception{
            ((Suspendable) service_).suspend();
        }
    }

    // -------------------------------------------------------------------------
    // ResumeAction
    // -------------------------------------------------------------------------

    /**
     * ResumeAction
     */
    class ResumeAction extends SmartAction{
        
        /**
         * Creates a ResumeAction.
         */
        public ResumeAction() {
            super("Resume", true, false, null);
        }


        /*
         * @see toolbox.util.ui.SmartAction#runAction(java.awt.event.ActionEvent)
         */
        public void runAction(ActionEvent e) throws Exception{
            ((Suspendable) service_).resume();
        }
    }

    // -------------------------------------------------------------------------
    // DestroyAction
    // -------------------------------------------------------------------------

    /**
     * Destroys the service.
     */
    class DestroyAction extends SmartAction{
        
        /**
         * Creates an DestroyAction.
         */
        public DestroyAction(){
            super("Destroy", true, false, null);
        }


        /*
         * @see toolbox.util.ui.SmartAction#runAction(java.awt.event.ActionEvent)
         */
        public void runAction(ActionEvent e) throws Exception{
            ((Destroyable) service_).destroy();
        }
    }
}