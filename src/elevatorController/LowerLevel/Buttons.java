package elevatorController.LowerLevel;

import Bus.*;
import Message.Message;
import elevatorController.Util.Direction;
import elevatorController.Util.FloorNDirection;
import static elevatorController.Util.ConstantsElevatorControl.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;

public class Buttons {
    private boolean callEnabled;
    private boolean multipleRequests;
    private int elevatorID;
    private List<FloorNDirection> destinations;
    private SoftwareBus softwareBus;
    private FloorNDirection lastRequest;

    public Buttons(int elevatorID, SoftwareBus softwareBus) {
        // softwareBus.subscribe(int recipientID, int topic)
        softwareBus.subscribe(this.elevatorID, BUTTON);
        softwareBus.subscribe(this.elevatorID, hallCall);

        this.callEnabled = true;
        this.multipleRequests = true;
        this.destinations = new ArrayList<>();
        this.softwareBus = softwareBus;
        this.elevatorID = elevatorID;
        this.lastRequest = null;
    }

    public void callReset(FloorNDirection floorNDirection) {
        destinations.remove(floorNDirection);
    }

    public void requestReset(int floor) {
        destinations.removeIf(d -> d.getFloor() == floor && d.getDirection() == Direction.STOPPED);
        if (!multipleRequests && lastRequest != null && lastRequest.getFloor() == floor) {
            lastRequest = null;
        }
        softwareBus.publish(new Message(NOTIFIER, LIGHT_OFF, floor));
    }

    public void enableCalls(){ this.callEnabled = true; }
    public void disableCalls(){ this.callEnabled = false; }
    public void enableAllRequests(){ this.multipleRequests = true; }
    public void enableSingleRequest(){ this.multipleRequests = false; }

    public FloorNDirection nextService(FloorNDirection currentStatus) {

        Message m;
        // Use get(int recipientID, int topic) and m.getTopic()/m.getBody()
        while (true) {
            m = softwareBus.get(this.elevatorID, BUTTON); // Cabin button presses
            if (m == null) {
                m = softwareBus.get(this.elevatorID, hallCall); // Hall call assignments
            }

            if (m == null) break;

            int floor = m.getBody();

            if (m.getTopic() == BUTTON) {
                // Cabin Request (always processed)
            } else if (m.getTopic() == hallCall && callEnabled) {
                // Hall Call (processed only if calls are enabled)
            } else {
                continue; // Ignore hall call if disabled
            }

            FloorNDirection newDest = new FloorNDirection(floor, Direction.STOPPED);

            if (multipleRequests) {
                if (!destinations.contains(newDest)) destinations.add(newDest);
            } else {
                lastRequest = newDest;
                destinations.clear();
                destinations.add(newDest);
            }
        }

        if (destinations.isEmpty()) return null;

        // Simple Elevator Scheduling: Closest destination in the current direction.
        final Direction currentDir = currentStatus.direction();
        final int currentFloor = currentStatus.floor();

        return destinations.stream()
                .filter(d -> {
                    if (currentDir == Direction.UP) return d.floor() >= currentFloor;
                    if (currentDir == Direction.DOWN) return d.floor() <= currentFloor;
                    return true;
                })
                .min(Comparator.comparingInt(d -> Math.abs(d.floor() - currentFloor)))
                .orElse(destinations.get(0));
    }
}