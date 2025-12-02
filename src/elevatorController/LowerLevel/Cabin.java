package elevatorController.LowerLevel;

import bus.Bus.SoftwareBus;
import elevatorController.Util.Direction;
import elevatorController.Util.FloorNDirection;

/**
 * The cabin provides a means for the elevator controller to send the elevator to a destination.
 * The cabin indirectly controls the motor by sending messages to the Software Bus.
 * Additionally, the cabin indirectly receives messages from physical sensors through the Software Bus.
 */
public class Cabin implements Runnable {
    private int currDest;
    private Direction currDirection;
    private int currFloor;
    private int topAlign;
    private int botAlign;
    private boolean motor;
    private SoftwareBus softwareBus;

    public Cabin(int elevatorID, SoftwareBus softwareBus){
        //TODO may need to take in int for elevator number for software bus subscription
        //TODO call subscribe on softwareBus w/ relevant topic/subtopic

        this.softwareBus = softwareBus;
        this.currDest = 0;
        this.currDirection = Direction.STOPPED;

        //Start Cabin Thread
        Thread thread = new Thread(this);
        thread.start();
    }

    /**
     * Run the Cabin
     */
    @Override
    public void run() {
        while (true) {
            stepTowardsDest();
            System.out.println("");
        }
    }

    public void gotoFloor(int floor){
        currDest = floor;
    }
    public FloorNDirection currentStatus(){return new FloorNDirection(currFloor,currDirection);}
    public boolean arrived(){return currFloor == currDest;}
    public int getTargetFloor(){return currDest;}


    // Internal methods

    private synchronized void stepTowardsDest() {
        topAlign = topAlignment();
        botAlign = bottomAlignment();
        currFloor = sensorToFloor(botAlign);
        if (motor && currFloor == currDest) {
            stopMotor();
        } else if (!motor){
            if (currFloor > currDest) currDirection = Direction.DOWN;
            else currDirection = Direction.UP;
            startMotor(currDirection);
        }
    }
    private int closestFloor() {
        return -69420;
    }

    private int sensorToFloor(int sensorPos) {
        return sensorPos/2 + 1;
    }


    //Wrapper methods for software bus messages
    private void startMotor(Direction direction) {
        motor = true;
        //TODO: send message
    }

    private void stopMotor() {
        motor = false;
        //TODO: your sister
    }

    private int topAlignment() {
        //TODO: get message from software bus
        return 0;
    }
    private int bottomAlignment() {
        //TODO: get message from software bus
        return 0;
    }

}