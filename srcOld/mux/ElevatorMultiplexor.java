package mux;

import Bus.*;
import motion.MotionAPI;
import motion.Util.Direction;
import pfdAPI.*;
import Message.*;

/**
 * Class that defines the ElevatorMultiplexor, which coordinates communication from the Elevator
 * Command Center to the relevant devices. Communication is accomplished via the software bus,
 * and both the PFDs and the motion devices are subject to control.
 *
 * Note: car and elevator are used interchangeably in this context.
 */
public class ElevatorMultiplexor {

    // Constructor
    public ElevatorMultiplexor(int ID){
        this.ID = ID;
        this.elev = new Elevator(ID, 10);
        initialize();
    }

    // Globals
    private int currentFloor = 1;
    private String currentDirection = "IDLE";
    private final int ID;
    private final Elevator elev;
    private final SoftwareBus bus = new SoftwareBus(false);
    private final MotionAPI motionAPI = new MotionAPI();
    private boolean lastFireKeyState = false;
    private boolean lastObstructedState = false;
    private boolean lastOverloadState = false;
    private int lastPressedFloor = 0;
    private int targetFloor = 0;
    private Integer lastTopSensorRead = motionAPI.top_alignment();
    private Integer lastBottomSensorRead = motionAPI.bottom_alignment();

    // Initialize the MUX
    public void initialize() {
        bus.subscribe(SoftwareBusCodes.doorControl, ID);
        bus.subscribe(SoftwareBusCodes.displayFloor, ID);
        bus.subscribe(SoftwareBusCodes.displayDirection, ID);
        bus.subscribe(SoftwareBusCodes.carDispatch, ID);
        bus.subscribe(SoftwareBusCodes.resetFloorSelection, ID);

        bus.subscribe(SoftwareBusCodes.carStop, ID);
        bus.subscribe(SoftwareBusCodes.selectionsEnable, ID);
        bus.subscribe(SoftwareBusCodes.selectionsType, ID);
        bus.subscribe(SoftwareBusCodes.playSound, ID);
        bus.subscribe(SoftwareBusCodes.fireAlarm, ID);

        System.out.println("ElevatorMUX " + ID + " initialized and subscribed");
        startBusPoller();
        startStatePoller();
    }


    /**
     * Incoming Message Polling
     */

