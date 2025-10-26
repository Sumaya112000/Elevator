package TestCode;

public class Elevator {
    public int CurrentFloor;
    public int DestinationFloor;
    public boolean moving;
    public boolean doorOpen;
    public String direction;
    public String status;

    public Elevator() {
        CurrentFloor = 0;
        DestinationFloor = 0;
        moving = false;
        doorOpen = false;
        direction = "UP";
        status = "ON";
    }

    /**
     * Getter to return current floor the elevator is on
     * @return the number of the floor
     */
    public int getCurrentFloor() { return CurrentFloor; }

    /**
     * Getter to return the destination the elevator is going to
     * @return the number of the floor in the request
     */
    public int getDestinationFloor() { return DestinationFloor; }

    /**
     * Getter to return weather or not the elevator is moving
     * @return boolean expression in moving variable
     */
    public boolean isMoving() { return moving; }

    /**
     * Getter to determine if the door is open/closed
     * @return boolean in doorOpen variable
     */
    public boolean isDoorOpen() { return doorOpen; }

    /**
     * Getter to return the current set direction
     * @return a string containing Up or down
     */
    public String getDirection() { return direction; }

    /**
     * Getter to return the status of the elevator
     * @return "ON" and "OFF"
     */
    public String getStatus() { return status; }

    /**
     * Setter to change the floor
     * @param floor the floor the elevator is currently at
     */
    public void setCurrentFloor(int floor) { CurrentFloor = floor; }

    /**
     * Setter to set status
     * @param status "ON" and "OFF"
     */
    public void setStatus(String status) {
        this.status = status;

         // If we turn the elevator off:
         // go down to first floor and open doors
        if (status.equals("OFF")) {
            if(CurrentFloor != 1){
                try{
                    request(1);
                } catch (Exception e){
                    String message = e.getMessage();
                    System.out.println(message);
                }
                CurrentFloor = 1;
            }
            doorOpen = true;
        }
    }

    /**
     * Setter to set the destination floor
     * @param floor the floor number in the request
     */
    public void setDestinationFloor(int floor) { DestinationFloor = floor; }

    /**
     * Setter to change movement status
     * @param moving true for elevator is moving, false otherwise
     */
    public void setMoving(boolean moving) { this.moving = moving; }

    /**
     * Setter to change door from open to close and vise versa
     * @param doorOpen true for open, false otherwise
     */
    public void setDoorOpen(boolean doorOpen) { this.doorOpen = doorOpen; }

    /**
     * Setter to assign direction
     * @param direction "UP" or "DOWN"
     */
    public void setDirection(String direction) { this.direction = direction; }

    /**
     * Function to simulate a request
     * @param destFloor the floor the elevator is going to
     */
    public void request(int destFloor) throws Exception {
        // validate the requested floor
        if(destFloor > 10 || destFloor < 1){
            throw new Exception("Invalid Floor Number");
        }

        // if the requested floor is the current floor, open door
        if (destFloor == CurrentFloor) {
            doorOpen = true;
            return;
        }

        // if door is open close it
        if(isDoorOpen()) {
            doorOpen = false;
        }
        // set destination floor
        DestinationFloor = destFloor;

        // set direction
        if(DestinationFloor > CurrentFloor) {
            direction = "UP";
        } else if(DestinationFloor < CurrentFloor) {
            direction = "DOWN";
        }

        // move the elevator
        moveElevator();
    }

    /**
     * Move the elevator to the destination floor
     */
    public void moveElevator() {
        if (moving) {
            if (direction.equals("UP")) {
                CurrentFloor++;
            } else if (direction.equals("DOWN")) {
                CurrentFloor--;
            }
            // Check if reached destination
            if (CurrentFloor == DestinationFloor) {
                moving = false;
                doorOpen = true;
            }
        }
    }
}
