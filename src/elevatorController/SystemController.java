package elevatorController;

import Bus.SoftwareBus;
import Bus.SoftwareBusCodes;
import Message.Message;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Main elevator system controller.
 * Communicates ONLY via SoftwareBus.
 *
 * Hall call encoding: UP = floor+100, DOWN = floor
 *
 * Modes: CENTRALIZED (assigns calls), INDEPENDENT (per-car only), FIRE (recall to floor 1)
 */
public class SystemController implements Runnable {

    private enum ElevatorState { IDLE, MOVING_UP, MOVING_DOWN, DOOR_OPEN, DOOR_CLOSING }
    private enum SystemMode { CENTRALIZED, INDEPENDENT, FIRE, STOPPED }

    private final SoftwareBus bus;
    private final int numElevators;
    private SystemMode currentMode = SystemMode.STOPPED;
    private boolean running = true;

    // Per-elevator state
    private final int[] currentFloors;
    private final ElevatorState[] states;
    private final boolean[] doorsOpen;
    private final boolean[] doorsFullyClosed;
    private final List<Integer>[] queues;
    private final Timer[] doorTimers;
    private final Set<String> activeHallCalls; // "floor-dir"

    // Constants
    private static final int DOOR_DWELL_MS = 5000;
    private static final int POLL_DELAY_MS = 50;
    private static final int FIRE_RECALL_FLOOR = 1;

    public SystemController(SoftwareBus bus, int numElevators) {
        this.bus = bus;
        this.numElevators = numElevators;

        currentFloors = new int[numElevators + 1];
        states = new ElevatorState[numElevators + 1];
        doorsOpen = new boolean[numElevators + 1];
        doorsFullyClosed = new boolean[numElevators + 1];
        queues = new List[numElevators + 1];
        doorTimers = new Timer[numElevators + 1];
        activeHallCalls = ConcurrentHashMap.newKeySet();

        for (int i = 1; i <= numElevators; i++) {
            currentFloors[i] = 1;
            states[i] = ElevatorState.IDLE;
            doorsOpen[i] = true;
            doorsFullyClosed[i] = false;
            queues[i] = Collections.synchronizedList(new ArrayList<>());
            doorTimers[i] = new Timer(true);
        }

        subscribeToTopics();
        System.out.println("SystemController initialized for " + numElevators + " elevators");
    }

    private void subscribeToTopics() {
        // System-level
        bus.subscribe(SoftwareBusCodes.systemStart, 0);
        bus.subscribe(SoftwareBusCodes.systemStop, 0);
        bus.subscribe(SoftwareBusCodes.setMode, 0);
        bus.subscribe(SoftwareBusCodes.clearFire, 0);
        bus.subscribe(SoftwareBusCodes.fireAlarmActive, 0);

        // Per-elevator
        for (int i = 1; i <= numElevators; i++) {
            bus.subscribe(SoftwareBusCodes.hallCall, i);
            bus.subscribe(SoftwareBusCodes.cabinSelect, i);
            bus.subscribe(SoftwareBusCodes.cabinPosition, i);
            bus.subscribe(SoftwareBusCodes.doorStatus, i);
            bus.subscribe(SoftwareBusCodes.doorSensor, i);
            bus.subscribe(SoftwareBusCodes.cabinLoad, i);
        }
    }

