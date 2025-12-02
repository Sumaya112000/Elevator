package elevatorController.LowerLevel;

import Bus.*;
import Message.*;

public class DoorAssembly implements Runnable {
    private boolean opened;
    private boolean closed;
    private volatile boolean obstructed;
    private volatile boolean fullyClosed;
    private volatile boolean fullyOpen;
    private volatile boolean overCapacity;
    private int elevatorID;
    private SoftwareBus softwareBus;

    public DoorAssembly(int elevatorID, SoftwareBus softwareBus) {
        this.elevatorID = elevatorID;
        this.softwareBus = softwareBus;

        softwareBus.subscribe(SoftwareBusCodes.doorStatus, elevatorID);
        softwareBus.subscribe(SoftwareBusCodes.doorSensor, elevatorID);
        softwareBus.subscribe(SoftwareBusCodes.cabinLoad, elevatorID);

        this.fullyOpen = true;
        this.fullyClosed = false;
        this.obstructed = false;
        this.overCapacity = false;

        Thread thread = new Thread(this);
        thread.setDaemon(true);
        thread.start();

        System.out.println("DoorAssembly " + elevatorID + " thread started");
    }

    public void open(){
        System.out.println("Elevator " + elevatorID + " opening doors");
        softwareBus.publish(new Message(SoftwareBusCodes.doorControl, elevatorID, 0));
    }

    public void close(){
        System.out.println("Elevator " + elevatorID + " closing doors");
        softwareBus.publish(new Message(SoftwareBusCodes.doorControl, elevatorID, 1));
    }

    @Override
    public void run() {
        while (true) {
            Message statusMsg = softwareBus.get(SoftwareBusCodes.doorStatus, elevatorID);
            if (statusMsg != null) {
                int status = statusMsg.getBody();
                fullyOpen = (status == 0);
                fullyClosed = (status == 1);
                System.out.println("Elevator " + elevatorID + " door status: " +
                        (fullyOpen ? "FULLY OPEN" : (fullyClosed ? "FULLY CLOSED" : "MOVING")));
            }

            Message obsMsg = softwareBus.get(SoftwareBusCodes.doorSensor, elevatorID);
            if (obsMsg != null) {
                obstructed = (obsMsg.getBody() == 0);
                System.out.println("Elevator " + elevatorID + " obstruction: " + obstructed);
            }

            Message loadMsg = softwareBus.get(SoftwareBusCodes.cabinLoad, elevatorID);
            if (loadMsg != null) {
                overCapacity = (loadMsg.getBody() == 1);
                System.out.println("Elevator " + elevatorID + " overload: " + overCapacity);
            }

            try { Thread.sleep(50); }
            catch (InterruptedException e) { break; }
        }
    }

    public boolean obstructed() {
        return obstructed;
    }

    public boolean fullyClosed() {
        return fullyClosed;
    }

    public boolean fullyOpen() {
        return fullyOpen;
    }

    public boolean overCapacity() {
        return overCapacity;
    }
}