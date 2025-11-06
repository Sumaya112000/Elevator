package GUI;

import javafx.animation.PauseTransition;
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
import javafx.util.Duration;
import javafx.geometry.Pos;
import javafx.geometry.Insets;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages all the visuals and logic for a single elevator.
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

    private final ConcurrentHashMap<Integer, DualDotIndicatorPanel> floorCallIndicators = new ConcurrentHashMap<>();
    private final DirectionIndicatorPanel directionIndicator;
    private final Label currentFloorDisplay;

    private static final double FLOOR_HEIGHT = 30.0;
    private static final double FLOOR_SPACING = 3.0;
    private static final double TOTAL_FLOOR_HEIGHT = FLOOR_HEIGHT + FLOOR_SPACING;
    private static final double ANIMATION_SPEED_PER_FLOOR = 400.0;

    /**
     * The call buttons (dots) for each floor.
     */
    private class DualDotIndicatorPanel extends VBox {
        private Circle upDot = new Circle(3, Color.web("#505050"));
        private Circle downDot = new Circle(3, Color.web("#505050"));

        /**
         * Creates the two dots.
         */
        public DualDotIndicatorPanel(int floor, ElevatorPanel parentPanel) {
            super(6);
            getChildren().addAll(upDot, downDot);
            setAlignment(Pos.CENTER);
            setPadding(new Insets(0, 5, 0, 5));
        }

        /**
         * Lights up the dot (or turns it off).
         */
        public void setDotLit(Direction direction, boolean lit) {
            Color color = lit ? Color.WHITE : Color.web("#505050");
            Platform.runLater(() -> {
                if (direction == Direction.UP) upDot.setFill(color);
                else if (direction == Direction.DOWN) downDot.setFill(color);
            });
        }
    }

    /**
     * The direction arrows (triangles) at the top.
     */
    private class DirectionIndicatorPanel extends VBox {
        private Polygon upTriangle, downTriangle;
        private final Color UNLIT_COLOR = Color.BLACK;

        /**
         * Creates the two triangles.
         */
        public DirectionIndicatorPanel() {
            super(6);
            upTriangle = new Polygon(6.0, 0.0, 0.0, 8.0, 12.0, 8.0);
            downTriangle = new Polygon(6.0, 8.0, 0.0, 0.0, 12.0, 0.0);
            setDirection(Direction.IDLE);
            getChildren().addAll(upTriangle, downTriangle);
            setAlignment(Pos.CENTER);
            setPadding(new Insets(5));
        }

        /**
         * Lights up the correct arrow.
         */
        public void setDirection(Direction newDirection) {
            Platform.runLater(() -> {
                upTriangle.setFill(newDirection == Direction.UP ? Color.WHITE : UNLIT_COLOR);
                downTriangle.setFill(newDirection == Direction.DOWN ? Color.WHITE : UNLIT_COLOR);
            });
        }
    }

    /**
     * Builds the visual parts of the elevator.
     */
    public ElevatorPanel(int id, ElevatorAPI api) {
        super(3);
        this.elevatorId = id;
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
     * Creates one row (dots and number) for the shaft.
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
     * Set enabled state from external commands (bus messages)
     */
    public void setEnabledState(boolean enabled) {
        this.isEnabled = enabled;
        Platform.runLater(() -> {
            if (enabled) {
                mainControlButton.setText(btnText_STOP);
                mainControlButton.setStyle(btnColor_STOP + " -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 0;");
            } else {
                mainControlButton.setText(btnText_START);
                mainControlButton.setStyle(btnColor_START + " -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 0;");

                // Stop any ongoing movement when disabled
                stopAllTimers();
                isMoving = false;
                setDirection(Direction.IDLE);
            }
        });
    }

    /**
     * Modified toggle method to send bus messages
     */
    private void toggleEnabledState() {
        boolean newState = !isEnabled;

        if (newState) {
            // Starting elevator - send individual start command
            api.sendIndividualStartCommand(elevatorId);
        } else {
            // Stopping elevator - send individual stop command
            api.sendIndividualStopCommand(elevatorId);
        }
    }

    /**
     * Updates the local START/STOP button from the API.
     */
    public void onSystemModeChange(String newMode) {
        if (newMode.equals("CENTRALIZED")) {
            isEnabled = true;
            Platform.runLater(() -> {
                mainControlButton.setText(btnText_STOP);
                mainControlButton.setStyle(btnColor_STOP + " -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 0;");
            });
        }
    }

    /**
     * Update elevator status from messages received via software bus
     */
    public void updateStatusFromBus(int floor, boolean doorOpen, boolean moving, Direction direction) {
        Platform.runLater(() -> {
            // Stop any ongoing animations
            stopAllTimers();

            // Update position if floor changed
            if (floor != this.currentFloor) {
                updateElevatorPosition(floor, true);
            } else {
                updateElevatorPosition(floor, false);
            }

            // Update door status
            setDoorStatus(doorOpen);

            // Update movement and direction
            this.isMoving = moving;
            setDirection(direction);

            // Update main control button based on movement
            if (moving) {
                mainControlButton.setText(btnText_STOP);
                mainControlButton.setStyle(btnColor_STOP + " -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 0;");
            }
        });
    }

    /**
     * Moves the elevator car visuals.
     */
    private void updateElevatorPosition(int newFloor, boolean animate) {
        double targetY = (10 - newFloor) * TOTAL_FLOOR_HEIGHT;
        int floorsToTravel = Math.abs(newFloor - this.currentFloor);
        this.currentFloor = newFloor;

        Platform.runLater(() -> {
            this.currentFloorDisplay.setText(String.valueOf(newFloor));
            this.carFloorLabel.setText(String.valueOf(newFloor));

            if (animate) {
                elevatorAnimation.setDuration(Duration.millis(floorsToTravel * ANIMATION_SPEED_PER_FLOOR));
                elevatorAnimation.setToY(targetY);
                elevatorAnimation.play();
            } else {
                movingCar.setTranslateY(targetY);
            }
        });
    }

    /**
     * Changes the car border (white=open, black=closed).
     */
    public void setDoorStatus(boolean open) {
        this.isDoorOpen = open;
        String borderColor = open ? "white" : "black";
        String style = "-fx-background-color: #606060;" +
                "-fx-border-color: " + borderColor + ";" +
                "-fx-border-width: 0 2 0 2;";

        Platform.runLater(() -> {
            movingCar.setStyle(style);
        });
    }

    /**
     * Changes the direction arrows.
     */
    public void setDirection(Direction newDirection) {
        this.currentDirection = newDirection;
        directionIndicator.setDirection(newDirection);
    }

    /**
     * Lights up the call dots.
     */
    public void lightExternalCall(int floor, Direction direction, boolean lit) {
        DualDotIndicatorPanel indicator = floorCallIndicators.get(floor);
        if (indicator != null) {
            indicator.setDotLit(direction, lit);
        }
    }

    /**
     * Stops all animations and pauses.
     */
    private void stopAllTimers() {
        Platform.runLater(() -> {
            elevatorAnimation.stop();
            if (waitAtFloor != null) waitAtFloor.stop();
            if (waitAfterClose != null) waitAfterClose.stop();
            if (checkAgainPause != null) checkAgainPause.stop();
        });
    }

    /**
     * FIRE mode: forces elevator to floor 1.
     */
    public void forceMoveAndOpen(int targetFloor) {
        Platform.runLater(() -> {
            stopAllTimers();
            isMoving = true;
            setDirection(Direction.IDLE);
            setDoorStatus(false);

            if (targetFloor == currentFloor) {
                setDoorStatus(true);
                isMoving = true;
            } else {
                elevatorAnimation.setOnFinished(e -> {
                    setDoorStatus(true);
                    isMoving = true;
                });
                updateElevatorPosition(targetFloor, true);
            }
        });
    }

    /**
     * CLEAR FIRE mode: closes door and idles.
     */
    public void releaseAndClose() {
        Platform.runLater(() -> {
            stopAllTimers();
            setDoorStatus(false);
            isMoving = false;
        });
    }

    /**
     * RESET button: forces elevator to floor 1, waits 5s.
     */
    public void forceReset() {
        Platform.runLater(() -> {
            stopAllTimers();

            isMoving = true;
            isEnabled = true;
            mainControlButton.setText(btnText_STOP);
            mainControlButton.setStyle(btnColor_STOP + " -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 0;");

            setDirection(Direction.IDLE);
            setDoorStatus(false);

            PauseTransition waitAfterReset = new PauseTransition(Duration.millis(5000));
            waitAfterReset.setOnFinished(e -> {
                isMoving = false;
            });

            elevatorAnimation.setOnFinished(e -> {
                setDoorStatus(false);
                waitAfterReset.play();
            });

            updateElevatorPosition(1, true);
        });
    }

    /**
     * Sending Elevator to specific floor.
     */
    public void requestFloor(int targetFloor) {
        if (!api.isSystemRunning() || api.getSystemMode().equals("FIRE") || !isEnabled) {
            System.out.println("ELEV " + elevatorId + ": Request to "
                    + targetFloor + " denied (system offline or FIRE).");
            return;
        }
        if (targetFloor == currentFloor) {
            System.out.println("ELEV " + elevatorId + ": Already at floor " + targetFloor);
            return;
        }
        Platform.runLater(() -> {
            stopAllTimers();
            System.out.println("ELEV " + elevatorId + ": Servicing new request to floor " + targetFloor);

            isMoving = true;
            setDoorStatus(false);
            setDirection(targetFloor > currentFloor ? Direction.UP : Direction.DOWN);

            waitAtFloor = new PauseTransition(Duration.millis(2000));
            waitAfterClose = new PauseTransition(Duration.millis(2000));

            waitAfterClose.setOnFinished(e -> {
                isMoving = false;
                System.out.println("ELEV " + elevatorId + ": Request finished. Now idle.");
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
            updateElevatorPosition(targetFloor, true);
        });
    }

    // Getters
    public int getCurrentFloor() {
        return this.currentFloor;
    }

    public boolean isMoving() {
        return this.isMoving;
    }

    public boolean isDoorOpen() {
        return this.isDoorOpen;
    }

    public Direction getCurrentDirection() {
        return this.currentDirection;
    }

    public boolean isEnabled() {
        return this.isEnabled;
    }
}