    @Override
    public void run() {
        System.out.println("SystemController main loop started");

        while (running) {
            try {
                processSystemMessages();

                for (int id = 1; id <= numElevators; id++) {
                    processElevatorMessages(id);
                    controlElevator(id);
                }

                Thread.sleep(POLL_DELAY_MS);
            } catch (Exception e) {
                System.err.println("SystemController error: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void processSystemMessages() {
        Message msg;

        // System start
        if ((msg = bus.get(SoftwareBusCodes.systemStart, 0)) != null) {
            if (currentMode == SystemMode.STOPPED) {
                currentMode = SystemMode.CENTRALIZED;
                System.out.println("System STARTED - CENTRALIZED mode");
                for (int i = 1; i <= numElevators; i++) {
                    bus.publish(new Message(SoftwareBusCodes.selectionsEnable, i, 1));
                }
            }
        }

        // System stop
        if ((msg = bus.get(SoftwareBusCodes.systemStop, 0)) != null) {
            currentMode = SystemMode.STOPPED;
            System.out.println("System STOPPED");
            for (int i = 1; i <= numElevators; i++) {
                bus.publish(new Message(SoftwareBusCodes.carStop, i, 0));
                bus.publish(new Message(SoftwareBusCodes.selectionsEnable, i, 0));
                queues[i].clear();
                states[i] = ElevatorState.IDLE;
                cancelDoorTimer(i);
            }
        }

        // Mode changes (from CommandPanel)
        if ((msg = bus.get(SoftwareBusCodes.setMode, 0)) != null) {
            int modeCode = msg.getBody();
            if (modeCode == SoftwareBusCodes.centralized) {
                if (currentMode != SystemMode.FIRE) {
                    currentMode = SystemMode.CENTRALIZED;
                    System.out.println("Mode: CENTRALIZED");
                }
            } else if (modeCode == SoftwareBusCodes.independent) {
                if (currentMode != SystemMode.FIRE) {
                    currentMode = SystemMode.INDEPENDENT;
                    System.out.println("Mode: INDEPENDENT");
                }
            } else if (modeCode == SoftwareBusCodes.fire) {
                enterFireMode();
            }
        }

        // Clear fire
        if ((msg = bus.get(SoftwareBusCodes.clearFire, 0)) != null) {
            if (currentMode == SystemMode.FIRE) {
                exitFireMode();
            }
        }

        // Fire alarm from BuildingMultiplexor (hardware fire alarm)
        if ((msg = bus.get(SoftwareBusCodes.fireAlarmActive, 0)) != null) {
            if (msg.getBody() == 1 && currentMode != SystemMode.FIRE) {
                enterFireMode();
            }
        }
    }

    private void processElevatorMessages(int id) {
        Message msg;

        // Position updates
        if ((msg = bus.get(SoftwareBusCodes.cabinPosition, id)) != null) {
            int newFloor = msg.getBody();
            if (newFloor != currentFloors[id]) {
                currentFloors[id] = newFloor;
                System.out.println("E" + id + " at floor " + newFloor);
            }
        }

        // Door status
        if ((msg = bus.get(SoftwareBusCodes.doorStatus, id)) != null) {
            int status = msg.getBody();
            doorsOpen[id] = (status == SoftwareBusCodes.doorOpen);
            doorsFullyClosed[id] = (status == SoftwareBusCodes.doorClose);
        }

        // Hall calls (ignored in FIRE / STOPPED)
        if (currentMode != SystemMode.FIRE && currentMode != SystemMode.STOPPED) {
            if ((msg = bus.get(SoftwareBusCodes.hallCall, id)) != null) {
                int body = msg.getBody();
                int floor = (body > 100) ? (body - 100) : body;
                String dir = (body > 100) ? "UP" : "DOWN";
                String key = floor + "-" + dir;

                if (!activeHallCalls.contains(key)) {
                    activeHallCalls.add(key);
                    System.out.println("Hall call: floor " + floor + " " + dir);

                    if (currentMode == SystemMode.CENTRALIZED) {
                        int bestElev = findBestElevator(floor);
                        addTarget(bestElev, floor);
                    } else {
                        addTarget(id, floor); // INDEPENDENT: use subtopic's car
                    }
                }
            }
        }

        // Cabin selections (ignored in FIRE / STOPPED)
        if (currentMode != SystemMode.FIRE && currentMode != SystemMode.STOPPED) {
            if ((msg = bus.get(SoftwareBusCodes.cabinSelect, id)) != null) {
                int floor = msg.getBody();
                System.out.println("E" + id + " cabin button " + floor);
                addTarget(id, floor);
            }
        }
    }

    private void controlElevator(int id) {
        if (currentMode == SystemMode.STOPPED) {
            return;
        }

        if (currentMode == SystemMode.FIRE) {
            controlElevatorFire(id);
            return;
        }

        ElevatorState state = states[id];
        int floor = currentFloors[id];
        List<Integer> queue = queues[id];

        // Normal (CENTRALIZED / INDEPENDENT) state machine
        switch (state) {
            case IDLE:
                if (!queue.isEmpty() && doorsFullyClosed[id]) {
                    int target = queue.get(0);
                    if (target > floor) {
                        bus.publish(new Message(SoftwareBusCodes.carDispatch, id, SoftwareBusCodes.up));   // UP (0)
                        bus.publish(new Message(SoftwareBusCodes.displayDirection, id, SoftwareBusCodes.up));
                        states[id] = ElevatorState.MOVING_UP;
                        System.out.println("E" + id + " dispatched UP to " + target);
                    } else if (target < floor) {
                        bus.publish(new Message(SoftwareBusCodes.carDispatch, id, SoftwareBusCodes.down)); // DOWN (1)
                        bus.publish(new Message(SoftwareBusCodes.displayDirection, id, SoftwareBusCodes.down));
                        states[id] = ElevatorState.MOVING_DOWN;
                        System.out.println("E" + id + " dispatched DOWN to " + target);
                    }
                } else if (!queue.isEmpty() && !doorsFullyClosed[id]) {
                    // Need to close doors first
                    if (doorsOpen[id]) {
                        bus.publish(new Message(SoftwareBusCodes.doorControl, id, SoftwareBusCodes.doorClose));
                        states[id] = ElevatorState.DOOR_CLOSING;
                    }
                }
                break;

            case MOVING_UP:
            case MOVING_DOWN:
                if (!queue.isEmpty() && floor == queue.get(0)) {
                    // Arrived at target
                    bus.publish(new Message(SoftwareBusCodes.carStop, id, 0));
                    bus.publish(new Message(SoftwareBusCodes.displayDirection, id, SoftwareBusCodes.none)); // IDLE (2)
                    queue.remove(0);

                    // Open doors
                    bus.publish(new Message(SoftwareBusCodes.doorControl, id, SoftwareBusCodes.doorOpen));
                    states[id] = ElevatorState.DOOR_OPEN;
                    System.out.println("E" + id + " arrived at floor " + floor);

                    // Reset buttons
                    bus.publish(new Message(SoftwareBusCodes.resetFloorSelection, id, floor));
                    clearHallCallsForFloor(floor);

                    // Schedule door close
                    cancelDoorTimer(id);
                    doorTimers[id] = new Timer(true);
                    final int elevId = id;
                    doorTimers[id].schedule(new TimerTask() {
                        @Override
                        public void run() {
                            if (states[elevId] == ElevatorState.DOOR_OPEN &&
                                    currentMode != SystemMode.FIRE) {
                                bus.publish(new Message(SoftwareBusCodes.doorControl,
                                        elevId,
                                        SoftwareBusCodes.doorClose));
                                states[elevId] = ElevatorState.DOOR_CLOSING;
                            }
                        }
                    }, DOOR_DWELL_MS);
                }
                break;

            case DOOR_OPEN:
                // Waiting for timer
                break;

            case DOOR_CLOSING:
                if (doorsFullyClosed[id]) {
                    states[id] = ElevatorState.IDLE;
                }
                break;
        }
    }

    /**
     * FIRE MODE BEHAVIOR
     *
     * - Ignore all hall/cabin calls (already handled in processElevatorMessages).
     * - For each elevator:
     *   - Close doors if not at recall floor.
     *   - Once fully closed, dispatch towards floor 1 (DOWN).
     *   - When at floor 1, stop car, open doors, and keep them open
     *     until CLEAR FIRE.
     */
    private void controlElevatorFire(int id) {
        int floor = currentFloors[id];
        ElevatorState state = states[id];
        List<Integer> queue = queues[id];

        // Ensure queue only has the recall floor
        if (queue.isEmpty() || queue.get(0) != FIRE_RECALL_FLOOR) {
            queue.clear();
            queue.add(FIRE_RECALL_FLOOR);
        }

        // If already at recall floor, make sure we're stopped and doors are open
        if (floor == FIRE_RECALL_FLOOR) {
            // Stop movement
            bus.publish(new Message(SoftwareBusCodes.carStop, id, 0));
            bus.publish(new Message(SoftwareBusCodes.displayDirection, id, SoftwareBusCodes.none)); // idle

            // Clear queue
            queue.clear();

            // Open doors and keep them open (no timers in FIRE)
            if (!doorsOpen[id]) {
                bus.publish(new Message(SoftwareBusCodes.doorControl, id, SoftwareBusCodes.doorOpen));
            }
            states[id] = ElevatorState.DOOR_OPEN;
            return;
        }

        // Not at recall floor yet:
        // 1. Close doors if open
        if (doorsOpen[id]) {
            bus.publish(new Message(SoftwareBusCodes.doorControl, id, SoftwareBusCodes.doorClose));
            states[id] = ElevatorState.DOOR_CLOSING;
            return;
        }

        // 2. Wait until doors are fully closed
        if (!doorsFullyClosed[id]) {
            // Just wait for doorStatus updates
            return;
        }

        // 3. Doors are closed, dispatch towards floor 1 if not already moving
        if (state != ElevatorState.MOVING_UP && state != ElevatorState.MOVING_DOWN) {
            if (floor > FIRE_RECALL_FLOOR) {
                // Move DOWN to floor 1
                bus.publish(new Message(SoftwareBusCodes.carDispatch, id, SoftwareBusCodes.down));
                bus.publish(new Message(SoftwareBusCodes.displayDirection, id, SoftwareBusCodes.down));
                states[id] = ElevatorState.MOVING_DOWN;
                System.out.println("FIRE: E" + id + " recalling DOWN to floor " + FIRE_RECALL_FLOOR);
            } else if (floor < FIRE_RECALL_FLOOR) {
                // (Theoretically shouldn't happen, but handle anyway)
                bus.publish(new Message(SoftwareBusCodes.carDispatch, id, SoftwareBusCodes.up));
                bus.publish(new Message(SoftwareBusCodes.displayDirection, id, SoftwareBusCodes.up));
                states[id] = ElevatorState.MOVING_UP;
                System.out.println("FIRE: E" + id + " recalling UP to floor " + FIRE_RECALL_FLOOR);
            }
        }

        // When MotionSimulation + ElevatorMUX drive the car and cabinPosition reaches floor 1,
        // the next call to controlElevatorFire() will hit the top branch (floor == FIRE_RECALL_FLOOR)
        // and open doors there and keep them open.
    }

    private void enterFireMode() {
        currentMode = SystemMode.FIRE;
        System.out.println("FIRE MODE - recalling all elevators to floor " + FIRE_RECALL_FLOOR);

        activeHallCalls.clear();

        for (int i = 1; i <= numElevators; i++) {
            // Cancel any door timers so they don't close doors during FIRE unexpectedly
            cancelDoorTimer(i);

            // Clear normal queues and set recall target
            queues[i].clear();
            queues[i].add(FIRE_RECALL_FLOOR);

            // Disable cabin selections
            bus.publish(new Message(SoftwareBusCodes.selectionsEnable, i, 0));

            // If already at recall floor, open doors now; otherwise close and let FIRE controller handle recall
            if (currentFloors[i] == FIRE_RECALL_FLOOR) {
                bus.publish(new Message(SoftwareBusCodes.carStop, i, 0));
                bus.publish(new Message(SoftwareBusCodes.displayDirection, i, SoftwareBusCodes.none));
                bus.publish(new Message(SoftwareBusCodes.doorControl, i, SoftwareBusCodes.doorOpen));
                states[i] = ElevatorState.DOOR_OPEN;
                System.out.println("FIRE: E" + i + " already at floor 1 - doors opened");
            } else {
                // Ensure doors are closed before motion
                if (doorsOpen[i]) {
                    bus.publish(new Message(SoftwareBusCodes.doorControl, i, SoftwareBusCodes.doorClose));
                    states[i] = ElevatorState.DOOR_CLOSING;
                } else {
                    states[i] = ElevatorState.IDLE; // FIRE controller will dispatch when fully closed
                }
            }
        }
    }

    private void exitFireMode() {
        System.out.println("Exiting FIRE mode - returning to CENTRALIZED");
        currentMode = SystemMode.CENTRALIZED;

        activeHallCalls.clear();

        for (int i = 1; i <= numElevators; i++) {
            // Re-enable selections
            bus.publish(new Message(SoftwareBusCodes.selectionsEnable, i, 1));

            // Clear any leftover FIRE queues
            queues[i].clear();

            // Close doors if open; after that, elevator is idle at its current floor
            if (doorsOpen[i]) {
                bus.publish(new Message(SoftwareBusCodes.doorControl, i, SoftwareBusCodes.doorClose));
                states[i] = ElevatorState.DOOR_CLOSING;
            } else {
                states[i] = ElevatorState.IDLE;
            }
        }
    }

    private void cancelDoorTimer(int id) {
        try {
            doorTimers[id].cancel();
        } catch (Exception ignored) {}
        doorTimers[id] = new Timer(true);
    }

    private int findBestElevator(int targetFloor) {
        int best = 1;
        int minDist = Integer.MAX_VALUE;

        for (int i = 1; i <= numElevators; i++) {
            if (states[i] == ElevatorState.IDLE && queues[i].isEmpty()) {
                int dist = Math.abs(currentFloors[i] - targetFloor);
                if (dist < minDist) {
                    minDist = dist;
                    best = i;
                }
            }
        }

        return best;
    }

    private void addTarget(int id, int floor) {
        if (!queues[id].contains(floor)) {
            queues[id].add(floor);
            System.out.println("E" + id + " queue: " + queues[id]);
        }
    }

    private void clearHallCallsForFloor(int floor) {
        activeHallCalls.remove(floor + "-UP");
        activeHallCalls.remove(floor + "-DOWN");
        bus.publish(new Message(SoftwareBusCodes.resetCall, 5, floor * 10 + 0)); // UP
        bus.publish(new Message(SoftwareBusCodes.resetCall, 5, floor * 10 + 1)); // DOWN
    }

    public void stop() {
        running = false;
    }
}