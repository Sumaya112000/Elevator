package elevatorController.LowerLevel;

import Bus.*;
import Message.*;
import elevatorController.Util.Direction;
import elevatorController.Util.FloorNDirection;

public class Notifier {
    private int elevatorID;
    private SoftwareBus softwareBus;

    public Notifier(int elevatorID, SoftwareBus softwareBus){
        this.elevatorID = elevatorID;
        this.softwareBus = softwareBus;

        System.out.println("Notifier " + elevatorID + " initialized");
    }

    public void arrivedAtFloor(FloorNDirection floorNDirection){
        int dirCode = (floorNDirection.direction() == Direction.UP) ? 0 :
                (floorNDirection.direction() == Direction.DOWN) ? 1 : 2;
        softwareBus.publish(new Message(SoftwareBusCodes.displayFloor, elevatorID, floorNDirection.floor()));
        softwareBus.publish(new Message(SoftwareBusCodes.displayDirection, elevatorID, dirCode));

        softwareBus.publish(new Message(SoftwareBusCodes.playSound, elevatorID, 0));

        System.out.println("Elevator " + elevatorID + " arrived at floor " + floorNDirection.floor());
    }

    public void elevatorStatus(FloorNDirection floorNDirection){
        int dirCode = (floorNDirection.direction() == Direction.UP) ? 0 :
                (floorNDirection.direction() == Direction.DOWN) ? 1 : 2;
        softwareBus.publish(new Message(SoftwareBusCodes.displayFloor, elevatorID, floorNDirection.floor()));
        softwareBus.publish(new Message(SoftwareBusCodes.displayDirection, elevatorID, dirCode));
    }

    public void playCapacityNoise(){
        softwareBus.publish(new Message(SoftwareBusCodes.playSound, elevatorID, 1));
    }

    public void stopCapacityNoise(){
    }
}