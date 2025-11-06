package GUI;

import bus.SoftwareBus;
import Message.Message;
import Message.Commands;
import Message.Channels;

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
 * ElevatorPanel now LISTENS on the SoftwareBus:
 * Subscriptions: (SYSTEM,0) and (id,0)
 * Acts on messages with opcodes from Commands.
 */
public class ElevatorPanel extends VBox {

    public enum Direction { UP, DOWN, IDLE }

    private final int elevatorId;
    private int currentFloor;
    private Direction currentDirection = Direction.IDLE;
    private boolean isMoving = false;
    private boolean isDoorOpen = false;
    private boolean isEnabled = true; // toggled by ENABLE/DISABLE (or UI button)

    private final SoftwareBus bus; // this car's client

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
    private PauseTransition waitAtFloor;

    private final ConcurrentHashMap<Integer, DualDotIndicatorPanel> floorCallIndicators = new ConcurrentHashMap<>();
    private final DirectionIndicatorPanel directionIndicator;
    private final Label currentFloorDisplay;

    private static final double FLOOR_HEIGHT = 30.0;
    private static final double FLOOR_SPACING = 3.0;
    private static final double TOTAL_FLOOR_HEIGHT = FLOOR_HEIGHT + FLOOR_SPACING;
    private static final double ANIMATION_SPEED_PER_FLOOR = 400.0;

