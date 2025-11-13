package GUI;

import bus.SoftwareBus;
import Message.Message;

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
 * CommandPanel (BUS-driven, spreadsheet protocol)
 *
 * Uses the BUS command spec:
 *
 *  Topic  Subtopic   Body(4 ints)  Meaning
 *    1       0       {0,0,0,0}    System Stop        (all elevators)
 *    2       0       {0,0,0,0}    System Start       (all elevators)
 *    3       0       {0,0,0,0}    System Reset       (all elevators)
 *    4       0       {0,0,0,0}    Clear Fire         (all elevators)
 *    5       0       {1,0,0,0}    Mode = Centralized
 *    5       0       {1,1,0,0}    Mode = Independent
 *    5       0       {1,1,1,0}    Mode = Test Fire
 *    6     1..4      {0,0,0,0}    Start individual elevator
 *    7     1..4      {0,0,0,0}    Stop individual elevator
 *
 * Here we encode Body(4 ints) as a simple 4-digit int:
 *   {1,0,0,0} → 1000
 *   {1,1,0,0} → 1100
 *   {1,1,1,0} → 1110
 *
 * All messages are sent over SoftwareBus using:
 *   new Message(topic, subtopic, bodyInt)
 */
public class CommandPanel extends GridPane {

    private final SoftwareBus bus; // Command Center's BUS client

    // UI controls
    private final Label  modeDisplay;
    private final Button autoButton;
    private final Button fireControlButton;
    private final Button startButton;
    private final Button stopButton;
    private final Button resetButton;

    // Local UI state (purely visual)
    private boolean systemRunning = true;
    private String systemMode = "CENTRALIZED"; // CENTRALIZED | INDEPENDENT | FIRE

    // Styling
    private final String modeDisplayBaseStyle =
            "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; " +
                    "-fx-alignment: center; -fx-background-radius: 10;";
    private final String buttonBaseStyle =
            "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; " +
                    "-fx-background-radius: 0;";
    private final String autoBtnStyle_ON  =
            "-fx-background-color: #00008B; -fx-border-color: #B8860B; -fx-border-width: 3; "
                    + buttonBaseStyle;
    private final String autoBtnStyle_OFF =
            "-fx-background-color: #00008B; -fx-border-color: transparent; -fx-border-width: 3; "
                    + buttonBaseStyle;
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
        for (int i = 0; i < 10; i++) {
            getRowConstraints().add(floorRow);
        }

        // Mode display badge
        modeDisplay = new Label("CENTRALIZED");
        modeDisplay.setPrefSize(150, 65);
        modeDisplay.setStyle(modeDisplayBaseStyle + modeColor_CEN);
        add(modeDisplay, 0, 2, 1, 2);

        // Buttons → publish spreadsheet-style messages
        fireControlButton = createButton("TEST FIRE", Color.web("#B22222"), 63,
                e -> onFirePressed());
        add(fireControlButton, 0, 4, 1, 2);

        autoButton = createButton("AUTO", Color.web("#00008B"), 63,
                e -> onAutoPressed());
        autoButton.setStyle(autoBtnStyle_ON);
        add(autoButton, 0, 6, 1, 2);

        startButton = createButton("START", Color.web("#228B22"), 63,
                e -> onStart());
        add(startButton, 0, 8, 1, 2);

        stopButton = createButton("STOP", Color.web("#B22222"), 63,
                e -> onStop());
        add(stopButton, 0, 10, 1, 2);

        resetButton = createButton("RESET", Color.BLACK, 63,
                e -> onReset());
        add(resetButton, 0, 12, 1, 2);

        updateButtonStates(true);
    }

    /* ---------- BUS PUBLISH HELPERS ---------- */

    /** Broadcast a message to all elevators (subtopic = 0). */
    private void publishAll(int topic, int body) {
        bus.publish(new Message(topic, 0, body));
    }

    /** Send a message to one elevator (subtopic = elevatorId 1..4). */
    public void publishToCar(int topic, int elevatorId, int body) {
        if (elevatorId < 1 || elevatorId > 4) return;
        bus.publish(new Message(topic, elevatorId, body));
    }

    /* ---------- BUTTON HANDLERS ---------- */

    private void onStart() {
        systemRunning = true;
        // Topic 2, Subtopic 0, Body 0000 → System Start (all elevators)
        publishAll(2, 0);
        updateButtonStates(true);
    }

    private void onStop() {
        systemRunning = false;
        // Topic 1, Subtopic 0, Body 0000 → System Stop (all elevators)
        publishAll(1, 0);
        updateButtonStates(false);
    }

    private void onReset() {
        systemRunning = true;
        systemMode = "CENTRALIZED";
        // Topic 3, Subtopic 0, Body 0000 → System Reset (all elevators)
        publishAll(3, 0);
        updateForReset();
    }

    private void onFirePressed() {
        if ("FIRE".equals(systemMode)) {
            // Clear fire
            systemMode = "CENTRALIZED";
            // Topic 4, Body 0000 → Clear Fire (all elevators)
            publishAll(4, 0);
            updateForFireMode(false);
        } else {
            // Enter Fire mode
            systemMode = "FIRE";
            // Topic 5, Body 1110 → Mode: Test Fire (all elevators)
            publishAll(5, 1110);
            updateForFireMode(true);
        }
    }

    private void onAutoPressed() {
        if ("FIRE".equals(systemMode)) return; // ignore during FIRE

        if ("CENTRALIZED".equals(systemMode)) {
            // Switch to INDEPENDENT
            systemMode = "INDEPENDENT";
            // Topic 5, Body 1100 → Mode: Independent
            publishAll(5, 1100);
            updateForAutoMode("INDEPENDENT");
        } else {
            // Back to CENTRALIZED
            systemMode = "CENTRALIZED";
            // Topic 5, Body 1000 → Mode: Centralized
            publishAll(5, 1000);
            updateForAutoMode("CENTRALIZED");
        }
    }

    /* ---------- LOCAL UI HELPERS (purely visual) ---------- */

    private Button createButton(String text, Color bgColor, double height,
                                EventHandler<ActionEvent> listener) {
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
        fireControlButton.setStyle(
                fireControlButton.getStyle()
                        .replaceFirst("-fx-background-color: #.*?;", fireBtnColor_OFF));
        autoButton.setStyle(autoBtnStyle_ON);
        updateButtonStates(true);
    }

    public void updateForFireMode(boolean isFire) {
        if (isFire) {
            modeDisplay.setText("FIRE");
            modeDisplay.setStyle(modeDisplayBaseStyle + modeColor_FIRE);
            fireControlButton.setText("CLEAR FIRE");
            fireControlButton.setStyle(
                    fireControlButton.getStyle()
                            .replaceFirst("-fx-background-color: #.*?;", fireBtnColor_ON));
        } else {
            modeDisplay.setText("CENTRALIZED");
            modeDisplay.setStyle(modeDisplayBaseStyle + modeColor_CEN);
            fireControlButton.setText("TEST FIRE");
            fireControlButton.setStyle(
                    fireControlButton.getStyle()
                            .replaceFirst("-fx-background-color: #.*?;", fireBtnColor_OFF));
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