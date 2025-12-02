package elevatorController.LowerLevel;

import bus.Bus.SoftwareBus;
import elevatorController.Util.FloorNDirection;
import elevatorController.Util.State;

/**
 * The mode serves as a means for the Elevator Controller to be put into and track its current mode.
 * The mode is indirectly being updated by the Control Room, a separate entity outside of the Elevator Controller system.
 * Additionally, the mode is responsible for taking in demands from the Control Room when the elevator is being remotely controlled.
 * The mode object receives messages via the software bus but does not post messages to the software bus.
 *
 * The modes:
 *         1 – NORMAL
 *         2 – FIRE_SAFETY
 *         3 - CONTROLLED
 */
public class Mode {
    private int elevatorID;
    private SoftwareBus softwareBus;
    private State currentMode;
    private FloorNDirection currDestination;

    /**
     * Instantiate a Mode object
     * @param elevatorID which elevator this Mode object is associated with
     *                   (for software bus messages)
     * @param softwareBus the means of communication
     */
    public Mode(int elevatorID, SoftwareBus softwareBus) {
        //TODO call subscribe on softwareBus w/ relevant topic/subtopic
        this.softwareBus = softwareBus;
        this.elevatorID = elevatorID;

        this.currDestination = null;

        // Initially in Normal mode
        this.currentMode = State.NORMAL;
    }

    /**
     * Call get() on softwareBus w/ appropriate topic/subtopic, until NULL is returned (only care about most recent mode
     * set), store last valid mode in currentMode, return currentMode
     * @return the currentMode this elevator is in
     */
    public State getMode(){
        setCurrentMode();
        return currentMode;
    }

    /**
     * Pulls all related messages from softwareBUs until null and
     * sets current mode equal to the last relevant message
     */
    private void setCurrentMode(){
        //Todo: Set current mode from software bus
    }

    /**
     * Call get() on softwareBus w/ appropriate topic/subtopic,
     * @return
     */
    public FloorNDirection nextService(){return null;}

}