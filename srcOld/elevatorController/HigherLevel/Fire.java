package elevatorController.HigherLevel;

import elevatorController.LowerLevel.*;
import elevatorController.Util.State;

/**
 * In fire mode, the elevator only listens to request buttons in the cabin if
 * the fire key has been inserted. Only one service button can be lit up at a
 * time. If two buttons are pressed, the most recently pressed button is the
 * only service request.
 */
public class Fire {
    private Mode mode;
    private Buttons buttons;
    private Cabin cabin;
    private DoorAssembly doorAssembly;
    private Notifier notifier;

    /**
     * Create an instance of the Fire Object/Procedure
     * @param mode the mode lower level object
     * @param buttons the buttons lower level object
     * @param cabin the cabin lower level object
     * @param doorAssembly the door assembly lower level object
     * @param notifier the notifier lower level object
     */
    public Fire(Mode mode, Buttons buttons, Cabin cabin,
                DoorAssembly doorAssembly, Notifier notifier) {
        this.mode = mode;
        this.buttons = buttons;
        this.cabin = cabin;
        this.doorAssembly = doorAssembly;
        this.notifier = notifier;
    }

    /**
     * Fire mode implementation
     * @return The state to switch too (normal or control)
     */
    public State fire(){
        return null;
    }
}