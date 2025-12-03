package pfdAPI;

import pfdGUI.gui;

/**
 * Class that defines the Building, with 10 floors, 10 floor call buttons,
 * and a fire alarm (inside call button objects).
 */
public class Building {

    // The building's elevator call buttons on each floor
    public final FloorCallButtons[] callButtons;
    public final int totalFloors;

    /**
     * Constructs a Building.
     * @param totalFloors the number of floors in the building (=10)
     */
    public Building(int totalFloors) {
        this.totalFloors = totalFloors;
        gui g = gui.getInstance();
        this.callButtons = new FloorCallButtons[totalFloors];
        for (int i = 0; i < totalFloors; i++) {
            this.callButtons[i] = new FloorCallButtons(i+1, totalFloors, g.internalState);
        }
    }
}