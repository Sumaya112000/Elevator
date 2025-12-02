package pfdAPI;
import pfdGUI.gui;

/**
 * Class that defines the functionality of the Floor Call Buttons. Represents
 * the pair of buttons on each floor that allow users to call an elevator for a
 * specific travel directions, UP/DOWN.
 * API:
 *      public boolean isUpCallPressed()
 *      public boolean isDownCallPressed()
 *      public void resetCallButton(String direction)
 * For GUI purposes:
 *      public void pressUpCall()
 *      public void pressDownCall()
 *      public static void setGuiListener(gui.listener l)
 */
public class FloorCallButtons implements FloorCallButtonsAPI {

    // Which landing this panel belongs to
    private final int floorNumber;
    // Total number of building floors (=10)
    private final int totalFloors;
    // Top floor has no Up
    private final boolean hasUp;
    // Bottom floor has no Down
    private final boolean hasDown;
    // True if Up call is active
    private boolean upPressed;
    // True if Down call is active
    private boolean downPressed;
    private final gui.GUIControl guiControl;

    /**
     * Constructs the floor call button panel.
     * @param floorNumber the floor the panel is located on
     * @param totalFloors total number of floors in the building (=10)
     */
    public FloorCallButtons(int floorNumber, int totalFloors, gui.GUIControl guiControl) {
        this.guiControl = guiControl;
        this.floorNumber = floorNumber;
        this.totalFloors = totalFloors;
        this.hasUp = floorNumber < totalFloors;
        this.hasDown = floorNumber > 1;
        this.upPressed = false;
        this.downPressed = false;
    }

    /**
     * Set the fire alarm status
     * @param status true if fire alarm is active, false otherwise
     */
    public synchronized void setFireAlarm(boolean status) { guiControl.setFireAlarm(status); }

    /**
     * Get the fire alarm status
     */
    public synchronized boolean getFireAlarmStatus() { return guiControl.getFireAlarm(); }

    /**
     * Returns whether the Up request button has been pressed. Inactive on the top floor.
     * @return boolean hasUp (false when top floor) && upPressed
     */
    @Override
    public synchronized boolean isUpCallPressed() {
        return guiControl.isCallButtonActive(floorNumber, "UP");
    }

    /**
     * Returns whether the Down request button has been pressed. Inactive on the bottom floor.
     * @return boolean hasDown (false when bottom floor) && downPressed
     */
    @Override
    public synchronized boolean isDownCallPressed() {
        return guiControl.isCallButtonActive(floorNumber, "DOWN");
    }

    /**
     * Reset the specified call indicator ("Up" or "Down") after service.
     * Both must be reset upon fire mode activation.
     * @param direction the button to be reset
     */
    @Override
    public synchronized void resetCallButton(String direction) {
        guiControl.resetCallButton(floorNumber, direction);
    }

    /**
     * Set EVERY button panel to disabled or enabled.
     * Applies to the entire building, despite being 1 panel. So only called on 1!
     * @param enabled, 0 = disabled 1 = enabled
     */
    public synchronized void setButtonsEnabled(int enabled){
        if(enabled == 1){
            guiControl.setCallButtonsDisabled(false);
        }else{
            guiControl.setCallButtonsDisabled(true);
        }
    }
}