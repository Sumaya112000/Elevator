package elevatorController.HigherLevel;

import elevatorController.LowerLevel.*;
import elevatorController.Util.State;

/**
 * Control mode is one where movement is controlled by the Control Room. Using
 * the Software Bus. The Control Room can give commands to the elevators and
 * assumes full control over the system.
 */
public class Control {
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
    public Control(Mode mode, Buttons buttons, Cabin cabin,
                   DoorAssembly doorAssembly, Notifier notifier) {
        this.mode = mode;
        this.buttons = buttons;
        this.cabin = cabin;
        this.doorAssembly = doorAssembly;
        this.notifier = notifier;
    }

    /**
     * Control mode implementation
     * @return The state to switch too (normal or fire)
     */
    public State control(){
        return null;
    }
}