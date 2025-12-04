package elevatorController.HigherLevel;

import elevatorController.LowerLevel.*;
import elevatorController.Util.FloorNDirection;
import elevatorController.Util.State;

public class Normal {
    private Mode mode;
    private Buttons buttons;
    private Cabin cabin;
    private DoorAssembly doorAssembly;
    private Notifier notifier;
    private boolean doorManagementActive;

    public Normal(Mode mode, Buttons buttons, Cabin cabin,
                  DoorAssembly doorAssembly, Notifier notifier) {
        this.mode = mode;
        this.buttons = buttons;
        this.cabin = cabin;
        this.doorAssembly = doorAssembly;
        this.notifier = notifier;
        this.doorManagementActive = false;
    }

    public State normal(){
        // Check for mode change
        State newMode = mode.getMode();
        if (newMode != State.NORMAL && newMode != State.NULL) {
            if (doorAssembly.fullyOpen()) doorAssembly.close();
            return newMode;
        }

        buttons.enableCalls();
        buttons.enableAllRequests();

        // Door Management
        if (cabin.arrived()) {
            FloorNDirection currentStatus = cabin.currentStatus();

            if (!doorAssembly.fullyOpen() && !doorManagementActive) {
                doorAssembly.open();
                notifier.arrivedAtFloor(currentStatus);
                doorManagementActive = true;

                // Clear the button light upon arrival/door opening
                buttons.callReset(currentStatus.floor());
            } else if (doorAssembly.fullyOpen()) {
                if (doorAssembly.overCapacity()) {
                    notifier.playCapacityNoise();
                } else {
                    notifier.stopCapacityNoise();
                }

                if (doorAssembly.obstructed()) {
                    // Doors obstructed, wait
                } else {
                    if (buttons.nextService(currentStatus) == null) {
                        doorAssembly.close();
                        doorManagementActive = false;
                    }
                }
            }
        }

        // Movement Logic (only if doors are fully closed or in motion)
        // Do NOT move if overload button is pressed
        if (doorAssembly.fullyClosed() && !doorAssembly.overCapacity()) {
            FloorNDirection nextDest = buttons.nextService(cabin.currentStatus());

            if (nextDest != null) {
                cabin.gotoFloor(nextDest.floor());
            }
        }

        notifier.elevatorStatus(cabin.currentStatus());

        return State.NORMAL;
    }
}