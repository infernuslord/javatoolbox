package toolbox.util.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;

import toolbox.util.ArrayUtil;
import toolbox.util.statemachine.StateMachine;
import toolbox.util.statemachine.StateMachineFactory;

/**
 * Service utility class. 
 */
public class ServiceUtil
{
    //--------------------------------------------------------------------------
    // Constructors
    //--------------------------------------------------------------------------
    
    /**
     * Prevent construction of this static singleton.
     */
    private ServiceUtil()
    {
    }

    //--------------------------------------------------------------------------
    // Public
    //--------------------------------------------------------------------------
    
    /**
     * Creates a state machine for the given list of Service classes.
     * 
     * @param name Name to assign to the StateMachine.
     * @return StateMachine
     */
    public static StateMachine createStateMachine(Class[] serviceClasses)
    {
        StateMachine machine = 
            StateMachineFactory.createStateMachine("ServiceStateMachine");
        
        Set natures = new HashSet();
        CollectionUtils.addAll(natures, serviceClasses);
    
        Set initDestroy = new HashSet(
            Arrays.asList(new Class[]{Initializable.class, Destroyable.class}));
    
        // Initializable/Destroyable -------------------------------------------
        
        if (CollectionUtils.isEqualCollection(natures, initDestroy))
        {
            // uninit    --> init
            // init      --> destroyed
            // destroyed --> init
            
            machine.addState(ServiceState.UNINITIALIZED);
            machine.addState(ServiceState.INITIALIZED);
            machine.addState(ServiceState.DESTROYED);
            
            machine.setBeginState(ServiceState.UNINITIALIZED);
            
            machine.addTransition(
                ServiceTransition.INITIALIZE, 
                ServiceState.UNINITIALIZED, 
                ServiceState.INITIALIZED);
            
            machine.addTransition(
                ServiceTransition.DESTROY, 
                ServiceState.INITIALIZED,
                ServiceState.DESTROYED);
                
            machine.addTransition(
                ServiceTransition.INITIALIZE, 
                ServiceState.DESTROYED, 
                ServiceState.INITIALIZED);
            
            machine.reset();
            return machine;
        }            
        
        
        if (ArrayUtil.contains(serviceClasses, Startable.class))
        {
            machine.addState(ServiceState.RUNNING);
            machine.addState(ServiceState.STOPPED);
            machine.setBeginState(ServiceState.STOPPED);
            
            machine.addTransition(
                ServiceTransition.START, 
                ServiceState.STOPPED, 
                ServiceState.RUNNING);
            
            machine.addTransition(
                ServiceTransition.STOP, 
                ServiceState.RUNNING,
                ServiceState.STOPPED);
        }
        
        if (ArrayUtil.contains(serviceClasses,Suspendable.class))
        {
            machine.addState(ServiceState.SUSPENDED);
            
            machine.addTransition(
                ServiceTransition.SUSPEND, 
                ServiceState.RUNNING, 
                ServiceState.SUSPENDED);
            
            machine.addTransition(
                ServiceTransition.RESUME, 
                ServiceState.SUSPENDED,
                ServiceState.RUNNING);
        }
        
        if (ArrayUtil.contains(serviceClasses, Initializable.class))
        {
            machine.addState(ServiceState.UNINITIALIZED);
            machine.addState(ServiceState.INITIALIZED);
            machine.setBeginState(ServiceState.UNINITIALIZED);
            
            machine.addTransition(
                ServiceTransition.INITIALIZE, 
                ServiceState.UNINITIALIZED, 
                ServiceState.INITIALIZED);
            
            machine.addTransition(
                ServiceTransition.START, 
                ServiceState.INITIALIZED,
                ServiceState.RUNNING);
        }
        
        if (ArrayUtil.contains(serviceClasses, Destroyable.class))
        {
            machine.addState(ServiceState.DESTROYED);
            
            machine.addTransition(
                ServiceTransition.DESTROY, 
                ServiceState.STOPPED, 
                ServiceState.DESTROYED);
            
            if ((ArrayUtil.contains(serviceClasses, Initializable.class)))
            {
                machine.addTransition(
                    ServiceTransition.DESTROY, 
                    ServiceState.INITIALIZED, 
                    ServiceState.DESTROYED);
            }
        }
        
        machine.reset();
        return machine;
    }

    /**
     * Creates a state machine given the implemtations of the given service.
     *  
     * @param service Service implementing Initializable, Startable, 
     *        Suspendable, and Destroyable.
     * @return StateMachine
     */
    public static StateMachine createStateMachine(Service service)
    {
        List natures = new ArrayList();
        
        if (service instanceof Startable)
            natures.add(Startable.class);
    
        if (service instanceof Suspendable)
            natures.add(Suspendable.class);
    
        if (service instanceof Initializable)
            natures.add(Initializable.class);
    
        if (service instanceof Destroyable)
            natures.add(Destroyable.class);
    
        //logger_.debug("Natures: " + ArrayUtil.toString(natures.toArray()));
        return createStateMachine((Class[]) natures.toArray(new Class[0]));
    }

}
