package GUI;

import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import javafx.geometry.Pos;
import javafx.geometry.Insets;

import java.util.concurrent.ConcurrentHashMap;

/**
 * This class represents a single elevator.
 * It manages the visual car, the buttons for each floor,
 * and the automatic movement loop.
 */
public class ElevatorPanel extends VBox {

    public enum Direction { UP, DOWN, IDLE }

    private final int elevatorId;
    private int currentFloor;
    private Direction currentDirection = Direction.IDLE;
    private boolean isMoving = false;

    private final ElevatorAPI api;

    private boolean isDoorOpen = false;
    private boolean isEnabled = true;
    private Button mainControlButton;
    private String btnText_START = "START";
    private String btnColor_START = "-fx-background-color: #228B22;";
    private String btnText_STOP = "STOP";
    private String btnColor_STOP = "-fx-background-color: #B22222;";

    private StackPane shaftPane;
    private VBox floorButtonColumn;
    private Pane carPane;
    private VBox movingCar;
    private Label carFloorLabel;
    private TranslateTransition elevatorAnimation;
    private PauseTransition waitAtFloor;
    private PauseTransition waitAfterClose;
    private PauseTransition checkAgainPause;

    private int[] automatedSequence;
    private int sequenceIndex = 0;
    private final ConcurrentHashMap<Integer, DualDotIndicatorPanel> floorCallIndicators = new ConcurrentHashMap<>();
    private final DirectionIndicatorPanel directionIndicator;
    private final Label currentFloorDisplay;

    private static final double FLOOR_HEIGHT = 30.0;
    private static final double FLOOR_SPACING = 3.0;
    private static final double TOTAL_FLOOR_HEIGHT = FLOOR_HEIGHT + FLOOR_SPACING;
    private static final double ANIMATION_SPEED_PER_FLOOR = 400.0;

    /**
     * Elevator Call Buttons.
     */
    private class DualDotIndicatorPanel extends VBox {
        private Circle upDot = new Circle(3, Color.web("#505050"));
        private Circle downDot = new Circle(3, Color.web("#505050"));
        public DualDotIndicatorPanel(int floor, ElevatorPanel parentPanel) {
            super(6);
            getChildren().addAll(upDot, downDot);
            setAlignment(Pos.CENTER);
            setPadding(new Insets(0, 5, 0, 5));
        }
        public void setDotLit(Direction direction, boolean lit) {
            Color color = lit ? Color.WHITE : Color.web("#505050");
            if (direction == Direction.UP) upDot.setFill(color);
            else if (direction == Direction.DOWN) downDot.setFill(color);
        }
    }

