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
    private final ElevatorControlSystem system;

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
     * Shows the two small dots (up/down) next to each floor number.
     */
    private class DualDotIndicatorPanel extends VBox {
        private Circle upDot = new Circle(3, Color.web("#505050"));
        private Circle downDot = new Circle(3, Color.web("#505050"));
        public DualDotIndicatorPanel(int floor, ElevatorPanel parentPanel) {
            super(6);
            getChildren().addAll(upDot, downDot);
            setAlignment(Pos.CENTER);
            setPadding(new Insets(0, 5, 0, 5));
            // Clicks are disabled for automated loop
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


    /**
     * Builds the visual parts of a single elevator panel.
     */
    public ElevatorPanel(int id, ElevatorControlSystem system) {
        super(3);
        this.elevatorId = id;
        this.system = system;
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
            if (system.getSystemMode().equals("INDEPENDENT")) {
                toggleEnabledState();
            } else {
                System.out.println("Per-elevator START/STOP only works in INDEPENDENT mode.");
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
     * Creates one floor row (the dots and the floor number).
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
     * Flips the local START/STOP button for this elevator.
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
     * Called by the main system when the global mode changes.
     */
    public void onSystemModeChange(String newMode) {
        if (newMode.equals("CENTRALIZED")) {
            isEnabled = true;
            mainControlButton.setText(btnText_STOP);
            mainControlButton.setStyle(btnColor_STOP + " -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 0;");
        }
    }

    /**
     * Moves the elevator car to a new floor, with or without animation.
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
     * Changes the elevator car's border to show if doors are open (white) or closed (black).
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
     * Changes the direction arrows (up, down, or idle).
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
     * Used by FIRE mode to force the elevator to floor 1 and hold doors open.
     */
    public void forceMoveAndOpen(int targetFloor) {
        elevatorAnimation.stop();
        isMoving = true;
        setDirection(Direction.IDLE);
        updateElevatorPosition(targetFloor, true);

        elevatorAnimation.setOnFinished(e -> {
            setDoorStatus(true);
            isMoving = true; // STAY locked
        });

        elevatorAnimation.play();
    }

    /**
     * Used to release the elevator from FIRE mode.
     */
    public void releaseAndClose() {
        setDoorStatus(false);
        isMoving = false;
    }

    /**
     * Called by the main system's RESET button.
     * Animates the car back to Floor 1, waits 5 seconds, then starts the loop.
     */
    public void forceReset() {
        elevatorAnimation.stop();
        isMoving = true;
        isEnabled = true;
        mainControlButton.setText(btnText_STOP);
        mainControlButton.setStyle(btnColor_STOP + " -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 0;");

        setDirection(Direction.IDLE);
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

        // First, check if the system is ON and not in FIRE mode.
        if (!system.isSystemRunning() || system.getSystemMode().equals("FIRE") || !isEnabled) {
            PauseTransition checkAgain = new PauseTransition(Duration.millis(1000));
            checkAgain.setOnFinished(e -> runNextMoveInSequence());
            checkAgain.play();
            return;
        }

        if (automatedSequence == null) {
            System.err.println("Error: Elevator " + elevatorId + " has no automated sequence.");
            return;
        }

        isMoving = true;

        // Get the next floor from the list, or loop to the start
        if (sequenceIndex >= automatedSequence.length) {
            sequenceIndex = 0;
        }
        int targetFloor = automatedSequence[sequenceIndex];
        sequenceIndex++;


        PauseTransition waitAtFloor = new PauseTransition(Duration.millis(2000));
        PauseTransition waitAfterClose = new PauseTransition(Duration.millis(2000));


        // After waiting 2 seconds (door closed), start the next move
        waitAfterClose.setOnFinished(e -> {
            isMoving = false;
            runNextMoveInSequence();
        });

        // After waiting 2 seconds (door open), close the door
        waitAtFloor.setOnFinished(e -> {
            setDoorStatus(false);
            waitAfterClose.play();
        });

        // When the elevator arrives at the floor, open the door
        elevatorAnimation.setOnFinished(e -> {
            setDirection(Direction.IDLE);
            setDoorStatus(true);
            waitAtFloor.play();
        });


        // Start the process
        if (targetFloor == currentFloor) {
            // If already at the floor, just open the door
            setDoorStatus(true);
            waitAtFloor.play();
        } else {
            // If at a different floor, start moving
            setDirection(targetFloor > currentFloor ? Direction.UP : Direction.DOWN);
            updateElevatorPosition(targetFloor, true);
            elevatorAnimation.play();
        }
    }
}