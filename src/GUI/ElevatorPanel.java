import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;
import javafx.geometry.Pos;
import javafx.geometry.Insets;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a single, complete elevator unit within the simulation.
 * This includes the visual shaft, the moving car, all floor call buttons,
 * and the per-elevator status indicators and controls (START/STOP).
 * It communicates with the central ElevatorControlSystem for global state.
 */
public class ElevatorPanel extends VBox {

    public enum Direction { UP, DOWN, IDLE }

    private final int elevatorId;
    private int currentFloor;
    private Direction currentDirection = Direction.IDLE;
    private boolean isMoving = false; // Prevents multiple calls
    private final ElevatorControlSystem system;

    // State for the per-elevator STOP/START button
    private boolean isEnabled = true;
    private Button mainControlButton;
    private String btnText_START = "START";
    private String btnColor_START = "-fx-background-color: #228B22;"; // Green
    private String btnText_STOP = "STOP";
    private String btnColor_STOP = "-fx-background-color: #B22222;"; // Red

    // Layout Panes
    private StackPane shaftPane;
    private VBox floorButtonColumn;
    private Pane carPane;
    private VBox movingCar;
    private Label carFloorLabel;

    // Animation
    private TranslateTransition elevatorAnimation;

    // UI Components
    private final ConcurrentHashMap<Integer, DualDotIndicatorPanel> floorCallIndicators = new ConcurrentHashMap<>();
    private final DirectionIndicatorPanel directionIndicator;
    private final Label currentFloorDisplay; // The static display at the top

    // Constants
    private static final double FLOOR_HEIGHT = 30.0;
    private static final double FLOOR_SPACING = 3.0;
    private static final double TOTAL_FLOOR_HEIGHT = FLOOR_HEIGHT + FLOOR_SPACING; // 33.0
    private static final double ANIMATION_SPEED_PER_FLOOR = 400.0;

    /*
     * Inner classes for UI components
     * DualDotIndicatorPanel: The (o) (o) call dots for each floor.
     * DirectionIndicatorPanel: The (▲) (▼) direction display at the top.
     * (Functionally unchanged from previous versions).
     */

    /**
     * A small VBox holding the 'up' and 'down' call dots for a single floor.
     */
    private class DualDotIndicatorPanel extends VBox {
        private Circle upDot = new Circle(3, Color.web("#505050"));
        private Circle downDot = new Circle(3, Color.web("#505050"));
        public DualDotIndicatorPanel(int floor, ElevatorPanel parentPanel) {
            super(6); // Spacing
            getChildren().addAll(upDot, downDot);
            setAlignment(Pos.CENTER);
            setPadding(new Insets(0, 5, 0, 5));
            upDot.setOnMouseClicked(e -> parentPanel.callElevator(floor, Direction.UP));
            downDot.setOnMouseClicked(e -> parentPanel.callElevator(floor, Direction.DOWN));
        }
        public void setDotLit(Direction direction, boolean lit) {
            Color color = lit ? Color.WHITE : Color.web("#505050");
            if (direction == Direction.UP) upDot.setFill(color);
            else if (direction == Direction.DOWN) downDot.setFill(color);
        }
    }

    /**
     * A small VBox holding the 'up' and 'down' triangles showing car direction.
     */
    private class DirectionIndicatorPanel extends VBox {
        private Polygon upTriangle, downTriangle;
        private final Color UNLIT_COLOR = Color.BLACK;
        public DirectionIndicatorPanel() {
            super(6); // Spacing
            upTriangle = new Polygon(6.0, 0.0, 0.0, 8.0, 12.0, 8.0);
            downTriangle = new Polygon(6.0, 8.0, 0.0, 0.0, 12.0, 0.0);
            setDirection(Direction.IDLE);
            getChildren().addAll(upTriangle, downTriangle);
            setAlignment(Pos.CENTER);
            setPadding(new Insets(5));
        }
        public void setDirection(Direction newDirection) {
            upTriangle.setFill(newDirection == Direction.UP ? Color.WHITE : UNLIT_COLOR);
            downTriangle.setFill(newDirection == Direction.DOWN ? Color.WHITE : UNLIT_COLOR);
        }
    }


