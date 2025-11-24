package CommandCenter;

import Message.*;
import Bus.*;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.util.Duration;

import java.util.concurrent.ConcurrentHashMap;

/**
 * ElevatorPanel
 */
public class ElevatorPanel extends VBox {

    public enum Direction { UP, DOWN, IDLE }

    // state flags
    // Start visually at floor 10 (top) for testing demo
    private int currentFloor = 10;
    private Direction currentDirection = Direction.IDLE;
    private boolean isDoorOpen = false;
    private boolean isEnabled = true;     // true = running
    private boolean autoMode = false;     // true = INDEPENDENT (AUTO)
    private boolean isFireMode = false;   // true = in FIRE recall


    // BUS client
    private final SoftwareBus bus;
    private final int elevatorId;



    // ui widgets
    private Button mainControlButton;
    private final String btnText_START = "START";
    private final String btnColor_START = "-fx-background-color: #228B22;";
    private final String btnText_STOP  = "STOP";
    private final String btnColor_STOP = "-fx-background-color: #B22222;";
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
    private static final double ANIMATION_SPEED_PER_FLOOR = 400.0; // ms per floor


    // ui components
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



    public ElevatorPanel(int id) {
        super(3);
        this.elevatorId = id;
        this.bus = new SoftwareBus(false);

        // Subscribe to all topics relevant for this car
        bus.subscribe(SoftwareBusCodes.systemStop, 0);          // System Stop
        bus.subscribe(SoftwareBusCodes.systemStart, 0);          // System Start
        bus.subscribe(SoftwareBusCodes.clearFire, 0);          // Clear Fire
        bus.subscribe(SoftwareBusCodes.setMode, 0);          // Mode
        bus.subscribe(SoftwareBusCodes.startElevator, elevatorId);         // Start this elevator
        bus.subscribe(SoftwareBusCodes.stopElevator, elevatorId);         // Stop this elevator
        bus.subscribe(SoftwareBusCodes.carDispatch, elevatorId);       // DISPATCH
        bus.subscribe(SoftwareBusCodes.cabinPosition, elevatorId);       // POSITION
        bus.subscribe(SoftwareBusCodes.doorStatus, elevatorId);       // DOOR
        bus.subscribe(SoftwareBusCodes.displayDirection, elevatorId);       // DIRECTION
        bus.subscribe(SoftwareBusCodes.displayFloor, elevatorId);          // FLOOR

        //layout
        setAlignment(Pos.CENTER);
        setStyle("-fx-background-color: #333333;");
        setPrefWidth(100);

        Label title = new Label("Elevator " + id);
        title.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

        mainControlButton = new Button(btnText_STOP);
        mainControlButton.setStyle(
                btnColor_STOP + " -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 0;");
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

        // Animation
        elevatorAnimation = new TranslateTransition();
        elevatorAnimation.setNode(movingCar);

        // Start visually at floor 10
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



    // BUS listener
    private void startBusListener() {
        Thread t = new Thread(() -> {
            while (true) {
                poll(SoftwareBusCodes.systemStop, 0);          // System Stop
                poll(SoftwareBusCodes.systemStart, 0);          // System Start
                poll(SoftwareBusCodes.clearFire, 0);          // Clear Fire
                poll(SoftwareBusCodes.setMode, 0);          // Mode
                poll(SoftwareBusCodes.startElevator, elevatorId);        // Start this car
                poll(SoftwareBusCodes.stopElevator, elevatorId);         // Stop this car
                poll(SoftwareBusCodes.carDispatch, elevatorId);
                poll(SoftwareBusCodes.cabinPosition, elevatorId);
                poll(SoftwareBusCodes.doorStatus, elevatorId);
                poll(SoftwareBusCodes.displayDirection, elevatorId);
                poll(SoftwareBusCodes.displayFloor, elevatorId);

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
        int t = m.getTopic();
        int st = m.getSubTopic();
        int body = m.getBody();
        if (t == SoftwareBusCodes.systemStop) {// System Stop (all)
            Platform.runLater(() -> {
                isEnabled = false;
                applyEnabledUI();
            });
        } else if(t == SoftwareBusCodes.systemStart) { // System Start (all)
            Platform.runLater(() -> {
                isEnabled = true;
                applyEnabledUI();
            });
        } else if(t == SoftwareBusCodes.clearFire) {
            Platform.runLater(() -> {
                isFireMode = false;
                closeDoor();
            });
        } else if(t == SoftwareBusCodes.setMode) {
            Platform.runLater(() -> {
                if (body == SoftwareBusCodes.centralized) {          // Centralized
                    autoMode = false;
                    isFireMode = false;
                } else if (body == SoftwareBusCodes.independent) {    // Independent
                    autoMode = true;
                    isFireMode = false;
                } else if (body == SoftwareBusCodes.fire) {   // Test Fire
                    isFireMode = true;
                }
            });
        } else if(t == SoftwareBusCodes.startElevator) {
            if (st == elevatorId) {
                Platform.runLater(() -> {
                    isEnabled = true;
                    applyEnabledUI();
                });
            }
        } else if(t == SoftwareBusCodes.stopElevator) {
            if (st == elevatorId) {
                Platform.runLater(() -> {
                    isEnabled = false;
                    applyEnabledUI();
                });
            }
        } else if(t == SoftwareBusCodes.carDispatch) {
            Platform.runLater(() -> {
                int assignedFloor = body;
                Direction d;
                if (assignedFloor > currentFloor) d = Direction.UP;
                else if (assignedFloor < currentFloor) d = Direction.DOWN;
                else d = Direction.IDLE;

                DualDotIndicatorPanel indicator = floorCallIndicators.get(assignedFloor);
                if (indicator != null) {
                    if (d == Direction.UP)  indicator.setDotLit(Direction.UP, true);
                    if (d == Direction.DOWN) indicator.setDotLit(Direction.DOWN, true);
                }
            });
        } else if(t == SoftwareBusCodes.cabinPosition) {
            Platform.runLater(() -> updateElevatorPosition(body, true));
            setDirection(Direction.IDLE);
        } else if(t == SoftwareBusCodes.doorStatus) {
            Platform.runLater(() -> setDoorStatus(body == 0));
        } else if(t == SoftwareBusCodes.displayDirection) {
            Platform.runLater(() -> {
                switch (body) {
                    case 0 -> setDirection(Direction.UP);
                    case 1 -> setDirection(Direction.DOWN);
                    default -> setDirection(Direction.IDLE);
                }
            });
        } else if(t == SoftwareBusCodes.displayFloor) {
            Platform.runLater(() -> {
                currentFloor = body;
                currentFloorDisplay.setText("" + body);
                carFloorLabel.setText("" + body);
            });
        } else {
            System.out.println("Unknown topic");
        }
    }



    // Movement display helper (animation only when POSITION messages arrive)
    private void updateElevatorPosition(int newFloor, boolean animate) {
        double targetY = (10 - newFloor) * TOTAL_FLOOR_HEIGHT;
        int floorsToTravel = Math.abs(newFloor - this.currentFloor);
        this.currentFloor = newFloor;

        // show the target floor in the displays
        this.currentFloorDisplay.setText(String.valueOf(newFloor));
        this.carFloorLabel.setText(String.valueOf(newFloor));

        if (animate) {
            elevatorAnimation.stop();
            elevatorAnimation.setDuration(
                    Duration.millis(Math.max(1, floorsToTravel) * ANIMATION_SPEED_PER_FLOOR));
            elevatorAnimation.setToY(targetY);
            elevatorAnimation.playFromStart();
        } else {
            movingCar.setTranslateY(targetY);
        }
        setDirection(Direction.IDLE);
    }


    private void setDoorStatus(boolean open) {
        this.isDoorOpen = open;
        String borderColor = open ? "white" : "black";
        movingCar.setStyle(
                "-fx-background-color: #606060;-fx-border-color: "
                        + borderColor + ";-fx-border-width: 0 2 0 2;");
    }


    private void closeDoor() {
        setDoorStatus(false);
    }


    private void setDirection(Direction d) {
        this.currentDirection = d;
        directionIndicator.setDirection(d);
    }


    // Getters
    public int getCurrentFloor()   { return currentFloor; }
    public boolean isDoorOpen()    { return isDoorOpen; }
    public boolean isAutoMode()    { return autoMode; }
    public boolean isFireMode()    { return isFireMode; }
    public boolean isEnabled()     { return isEnabled; }
}

