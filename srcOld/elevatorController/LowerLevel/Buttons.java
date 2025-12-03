package elevatorController.LowerLevel;

import Bus.*;
import elevatorController.Util.FloorNDirection;

import java.util.ArrayList;
import java.util.List;

/**
 * The buttons object enables the Elevator Controller to track and schedule its destinations. The buttons object
 * indirectly receives floor requests via the physical buttons on the panel inside the cabin, as well as the call
 * buttons on each level. These button events are being received via the software bus.
 * The buttons object does not post any messages to the Software Bus.
 */
public class Buttons {
    private boolean callEnabled;
    private boolean multipleRequests;
    private int elevatorID;
    private List<FloorNDirection> destinations;
    private SoftwareBus softwareBus;

    /**
     * Instantiate a Buttons Object
     * @param elevatorID the elevator number associated with this Buttons Object
     *                   (for software bus messages)
     * @param softwareBus the means of communication
     */
    public Buttons(int elevatorID, SoftwareBus softwareBus) {
        //TODO call subscribe on softwareBus w/ relevant topic/subtopic

        // Assuming normal mode settings initially
        this.callEnabled = true;
        this.multipleRequests = true;

        this.destinations = new ArrayList<>();
        this.softwareBus = softwareBus;
        this.elevatorID = elevatorID;
    }

    /**
     * Call publish on the softwareBus with a message that the call button of the given floor, and given direction can be
     * turned off
     * Remove that floor from destinations
     * @param floorNDirection The call button and direction which is no longer relevant
     */
    public void callReset(FloorNDirection floorNDirection) {}

    /**
     * Call publish on softwareBus with a message that the call button on the given floor, and given direction can be
     * turned off
     * Remove that floor from destinations
     * @param floor the floor request button that is no longer relevant
     */
    public void requestReset(int floor) {}

    /**
     * In normal mode, level call buttons are enabled
     */
    public void enableCalls(){
        this.callEnabled = true;
    }

    /**
     * In fire mode, and controlled mode call buttons are disabled
     */
    public void disableCalls(){
        this.callEnabled = false;
    }

    /**
     * In Normal mode, all request buttons are enabled
     */
    public void enableAllRequests(){
        this.multipleRequests = true;
    }

    /**
     * In Fire mode, the request buttons in the cabin are mutually exclusive
     */
    public void enableSingleRequest(){
        this.multipleRequests = false;
    }


    /*
     * Note; call events have associated directions, and request events do not
     *
     * If multipleRequests enabled, keep calling get() on software bus, add them to associated lists, use given
     * floorNDirection to determine which is best.
     *
     * If multipleRequests are disabled, keep calling get() on software bus, ignore all but the most recent.
     *
     * If calls are disabled, ignore all call button presses (those associated with level)
     *
     * @param floorNDirection record holding the floor and direction
     * @return next service direction and floor (direction non-null for call buttons, null for requests)
     */
    public FloorNDirection nextService(FloorNDirection floorNDirection) {return null;}
}