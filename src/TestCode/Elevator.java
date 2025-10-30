package TestCode;

public class Elevator {

    public enum Status { ON, OFF }
    public enum Mode { NORMAL, FIRE, AUTO }
    public enum Direction { UP, DOWN, IDLE }

    private static final int MIN_FLOOR = 1;
    private static final int MAX_FLOOR = 10;

    private int currentFloor;
    private int destinationFloor;
    private boolean moving;
    private boolean doorOpen;
    private Direction direction;
    private Status status;
    private Mode mode;

    public Elevator() {
        this.currentFloor = MIN_FLOOR;       // start at floor 1
        this.destinationFloor = MIN_FLOOR;
        this.moving = false;
        this.doorOpen = false;
        this.direction = Direction.IDLE;
        this.status = Status.ON;
        this.mode = Mode.NORMAL;
    }

    // -------- Getters
    public int getCurrentFloor() { return currentFloor; }
    public int getDestinationFloor() { return destinationFloor; }
    public boolean isMoving() { return moving; }
    public boolean isDoorOpen() { return doorOpen; }
    public Direction getDirection() { return direction; }
    public Status getStatus() { return status; }
    public Mode getMode() { return mode; }

    // -------- Controlled setters (internal safety)
    public void setCurrentFloor(int floor) { this.currentFloor = floor; }
    public void setDoorOpen(boolean doorOpen) { this.doorOpen = doorOpen; }
    public void setMoving(boolean moving) { this.moving = moving; }
    public void setDirection(Direction direction) { this.direction = direction; }
    public void setDestinationFloor(int floor) { this.destinationFloor = floor; }

    public void setStatus(String statusText) {
        Status newStatus = Status.valueOf(statusText.toUpperCase());
        this.status = newStatus;

        // If turned OFF: recall to 1st floor and open doors
        if (this.status == Status.OFF) {
            if (currentFloor != MIN_FLOOR) {
                this.mode = Mode.NORMAL;
                forceGoToFirstFloor();
            }
            doorOpen = true;
            moving = false;
            direction = Direction.IDLE;
        }
    }

    public void setMode(String modeText) {
        Mode newMode = Mode.valueOf(modeText.toUpperCase());
        this.mode = newMode;

        if (this.mode == Mode.FIRE) {
            // Fire recall
            forceGoToFirstFloor();
            doorOpen = true;
            moving = false;
            direction = Direction.IDLE;
            this.status = Status.ON; // powered but restricted
        } else if (this.mode == Mode.AUTO) {
            this.status = Status.ON;
        } else {
            this.status = Status.ON;
        }
    }

    /**
     * Handle a floor request.
     * @param destFloor desired floor (1..10)
     * @throws Exception if invalid or not allowed in current mode/status
     */
    public void request(int destFloor) throws Exception {
        if (status == Status.OFF) throw new Exception("Elevator is OFF. Request denied.");
        if (destFloor < MIN_FLOOR || destFloor > MAX_FLOOR) throw new Exception("Invalid Floor Number");

        if (mode == Mode.FIRE) {
            if (destFloor != MIN_FLOOR) throw new Exception("Elevator is in FIRE mode (only floor 1 allowed).");
            if (currentFloor == MIN_FLOOR) {
                doorOpen = true; moving = false; direction = Direction.IDLE; destinationFloor = MIN_FLOOR;
                return;
            }
        }

        if (destFloor == currentFloor) {
            doorOpen = true; moving = false; direction = Direction.IDLE; destinationFloor = currentFloor;
            return;
        }

        if (doorOpen) doorOpen = false;
        destinationFloor = destFloor;
        direction = (destinationFloor > currentFloor) ? Direction.UP : Direction.DOWN;
        moving = true;
    }

    /** Advance one floor toward destination (simulation step). */
    public void moveElevator() {
        if (!moving) return;

        if (direction == Direction.UP) {
            currentFloor = Math.min(currentFloor + 1, MAX_FLOOR);
        } else if (direction == Direction.DOWN) {
            currentFloor = Math.max(currentFloor - 1, MIN_FLOOR);
        }

        if (currentFloor == destinationFloor) {
            moving = false;
            direction = Direction.IDLE;
            doorOpen = true;
        }
    }

    // -------- Helpers
    private void forceGoToFirstFloor() {
        doorOpen = false;
        destinationFloor = MIN_FLOOR;
        direction = (currentFloor > MIN_FLOOR) ? Direction.DOWN : Direction.IDLE;
        moving = (currentFloor != MIN_FLOOR);
        while (moving) moveElevator();
    }
}