    /**
     * Constructs a single elevator panel, including its controls, shaft, and car.
     *
     * @param id     The identifier for this elevator (e.g., 1, 2, 3, 4).
     * @param system A reference to the main control system for global state.
     */
    public ElevatorPanel(int id, ElevatorControlSystem system) {
        super(3); // Spacing for top controls
        this.elevatorId = id;
        this.system = system;
        setAlignment(Pos.CENTER);
        setStyle("-fx-background-color: #333333;");
        setPrefWidth(100);

        // 1. Setup top controls (Title, STOP/START, floor display)
        this.currentFloor = (id == 1) ? 7 : (id == 2) ? 8 : 1;

        // Set initial START/STOP state based on elevator ID
        isEnabled = (id > 2); // Elevators 3 & 4 start as "START" (isEnabled=true)
        String btnText = (id <= 2) ? btnText_STOP : btnText_START;
        String btnColor = (id <= 2) ? btnColor_STOP : btnColor_START;
        // Fix initial state: Elev 1 & 2 are STOPPED, so they are *NOT* enabled
        if (id <= 2) { isEnabled = false; }


        Label title = new Label("Elevator " + id);
        title.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

        // Setup the local START/STOP button
        mainControlButton = new Button(btnText);
        mainControlButton.setStyle(btnColor + " -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 0;");
        mainControlButton.setPrefWidth(90);

        // Add logic to the per-elevator button
        mainControlButton.setOnAction(e -> {
            // This button ONLY works in INDEPENDENT mode
            if (system.getSystemMode().equals("INDEPENDENT")) {
                toggleEnabledState();
            } else {
                System.out.println("Per-elevator START/STOP only works in INDEPENDENT mode.");
            }
        });

        // The row containing the static floor display and direction triangles
        HBox statusRow = new HBox(5);
        statusRow.setAlignment(Pos.CENTER_RIGHT);
        statusRow.setPrefWidth(90);

        currentFloorDisplay = new Label(String.valueOf(this.currentFloor));
        currentFloorDisplay.setStyle("-fx-background-color: white; -fx-text-fill: black; -fx-font-size: 18px; -fx-font-weight: bold; -fx-alignment: center;");
        currentFloorDisplay.setPrefSize(30, 30);

        directionIndicator = new DirectionIndicatorPanel();
        statusRow.getChildren().addAll(currentFloorDisplay, directionIndicator);

        getChildren().addAll(title, mainControlButton, statusRow);


        // 2. Setup the main elevator shaft and the static floor buttons
        shaftPane = new StackPane();
        floorButtonColumn = new VBox(FLOOR_SPACING);
        carPane = new Pane(); // This pane will hold the moving car
        carPane.setMouseTransparent(true); // Allows clicks to pass through to buttons underneath

        for (int i = 10; i >= 1; i--) {
            floorButtonColumn.getChildren().add(createFloorRow(i));
        }

        // 3. Create the visual, moving elevator car itself
        carFloorLabel = new Label(String.valueOf(this.currentFloor));
        carFloorLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: white; -fx-font-weight: bold;");

        movingCar = new VBox(carFloorLabel);
        movingCar.setAlignment(Pos.CENTER);
        movingCar.setPrefSize(40, FLOOR_HEIGHT);
        movingCar.setStyle(
                "-fx-background-color: #606060;" +
                        "-fx-border-color: black;" + // Black border = doors closed
                        "-fx-border-width: 0 2 0 2;"
        );

        carPane.getChildren().add(movingCar);
        movingCar.setLayoutX(40.5); // Center the car in the shaft

        shaftPane.getChildren().addAll(floorButtonColumn, carPane);
        getChildren().add(shaftPane);

        // 4. Initialize the animation and set the car's starting position
        elevatorAnimation = new TranslateTransition();
        elevatorAnimation.setNode(movingCar);
        updateElevatorPosition(this.currentFloor, false); // false = no animation
    }