    // Polls the software bus for messages and handles them accordingly
    public void startBusPoller() {
        Thread t = new Thread(() -> {
            // keep polling
            while (true) {
                Message msg;
                msg = bus.get(SoftwareBusCodes.doorControl, ID);
                if (msg != null) {
                    handleDoorControl(msg);
                }
                msg = bus.get(SoftwareBusCodes.displayFloor, ID);
                if (msg != null) {
                    handleDisplayFloor(msg);
                }
                msg = bus.get(SoftwareBusCodes.displayDirection, ID);
                if (msg != null) {
                    handleDisplayDirection(msg);
                }
                msg = bus.get(SoftwareBusCodes.carDispatch, ID);
                if (msg != null) {
                    handleCarDispatch(msg);
                }
                msg = bus.get(SoftwareBusCodes.resetFloorSelection, ID);
                if (msg != null) {
                    int floorNumber = msg.getBody();
                    elev.panel.resetFloorButton(floorNumber);
                }
                msg = bus.get(SoftwareBusCodes.carStop, ID);
                if (msg != null) {
                    handleCarStop(msg);
                }
                msg = bus.get(SoftwareBusCodes.selectionsEnable, ID);
                if (msg != null) {
                    handleSelectionEnable(msg);
                }
                msg = bus.get(SoftwareBusCodes.selectionsType, ID);
                if (msg != null) {
                    handleSelectionType(msg);
                }
                msg = bus.get(SoftwareBusCodes.playSound, ID);
                if (msg != null) {
                    handlePlaySound(msg);
                }
                msg = bus.get(SoftwareBusCodes.fireAlarm, ID);
                if (msg != null) {
                    handleFireAlarm(msg);
                }

                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        t.start();
    }

    /**
     * Internal State Polling Functions
     */

    // Polls the elevator state periodically and publishes updates to the bus
    private void startStatePoller() {
        Thread statePoller = new Thread(() -> {
            while (true) {
                pollFireKeyState();
                pollPressedFloors();
                pollDoorObstruction();
                pollCabinOverload();
                pollCarPosition();

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        statePoller.start();
    }

    // Poll and publish fire key state changes
    private void pollFireKeyState() {
        boolean fireKeyActive = elev.panel.isFireKeyActive();
        if (fireKeyActive != lastFireKeyState) {
            // Emit FIRE_KEY message (Topic 206) only on state change
            int v;
            if (fireKeyActive) v = 1;
            else v = 0;
            Message fireMsg = new Message(SoftwareBusCodes.fireKey, ID, v);
            bus.publish(fireMsg);
            lastFireKeyState = fireKeyActive;
        }
    }

    // Poll and publish pressed floor buttons
    private void pollPressedFloors() {
        int targetFloor = elev.panel.getPressedFloor();
        if (targetFloor != 0 && targetFloor != lastPressedFloor) {
            Message selectMsg = new Message(SoftwareBusCodes.cabinSelect, ID, targetFloor);
            bus.publish(selectMsg);
            lastPressedFloor = targetFloor;
        }
    }

    // Poll and publish door obstruction state changes
    private void pollDoorObstruction() {
        boolean isObstructed = elev.door.isObstructed();
        int body;
        if(isObstructed){
            body = 0;
        }else{
            body = 1;
        }
        // Update obstruction state
        if (isObstructed != lastObstructedState) {
            Message statusMsg = new Message(SoftwareBusCodes.doorSensor, ID, body);
            bus.publish(statusMsg);
            lastObstructedState = isObstructed;
        }
    }


    // Poll and publish cabin overload state changes
    private void pollCabinOverload() {
        boolean isOverloaded = elev.display.isOverloaded();
        if (isOverloaded != lastOverloadState) {
            // Emit CABIN_LOAD message (Topic 205) only on state change
            int v;
            if (isOverloaded) v = 1;
            else v = 0;
            Message loadMsg = new Message(SoftwareBusCodes.cabinLoad, ID, v);
            bus.publish(loadMsg);
            lastOverloadState = isOverloaded;
        }
    }

    // Poll car position
    private void pollCarPosition() {
        Integer topSensor = motionAPI.top_alignment();
        Integer bottomSensor = motionAPI.bottom_alignment();

        boolean topChanged = (topSensor != null && !topSensor.equals(lastTopSensorRead));
        boolean botChanged = (bottomSensor != null && !bottomSensor.equals(lastBottomSensorRead));

        // Return if no state change has occured
        if (!topChanged && !botChanged) return;

        // Publish sensor data if has changed
        if (topChanged) bus.publish(new Message(SoftwareBusCodes.topSensor, ID, topSensor));
        if (botChanged) bus.publish(new Message(SoftwareBusCodes.bottomSensor, ID, bottomSensor));

        // Calculate new floor
        int newFloor = (bottomSensor / 2) + 1; // +1 for indexing
        if (newFloor < 1 || newFloor > elev.totalFloors) { // Invalid floor
            lastTopSensorRead = topSensor;
            lastBottomSensorRead = bottomSensor;
            System.out.println("ElevatorMUX " + ID + ": Invalid floor detected: " + newFloor);
            return;
        }

        // Only update when actually moving floors
        if (newFloor != currentFloor) {
            if (newFloor > currentFloor) currentDirection = "UP";
            else currentDirection = "DOWN";
            currentFloor = newFloor;
            System.out.println("ElevatorMUX " + ID + ": Arrived at floor " + currentFloor + " going " + currentDirection);

            // Update GUI and publish position
            elev.display.updateFloorIndicator(currentFloor, currentDirection);
            elev.panel.setDisplay(currentFloor, currentDirection);
            bus.publish(new Message(SoftwareBusCodes.cabinPosition, ID, currentFloor));
        }

        // Update last sensor reads
        lastTopSensorRead = topSensor;
        lastBottomSensorRead = bottomSensor;
    }


    // Getter for Elevator
    public Elevator getElevator() {
        return elev;
    }

    /**
     * Incoming Message Handlers
     */

    // Handle door control messages
    private void handleDoorControl(Message msg) {
        int command = msg.getBody();
        Message positionMsg = null;
        if (command == 0) {
            elev.door.open();
            if(elev.door.isFullyOpen()){
                positionMsg = new Message(SoftwareBusCodes.doorStatus, ID, 0);
            } else {
                positionMsg = new Message(SoftwareBusCodes.doorStatus, ID, 1);
            }
        } else if (command == 1) {
            elev.door.close();
            if(elev.door.isFullyClosed()){
                positionMsg = new Message(SoftwareBusCodes.doorStatus, ID, 1);
            } else {
                positionMsg = new Message(SoftwareBusCodes.doorStatus, ID, 0);
            }
        }
        bus.publish(positionMsg);
    }

    // Handle display floor messages
    private void handleDisplayFloor(Message msg) {
        int floor = msg.getBody();
        elev.display.updateFloorIndicator(floor, currentDirection);
        elev.panel.setDisplay(floor, currentDirection);
    }

    // Handle display direction messages
    private void handleDisplayDirection(Message msg) {
        int dir = msg.getBody();
        if (dir == 0){
            elev.display.updateFloorIndicator(currentFloor, "UP");
            elev.panel.setDisplay(currentFloor, "UP");
        } else if (dir == 1) {
            elev.display.updateFloorIndicator(currentFloor, "DOWN");
            elev.panel.setDisplay(currentFloor, "DOWN");
        } else {
            elev.display.updateFloorIndicator(currentFloor, "IDLE");
            elev.panel.setDisplay(currentFloor, "IDLE");
        }
    }

    // Handle car dispatch messages
    private void handleCarDispatch(Message msg) {
        int dir = msg.getBody();
        if(elev.door.isFullyClosed()){
            if (dir == 0) {
                currentDirection = "UP";
                motionAPI.set_direction(Direction.UP);
                bus.publish(new Message(SoftwareBusCodes.currDirection, ID, 0));
            } else if (dir == 1) {
                currentDirection = "DOWN";
                motionAPI.set_direction(Direction.DOWN);
                bus.publish(new Message(SoftwareBusCodes.currDirection, ID, 1));
            }
            bus.publish(new Message(SoftwareBusCodes.currMovement, ID, 1));

            elev.display.updateFloorIndicator(currentFloor, currentDirection);
            elev.panel.setDisplay(currentFloor, currentDirection);
            motionAPI.start();
        }
    }

    // Handle Car Stop Message
    private void handleCarStop(Message msg){
        motionAPI.stop();
        elev.display.updateFloorIndicator(currentFloor, "IDLE");
        elev.panel.setDisplay(currentFloor, "IDLE");
        motionAPI.set_direction(Direction.NULL);
        bus.publish(new Message(SoftwareBusCodes.currDirection, ID, 2));
        bus.publish(new Message(SoftwareBusCodes.currMovement, ID, 0));
    }

    // Handle Selection Disable/Enable Message
    private void handleSelectionEnable(Message msg) {
        int body = msg.getBody();
        if(body == 0) {
            elev.panel.clearPressedFloors();
        }
        elev.panel.setButtonsDisabled(body);
    }

    // Handle Selection allow single/multiple Message
    private void handleSelectionType(Message msg) {
        int body = msg.getBody();
        elev.panel.setButtonsSingle(body);
    }

    // Handle play arrival/overload Message
    public void handlePlaySound(Message msg){
        int type = msg.getBody();
        if (type == 0) {
            elev.display.playArrivalChime();
        } else {
            elev.display.playOverLoadWarning();
        }
    }

    // Handle Fire Alarm Message
    public void handleFireAlarm(Message msg) {
        int modeCode = msg.getBody();
        if (modeCode == 1) {
            elev.panel.clearPressedFloors();
        }
    }
}