    /**
     * Shows the up and down arrows at the top of the elevator.
     */
    private class DirectionIndicatorPanel extends VBox {
        private Polygon upTriangle, downTriangle;
        private final Color UNLIT_COLOR = Color.BLACK;
        public DirectionIndicatorPanel() {
            super(6);
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

    /** Builds the visual parts of the elevator. **/

    public ElevatorPanel(int id, ElevatorAPI api) {
        super(3);
        this.elevatorId =id;
        this.api = api;
        setAlignment(Pos.CENTER);
        setStyle("-fx-background-color: #333333;");
        setPrefWidth(100);

        this.currentFloor = 1;
        isEnabled = true;
        String btnText = btnText_STOP;
        String btnColor = btnColor_STOP;

        Label title = new Label("Elevator " + id);
        title.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

        mainControlButton = new Button(btnText);
        mainControlButton.setStyle(btnColor + " -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 0;");
        mainControlButton.setPrefWidth(90);

        mainControlButton.setOnAction(e -> {
            if (api.getSystemMode().equals("INDEPENDENT") || api.getSystemMode().equals("CENTRALIZED")) {
                toggleEnabledState();
            }
        });
        HBox statusRow = new HBox(5);
        statusRow.setAlignment(Pos.CENTER_RIGHT);
        statusRow.setPrefWidth(90);

        currentFloorDisplay = new Label(String.valueOf(this.currentFloor));
        currentFloorDisplay.setStyle("-fx-background-color: white; -fx-text-fill: black; -fx-font-size: 18px; -fx-font-weight: bold; -fx-alignment: center;");
        currentFloorDisplay.setPrefSize(30, 30);

        directionIndicator = new DirectionIndicatorPanel();
        statusRow.getChildren().addAll(currentFloorDisplay, directionIndicator);

        getChildren().addAll(title, mainControlButton, statusRow);
        shaftPane = new StackPane();
        floorButtonColumn = new VBox(FLOOR_SPACING);
        carPane = new Pane();
        carPane.setMouseTransparent(true);

        for (int i = 10; i >= 1; i--) {
            floorButtonColumn.getChildren().add(createFloorRow(i));
        }

        carFloorLabel = new Label(String.valueOf(this.currentFloor));
        carFloorLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: white; -fx-font-weight: bold;");

        movingCar = new VBox(carFloorLabel);
        movingCar.setAlignment(Pos.CENTER);
        movingCar.setPrefSize(40, FLOOR_HEIGHT);
        movingCar.setStyle(
                "-fx-background-color: #606060;" +
                        "-fx-border-color: black;" +
                        "-fx-border-width: 0 2 0 2;"
        );

        carPane.getChildren().add(movingCar);
        movingCar.setLayoutX(40.5);

        shaftPane.getChildren().addAll(floorButtonColumn, carPane);
        getChildren().add(shaftPane);

        elevatorAnimation = new TranslateTransition();
        elevatorAnimation.setNode(movingCar);
        updateElevatorPosition(this.currentFloor, false);
    }

    /**
     * Creates one floor row (the elevator call button and the floor number).
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
     * Changing Start/Stop status of individual elevator.
     */
    private void toggleEnabledState() {
        isEnabled = !isEnabled;
        if (isEnabled) {
            mainControlButton.setText(btnText_STOP);
            mainControlButton.setStyle(btnColor_STOP + " -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 0;");
        } else {
            mainControlButton.setText(btnText_START);
            mainControlButton.setStyle(btnColor_START + " -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 0;");
        }
    }

    /**
     * Changing Modes of elevator(Centralized, Independent) from buttons.
     */
    public void onSystemModeChange(String newMode) {
        if (newMode.equals("CENTRALIZED")) {
            isEnabled = true;
            mainControlButton.setText(btnText_STOP);
            mainControlButton.setStyle(btnColor_STOP + " -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 0;");
        }
    }

    /**
     * Moves the elevator car to a new floor.
     */
    private void updateElevatorPosition(int newFloor, boolean animate) {
        double targetY = (10 - newFloor) * TOTAL_FLOOR_HEIGHT;
        int floorsToTravel = Math.abs(newFloor - this.currentFloor);
        this.currentFloor = newFloor;

        this.currentFloorDisplay.setText(String.valueOf(newFloor));
        this.carFloorLabel.setText(String.valueOf(newFloor));

        if (animate) {
            elevatorAnimation.setDuration(Duration.millis(floorsToTravel * ANIMATION_SPEED_PER_FLOOR));
            elevatorAnimation.setToY(targetY);
        } else {
            movingCar.setTranslateY(targetY);
        }
    }

    /**
     * Know Door open/close status.
     * White Border = Open
     * Black Border = Close
     */
    public void setDoorStatus(boolean open) {
        this.isDoorOpen = open;
        String borderColor = open ? "white" : "black";
        movingCar.setStyle(
                "-fx-background-color: #606060;" +
                        "-fx-border-color: " + borderColor + ";" +
                        "-fx-border-width: 0 2 0 2;"
        );
    }
    /**
     * Changes the direction arrows.
     */
    public void setDirection(Direction newDirection) {
        this.currentDirection = newDirection;
        directionIndicator.setDirection(newDirection);
    }

    /**
     * Turns the floor call dots on or off.
     */
    public void lightExternalCall(int floor, Direction direction, boolean lit) {
        DualDotIndicatorPanel indicator = floorCallIndicators.get(floor);
        if (indicator != null) {
            indicator.setDotLit(direction, lit);
        }
    }
    /**
     * Stops all running animations and pauses to prevent race conditions.
     */
    private void stopAllTimers() {
        elevatorAnimation.stop();
        if (waitAtFloor != null) waitAtFloor.stop();
        if (waitAfterClose != null) waitAfterClose.stop();
        if (checkAgainPause != null) checkAgainPause.stop();
    }

    /**
     * FIRE Mode
     * Forces Elevators to go floor 1 and have doors open.
     */
    public void forceMoveAndOpen(int targetFloor) {
        stopAllTimers();
        isMoving = true;
        setDirection(Direction.IDLE);
        setDoorStatus(false);

        if (targetFloor == currentFloor) {
            setDoorStatus(true);
            isMoving = true;
        } else {
            updateElevatorPosition(targetFloor, true);
            elevatorAnimation.setOnFinished(e -> {
                setDoorStatus(true);
                isMoving = true;
            });
            elevatorAnimation.play();
        }
    }

    /**
     * Clear Fire Mode
     */
    public void releaseAndClose() {
        stopAllTimers();
        setDoorStatus(false);
        isMoving = false; // Release the lock

        PauseTransition restartPause = new PauseTransition(Duration.millis(2000));
        restartPause.setOnFinished(e -> {
            runNextMoveInSequence();
        });
        restartPause.play();
    }

    /**
     * Called by the main system's RESET button.
     * Animates the car back to Floor 1, waits 5 seconds, then starts the loop.
     */
    public void forceReset() {
        stopAllTimers(); // Stop any in-progress loops

        isMoving = true;
        isEnabled = true;
        mainControlButton.setText(btnText_STOP);
        mainControlButton.setStyle(btnColor_STOP + " -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 0;");

        setDirection(Direction.IDLE);
        setDoorStatus(false); // Immediately close door

        updateElevatorPosition(1, true); // Animate to floor 1

        PauseTransition waitAfterReset = new PauseTransition(Duration.millis(5000));

        waitAfterReset.setOnFinished(e -> {
            isMoving = false;
            runNextMoveInSequence();
        });

        elevatorAnimation.setOnFinished(e -> {
            setDoorStatus(false);
            waitAfterReset.play();
        });

        elevatorAnimation.play();
    }

    /**
     * Gives the elevator its list of floors to visit in its loop.
     */
    public void startAutomatedLoop(int[] sequence) {
        this.automatedSequence = sequence;
        this.sequenceIndex = 0;

        // This makes the elevator start its loop immediately on startup.
        runNextMoveInSequence();
    }

    /**
     * This is the core "loop" that makes the elevator move automatically.
     */
    private void runNextMoveInSequence() {


        if (!api.isSystemRunning() || api.getSystemMode().equals("FIRE") || !isEnabled) {
            checkAgainPause = new PauseTransition(Duration.millis(1000));
            checkAgainPause.setOnFinished(e -> runNextMoveInSequence());
            checkAgainPause.play();
            return;
        }

        isMoving = true;

        if (sequenceIndex >= automatedSequence.length) {
            sequenceIndex = 0;
        }
        int targetFloor = automatedSequence[sequenceIndex];
        sequenceIndex++;

        waitAtFloor = new PauseTransition(Duration.millis(2000));
        waitAfterClose = new PauseTransition(Duration.millis(2000));

        waitAfterClose.setOnFinished(e -> {
            isMoving = false;
            runNextMoveInSequence();
        });

        waitAtFloor.setOnFinished(e -> {
            setDoorStatus(false);
            waitAfterClose.play();
        });

        elevatorAnimation.setOnFinished(e -> {
            setDirection(Direction.IDLE);
            setDoorStatus(true);
            waitAtFloor.play();
        });

        if (targetFloor == currentFloor) {
            setDoorStatus(true);
            waitAtFloor.play();
        } else {
            setDirection(targetFloor > currentFloor ? Direction.UP : Direction.DOWN);
            updateElevatorPosition(targetFloor, true);
            elevatorAnimation.play();
        }
    }

    /**
     * Returns the elevator's current floor.
     * @return An integer (1-10)
     */
    public int getCurrentFloor() {
        return this.currentFloor;
    }

    /**
     * Checks if the elevator is currently moving.
     * @return true if moving, false if stopped.
     */
    public boolean isMoving() {
        return this.isMoving;
    }

    /**
     * Checks if the elevator door is currently open.
     * @return true if the door is open.
     */
    public boolean isDoorOpen() {
        return this.isDoorOpen;
    }

    /**
     * Returns the elevator current direction.
     * @return Direction enum
     */
    public ElevatorPanel.Direction getCurrentDirection() {
        return this.currentDirection;
    }

}