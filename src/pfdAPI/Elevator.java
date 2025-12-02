package pfdAPI;

import pfdGUI.gui;

/**
 * Class that defines a given Elevator (4 in total).
 * Elevators each have their own doors and floor displays for
 * the sake of simplicity (in actuality, would exist on all 10 floors,
 * but still ascribed to a specific elevator).
 */
public class Elevator {
    // ID of the elevator
    public final int carId;
    // ID of the elevator
    public final int totalFloors;
    // The elevator's doors
    public final ElevatorDoorsAssembly door;
    // The elevator's passenger panel
    public final CabinPassengerPanel panel;
    // The elevator's floor display
    public final ElevatorFloorDisplay display;

    /**
     * Constructs an Elevator.
     * @param carId the ID of the elevator (1-4)
     * @param totalFloors the number of floors in the building (=10)
     */
    public Elevator(int carId, int totalFloors) {
        gui g = gui.getInstance();
        this.carId = carId;
        this.totalFloors = totalFloors;
        this.door  = new ElevatorDoorsAssembly(carId, g.internalState);
        this.panel  = new CabinPassengerPanel(carId, totalFloors, g.internalState);
        this.display = new ElevatorFloorDisplay(carId, g.internalState);
    }
}