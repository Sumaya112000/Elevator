package elevatorController.HigherLevel;

import elevatorController.LowerLevel.*;
import elevatorController.Util.State;

public class Control {
    private Mode mode;
    private Buttons buttons;
    private Cabin cabin;
    private DoorAssembly doorAssembly;
    private Notifier notifier;

    public Control(Mode mode, Buttons buttons, Cabin cabin,
                   DoorAssembly doorAssembly, Notifier notifier) {
        this.mode = mode;
        this.buttons = buttons;
        this.cabin = cabin;
        this.doorAssembly = doorAssembly;
        this.notifier = notifier;
    }

    public State control(){
        System.out.println("Entering CONTROL mode");

        while (true) {
            State newMode = mode.getMode();
            if (newMode != State.CONTROL) {
                System.out.println("Exiting CONTROL mode to: " + newMode);
                return newMode;
            }

            try { Thread.sleep(100); }
            catch (InterruptedException e) { break; }
        }

        return State.NORMAL;
    }
}