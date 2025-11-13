package GUI;

import bus.SoftwareBus;
import Message.Message;

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
 * ElevatorPanel (reactive BUS client, spreadsheet protocol)
 *
 * Subscriptions per car i (1..4):
 *   (1,0) System Stop
 *   (2,0) System Start
 *   (3,0) System Reset
 *   (4,0) Clear Fire
 *   (5,0) Mode (Centralized / Independent / Test Fire)
 *   (6,i) Start this elevator
 *   (7,i) Stop this elevator
 *
 * Behaviour:
 *   - System Start/Stop enable or disable the car.
 *   - Reset moves the car to floor 1 and opens the doors.
 *   - Mode with body 1110 (Test Fire) recalls the car to floor 1 and opens doors.
 *   - Clear Fire closes the doors and exits fire mode.
 *   - Individual Start/Stop (topics 6 & 7) toggle the STOP/START state for that car.
 */
public class ElevatorPanel extends VBox {

    public enum Direction { UP, DOWN, IDLE }

    private final int elevatorId;

    // State flags
    private int currentFloor = 1;
    private Direction currentDirection = Direction.IDLE;
    private boolean isDoorOpen = false;
    private boolean isEnabled = true;   // true = running
    private boolean autoMode = false;   // true = INDEPENDENT (AUTO)
    private boolean isFireMode = false; // true = in FIRE recall

    // BUS
    private final SoftwareBus bus;

    // UI widgets
    private Button mainControlButton;
    private final String btnText_START = "START";
    private final String btnColor_START = "-fx-background-color: #228B22;";
    private final String btnText_STOP  = "STOP";
    private final String btnColor_STOP  = "-fx-background-color: #B22222;";

    private StackPane shaftPane;
    private VBox floorButtonColumn;
    private Pane carPane;
    private VBox movingCar;
    private Label carFloorLabel;
    private TranslateTransition elevatorAnimation;

    private final ConcurrentHashMap<Integer, DualDotIndicatorPanel> floorCallIndicators =
            new ConcurrentHashMap<>();
    private final DirectionIndicatorPanel directionIndicator;
    private final Label currentFloorDisplay;

    private static final double FLOOR_HEIGHT = 30.0;
    private static final double FLOOR_SPACING = 3.0;
    private static final double TOTAL_FLOOR_HEIGHT = FLOOR_HEIGHT + FLOOR_SPACING;
    private static final double ANIMATION_SPEED_PER_FLOOR = 400.0;

    // --- UI subcomponents ----------------------------------------------------

    private class DualDotIndicatorPanel extends VBox {
        private final Circle upDot = new Circle(3, Color.web("#505050"));
        private final Circle downDot = new Circle(3, Color.web("#505050"));
        DualDotIndicatorPanel(int floor, ElevatorPanel parentPanel) {
            super(6);
            getChildren().addAll(upDot, downDot);
            setAlignment(Pos.CENTER);
            setPadding(new Insets(0, 5, 0, 5));
        }
        void setDotLit(Direction direction, boolean lit) {
            Color color = lit ? Color.WHITE : Color.web("#505050");
            if (direction == Direction.UP)   upDot.setFill(color);
            if (direction == Direction.DOWN) downDot.setFill(color);
        }
    }

    private class DirectionIndicatorPanel extends VBox {
        private final Polygon upTriangle, downTriangle;
        private final Color UNLIT_COLOR = Color.BLACK;
        DirectionIndicatorPanel() {
            super(6);
            upTriangle   = new Polygon(6.0, 0.0, 0.0, 8.0, 12.0, 8.0);
            downTriangle = new Polygon(6.0, 8.0, 0.0, 0.0, 12.0, 0.0);
            setDirection(Direction.IDLE);
            getChildren().addAll(upTriangle, downTriangle);
            setAlignment(Pos.CENTER);
            setPadding(new Insets(5));
        }
        void setDirection(Direction newDirection) {
            upTriangle.setFill(newDirection == Direction.UP   ? Color.WHITE : UNLIT_COLOR);
            downTriangle.setFill(newDirection == Direction.DOWN ? Color.WHITE : UNLIT_COLOR);
        }
    }

    // --- Constructor ---------------------------------------------------------

