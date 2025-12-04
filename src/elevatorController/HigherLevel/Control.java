package elevatorController.HigherLevel;

import elevatorController.LowerLevel.*;
import elevatorController.Util.FloorNDirection;
import elevatorController.Util.State;
import static elevatorController.Util.ConstantsElevatorControl.*;

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
        State newMode = mode.getMode();
        if (newMode != State.CONTROL && newMode != State.NULL) {
            return newMode;
        }

        buttons.disableCalls();
        buttons.enableSingleRequest();

        FloorNDirection controlDest = mode.nextService();

        if (cabin.arrived()) {
            if (!doorAssembly.fullyOpen()) {
                doorAssembly.open();
                notifier.arrivedAtFloor(cabin.currentStatus());
            }
        } else {
            if (!doorAssembly.fullyClosed()) {
                doorAssembly.close();
            } else if (controlDest != null) {
                cabin.gotoFloor(controlDest.floor());
            }
        }

        notifier.elevatorStatus(cabin.currentStatus());

        return State.CONTROL;
    }
}