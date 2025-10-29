package TestCode;

public class Main {
    public static void main(String[] args) {
        Elevator elevator = new Elevator();
        System.out.println("Initial Elevator State");
        displayElevatorStatus(elevator);
        System.out.println("\nTesting Normal Operations");
        //requests to go to floor 5
        try {
            System.out.println("Requesting floor 5");
            elevator.request(5);
            System.out.println("To floor " + elevator.getDestinationFloor());
            simulateMovement(elevator);
            displayElevatorStatus(elevator);
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }

        // requests to go to floor 3
        try {
            System.out.println("\nRequesting floor 3");
            elevator.request(3);
            System.out.println("To floor " + elevator.getDestinationFloor());
            simulateMovement(elevator);
            displayElevatorStatus(elevator);
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
        // requests current floor
        // should open doors if so
        try {
            System.out.println("\nRequesting current floor (3)");
            elevator.request(3);
            displayElevatorStatus(elevator);
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
        System.out.println("\nTesting Fire Mode");
        elevator.setMode("FIRE");
        displayElevatorStatus(elevator);
        // requests during fire mode
        // should ignore requests
        try {
            System.out.println("Trying to request floor 8 during fire mode");
            elevator.request(8);
        } catch (Exception e) {
            System.out.println("Expected Error: " + e.getMessage());
        }
        System.out.println("\nTesting Auto Mode");
        elevator.setMode("AUTO");
        displayElevatorStatus(elevator);
        System.out.println("\nTesting Invalid Floor Request");
        try {
            System.out.println("Requesting floor 15 (invalid)");
            elevator.request(15);
        } catch (Exception e) {
            System.out.println("Expected Error: " + e.getMessage());
        }
        System.out.println("\nTesting Elevator off State");
        elevator.setStatus("OFF");
        displayElevatorStatus(elevator);
        // request when elevator is off
        // should ignore requests
        try {
            System.out.println("Trying to request floor 7 when elevator is off");
            elevator.request(7);
        } catch (Exception e) {
            System.out.println("Expected Error: " + e.getMessage());
        }
    }
    /**
     * helper method displays current elevator status
     */
    private static void displayElevatorStatus(Elevator elevator) {
        System.out.println("Current Floor: " + elevator.getCurrentFloor());
        System.out.println("Destination Floor: " + elevator.getDestinationFloor());
        System.out.println("Moving: " + elevator.isMoving());
        System.out.println("Door Open: " + elevator.isDoorOpen());
        System.out.println("Direction: " + elevator.getDirection());
        System.out.println("Status: " + elevator.getStatus());
        System.out.println("Mode: " + elevator.getMode());
    }
    /**
     * helper method simulates elevator movement between floors
     */
    private static void simulateMovement(Elevator elevator) {
        elevator.setMoving(true);
        int startFloor = elevator.getCurrentFloor();
        int destination = elevator.getDestinationFloor();
        System.out.print("From floor " + startFloor + " to " + destination + ": ");
        // floor by floor movement
        while (elevator.getCurrentFloor() != destination) {
            elevator.moveElevator();
            System.out.print(elevator.getCurrentFloor() + " ");
        }
        System.out.println("\nArrived at floor " + elevator.getCurrentFloor());
    }
}