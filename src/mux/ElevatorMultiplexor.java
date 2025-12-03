package mux;

import Bus.*;
import Message.*;
import motion.MotionAPI;
import motion.Util.Direction;
import pfdAPI.*;

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
    // Whether this elevator is enabled (can accept movement requests)
    private boolean enabled = true;
    // Fire recall state: when true, elevator should recall to floor 1 and hold doors open
    private boolean inFireMode = false;
    private boolean doorsHeldForFire = false;
    // Debounce pending floor detection to avoid oscillation between adjacent floors
    private int pendingDetectedFloor = -1;
    private int pendingDetectCount = 0;
    private final int FLOOR_CONFIRM_THRESHOLD = 2;

    // Initialize the MUX
    public void initialize() {
        bus.subscribe(SoftwareBusCodes.doorControl, ID);
        bus.subscribe(SoftwareBusCodes.displayFloor, ID);
        bus.subscribe(SoftwareBusCodes.displayDirection, ID);
        bus.subscribe(SoftwareBusCodes.carDispatch, ID);
        bus.subscribe(SoftwareBusCodes.systemStart, 0);
        bus.subscribe(SoftwareBusCodes.systemStop, 0);
        bus.subscribe(SoftwareBusCodes.startElevator, ID);
        bus.subscribe(SoftwareBusCodes.stopElevator, ID);
        bus.subscribe(SoftwareBusCodes.cabinSelect, ID);
        bus.subscribe(SoftwareBusCodes.hallCall, ID);
        bus.subscribe(SoftwareBusCodes.setMode, 0);
        bus.subscribe(SoftwareBusCodes.clearFire, 0);
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
                msg = bus.get(SoftwareBusCodes.systemStart, 0);
                if (msg != null) {
                    handleSystemStart();
                }
                msg = bus.get(SoftwareBusCodes.systemStop, 0);
                if (msg != null) {
                    handleSystemStop();
                }
                msg = bus.get(SoftwareBusCodes.startElevator, ID);
                if (msg != null) {
                    handleStartElevator();
                }
                msg = bus.get(SoftwareBusCodes.stopElevator, ID);
                if (msg != null) {
                    handleStopElevator();
                }
                msg = bus.get(SoftwareBusCodes.setMode, 0);
                if (msg != null) {
                    handleSetMode(msg);
                }
                msg = bus.get(SoftwareBusCodes.clearFire, 0);
                if (msg != null) {
                    handleClearFire();
                }
                msg = bus.get(SoftwareBusCodes.cabinSelect, ID);
                if (msg != null) {
                    handleCabinSelect(msg);
                }
                msg = bus.get(SoftwareBusCodes.hallCall, ID);
                if (msg != null) {
                    handleHallCall(msg);
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

        System.out.println("ElevatorMUX " + ID + " pollCarPosition sensors: top=" + topSensor + " bottom=" + bottomSensor + " targetFloor=" + targetFloor + " currentFloor=" + currentFloor);

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
            System.out.println("ElevatorMUX " + ID + ": Arrived at floor " + currentFloor + " going " + currentDirection + " (target=" + targetFloor + ")");

            // Update GUI and publish position
            elev.display.updateFloorIndicator(currentFloor, currentDirection);
            elev.panel.setDisplay(currentFloor, currentDirection);
            System.out.println("ElevatorMUX " + ID + " publishing cabinPosition floor=" + currentFloor);
            bus.publish(new Message(SoftwareBusCodes.cabinPosition, ID, currentFloor));

            // If we have a target floor and we've arrived, stop and open doors
            if (targetFloor != 0 && currentFloor == targetFloor) {
                System.out.println("ElevatorMUX " + ID + " *** REACHED TARGET FLOOR " + targetFloor + " - STOPPING AND OPENING DOORS ***");
                motionAPI.stop();
                bus.publish(new Message(SoftwareBusCodes.currMovement, ID, 0));
                bus.publish(new Message(SoftwareBusCodes.currDirection, ID, 2));
                elev.door.open();
                    // publish door status
                    bus.publish(new Message(SoftwareBusCodes.doorStatus, ID, SoftwareBusCodes.doorOpen));
                    // If not in fire-hold, schedule an automatic close after a short dwell time
                    if (!inFireMode) {
                        new Thread(() -> {
                            try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                            // Close doors and publish status (only if not obstructed)
                            elev.door.close();
                            if (elev.door.isFullyClosed()) {
                                bus.publish(new Message(SoftwareBusCodes.doorStatus, ID, SoftwareBusCodes.doorClose));
                            } else {
                                bus.publish(new Message(SoftwareBusCodes.doorStatus, ID, SoftwareBusCodes.doorOpen));
                            }
                        }).start();
                    }
                // Reset the cabin button in the UI (clear the floor selection)
                bus.publish(new Message(SoftwareBusCodes.resetFloorSelection, ID, targetFloor));
                System.out.println("ElevatorMUX " + ID + " published resetFloorSelection for floor " + targetFloor);
                // If we are in fire recall, hold doors open until clearFire
                if (inFireMode) {
                    doorsHeldForFire = true;
                    System.out.println("ElevatorMUX " + ID + " - FIRE: holding doors open at floor " + currentFloor);
                    // keep targetFloor cleared so we don't re-trigger movement
                    targetFloor = 0;
                    // Also reset any hallway call indicators for this floor (both directions)
                    int floorIndex = currentFloor - 1;
                    int upBody = floorIndex * 10 + 0;
                    int downBody = floorIndex * 10 + 1;
                    bus.publish(new Message(SoftwareBusCodes.resetCall, 5, upBody));
                    bus.publish(new Message(SoftwareBusCodes.resetCall, 5, downBody));
                } else {
                    System.out.println("ElevatorMUX " + ID + " - normal arrival: clearing targetFloor");
                    // Clear target and reset hallway call indicators for this floor
                    int floorIndex = currentFloor - 1;
                    int upBody = floorIndex * 10 + 0;
                    int downBody = floorIndex * 10 + 1;
                    bus.publish(new Message(SoftwareBusCodes.resetCall, 5, upBody));
                    bus.publish(new Message(SoftwareBusCodes.resetCall, 5, downBody));
                    targetFloor = 0;
                }
            }
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
        System.out.println("ElevatorMUX " + ID + " handleCarDispatch: dir=" + dir + " doorClosed=" + elev.door.isFullyClosed() + " enabled=" + enabled);

        // If elevator is stopped (disabled), do not dispatch
        if (!enabled) {
            System.out.println("ElevatorMUX " + ID + " - carDispatch ignored: elevator is stopped/disabled");
            return;
        }
        // If fire recall is active, prevent normal dispatches
        if (inFireMode) {
            System.out.println("ElevatorMUX " + ID + " - carDispatch ignored: in FIRE recall");
            return;
        }

        // If doors are not closed, attempt to close them first
        if (!elev.door.isFullyClosed()) {
            System.out.println("ElevatorMUX " + ID + " - doors open, attempting to close before dispatch");
            // Request door close (this blocks briefly while simulating door movement)
            elev.door.close();
            System.out.println("ElevatorMUX " + ID + " - door close attempt finished. closed=" + elev.door.isFullyClosed());
            // Publish door status after attempting close so GUIs stay in sync
            if (elev.door.isFullyClosed()) {
                bus.publish(new Message(SoftwareBusCodes.doorStatus, ID, SoftwareBusCodes.doorClose));
            } else {
                bus.publish(new Message(SoftwareBusCodes.doorStatus, ID, SoftwareBusCodes.doorOpen));
            }
        }

        // If doors are now closed, proceed to set direction and start motion
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
            System.out.println("ElevatorMUX " + ID + " calling motionAPI.start()");
            motionAPI.start();
        } else {
            System.out.println("ElevatorMUX " + ID + " carDispatch cancelled - doors remain open");
        }
    }

    // Handle cabin selections (internal panel or external PFD cabin select message)
    private void handleCabinSelect(Message msg) {
        int floor = msg.getBody();
        System.out.println("ElevatorMUX " + ID + " handleCabinSelect: targetFloor=" + floor + " currentFloor=" + currentFloor);
        // Ignore cabin selections during fire recall
        if (inFireMode) {
            System.out.println("ElevatorMUX " + ID + " - cabinSelect ignored: in FIRE recall");
            return;
        }
        if (floor == currentFloor) {
            System.out.println("ElevatorMUX " + ID + " cabinSelect equals currentFloor - opening doors");
            elev.door.open();
            bus.publish(new Message(SoftwareBusCodes.doorStatus, ID, SoftwareBusCodes.doorOpen));
            // auto-close after short dwell if not fire-held
            if (!inFireMode) {
                new Thread(() -> {
                    try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                    elev.door.close();
                    if (elev.door.isFullyClosed()) bus.publish(new Message(SoftwareBusCodes.doorStatus, ID, SoftwareBusCodes.doorClose));
                    else bus.publish(new Message(SoftwareBusCodes.doorStatus, ID, SoftwareBusCodes.doorOpen));
                }).start();
            }
            return;
        }
        // Record the selection but do not start motion until a hallCall arrives (demo behavior)
        targetFloor = floor;
        System.out.println("ElevatorMUX " + ID + " cabin selection recorded, awaiting hall call to start movement (target=" + targetFloor + ")");
    }

    // Handle a hall call targeted at this car (demo fallback when no scheduler exists)
    private void handleHallCall(Message msg) {
        int floor = msg.getBody();
        System.out.println("ElevatorMUX " + ID + " *** HANDLING HALL CALL: floor=" + floor + " currentFloor=" + currentFloor + " targetFloor=" + targetFloor + " enabled=" + enabled + " inFireMode=" + inFireMode);
        // Ignore normal requests while in fire recall
        if (inFireMode) {
            System.out.println("ElevatorMUX " + ID + " - hallCall ignored: in FIRE recall");
            return;
        }
        if (!enabled) {
            System.out.println("ElevatorMUX " + ID + " - hallCall ignored: elevator is stopped/disabled");
            return;
        }
        if (floor == currentFloor) {
            System.out.println("ElevatorMUX " + ID + " - already at floor " + floor + ", just opening doors");
            elev.door.open();
            bus.publish(new Message(SoftwareBusCodes.doorStatus, ID, SoftwareBusCodes.doorOpen));
            // Reset hallway call indicators for this floor (both directions)
            int floorIndex = currentFloor - 1;
            int upBody = floorIndex * 10 + 0;
            int downBody = floorIndex * 10 + 1;
            bus.publish(new Message(SoftwareBusCodes.resetCall, 5, upBody));
            bus.publish(new Message(SoftwareBusCodes.resetCall, 5, downBody));
            // auto-close after short dwell if not fire-held
            if (!inFireMode) {
                new Thread(() -> {
                    try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                    elev.door.close();
                    if (elev.door.isFullyClosed()) bus.publish(new Message(SoftwareBusCodes.doorStatus, ID, SoftwareBusCodes.doorClose));
                    else bus.publish(new Message(SoftwareBusCodes.doorStatus, ID, SoftwareBusCodes.doorOpen));
                }).start();
            }
            return;
        }
        targetFloor = floor;
        int dir = (floor > currentFloor) ? 0 : 1;
        System.out.println("ElevatorMUX " + ID + " - setting targetFloor=" + floor + " and dispatching direction=" + (dir == 0 ? "UP" : "DOWN"));
        handleCarDispatch(new Message(SoftwareBusCodes.carDispatch, ID, dir));
    }

    // Handle start individual elevator
    private void handleStartElevator() {
        enabled = true;
        System.out.println("ElevatorMUX " + ID + " - START received: elevator enabled");
        // Enable cabin panel buttons
        elev.panel.setButtonsDisabled(1);
    }

    // Handle stop individual elevator
    private void handleStopElevator() {
        enabled = false;
        System.out.println("ElevatorMUX " + ID + " - STOP received: stopping elevator and closing doors");
        // Stop motion and close doors, keep doors closed
        motionAPI.stop();
        motionAPI.set_direction(Direction.NULL);
        elev.door.close();
        if (elev.door.isFullyClosed()) {
            bus.publish(new Message(SoftwareBusCodes.doorStatus, ID, SoftwareBusCodes.doorClose));
        } else {
            bus.publish(new Message(SoftwareBusCodes.doorStatus, ID, SoftwareBusCodes.doorOpen));
        }
        // Disable cabin panel buttons to prevent new requests
        elev.panel.setButtonsDisabled(0);
        // Publish status idle
        bus.publish(new Message(SoftwareBusCodes.currMovement, ID, 0));
        bus.publish(new Message(SoftwareBusCodes.currDirection, ID, 2));
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

    private void handleSystemStart() {
        enabled = true;
        System.out.println("ElevatorMUX " + ID + " - SYSTEM START received: enabling elevator");
    }

    private void handleSystemStop() {
        enabled = false;
        System.out.println("ElevatorMUX " + ID + " - SYSTEM STOP received: disabling elevator");
        // Stop motion and close doors
        motionAPI.stop();
        elev.door.close();
        if (elev.door.isFullyClosed()) bus.publish(new Message(SoftwareBusCodes.doorStatus, ID, SoftwareBusCodes.doorClose));
        else bus.publish(new Message(SoftwareBusCodes.doorStatus, ID, SoftwareBusCodes.doorOpen));
        bus.publish(new Message(SoftwareBusCodes.currMovement, ID, 0));
        bus.publish(new Message(SoftwareBusCodes.currDirection, ID, 2));
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

    // Handle setMode commands (global control) - implement Fire recall
    private void handleSetMode(Message msg) {
        int mode = msg.getBody();
        System.out.println("ElevatorMUX " + ID + " *** handleSetMode: mode=" + mode + " (fire=" + SoftwareBusCodes.fire + ")");
        if (mode == SoftwareBusCodes.fire) {
            // Enter fire recall: clear selections and recall to floor 1
            System.out.println("ElevatorMUX " + ID + " *** FIRE MODE ACTIVATED - Recalling to floor 1 ***");
            elev.panel.clearPressedFloors();
            inFireMode = true;
            doorsHeldForFire = false;
            targetFloor = 1;
            System.out.println("ElevatorMUX " + ID + " - FIRE: targetFloor=1, currentFloor=" + currentFloor);

            // If already at floor 1, open doors and hold
            if (currentFloor == 1) {
                System.out.println("ElevatorMUX " + ID + " - FIRE: Already at floor 1, opening doors and holding");
                // Ensure doors are open and publish status
                if (!elev.door.isFullyOpen()) elev.door.open();
                bus.publish(new Message(SoftwareBusCodes.doorStatus, ID, SoftwareBusCodes.doorOpen));
                // publish movement/direction idle
                bus.publish(new Message(SoftwareBusCodes.currMovement, ID, 0));
                bus.publish(new Message(SoftwareBusCodes.currDirection, ID, 2));
                doorsHeldForFire = true;
                // keep targetFloor cleared so poll loop won't re-trigger
                targetFloor = 0;
            } else {
                // Need to move down to floor 1: close doors first and publish status
                System.out.println("ElevatorMUX " + ID + " - FIRE: Not at floor 1, closing doors and moving DOWN");
                if (!elev.door.isFullyClosed()) {
                    elev.door.close();
                    if (elev.door.isFullyClosed()) {
                        bus.publish(new Message(SoftwareBusCodes.doorStatus, ID, SoftwareBusCodes.doorClose));
                    } else {
                        bus.publish(new Message(SoftwareBusCodes.doorStatus, ID, SoftwareBusCodes.doorOpen));
                    }
                }
                currentDirection = "DOWN";
                motionAPI.set_direction(Direction.DOWN);
                bus.publish(new Message(SoftwareBusCodes.currDirection, ID, 1));
                bus.publish(new Message(SoftwareBusCodes.currMovement, ID, 1));
                System.out.println("ElevatorMUX " + ID + " - FIRE: Calling motionAPI.start() to move DOWN to floor 1");
                motionAPI.start();
            }
        }
    }

    // Handle Clear Fire message (elevator should stop where it is)
    private void handleClearFire() {
        System.out.println("ElevatorMUX " + ID + " *** FIRE MODE CLEARED - Stopping elevator ***");
        // Exit fire mode — stop recall behavior and resume normal operation
        inFireMode = false;
        // If we were holding doors for fire, close them now (and publish status)
        if (doorsHeldForFire) {
            System.out.println("ElevatorMUX " + ID + " - FIRE CLEARED: Closing held doors");
            elev.door.close();
            if (elev.door.isFullyClosed()) {
                bus.publish(new Message(SoftwareBusCodes.doorStatus, ID, SoftwareBusCodes.doorClose));
            } else {
                bus.publish(new Message(SoftwareBusCodes.doorStatus, ID, SoftwareBusCodes.doorOpen));
            }
            doorsHeldForFire = false;
        }
        targetFloor = 0;
        motionAPI.stop();
        bus.publish(new Message(SoftwareBusCodes.currMovement, ID, 0));
        bus.publish(new Message(SoftwareBusCodes.currDirection, ID, 2));
        System.out.println("ElevatorMUX " + ID + " - FIRE CLEARED: Elevator stopped, normal requests accepted");
    }
}