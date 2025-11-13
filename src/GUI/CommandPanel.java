package GUI;

import bus.SoftwareBus;
import Message.Message;
import Message.Commands;
import Message.Channels;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;
import javafx.scene.paint.Color;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.VPos;

/**
 * CommandPanel (BUS-driven)
 *
 * - No direct ElevatorAPI calls.
 * - Publishes integer command messages onto the SoftwareBus.
 *   topic: Channels.SYSTEM (0) to broadcast to all elevators
 *   subtopic: 0 (unused by our protocol)
 *   body: Commands.encode(OPCODE, ARG)
 *
 * Opcodes you’ll typically send:
 *   START, STOP, RESET, FIRE_ON, FIRE_CLEAR, ENABLE, DISABLE, GOTO(floor)
 *
 * Helpers included:
 *   sendToAll(opcode)
 *   sendGotoToCar(elevatorId, floor)  // targeted command (topic = elevatorId)
 */
public class CommandPanel extends GridPane {

    private final SoftwareBus bus; // Command Center's client socket

    // UI controls
    private final Label  modeDisplay;
    private final Button autoButton;
    private final Button fireControlButton;
    private final Button startButton;
    private final Button stopButton;
    private final Button resetButton;

    // Local UI state (purely for panel visuals)
    private boolean systemRunning = true;
    private String systemMode = "CENTRALIZED"; // CENTRALIZED | INDEPENDENT | FIRE

    // Styling
    private final String modeDisplayBaseStyle = "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-alignment: center; -fx-background-radius: 10;";
    private final String buttonBaseStyle      = "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-background-radius: 0;";
    private final String autoBtnStyle_ON  = "-fx-background-color: #00008B; -fx-border-color: #B8860B; -fx-border-width: 3; " + buttonBaseStyle;
    private final String autoBtnStyle_OFF = "-fx-background-color: #00008B; -fx-border-color: transparent; -fx-border-width: 3; " + buttonBaseStyle;
    private final String fireBtnColor_ON  = "-fx-background-color: #FF8C00;";
    private final String fireBtnColor_OFF = "-fx-background-color: #B22222;";
    private final String modeColor_CEN    = "-fx-background-color: #006400;";
    private final String modeColor_IND    = "-fx-background-color: #505050;";
    private final String modeColor_FIRE   = "-fx-background-color: red;";

    public CommandPanel(SoftwareBus bus) {
        this.bus = bus;

        // Layout grid
        setStyle("-fx-background-color: #333333;");
        setPadding(new Insets(10, 20, 10, 20));
        setHgap(10);
        setVgap(3);

        RowConstraints row0 = rc(20);
        RowConstraints row1 = rc(30);
        RowConstraints row2 = rc(30);
        RowConstraints row3 = rc(32);
        getRowConstraints().addAll(row0, row1, row2, row3);
        RowConstraints floorRow = rc(30);
        for (int i = 0; i < 10; i++) getRowConstraints().add(floorRow);

        // Mode display badge
        modeDisplay = new Label("CENTRALIZED");
        modeDisplay.setPrefSize(150, 65);
        modeDisplay.setStyle(modeDisplayBaseStyle + modeColor_CEN);
        add(modeDisplay, 0, 2, 1, 2);

        // Buttons → publish to bus
        fireControlButton = createButton("TEST FIRE", Color.web("#B22222"), 63, e -> onFirePressed());
        add(fireControlButton, 0, 4, 1, 2);

        autoButton = createButton("AUTO", Color.web("#00008B"), 63, e -> onAutoPressed());
        autoButton.setStyle(autoBtnStyle_ON);
        add(autoButton, 0, 6, 1, 2);

        startButton = createButton("START", Color.web("#228B22"), 63, e -> onStart());
        add(startButton, 0, 8, 1, 2);

        stopButton = createButton("STOP", Color.web("#B22222"), 63, e -> onStop());
        add(stopButton, 0, 10, 1, 2);

        resetButton = createButton("RESET", Color.BLACK, 63, e -> onReset());
        add(resetButton, 0, 12, 1, 2);

        updateButtonStates(true);
    }

    /* ---------- PUBLISH HELPERS ---------- */

    /** Broadcast a spreadsheet-style message to all cars. */
    private void publishAll(int topic, int body) {
        bus.publish(new Message(topic, 0, body));
    }

    /** Send a spreadsheet-style message to one car (subtopic = elevatorId). */
    public void publishToCar(int topic, int elevatorId, int body) {
        if (elevatorId < 1 || elevatorId > 4) return;
        bus.publish(new Message(topic, elevatorId, body));
    }

