package elevatorController.LowerLevel;

import bus.Bus.SoftwareBus;

/**
 * The door assembly is a virtualization of the physical interfaces which
 * comprise the doors: fully open sensors, fully closed sensors, door
 * obstruction sensors, the scale, and the door motor. The door assembly posts
 * and receives messages from its physical counterparts via the software bus;
 * posting to the motor; receiving from the fully closed sensors, fully open
 * sensors, the scale, and the door obstruction sensors.
 */
public class DoorAssembly implements Runnable {
    private boolean opened;  // TODO: do we need to store this in Door Assembly?
    private boolean closed;
    private boolean obstructed;
    private boolean fullyClosed;
    private boolean fullyOpen;
    private boolean overCapacity;
    private int elevatorID;
    private SoftwareBus softwareBus;

    /**
     * Instantiate a DoorAssembly object, and run its thread
     * @param elevatorID For software bus messages
     * @param softwareBus The means of communication
     */
    public DoorAssembly(int elevatorID, SoftwareBus  softwareBus) {
        //TODO may need to take in int for elevator number for software bus subscription
        //TODO call subscribe on softwareBus w/ relevant topic/subtopic

        this.opened = true;
        this.closed = false;
        this.obstructed = false;
        this.fullyClosed = false;
        this.fullyOpen = true;
        this.overCapacity = false;
        this.softwareBus = softwareBus;
        this.elevatorID = this.elevatorID;

        //Start DoorAssembly Thread
        Thread thread = new Thread(this);
        thread.start();
    }

    //Todo: Write these methods
    /**
     * Send message to softwareBus to open the doors (which sends the message
     * to the MUX)
     */
    public void open(){}

    /**
     * Send message to softwareBus to close the doors (which sends the message
     * to the MUX)
     */
    public void close(){}

    /**
     * @return true if obstruction sensor triggered, false otherwise
     */
    public boolean obstructed(){return obstructed;}

    /**
     * @return true if fully closed sensor triggered, false otherwise
     */
    public boolean fullyClosed(){return fullyClosed;}

    /**
     * @return true if fully open sensor triggered, false otherwise
     */
    public boolean fullyOpen(){return fullyOpen;}

    /**
     * @return true if an over capacity message was received, false if an under
     *         capacity message was received, true initially
     */
    public boolean overCapacity(){return overCapacity;}

    /**
     * Runs this operation.
     * query SoftwareBus and set variables appropriately
     */
    @Override
    public void run() {

    }
}