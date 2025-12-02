package elevatorController.Util;

public record FloorNDirection(int floor, Direction direction) {

    public int getFloor() {
        return floor;
    }

    public Direction getDirection() {
        return direction;
    }
}