    /** Convenience: tell one car to go to a floor (clamped 1..10). */
    public void sendGotoToCar(int elevatorId, int floor) {
        int f = Math.max(1, Math.min(10, floor));
        publishToCar(elevatorId, Commands.GOTO, f);
    }

    /** Convenience: broadcast a simple opcode to all cars. */
    public void sendToAll(int opcode) { publishAll(opcode, 0); }

    /* ---------- BUTTON HANDLERS (publish + update local UI) ---------- */

    private void onStart() {
        systemRunning = true;
        publishAll(2, 0);       // Topic 2 = System Start, body 0000
        updateButtonStates(true);
    }

    private void onStop() {
        systemRunning = false;
        publishAll(1, 0);       // Topic 1 = System Stop
        updateButtonStates(false);
    }

    private void onReset() {
        systemRunning = true;
        systemMode = "CENTRALIZED";
        publishAll(3, 0);       // Topic 3 = System Reset
        updateForReset();
    }

    private void onFirePressed() {
        if ("FIRE".equals(systemMode)) {
            systemMode = "CENTRALIZED";
            publishAll(4, 0);   // Topic 4 = Clear Fire
            updateForFireMode(false);
        } else {
            systemMode = "FIRE";
            publishAll(5, 1110); // Topic 5, body 1110 = Test Fire
            updateForFireMode(true);
        }
    }

    private void onAutoPressed() {
        if ("FIRE".equals(systemMode)) return;
        if ("CENTRALIZED".equals(systemMode)) {
            systemMode = "INDEPENDENT";
            publishAll(5, 1100); // Independent
            updateForAutoMode("INDEPENDENT");
        } else {
            systemMode = "CENTRALIZED";
            publishAll(5, 1000); // Centralized
            updateForAutoMode("CENTRALIZED");
        }
    }

    /* ---------- LOCAL UI HELPERS (purely visual) ---------- */

    private Button createButton(String text, Color bgColor, double height, EventHandler<ActionEvent> listener) {
        Button button = new Button(text);
        String hex = String.format("#%02X%02X%02X",
                (int) (bgColor.getRed() * 255),
                (int) (bgColor.getGreen() * 255),
                (int) (bgColor.getBlue() * 255));
        button.setStyle("-fx-background-color: " + hex + ";" + buttonBaseStyle);
        button.setPrefSize(150, height);
        button.setMaxHeight(height);
        if (listener != null) button.setOnAction(listener);
        return button;
    }

    private RowConstraints rc(double h) {
        RowConstraints rc = new RowConstraints(h);
        rc.setValignment(VPos.CENTER);
        return rc;
    }

    public void updateButtonStates(boolean isRunning) {
        startButton.setDisable(isRunning);
        stopButton.setDisable(!isRunning);
        fireControlButton.setDisable(!isRunning);
        autoButton.setDisable(!isRunning);
        resetButton.setDisable(!isRunning);
    }

    public void updateForReset() {
        modeDisplay.setText("CENTRALIZED");
        modeDisplay.setStyle(modeDisplayBaseStyle + modeColor_CEN);
        fireControlButton.setText("TEST FIRE");
        fireControlButton.setStyle(fireControlButton.getStyle().replaceFirst("-fx-background-color: #.*?;", fireBtnColor_OFF));
        autoButton.setStyle(autoBtnStyle_ON);
        updateButtonStates(true);
    }

    public void updateForFireMode(boolean isFire) {
        if (isFire) {
            modeDisplay.setText("FIRE");
            modeDisplay.setStyle(modeDisplayBaseStyle + modeColor_FIRE);
            fireControlButton.setText("CLEAR FIRE");
            fireControlButton.setStyle(fireControlButton.getStyle().replaceFirst("-fx-background-color: #.*?;", fireBtnColor_ON));
        } else {
            modeDisplay.setText("CENTRALIZED");
            modeDisplay.setStyle(modeDisplayBaseStyle + modeColor_CEN);
            fireControlButton.setText("TEST FIRE");
            fireControlButton.setStyle(fireControlButton.getStyle().replaceFirst("-fx-background-color: #.*?;", fireBtnColor_OFF));
            autoButton.setStyle(autoBtnStyle_ON);
        }
    }

    public void updateForAutoMode(String mode) {
        if ("CENTRALIZED".equals(mode)) {
            modeDisplay.setText("CENTRALIZED");
            modeDisplay.setStyle(modeDisplayBaseStyle + modeColor_CEN);
            autoButton.setStyle(autoBtnStyle_ON);
        } else {
            modeDisplay.setText("INDEPENDENT");
            modeDisplay.setStyle(modeDisplayBaseStyle + modeColor_IND);
            autoButton.setStyle(autoBtnStyle_OFF);
        }
    }
}