    /**
     * Factory method to create a single floor row (call dots + floor number label).
     *
     * @param floor The floor number this row represents.
     * @return A styled HBox for the given floor.
     */
    private HBox createFloorRow(int floor) {
        HBox row = new HBox(5);
        row.setAlignment(Pos.CENTER);
        row.setPrefSize(90, FLOOR_HEIGHT);

        DualDotIndicatorPanel callIndicator = new DualDotIndicatorPanel(floor, this);
        floorCallIndicators.put(floor, callIndicator);

        Label floorLabel = new Label(String.valueOf(floor));
        floorLabel.setStyle("-fx-background-color: #404040; -fx-text-fill: white; -fx-background-radius: 0;");
        floorLabel.setPrefSize(40, 25);
        floorLabel.setAlignment(Pos.CENTER);

        row.getChildren().addAll(callIndicator, floorLabel);
        return row;
    }

    /**
     * Toggles the local START/STOP state of this specific elevator.
     * This only has an effect when the system is in INDEPENDENT mode.
     */
    private void toggleEnabledState() {
        isEnabled = !isEnabled; // Flip the state
        if (isEnabled) {
            mainControlButton.setText(btnText_STOP);
            mainControlButton.setStyle(btnColor_STOP + " -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 0;");
        } else {
            mainControlButton.setText(btnText_START);
            mainControlButton.setStyle(btnColor_START + " -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 0;");
        }
    }

    /**
     * Called by the main system when the global mode changes.
     * This forces the elevator's local START/STOP button to update its appearance,
     * especially when entering/exiting CENTRALIZED mode.
     *
     * @param newMode The new global mode (e.g., "CENTRALIZED", "FIRE").
     */
    public void onSystemModeChange(String newMode) {
        if (newMode.equals("CENTRALIZED")) {
            // In CENTRALIZED mode, all elevators are "ON" (show STOP)
            isEnabled = true;
            mainControlButton.setText(btnText_STOP);
            mainControlButton.setStyle(btnColor_STOP + " -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 0;");
        }
        // When switching to INDEPENDENT, it remembers its last state
    }

    /**
     * This is the primary method for processing a hall call (from the dots).
     * It checks if the call is allowed (system running, not fire mode, elevator enabled),
     * then sequences the entire "move -> open door -> wait -> close door" process.
     *
     * @param targetFloor The floor being called.
     * @param direction   The direction requested (UP or DOWN).
     */
    public void callElevator(int targetFloor, Direction direction) {
        // Check system-wide and local states before accepting a call
        if (!system.isSystemRunning() || system.getSystemMode().equals("FIRE") || !isEnabled) {
            System.out.println("Call ignored: System stopped, in fire mode, or elevator disabled.");
            return;
        }

        // Ignore call if already moving or already at the target floor
        if (isMoving || currentFloor == targetFloor) {
            return;
        }

        isMoving = true;
        System.out.println("Hall Call for Elevator " + elevatorId + " at floor " + targetFloor + ", direction " + direction);

        lightExternalCall(targetFloor, direction, true);
        Direction travelDirection = (targetFloor > currentFloor) ? Direction.UP : Direction.DOWN;
        setDirection(travelDirection);

        // Animate the car's movement
        updateElevatorPosition(targetFloor, true); // true = animate

        // Create a pause for how long the door stays open
        PauseTransition openDoors = new PauseTransition(Duration.millis(2000));

        // When the car arrives at the floor...
        elevatorAnimation.setOnFinished(e -> {
            setDirection(Direction.IDLE);
            setDoorStatus(true); // "Open" the door (white border)
            openDoors.play(); // Start the 2-second "open" timer
        });

        // When the "open" timer finishes...
        openDoors.setOnFinished(e -> {
            setDoorStatus(false); // "Close" the door (black border)
            lightExternalCall(targetFloor, direction, false);
            isMoving = false; // Release the lock, ready for next call
        });

        elevatorAnimation.play();
    }