    public ElevatorPanel(int id) {
        super(3);
        this.elevatorId = id;

        // Each panel is its own BUS client
        this.bus = new SoftwareBus(false);

        // Subscribe to all spreadsheet topics relevant for this car
        bus.subscribe(1, 0);          // System Stop
        bus.subscribe(2, 0);          // System Start
        bus.subscribe(3, 0);          // System Reset
        bus.subscribe(4, 0);          // Clear Fire
        bus.subscribe(5, 0);          // Mode (Centralized / Independent / Test Fire)
        bus.subscribe(6, elevatorId); // Start this elevator
        bus.subscribe(7, elevatorId); // Stop this elevator

        // Layout setup
        setAlignment(Pos.CENTER);
        setStyle("-fx-background-color: #333333;");
        setPrefWidth(100);

        Label title = new Label("Elevator " + id);
        title.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

        mainControlButton = new Button(btnText_STOP);
        mainControlButton.setStyle(btnColor_STOP
                + " -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 0;");
        mainControlButton.setPrefWidth(90);
        // Local toggle only (visual); real RUN/STOP comes from BUS topics 1,2,6,7
        mainControlButton.setOnAction(e -> toggleEnabledState());

        HBox statusRow = new HBox(5);
        statusRow.setAlignment(Pos.CENTER_RIGHT);
        statusRow.setPrefWidth(90);

        currentFloorDisplay = new Label(String.valueOf(this.currentFloor));
        currentFloorDisplay.setStyle(
                "-fx-background-color: white; -fx-text-fill: black; " +
                        "-fx-font-size: 18px; -fx-font-weight: bold; -fx-alignment: center;");
        currentFloorDisplay.setPrefSize(30, 30);

        directionIndicator = new DirectionIndicatorPanel();
        statusRow.getChildren().addAll(currentFloorDisplay, directionIndicator);

        getChildren().addAll(title, mainControlButton, statusRow);

        // Shaft + car layout
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
        movingCar.setStyle("-fx-background-color: #606060;-fx-border-color: black;-fx-border-width: 0 2 0 2;");
        carPane.getChildren().add(movingCar);
        movingCar.setLayoutX(40.5);

        shaftPane.getChildren().addAll(floorButtonColumn, carPane);
        getChildren().add(shaftPane);

        elevatorAnimation = new TranslateTransition();
        elevatorAnimation.setNode(movingCar);
        updateElevatorPosition(this.currentFloor, false);

        startBusListener();
    }

    private HBox createFloorRow(int floor) {
        HBox row = new HBox(5);
        row.setAlignment(Pos.CENTER);
        row.setPrefSize(90, FLOOR_HEIGHT);

        DualDotIndicatorPanel callIndicator = new DualDotIndicatorPanel(floor, this);
        floorCallIndicators.put(floor, callIndicator);

        Label floorLabel = new Label(String.valueOf(floor));
        floorLabel.setStyle("-fx-background-color: #404040; -fx-text-fill: white;");
        floorLabel.setPrefSize(40, 25);
        floorLabel.setAlignment(Pos.CENTER);

        row.getChildren().addAll(callIndicator, floorLabel);
        return row;
    }

    private void toggleEnabledState() {
        isEnabled = !isEnabled;
        applyEnabledUI();
    }

    private void applyEnabledUI() {
        if (isEnabled) {
            mainControlButton.setText(btnText_STOP);
            mainControlButton.setStyle(btnColor_STOP
                    + " -fx-text-fill: white; -fx-font-weight: bold;");
        } else {
            mainControlButton.setText(btnText_START);
            mainControlButton.setStyle(btnColor_START
                    + " -fx-text-fill: white; -fx-font-weight: bold;");
        }
    }

    // --- BUS listener --------------------------------------------------------