    /* === UI subcomponents === */

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
            if (direction == Direction.UP) upDot.setFill(color);
            else if (direction == Direction.DOWN) downDot.setFill(color);
        }
    }

    private class DirectionIndicatorPanel extends VBox {
        private final Polygon upTriangle, downTriangle;
        private final Color UNLIT_COLOR = Color.BLACK;
        DirectionIndicatorPanel() {
            super(6);
            upTriangle = new Polygon(6.0, 0.0, 0.0, 8.0, 12.0, 8.0);
            downTriangle = new Polygon(6.0, 8.0, 0.0, 0.0, 12.0, 0.0);
            setDirection(Direction.IDLE);
            getChildren().addAll(upTriangle, downTriangle);
            setAlignment(Pos.CENTER);
            setPadding(new Insets(5));
        }
        void setDirection(Direction newDirection) {
            upTriangle.setFill(newDirection == Direction.UP ? Color.WHITE : UNLIT_COLOR);
            downTriangle.setFill(newDirection == Direction.DOWN ? Color.WHITE : UNLIT_COLOR);
        }
    }

    /* === ctor === */

    public ElevatorPanel(int id) {
        super(3);
        this.elevatorId = id;



        this.bus = new SoftwareBus(false); // client
        // subscribe to system-wide and per-car channels
        bus.subscribe(Channels.SYSTEM, 0);
        bus.subscribe(id, 0);





        setAlignment(Pos.CENTER);
        setStyle("-fx-background-color: #333333;");
        setPrefWidth(100);

        this.currentFloor = 1;

        Label title = new Label("Elevator " + id);
        title.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

        mainControlButton = new Button(btnText_STOP);
        mainControlButton.setStyle(btnColor_STOP + " -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 0;");
        mainControlButton.setPrefWidth(90);
        mainControlButton.setOnAction(e -> toggleEnabledState());

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

        for (int i = 10; i >= 1; i--) floorButtonColumn.getChildren().add(createFloorRow(i));

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






        // Start BUS listen loop
        startBusListener();



    }

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

    /* === BUS handling === */
    /** Background thread: receive messages and apply them to this car’s UI/logic. */
    private void startBusListener() {
        Thread t = new Thread(() -> {
            while (true) {
                // Get any system-wide message
                Message m = bus.get(Channels.SYSTEM, 0);
                if (m != null) handleCommand(m);
                // Get any per-car message
                Message m2 = bus.get(elevatorId, 0);
                if (m2 != null) handleCommand(m2);

                try { Thread.sleep(10); } catch (InterruptedException ignored) {}
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private void handleCommand(Message message) {
        int op = Commands.opcode(message.getBody());
        int arg = Commands.arg(message.getBody());

        switch (op) {
            case Commands.START -> { isEnabled = true; Platform.runLater(this::applyEnabledUI); }
            case Commands.STOP  -> Platform.runLater(() -> goToAndOpen(1)); // simple "recall and open"
            case Commands.RESET -> Platform.runLater(this::resetToOne);
            case Commands.FIRE_ON -> Platform.runLater(() -> goToAndOpen(1));
            case Commands.FIRE_CLEAR -> Platform.runLater(this::closeDoor);
            case Commands.ENABLE -> { isEnabled = true; Platform.runLater(this::applyEnabledUI); }
            case Commands.DISABLE -> { isEnabled = false; Platform.runLater(this::applyEnabledUI); }
            case Commands.GOTO -> {
                int target = Math.max(1, Math.min(10, arg));
                Platform.runLater(() -> moveTo(target));
            }
            default -> { /* ignore unknown */ }
        }
    }

    private void applyEnabledUI() {
        if (isEnabled) {
            mainControlButton.setText(btnText_STOP);
            mainControlButton.setStyle(btnColor_STOP + " -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 0;");
        } else {
            mainControlButton.setText(btnText_START);
            mainControlButton.setStyle(btnColor_START + " -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 0;");
        }
    }

    /* === Movement/door helpers === */

    private void updateElevatorPosition(int newFloor, boolean animate) {
        double targetY = (10 - newFloor) * TOTAL_FLOOR_HEIGHT;
        int floorsToTravel = Math.abs(newFloor - this.currentFloor);
        this.currentFloor = newFloor;

        this.currentFloorDisplay.setText(String.valueOf(newFloor));
        this.carFloorLabel.setText(String.valueOf(newFloor));

        if (animate) {
            elevatorAnimation.stop();
            elevatorAnimation.setDuration(Duration.millis(Math.max(1, floorsToTravel) * ANIMATION_SPEED_PER_FLOOR));
            elevatorAnimation.setToY(targetY);
            elevatorAnimation.play();
        } else {
            movingCar.setTranslateY(targetY);
        }
    }

    private void setDoorStatus(boolean open) {
        this.isDoorOpen = open;
        String borderColor = open ? "white" : "black";
        movingCar.setStyle("-fx-background-color: #606060;-fx-border-color: " + borderColor + ";-fx-border-width: 0 2 0 2;");
    }

    private void setDirection(Direction newDirection) {
        this.currentDirection = newDirection;
        directionIndicator.setDirection(newDirection);
    }

    private void moveTo(int targetFloor) {
        if (!isEnabled) return;
        setDoorStatus(false);
        setDirection(targetFloor > currentFloor ? Direction.UP : (targetFloor < currentFloor ? Direction.DOWN : Direction.IDLE));
        updateElevatorPosition(targetFloor, true);
        elevatorAnimation.setOnFinished(e -> {
            setDirection(Direction.IDLE);
            setDoorStatus(true);
        });
    }

    private void goToAndOpen(int targetFloor) {
        setDoorStatus(false);
        moveTo(targetFloor);
        elevatorAnimation.setOnFinished(e -> setDoorStatus(true));
    }

    private void resetToOne() {
        setDoorStatus(false);
        moveTo(1);
        elevatorAnimation.setOnFinished(e -> setDoorStatus(true));
    }

    private void closeDoor() { setDoorStatus(false); }

    /* === Public getters (if needed elsewhere) === */
    public int getCurrentFloor() { return this.currentFloor; }
    public boolean isMoving() { return this.isMoving; } // kept for compatibility; not used now
    public boolean isDoorOpen() { return this.isDoorOpen; }
    public ElevatorPanel.Direction getCurrentDirection() { return this.currentDirection; }
}