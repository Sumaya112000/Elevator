package elevatorController.LowerLevel;

import Bus.*;
import Message.Message;
import elevatorController.Util.FloorNDirection;
import static elevatorController.Util.ConstantsElevatorControl.*;

public class Notifier {
    private int elevatorID;
    private SoftwareBus softwareBus;

    public Notifier(int elevatorID, SoftwareBus softwareBus){
        this.elevatorID = elevatorID;
        this.softwareBus = softwareBus;
    }

    public void arrivedAtFloor(FloorNDirection floorNDirection){
        // Notify arrival (e.g., chime)
        softwareBus.publish(new Message(NOTIFIER, elevatorID, floorNDirection.floor()));
    }

    public void elevatorStatus(FloorNDirection floorNDirection){
        // Notify status (floor display/direction)
        softwareBus.publish(new Message(NOTIFIER, elevatorID, floorNDirection.floor()));
    }

    public void playCapacityNoise(){
        softwareBus.publish(new Message(NOTIFIER, elevatorID, CAPON_ONE));
    }

    public void stopCapacityNoise(){
        softwareBus.publish(new Message(NOTIFIER, elevatorID, CAPOFF_ONE));
    }
}