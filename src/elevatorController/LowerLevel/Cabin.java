package elevatorController.LowerLevel;

import Bus.*;
import Message.*;
import elevatorController.Util.Direction;
import elevatorController.Util.FloorNDirection;

public class Cabin implements Runnable {
    private int elevatorID;
    private volatile int currDest;
    private volatile Direction currDirection;
    private volatile int currFloor;
    private int topAlign;
    private int botAlign;
    private volatile boolean motor;
    private SoftwareBus softwareBus;

    public Cabin(int elevatorID, SoftwareBus softwareBus){
        this.elevatorID = elevatorID;
        this.softwareBus = softwareBus;
        this.currDest = 1;
        this.currFloor = 1;
        this.currDirection = Direction.STOPPED;
        this.motor = false;

        softwareBus.subscribe(SoftwareBusCodes.cabinPosition, elevatorID);
        softwareBus.subscribe(SoftwareBusCodes.currDirection, elevatorID);
        softwareBus.subscribe(SoftwareBusCodes.currMovement, elevatorID);

        Thread thread = new Thread(this);
        thread.setDaemon(true);
        thread.start();

        System.out.println("Cabin " + elevatorID + " thread started");
    }

    @Override
    public void run() {
        while (true) {
            Message posMsg = softwareBus.get(SoftwareBusCodes.cabinPosition, elevatorID);
            if (posMsg != null) {
                currFloor = posMsg.getBody();
                System.out.println("Elevator " + elevatorID + " position update: floor " + currFloor);

                if (currFloor == currDest && motor) {
                    stopMotor();
                }
            }

            Message dirMsg = softwareBus.get(SoftwareBusCodes.currDirection, elevatorID);
            if (dirMsg != null) {
                int dir = dirMsg.getBody();
                currDirection = (dir == 0) ? Direction.UP :
                        (dir == 1) ? Direction.DOWN : Direction.STOPPED;
            }

            try { Thread.sleep(50); }
            catch (InterruptedException e) { break; }
        }
    }

    public synchronized void gotoFloor(int floor){
        if (floor < 1 || floor > 10) return;

        System.out.println("Elevator " + elevatorID + " gotoFloor(" + floor + ") called. Current: " + currFloor);

        currDest = floor;

        if (currFloor == currDest) {
            System.out.println("Elevator " + elevatorID + " already at destination floor " + floor);
            return;
        }

        Direction dir = (currDest > currFloor) ? Direction.UP : Direction.DOWN;
        int dirCode = (dir == Direction.UP) ? 0 : 1;

        System.out.println("Elevator " + elevatorID + " dispatching: direction=" + dir + ", dirCode=" + dirCode);

        softwareBus.publish(new Message(SoftwareBusCodes.carDispatch, elevatorID, dirCode));
        motor = true;
    }

    private synchronized void stopMotor() {
        System.out.println("Elevator " + elevatorID + " stopping motor at floor " + currFloor);
        motor = false;
        softwareBus.publish(new Message(SoftwareBusCodes.carStop, elevatorID, 0));
        currDirection = Direction.STOPPED;
    }

    public FloorNDirection currentStatus() {
        return new FloorNDirection(currFloor, currDirection);
    }

    public boolean arrived() {
        return currFloor == currDest;
    }

    public int getTargetFloor() {
        return currDest;
    }
}