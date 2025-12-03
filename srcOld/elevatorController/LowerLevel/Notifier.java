package elevatorController.LowerLevel;

import Bus.*;
import elevatorController.Util.FloorNDirection;

/**
 * The notifier object is used to communicate all necessary visual and audio
 * information. The notifier sends messages to the speakers, button lights, and
 * floor display (up/down arrows and LEDs for displaying the floor number). The
 * notifier object does not receive any messages from the Software Bus.
 */
public class Notifier {
    private int elevatorID;
    private SoftwareBus softwareBus;

    public  Notifier(int elevatorID, SoftwareBus softwareBus){
        //TODO: does notifier need to subscribe? or can it just publish messages?
        //TODO call subscribe on softwareBus w/ relevant topic/subtopic
        this.elevatorID = elevatorID;
        this.softwareBus = softwareBus;
    }

    /**
     * Notify Control Center and MUX of elevator status (arrived => play arrival
     * chime)
     * @param floorNDirection This elevator's current floor and direction
     */
    public void arrivedAtFloor(FloorNDirection floorNDirection){}

    /**
     * Notify Control Center and MUX of this elevator's status
     * @param floorNDirection This elevator's floor and direction
     */
    public void elevatorStatus(FloorNDirection floorNDirection){}

    /**
     * Notify the MUX to play the capacity buzzer
     */
    public void playCapacityNoise(){}

    /**
     * Notify the MUX to stop playing the capacity buzzer
     */
    public void stopCapacityNoise(){}

}