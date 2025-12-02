package elevatorController.HigherLevel;

import elevatorController.LowerLevel.*;
import elevatorController.Util.State;

/**
 * Normal mode is the default mode that the system starts in. The initial state
 * of each elevator is on the first floor with the doors open.  In this mode,
 * no messages from the supervisor are expected, other than a change in mode;
 * the movement is determined solely by button presses. This mode provides the
 * typical elevator functionality: handling requests and navigating floors.
 */
public class Normal {
    private Mode mode;
    private Buttons buttons;
    private Cabin cabin;
    private DoorAssembly doorAssembly;
    private Notifier notifier;

    /**
     * Create an instance of the Normal Object/Procedure
     * @param mode the mode lower level object
     * @param buttons the buttons lower level object
     * @param cabin the cabin lower level object
     * @param doorAssembly the door assembly lower level object
     * @param notifier the notifier lower level object
     */
    public Normal(Mode mode, Buttons buttons, Cabin cabin,
                  DoorAssembly doorAssembly, Notifier notifier) {
        this.mode = mode;
        this.buttons = buttons;
        this.cabin = cabin;
        this.doorAssembly = doorAssembly;
        this.notifier = notifier;
    }

    /**
     * Normal mode implementation
     * @return The state to switch too (fire or control)
     */
    public State normal(){
        return null;
    }
}