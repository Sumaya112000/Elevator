package elevatorController.LowerLevel;

import Bus.*;
import Message.*;
import elevatorController.Util.FloorNDirection;
import elevatorController.Util.State;

public class Mode {
    private int elevatorID;
    private SoftwareBus softwareBus;
    private State currentMode;
    private FloorNDirection currDestination;

    public Mode(int elevatorID, SoftwareBus softwareBus) {
        this.softwareBus = softwareBus;
        this.elevatorID = elevatorID;
        this.currDestination = null;
        this.currentMode = State.NORMAL;

        softwareBus.subscribe(SoftwareBusCodes.setMode, 0);

        System.out.println("Mode " + elevatorID + " initialized in NORMAL mode");
    }

    public State getMode(){
        setCurrentMode();
        return currentMode;
    }

    private void setCurrentMode(){
        Message modeMsg;
        while ((modeMsg = softwareBus.get(SoftwareBusCodes.setMode, 0)) != null) {
            int body = modeMsg.getBody();
            if (body == SoftwareBusCodes.centralized || body == SoftwareBusCodes.independent) {
                currentMode = State.NORMAL;
                System.out.println("Elevator " + elevatorID + " mode changed to NORMAL");
            } else if (body == SoftwareBusCodes.fire) {
                currentMode = State.FIRE;
                System.out.println("Elevator " + elevatorID + " mode changed to FIRE");
            }
        }
    }

    public FloorNDirection nextService(){
        Message dispatchMsg = softwareBus.get(SoftwareBusCodes.carDispatch, elevatorID);
        if (dispatchMsg != null) {
            int body = dispatchMsg.getBody();
        }
        return null;
    }
}