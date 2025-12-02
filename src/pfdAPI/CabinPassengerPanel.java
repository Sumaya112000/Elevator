package pfdAPI;

import java.util.ArrayList;
import java.util.List;
import pfdGUI.gui;

/**
 * Device inside of elevators that allows for user-interaction. Allows cabin riders to
 * select destination floors, view the elevator's current floor and direction, and
 * interact with the fire key.
 * API:
 *      public int getPressedFloor()
 *      public void clearPressedFloors()
 *      public void resetFloorButton(int floorNumber)
 *      public void setDisplay(int currentFloor, String direction)
 *      public void playCabinArrivalChime()
 *      public void playCabinOverloadWarning()
 *      public boolean isFireKeyActive()
 */
public class CabinPassengerPanel implements CabinPassengerPanelAPI {

    // The total number of floors (=10)
    private final int totalFloors;
    // Array of the pressed state of each floor button. True when customer presses,
    // requesting service
    private final boolean[] floorButtons;
    // Array of the queue of pressed floor buttons
    private final List<Integer> pressedFloorsQueue;
    // The current floor the elevator is on/nearest
    private int currentFloor;
    // The current direction the elevator is moving in. "UP" "DOWN" and "IDLE
    private String direction;
    // State of the fire key; is active/is not active
    private boolean fireKeyActive;
    // The ID of the associated elevator
    private final int carId;
    // GUI Control reference
    private final gui.GUIControl guiControl;

    /**
     * Constructor of the CabinPassengerPanel.
     * @param totalFloors Number of floors in the building (=10)
     */
    public CabinPassengerPanel(int carId, int totalFloors, gui.GUIControl guiControl) {
        this.carId = carId;
        this.guiControl = guiControl;
        this.totalFloors = totalFloors;
        this.floorButtons = new boolean[totalFloors];
        this.pressedFloorsQueue = new ArrayList<>();
        this.currentFloor = 1;
        this.direction = "IDLE";
        this.fireKeyActive = false;
    }

    /**
     * Function for simulating pressing a floor button.
     * FOR GUI SIMULATION PURPOSES
     * @param floorNumber The floor being requested
     */
    public synchronized void pressFloorButton(int floorNumber) {
        if (floorNumber >= 1 && floorNumber <= totalFloors && !floorButtons[floorNumber - 1]) {
            floorButtons[floorNumber - 1] = true;
            pressedFloorsQueue.add(floorNumber);
            guiControl.pressPanelButton(carId, floorNumber);
        }
    }

    /**
     * Returns the most recent pressed floor number since the last poll.
     * Requests made by the riders must be serviced when not in emergency mode.
     * @return top int floor of the pressedFloorsQueue
     */
    @Override
    public synchronized int getPressedFloor() {
        if (pressedFloorsQueue.isEmpty()) {
            return 0;
        }
        return pressedFloorsQueue.remove(0); // Remove and return the head
    }

    /**
     * Clears all stored pressed floor events.
     *  Decide: should this also be called upon button disables or single/multiple mode switches?
     */
    @Override
    public synchronized void clearPressedFloors() {
        pressedFloorsQueue.clear();
        guiControl.resetPanel(carId);
    }

    /**
     * Disables/Enables the floor selection buttons.
     * DOES NOT RESET ANY BUTTONS AUTOMATICALLY.
     * @param disabled, 0 = disable 1 = enable
     */
    public synchronized void setButtonsDisabled(int disabled){
        if(disabled == 0){
            guiControl.setPanelButtonsDisabled(carId,  true);
        }else {
            guiControl.setPanelButtonsDisabled(carId,  false);
        }
    }

    /**
     * Sets the selection mode on the floor selection buttons to
     * single/multiple. Upon setting the mode to single, always
     * allows 1 more selection, even if some buttons are already active.
     * DOES NOT RESET ANY BUTTONS AUTOMATICALLY.
     * @param single, 0 = single mode 1 = multiple mode
     */
    public synchronized void setButtonsSingle(int single){
        if(single == 0){
            guiControl.setSingleSelection(carId, true);
        }else{
            guiControl.setSingleSelection(carId, false);
        }
    }

    /**
     * Resets a specific floor button's indicator. Resets occur after the travel
     * request has been serviced.
     * @param floorNumber the reset floor button's associated floor
     */
    @Override
    public synchronized void resetFloorButton(int floorNumber) {
        if (floorNumber >= 1 && floorNumber <= totalFloors) {
            floorButtons[floorNumber - 1] = false;
            guiControl.resetPanelButton(carId, floorNumber);
            System.out.println("DEBUG: Reset floor button " + floorNumber + " for elevator " + carId);


        }
    }

    /**
     * Updates the cabin display to show the current floor and direction. Must be updated
     * when either of these two aspects change.
     * @param currentFloor Location of the elevator
     * @param direction Direction the cabin is moving in
     */
    @Override
    public synchronized void setDisplay(int currentFloor, String direction) {
        this.currentFloor = currentFloor;
        this.direction = direction;
        guiControl.setDisplay(carId, currentFloor, direction);
        System.out.println("Display: Floor " + currentFloor + " | Direction: " + direction);
    }

    /**
     * Plays the arrival chime sound. Called when travel requests have been successfully serviced.
     * NOTE: Never used. API remains for logical purposes, but useless in the actual GUI.
     */
    @Override
    public void playCabinArrivalChime() {
        System.out.println("*Ding!* Also played in the cabin of Elevator " + carId + "!");
    }

    /**
     * Plays the overload warning buzz. Called when the GUI option for overload is selected.
     * In this state, the elevator cannot move and the doors will remain open.
     * NOTE: Never used. API remains for logical purposes, but useless in the actual GUI.
     */
    @Override
    public synchronized void playCabinOverloadWarning() {
        System.out.println("*Buzz!* Also played in the cabin of Elevator " + carId + "!");
    }

    /**
     * Reads the fire key state. When the fire key is active, the elevator panel
     * should be set to single selection mode. When inactive, the panel is
     * disabled during fire mode.
     * @return boolean fireKeyActive
     */
    @Override
    public synchronized boolean isFireKeyActive() {
        return guiControl.getIsFireKeyActive(carId);
    }

    /**
     * Toggles the fire key for simulation purposes.
     */
    public synchronized void toggleFireKey() {
        this.fireKeyActive = !this.fireKeyActive;
        guiControl.setFireAlarm(fireKeyActive);
    }
}