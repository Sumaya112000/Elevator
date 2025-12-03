package pfdAPI;

import javafx.application.Platform;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import pfdGUI.gui;

import java.net.URL;

/**
 * Class that defines the functionality of the Elevator floor displays. Represents
 * the panel above elevator doors that show the elevator's location and direction of movement.
 * API:
 *      public void updateFloorIndicator(int currentFloor, String direction)
 *      public void playArrivalChime()
 *      public void playOverLoadWarning()
 * For GUI purposes:
 *      public static void setGuiListener(gui.listener l)
 */
public class ElevatorFloorDisplay {
    // The current floor location of the elevator
    private int currentFloor;
    // The current movement direction of the elevator, "UP" "DOWN" "IDLE"
    private String direction;
    // GUI Control reference
    private final gui.GUIControl guiControl;
    // The ID of the associated elevator
    private final int carId;

    /**
     * Constructs the ElevatorFloorDisplay.
     * @param carId the ID of the associated elevator
     */
    public ElevatorFloorDisplay(int carId, gui.GUIControl guiControl) {
        this.carId = carId;
        this.guiControl = guiControl;
        this.currentFloor = 1;
        this.direction = "IDLE";
    }

    /**
     * Updates the display to show the elevator's current floor and direction.
     * @param currentFloor the floor currently displayed
     * @param direction the direction the elevator is going (UP/DOWN/IDLE)
     */
    public synchronized void updateFloorIndicator(int currentFloor, String direction) {
        this.currentFloor = currentFloor;
        this.direction = direction;
        guiControl.setDisplay(carId, currentFloor, direction);
    }

    /**
     * Simulates the arrival noise.
     */
    public synchronized void playArrivalChime() {
        // again simulating the Ding noise
        System.out.println("*Ding! Elevator " + carId + " has arrived.");
        Platform.runLater(() -> {
            try {
                URL sound = getClass().getResource("Elevator/res/sounds/ding.mp3");
                if (sound == null) {
                    System.err.println("Sound file not found.");
                    return;
                }

                Media media = new Media(sound.toExternalForm());
                MediaPlayer player = new MediaPlayer(media);
                player.play();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Simulates the overload buzz.
     */
    public synchronized void playOverLoadWarning() {
        // simulating the buzzing noise
        System.out.println("*Buzz! Warning: Overload detected on Elevator " + carId);
        Platform.runLater(() -> {
            try {
                URL sound = getClass().getResource("Elevator/res/sounds/buzz.mp3");
                if (sound == null) {
                    System.err.println("Sound file not found.");
                    return;
                }

                Media media = new Media(sound.toExternalForm());
                MediaPlayer player = new MediaPlayer(media);
                player.play();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Returns whether the Cabin is overloaded.
     * Why is this in the display specifically? IDK!
     * @return true when Overload is selected in GUI, otherwise false
     */
    public boolean isOverloaded(){
        return guiControl.getIsCabinOverloaded(carId);
    }
}