    /**
     * Moves the elevator car to a new floor, either instantly or with animation.
     * Also updates the floor number labels on the car and the top display.
     *
     * @param newFloor The destination floor.
     * @param animate  True to animate the movement, false to snap instantly.
     */
    private void updateElevatorPosition(int newFloor, boolean animate) {
        // Y-coordinate is inverted (0 is top, 300 is bottom)
        double targetY = (10 - newFloor) * TOTAL_FLOOR_HEIGHT;
        int floorsToTravel = Math.abs(newFloor - this.currentFloor);
        this.currentFloor = newFloor;

        // Update both the static top display and the label on the car
        this.currentFloorDisplay.setText(String.valueOf(newFloor));
        this.carFloorLabel.setText(String.valueOf(newFloor));

        if (animate) {
            elevatorAnimation.setDuration(Duration.millis(floorsToTravel * ANIMATION_SPEED_PER_FLOOR));
            elevatorAnimation.setToY(targetY);
        } else {
            // Snap to position without animation (used for initialization)
            movingCar.setTranslateY(targetY);
        }
    }

    /**
     * Visually simulates the door opening (white border) or closing (black border).
     *
     * @param open True to show "open" (white border), false for "closed" (black border).
     */
    public void setDoorStatus(boolean open) {
        String borderColor = open ? "white" : "black";
        movingCar.setStyle(
                "-fx-background-color: #606060;" +
                        "-fx-border-color: " + borderColor + ";" +
                        "-fx-border-width: 0 2 0 2;"
        );
    }

    /**
     * Updates the visual direction indicator (triangles) and the internal state.
     *
     * @param newDirection The new direction (UP, DOWN, or IDLE).
     */
    public void setDirection(Direction newDirection) {
        this.currentDirection = newDirection;
        directionIndicator.setDirection(newDirection);
    }

    /**
     * Lights or un-lights the call dots for a specific floor.
     *
     * @param floor     The floor number (1-10).
     * @param direction The direction dot to change (UP or DOWN).
     * @param lit       True to light the dot, false to turn it off.
     */
    public void lightExternalCall(int floor, Direction direction, boolean lit) {
        DualDotIndicatorPanel indicator = floorCallIndicators.get(floor);
        if (indicator != null) {
            indicator.setDotLit(direction, lit);
        }
    }

    /**
     * Simple getter for the elevator's current floor.
     * @return The current floor number (1-10).
     */
    public int getCurrentFloor() {
        return currentFloor;
    }

    /**
     * An override command (used for FIRE mode) to send the elevator to a floor
     * and hold the doors open, ignoring all other calls.
     *
     * @param targetFloor The floor to move to (typically 1).
     */
    public void forceMoveAndOpen(int targetFloor) {
        elevatorAnimation.stop();
        isMoving = true; // Lock the elevator
        setDirection(Direction.IDLE);
        updateElevatorPosition(targetFloor, true); // Animate to target

        // When it arrives, just open the door and stay locked
        elevatorAnimation.setOnFinished(e -> {
            setDoorStatus(true);
            isMoving = true; // STAY locked
        });

        elevatorAnimation.play();
    }

    /**
     * Releases the elevator from a forced state (like FIRE) and closes its doors.
     */
    public void releaseAndClose() {
        setDoorStatus(false);
        isMoving = false; // Release the lock
    }

    /**
     * Called by the main system's RESET button.
     * Stops all motion, resets the local button to "STOP" (enabled),
     * and animates the car back to Floor 1.
     */
    public void forceReset() {
        elevatorAnimation.stop();
        isMoving = true; // Lock during reset
        isEnabled = true; // Re-enable the elevator
        mainControlButton.setText(btnText_STOP);
        mainControlButton.setStyle(btnColor_STOP + " -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 0;");

        setDirection(Direction.IDLE);
        updateElevatorPosition(1, true); // Animate to floor 1

        // When it arrives at floor 1...
        elevatorAnimation.setOnFinished(e -> {
            setDoorStatus(false); // Close door
            isMoving = false; // Release lock
        });
        elevatorAnimation.play();
    }
}