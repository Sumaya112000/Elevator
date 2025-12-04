package elevatorController.LowerLevel;

import Bus.*;
import Message.Message;
import static elevatorController.Util.ConstantsElevatorControl.*;

public class DoorAssembly implements Runnable {
    private boolean opened;
    private boolean closed;
    private boolean obstructed;
    private boolean fullyClosed;
    private boolean fullyOpen;
    private boolean overCapacity;
    private int elevatorID;
    private SoftwareBus softwareBus;

    public DoorAssembly(int elevatorID, SoftwareBus softwareBus) {
        // SoftwareBus.subscribe(int recipientID, int topic)
        softwareBus.subscribe(elevatorID, DOORASSEMBLY);

        this.opened = true;
        this.closed = false;
        this.obstructed = false;
        this.fullyClosed = false;
        this.fullyOpen = true;
        this.overCapacity = false;
        this.softwareBus = softwareBus;
        this.elevatorID = elevatorID;

        Thread thread = new Thread(this);
        thread.setName("DoorAssembly-" + elevatorID);
        thread.start();
    }

    public void open(){
        softwareBus.publish(new Message(DOORASSEMBLY, elevatorID, OPEN));
        closed = false;
        fullyClosed = false;
    }

    public void close(){
        softwareBus.publish(new Message(DOORASSEMBLY, elevatorID, CLOSE));
        opened = false;
        fullyOpen = false;
    }

    public boolean obstructed(){ return obstructed; }
    public boolean fullyClosed(){ return fullyClosed; }
    public boolean fullyOpen(){ return fullyOpen; }
    public boolean overCapacity(){ return overCapacity; }

    @Override
    public void run() {
        while(true) {
            Message m;
            // Use get(int recipientID, int topic) and m.getBody()
            while ((m = softwareBus.get(elevatorID, DOORASSEMBLY)) != null) {

                if (m.getBody() == OBSTRUCTED) { obstructed = true; }
                else if (m.getBody() == FULLYCLOSED) { fullyClosed = true; closed = true; fullyOpen = false; }
                else if (m.getBody() == FULLYOPEN) { fullyOpen = true; opened = true; fullyClosed = false; }
                else if (m.getBody() == OVERCAPACITY) { overCapacity = true; }
                else if (m.getBody() == CLOSE) { /* Command received */ }
                else if (m.getBody() == OPEN) { /* Command received */ }

                // Simplified reset logic for sensors
                if (m.getBody() != OBSTRUCTED) obstructed = false;
                if (m.getBody() != OVERCAPACITY) overCapacity = false;
            }

            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}