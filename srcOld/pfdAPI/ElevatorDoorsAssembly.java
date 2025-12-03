package pfdAPI;

import pfdGUI.gui;

/**
 * Class that defines the functionality of the Elevator doors. Represents
 * the pair of doors that open to a specific elevator on each floor.
 * API:
 *      public void open()
 *      public void close()
 *      public boolean isObstructed()
 *      public boolean isFullyOpen()
 *      public boolean isFullyClosed()
 */
public class ElevatorDoorsAssembly {
    private boolean isOpen;
    private boolean isClosed;
    // True when an obstruction is placed
    private boolean isObstructed;
    private boolean isMoving;

    // GUI Control reference
    private final gui.GUIControl guiControl;
    // Car ID reference
    private final int carId;

    /**
     * Constructor of the ElevatorDoorsAssembly.
     */
    public ElevatorDoorsAssembly(int carId, gui.GUIControl guiControl) {
        this.carId = carId;
        this.guiControl = guiControl;
        this.isOpen = true;
        this.isClosed = false;
        this.isObstructed = false;
        this.isMoving = false;
    }

    /**
     * Commands the door assembly to open.
     * If an obstruction is detected, opening is halted automatically.
     */
    public synchronized void open(){
        if (!isOpen) {
            isMoving = true;
            System.out.println("[Doors] Opening...");
            simulateDelay(2000);
            isOpen = true;
            guiControl.changeDoorState(carId, isOpen);
            isMoving = false;
            isClosed = false;
            System.out.println("[Doors] Fully open.");

        }
    }


    /**
     * Commands the door assembly to close.
     * If obstruction occurs during closing, doors reopen automatically.
     */
    public synchronized void close() {
        isObstructed();
        if (isOpen) {
            isMoving = true;
            System.out.println("[Doors] Closing...");
            simulateDelay(2000);
            isOpen = false;
            isClosed = true;
            guiControl.changeDoorState(carId, isOpen);

            if(isObstructed){
                isOpen = true;
                isClosed = false;
                System.out.println("[Doors] Obstruction detected. Reopened.");
            } else {
                System.out.println("[Doors] Fully closed.");
            }
            isMoving = false;
        }
    }

    /**
     * Simulates time delay for the open and close animations. For simulation purposes.
     * @param ms time to be elapsed
     */
    private synchronized void simulateDelay(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Returns whether the doors are currently obstructed.
     * @return boolean isObstructed
     */
    public synchronized boolean isObstructed() {
        isObstructed = guiControl.getIsDoorObstructed(carId);
        return isObstructed;
    }

    /**
     * Sets obstruction state manually for simulation/testing.
     * @param obstructed whether the GUI has the obstruction box present
     */
    public synchronized void setObstruction(boolean obstructed) {
        this.isObstructed = obstructed;
        guiControl.setDoorObstruction(carId, obstructed);
    }

    /**
     * Returns whether the doors are completely open (not closed, not half-open).
     * Elevator cannot be allowed to move.
     * @return boolean isOpen
     */
    public synchronized boolean isFullyOpen() {
        return isOpen;
    }

    /**
     * Returns whether the doors are completely closed (not open, not half-open).
     * Elevator can now move.
     * @return boolean isOpen
     */
    public synchronized boolean isFullyClosed() {
        return isClosed;
    }


}