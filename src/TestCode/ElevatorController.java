package TestCode;

import java.util.NavigableSet;
import java.util.TreeSet;

public class ElevatorController {

    private final Elevator car;
    private final NavigableSet<Integer> upStops = new TreeSet<>();
    private final NavigableSet<Integer> downStops = new TreeSet<>();
    private int doorDwellTicks = 0;

    public ElevatorController(Elevator car) { this.car = car; }
    public Elevator getCar() { return car; }

    public void addCarRequest(int floor) throws Exception {
        validateFloor(floor);
        queueStop(floor);
    }

    public void addHallRequest(int floor, Elevator.Direction dir) throws Exception {
        validateFloor(floor);
        if (dir == Elevator.Direction.UP) upStops.add(floor);
        else if (dir == Elevator.Direction.DOWN) downStops.add(floor);
    }

    public void clearAllRequests() { upStops.clear(); downStops.clear(); }

    /** Advance one simulation step (no GUI needed). */
    public void tick() {
        if (car.getMode() == Elevator.Mode.FIRE) return;

        // Doors: dwell; only close if more work remains
        if (car.isDoorOpen()) {
            if (doorDwellTicks > 0) { doorDwellTicks--; return; }
            if (!hasPendingStops()) { car.setMoving(false); car.setDirection(Elevator.Direction.IDLE); return; }
            car.setDoorOpen(false);
        }

        final int here = car.getCurrentFloor();

        boolean serviced = false;
        if (car.getDirection() == Elevator.Direction.UP || car.getDirection() == Elevator.Direction.IDLE) {
            if (upStops.contains(here)) { upStops.remove(here); stopHere(); serviced = true; }
        }
        if (!serviced && (car.getDirection() == Elevator.Direction.DOWN || car.getDirection() == Elevator.Direction.IDLE)) {
            if (downStops.contains(here)) { downStops.remove(here); stopHere(); serviced = true; }
        }
        if (serviced) return;

        Elevator.Direction dir = car.getDirection();
        if (dir == Elevator.Direction.IDLE) {
            Integer upNext = upStops.ceiling(here);
            Integer downNext = downStops.floor(here);
            if (upNext != null && downNext != null) {
                dir = (Math.abs(upNext - here) <= Math.abs(here - downNext)) ? Elevator.Direction.UP : Elevator.Direction.DOWN;
            } else if (upNext != null) dir = Elevator.Direction.UP;
            else if (downNext != null) dir = Elevator.Direction.DOWN;
            else return; // nothing to do
            car.setDirection(dir);
            car.setMoving(true);
        }

        if (dir == Elevator.Direction.UP) {
            Integer target = upStops.ceiling(here);
            if (target == null) {
                if (!downStops.isEmpty()) { car.setDirection(Elevator.Direction.DOWN); return; }
                car.setMoving(false); car.setDirection(Elevator.Direction.IDLE); return;
            }
            car.setMoving(true);
            car.setDoorOpen(false);

            // step one floor
            car.moveElevator();

            // NEW: service immediately if we landed on an UP stop
            int now = car.getCurrentFloor();
            if (upStops.contains(now)) {
                upStops.remove(now);
                stopHere();
                return;
            }

        } else if (dir == Elevator.Direction.DOWN) {
            Integer target = downStops.floor(here);
            if (target == null) {
                if (!upStops.isEmpty()) { car.setDirection(Elevator.Direction.UP); return; }
                car.setMoving(false); car.setDirection(Elevator.Direction.IDLE); return;
            }
            car.setMoving(true);
            car.setDoorOpen(false);

            // step one floor
            car.moveElevator();

            // NEW: service immediately if we landed on a DOWN stop
            int now = car.getCurrentFloor();
            if (downStops.contains(now)) {
                downStops.remove(now);
                stopHere();
                return;
            }
        }
    }

    private void stopHere() {
        car.setMoving(false);
        car.setDoorOpen(true);
        car.setDirection(Elevator.Direction.IDLE);
        doorDwellTicks = 2; // small dwell for testing
    }

    private void queueStop(int floor) {
        int here = car.getCurrentFloor();
        if (floor > here) upStops.add(floor);
        else if (floor < here) downStops.add(floor);
        else { // same floor → open door now
            car.setDoorOpen(true);
            car.setMoving(false);
            car.setDirection(Elevator.Direction.IDLE);
        }
    }

    private boolean hasPendingStops() {
        return !upStops.isEmpty() || !downStops.isEmpty();
    }

    private void validateFloor(int f) throws IllegalArgumentException {
        if (f < 1 || f > 10) throw new IllegalArgumentException("Invalid floor " + f);
    }
}