    private void startBusListener() {
        Thread t = new Thread(() -> {
            while (true) {
                poll(1, 0);          // System Stop
                poll(2, 0);          // System Start
                poll(3, 0);          // System Reset
                poll(4, 0);          // Clear Fire
                poll(5, 0);          // Mode
                poll(6, elevatorId); // Start this car
                poll(7, elevatorId); // Stop this car

                try {
                    Thread.sleep(10);
                } catch (InterruptedException ignored) {}
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private void poll(int topic, int subtopic) {
        Message m = bus.get(topic, subtopic);
        if (m != null) handleCommand(m);
    }

    private void handleCommand(Message m) {
        int topic    = m.getTopic();
        int subtopic = m.getSubTopic();
        int body     = m.getBody();   // e.g. 0, 1000, 1100, 1110

        switch (topic) {
            case 1 -> // System Stop (all)
                    Platform.runLater(() -> {
                        isEnabled = false;
                        applyEnabledUI();
                    });

            case 2 -> // System Start (all)
                    Platform.runLater(() -> {
                        isEnabled = true;
                        applyEnabledUI();
                    });

            case 3 -> // System Reset (all)
                    Platform.runLater(this::resetToOne);

            case 4 -> // Clear Fire (all)
                    Platform.runLater(() -> {
                        isFireMode = false;
                        closeDoor();
                    });

            case 5 -> // Mode (body 1000 / 1100 / 1110)
                    Platform.runLater(() -> {
                        if (body == 1000) {          // Centralized
                            autoMode = false;
                            isFireMode = false;
                        } else if (body == 1100) {   // Independent
                            autoMode = true;
                            isFireMode = false;
                        } else if (body == 1110) {   // Test Fire
                            isFireMode = true;
                            recallToLobby();
                        }
                    });

            case 6 -> { // Start this elevator (subtopic == elevatorId)
                if (subtopic == elevatorId) {
                    Platform.runLater(() -> {
                        isEnabled = true;
                        applyEnabledUI();
                    });
                }
            }

            case 7 -> { // Stop this elevator (subtopic == elevatorId)
                if (subtopic == elevatorId) {
                    Platform.runLater(() -> {
                        isEnabled = false;
                        applyEnabledUI();
                    });
                }
            }

            default -> {
                // ignore unknown topics
            }
        }
    }

    // --- Movement & door helpers --------------------------------------------

    private void updateElevatorPosition(int newFloor, boolean animate) {
        double targetY = (10 - newFloor) * TOTAL_FLOOR_HEIGHT;
        int floorsToTravel = Math.abs(newFloor - this.currentFloor);
        this.currentFloor = newFloor;

        this.currentFloorDisplay.setText(String.valueOf(newFloor));
        this.carFloorLabel.setText(String.valueOf(newFloor));

        if (animate) {
            elevatorAnimation.stop();
            elevatorAnimation.setDuration(
                    Duration.millis(Math.max(1, floorsToTravel) * ANIMATION_SPEED_PER_FLOOR));
            elevatorAnimation.setToY(targetY);
            elevatorAnimation.play();
        } else {
            movingCar.setTranslateY(targetY);
        }
    }

    private void setDoorStatus(boolean open) {
        this.isDoorOpen = open;
        String borderColor = open ? "white" : "black";
        movingCar.setStyle("-fx-background-color: #606060;-fx-border-color: "
                + borderColor + ";-fx-border-width: 0 2 0 2;");
    }

    private void setDirection(Direction d) {
        this.currentDirection = d;
        directionIndicator.setDirection(d);
    }

    /** Generic move helper, used by reset and fire recall. */
    private void moveTo(int targetFloor) {
        if (!isEnabled || isFireMode) return;
        setDoorStatus(false);
        setDirection(targetFloor > currentFloor ? Direction.UP :
                targetFloor < currentFloor ? Direction.DOWN : Direction.IDLE);
        updateElevatorPosition(targetFloor, true);
        elevatorAnimation.setOnFinished(e -> {
            setDirection(Direction.IDLE);
            setDoorStatus(true);
        });
    }

    /** Fire recall (or forced recall) to lobby floor 1. */
    private void recallToLobby() {
        if (currentFloor == 1) {
            setDoorStatus(true);
            return;
        }
        setDoorStatus(false);
        setDirection(Direction.DOWN);
        updateElevatorPosition(1, true);
        elevatorAnimation.setOnFinished(e -> {
            setDirection(Direction.IDLE);
            setDoorStatus(true);
        });
    }

    private void resetToOne() {
        setDoorStatus(false);
        setDirection(currentFloor > 1 ? Direction.DOWN : Direction.IDLE);
        updateElevatorPosition(1, true);
        elevatorAnimation.setOnFinished(e -> {
            setDirection(Direction.IDLE);
            setDoorStatus(true);
        });
    }

    private void closeDoor() { setDoorStatus(false); }

    // --- Getters (optional, for diagnostics) --------------------------------
    public int getCurrentFloor() { return currentFloor; }
    public boolean isDoorOpen() { return isDoorOpen; }
    public boolean isAutoMode() { return autoMode; }
    public boolean isFireMode() { return isFireMode; }
    public boolean isEnabled() { return isEnabled; }
}