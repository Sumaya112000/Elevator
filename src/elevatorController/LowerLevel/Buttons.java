package elevatorController.LowerLevel;

import Bus.*;
import Message.*;
import elevatorController.Util.Direction;
import elevatorController.Util.FloorNDirection;

import java.util.ArrayList;
import java.util.List;

public class Buttons {
    private boolean callEnabled;
    private boolean multipleRequests;
    private int elevatorID;
    private List<FloorNDirection> destinations;
    private SoftwareBus softwareBus;

    public Buttons(int elevatorID, SoftwareBus softwareBus) {
        this.elevatorID = elevatorID;
        this.softwareBus = softwareBus;
        this.destinations = new ArrayList<>();
        this.callEnabled = true;
        this.multipleRequests = true;

        softwareBus.subscribe(SoftwareBusCodes.cabinSelect, elevatorID);
        softwareBus.subscribe(SoftwareBusCodes.hallCall, elevatorID);

        System.out.println("Buttons " + elevatorID + " initialized");
    }

    public void callReset(FloorNDirection floorNDirection) {
        destinations.remove(floorNDirection);

        int dirCode = (floorNDirection.direction() == Direction.UP) ? 0 : 1;
        int body = floorNDirection.floor() * 10 + dirCode;
        softwareBus.publish(new Message(SoftwareBusCodes.resetCall, 5, body));
    }

    public void requestReset(int floor) {
        destinations.removeIf(fnd -> fnd.floor() == floor && fnd.direction() == null);

        System.out.println("Elevator " + elevatorID + " resetting floor button " + floor);
        softwareBus.publish(new Message(SoftwareBusCodes.resetFloorSelection, elevatorID, floor));
    }

    public void enableCalls(){
        this.callEnabled = true;
    }

    public void disableCalls(){
        this.callEnabled = false;
    }

    public void enableAllRequests(){
        this.multipleRequests = true;
    }

    public void enableSingleRequest(){
        this.multipleRequests = false;
    }

    public FloorNDirection nextService(FloorNDirection currentPos) {
        Message cabinMsg;
        while ((cabinMsg = softwareBus.get(SoftwareBusCodes.cabinSelect, elevatorID)) != null) {
            int floor = cabinMsg.getBody();
            System.out.println("Elevator " + elevatorID + " received cabin request for floor " + floor);

            if (multipleRequests || destinations.isEmpty()) {
                destinations.add(new FloorNDirection(floor, null));
            } else {
                destinations.clear();
                destinations.add(new FloorNDirection(floor, null));
            }
        }

        if (callEnabled) {
            Message hallMsg;
            while ((hallMsg = softwareBus.get(SoftwareBusCodes.hallCall, elevatorID)) != null) {
                int body = hallMsg.getBody();
                int floor = (body > 100) ? (body - 100) : body;
                Direction dir = (body > 100) ? Direction.UP : Direction.DOWN;
                System.out.println("Elevator " + elevatorID + " received hall call for floor " + floor + " direction " + dir);
                destinations.add(new FloorNDirection(floor, dir));
            }
        }

        if (destinations.isEmpty()) return null;

        FloorNDirection next = destinations.remove(0);
        System.out.println("Elevator " + elevatorID + " next service: floor " + next.floor());
        return next;
    }
}