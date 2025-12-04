package elevatorController.LowerLevel;

import Bus.*;
import Message.Message;
import elevatorController.Util.Direction;
import elevatorController.Util.FloorNDirection;
import static elevatorController.Util.ConstantsElevatorControl.*;

public class Cabin implements Runnable {
    private int elevatorID;
    private int currDest;
    private Direction currDirection;
    private int currFloor;
    private boolean motor;
    private SoftwareBus softwareBus;
    private long lastMoveTime;
    private final int MOVE_TIME_MS = 1000;

    public Cabin(int elevatorID, SoftwareBus softwareBus){
        this.softwareBus = softwareBus;
        this.elevatorID = elevatorID;

        this.currDest = FLOOR_ONE;
        this.currDirection = Direction.STOPPED;
        this.currFloor = FLOOR_ONE;
        this.motor = false;
        this.lastMoveTime = System.currentTimeMillis();

        Thread thread = new Thread(this);
        thread.setName("Cabin-" + elevatorID);
        thread.start();
    }

    @Override
    public void run() {
        while (true) {
            stepTowardsDest();
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    public void gotoFloor(int floor){ currDest = floor; }
    public FloorNDirection currentStatus(){ return new FloorNDirection(currFloor, currDirection); }
    public boolean arrived(){ return currFloor == currDest && !motor; }

    private synchronized void stepTowardsDest() {
        if (!motor && currFloor != currDest) {
            if (currFloor > currDest) currDirection = Direction.DOWN;
            else currDirection = Direction.UP;
            startMotor(currDirection);
        }

        if (motor) {
            if (currFloor == currDest) {
                stopMotor();
            } else if (System.currentTimeMillis() - lastMoveTime >= MOVE_TIME_MS) {

                if (currDirection == Direction.UP) currFloor++;
                else if (currDirection == Direction.DOWN) currFloor--;

                if ((currDirection == Direction.UP && currFloor > currDest) ||
                        (currDirection == Direction.DOWN && currFloor < currDest)) {
                    currFloor = currDest;
                    stopMotor();
                } else {
                    lastMoveTime = System.currentTimeMillis();
                }
            }
        } else {
            currDirection = Direction.STOPPED;
        }
    }

    private void startMotor(Direction direction) {
        motor = true;
        int messageBody = (direction == Direction.UP) ? MOTOR_MOVE_UP : MOTOR_MOVE_DOWN;
        softwareBus.publish(new Message(CABIN, elevatorID, messageBody));
        lastMoveTime = System.currentTimeMillis();
    }

    private void stopMotor() {
        motor = false;
        currDirection = Direction.STOPPED;
        softwareBus.publish(new Message(CABIN, elevatorID, MOTOR_STOP));
    }
}