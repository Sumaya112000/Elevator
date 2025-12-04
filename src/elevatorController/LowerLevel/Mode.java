package elevatorController.LowerLevel;

import Bus.*;
import Message.Message;
import elevatorController.Util.FloorNDirection;
import elevatorController.Util.State;
import static elevatorController.Util.ConstantsElevatorControl.*;

public class Mode {
    private int elevatorID;
    private SoftwareBus softwareBus;
    private State currentMode;
    private FloorNDirection currDestination;

    public Mode(int elevatorID, SoftwareBus softwareBus) {
        this.softwareBus = softwareBus;
        this.elevatorID = elevatorID;

        // Subscribing to mode topics (2-argument signature)
        softwareBus.subscribe(this.elevatorID, MODE);
        softwareBus.subscribe(this.elevatorID, clearFire); // Separate topic for clear fire signal
        softwareBus.subscribe(this.elevatorID, CABIN); // For CONTROL commands
        softwareBus.subscribe(this.elevatorID, FIREKEY); // For Fire Key status

        this.currDestination = null;
        this.currentMode = State.NORMAL;
    }

    public State getMode(){
        setCurrentMode();
        return currentMode;
    }

    private void setCurrentMode(){
        Message m;
        State lastValidMode = currentMode;

        // Use get(int recipientID, int topic) and m.getBody()
        while ((m = softwareBus.get(this.elevatorID, MODE)) != null) {

            if (m.getBody() == FIRE) {
                lastValidMode = State.FIRE;
            } else if (m.getBody() == NORMAL) {
                lastValidMode = State.NORMAL;
            } else if (m.getBody() == CONTROLL) {
                lastValidMode = State.CONTROL;
            }
        }

        // Handle clearFire signal (separate topic)
        while ((m = softwareBus.get(this.elevatorID, clearFire)) != null) {
            lastValidMode = State.NORMAL;
        }

        // Handle Fire Key signal (separate topic)
        while ((m = softwareBus.get(this.elevatorID, FIREKEY)) != null) {
            if (currentMode == State.FIRE) {
                // Fire key inserted (body=1) allows for controlled movement within Fire mode
                if (m.getBody() == 1) lastValidMode = State.CONTROL;
                    // Fire key removed (body=0) reverts to pure Fire mode
                else if (m.getBody() == 0) lastValidMode = State.FIRE;
            }
        }

        currentMode = lastValidMode;
    }

    public FloorNDirection nextService(){
        Message m;
        // Use get(int recipientID, int topic) and m.getBody()
        while ((m = softwareBus.get(this.elevatorID, CABIN)) != null) {
            int floor = m.getBody();
            if (floor >= FLOOR_ONE && floor <= FLOOR_TEN) {
                currDestination = new FloorNDirection(floor, elevatorController.Util.Direction.STOPPED);
            }
        }
        return currDestination;
    }
}