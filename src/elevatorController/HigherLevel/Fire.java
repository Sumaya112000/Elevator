package elevatorController.HigherLevel;

import elevatorController.LowerLevel.*;
import elevatorController.Util.FloorNDirection;
import elevatorController.Util.State;
import static elevatorController.Util.ConstantsElevatorControl.*;

public class Fire {
    private Mode mode;
    private Buttons buttons;
    private Cabin cabin;
    private DoorAssembly doorAssembly;
    private Notifier notifier;
    private static final int RECALL_FLOOR = FLOOR_ONE;
    private boolean recalled;

    public Fire(Mode mode, Buttons buttons, Cabin cabin,
                DoorAssembly doorAssembly, Notifier notifier) {
        this.mode = mode;
        this.buttons = buttons;
        this.cabin = cabin;
        this.doorAssembly = doorAssembly;
        this.notifier = notifier;
        this.recalled = false;
    }

    public State fire(){
        State newMode = mode.getMode();
        if (newMode != State.FIRE && newMode != State.NULL && newMode != State.CONTROL) {
            this.recalled = false;
            return newMode;
        }

        buttons.disableCalls();
        buttons.enableSingleRequest();

        // Forced Recall to the main floor
        if (!recalled && !cabin.arrived()) {
            if (!doorAssembly.fullyClosed()) {
                doorAssembly.close();
            } else {
                cabin.gotoFloor(RECALL_FLOOR);
                if (cabin.currentStatus().floor() == RECALL_FLOOR) {
                    recalled = true;
                }
            }
        }

        // Management at the Recall Floor
        if (cabin.currentStatus().floor() == RECALL_FLOOR && recalled) {
            if (cabin.arrived()) {
                if (!doorAssembly.fullyOpen()) {
                    doorAssembly.open();
                    notifier.arrivedAtFloor(cabin.currentStatus());
                }
            }
        } else {
            recalled = false;
        }

        // Emergency Cabin Service (if in CONTROL mode via Fire Key)
        if (newMode == State.CONTROL && doorAssembly.fullyClosed()) {
            FloorNDirection nextDest = buttons.nextService(cabin.currentStatus());

            if (nextDest != null) {
                cabin.gotoFloor(nextDest.floor());
                buttons.requestReset(nextDest.floor());
            }
        }

        notifier.elevatorStatus(cabin.currentStatus());

        return State.FIRE;
    }
}