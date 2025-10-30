import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;

/**
 * CommandPanel represents the master control panel for the entire elevator system.
 * It provides system-wide controls like START, STOP, RESET, Fire Mode, and
 * toggling between Centralized and Independent operation modes.
 * It does NOT control individual elevator cars (that's ElevatorPanel).
 */
public class CommandPanel extends GridPane {

    // --- Core System & UI Components ---
    private final ElevatorControlSystem system;
    private final ElevatorPanel[] elevators;
    private final Label modeDisplay;
    private final Button autoButton;
    private final Button fireControlButton;

    private final Button startButton;
    private final Button stopButton;
    private final Button resetButton;

    // --- Style Definitions ---
    private final String modeDisplayBaseStyle = "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-alignment: center; -fx-background-radius: 0;";
    private final String autoBtnColor_ON = "-fx-background-color: #B8860B;";
    private final String autoBtnColor_OFF = "-fx-background-color: #00008B;";
    private final String fireBtnColor_ON = "-fx-background-color: #FF8C00;";
    private final String fireBtnColor_OFF = "-fx-background-color: #B22222;";
    private final String modeDisplayColor_CEN = "-fx-background-color: #006400;";
    private final String modeDisplayColor_IND = "-fx-background-color: #505050;";
    private final String modeDisplayColor_FIRE = "-fx-background-color: red;";

    /**
     * Constructs the main command panel and lays out all its UI controls.
     *
     * @param system    The main ElevatorControlSystem to send commands to.
     * @param elevators A reference to all ElevatorPanels for fire mode control.
     */
    public CommandPanel(ElevatorControlSystem system, ElevatorPanel[] elevators) {
        this.system = system;
        this.elevators = elevators;

        // --- GRIDPANE SETUP ---
        setStyle("-fx-background-color: #333333;");
        setPadding(new Insets(10, 20, 10, 20));
        setHgap(10);
        setVgap(3);

        // --- ROW CONSTRAINTS (Defines the height of each row) ---
        RowConstraints row0 = new RowConstraints(20);
        row0.setValignment(VPos.CENTER);
        RowConstraints row1 = new RowConstraints(30);
        row1.setValignment(VPos.CENTER);
        RowConstraints row2 = new RowConstraints(30);
        row2.setValignment(VPos.CENTER);
        RowConstraints row3 = new RowConstraints(32);
        row3.setValignment(VPos.CENTER);
        RowConstraints floorRow = new RowConstraints(30);
        floorRow.setValignment(VPos.CENTER);

        getRowConstraints().addAll(row0, row1, row2, row3);
        for (int i = 0; i < 10; i++) {
            getRowConstraints().add(floorRow);
        }

        // --- CREATE AND ADD COMPONENTS TO THE GRID ---

        // 1. Mode Display (Spans 2 rows)
        modeDisplay = new Label("CENTRALIZED");
        modeDisplay.setPrefSize(150, 65); // Spans row 2 (30px) + row 3 (32px) + vgap (3px)
        modeDisplay.setStyle(modeDisplayBaseStyle + modeDisplayColor_CEN);
        add(modeDisplay, 0, 2, 1, 2);

        // 2. Fire Button (Spans 2 rows)
        fireControlButton = createButton("TEST FIRE", Color.web("#B22222"), 63, e -> fireAction());
        add(fireControlButton, 0, 4, 1, 2);

        // 3. AUTO Button (Spans 2 rows)
        autoButton = createButton("AUTO", Color.web("#B8860B"), 63, e -> autoToggleAction());
        add(autoButton, 0, 6, 1, 2);

        // 4. START Button (Spans 2 rows)
        startButton = createButton("START", Color.web("#228B22"), 63, e -> {
            system.setSystemRunning(true);
        });
        add(startButton, 0, 8, 1, 2);

        // 5. STOP Button (Spans 2 rows)
        stopButton = createButton("STOP", Color.web("#B22222"), 63, e -> {
            system.setSystemRunning(false);
        });
        add(stopButton, 0, 10, 1, 2);

        // 6. RESET Button (Spans 2 rows)
        resetButton = createButton("RESET", Color.BLACK, 63, e -> {
            system.resetSystemToInitialState();
        });
        add(resetButton, 0, 12, 1, 2);
    }

    /**
     * A helper factory method to create and style the command buttons consistently.
     *
     * @param text     The text to display on the button.
     * @param bgColor  The background color of the button.
     * @param height   The preferred height of the button.
     * @param listener The action to perform when the button is clicked.
     * @return A fully styled Button.
     */
    private Button createButton(String text, Color bgColor, double height, EventHandler<ActionEvent> listener) {
        Button button = new Button(text);
        String hexColor = String.format("#%02X%02X%02X",
                (int) (bgColor.getRed() * 255),
                (int) (bgColor.getGreen() * 255),
                (int) (bgColor.getBlue() * 255));
        button.setStyle(
                "-fx-background-color: " + hexColor + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-size: 14px;" +
                        "-fx-background-radius: 0;"
        );
        button.setPrefSize(150, height);
        button.setMaxHeight(height);
        if (listener != null) {
            button.setOnAction(listener);
        }
        return button;
    }

    /**
     * Toggles the enabled/disabled state of buttons based on the system's running state.
     * (e.g., disables START when running, disables STOP when stopped).
     *
     * @param isRunning True if the system is running, false if stopped.
     */
    public void updateButtonStates(boolean isRunning) {
        startButton.setDisable(isRunning);
        stopButton.setDisable(!isRunning);
        fireControlButton.setDisable(!isRunning);
        autoButton.setDisable(!isRunning);
        resetButton.setDisable(!isRunning);
    }

    /**
     * Resets the visual state of the command panel to its default (startup) appearance.
     * Called when the main RESET button is pressed.
     */
    public void updateForReset() {
        modeDisplay.setText("CENTRALIZED");
        modeDisplay.setStyle(modeDisplayBaseStyle + modeDisplayColor_CEN);

        fireControlButton.setText("TEST FIRE");
        fireControlButton.setStyle(fireControlButton.getStyle().replaceFirst("-fx-background-color: #.*?;", fireBtnColor_OFF));

        autoButton.setStyle(autoButton.getStyle().replaceFirst("-fx-background-color: #.*?;", autoBtnColor_ON));

        updateButtonStates(true); // Enable all buttons (except START)
    }

    // --- Action Handlers ---

    /**
     * Main handler for the fire button. Toggles between testing and clearing fire mode.
     */
    private void fireAction() {
        if (system.getSystemMode().equals("FIRE")) {
            clearFireLogic();
        } else {
            testFireLogic();
        }
    }

    /**
     * Activates the fire emergency mode.
     * Sets the system mode, updates UI, and commands all elevators to floor 1.
     */
    private void testFireLogic() {
        if (!system.getSystemMode().equals("FIRE")) {
            system.setSystemMode("FIRE");
            modeDisplay.setText("FIRE");
            modeDisplay.setStyle(modeDisplayBaseStyle + modeDisplayColor_FIRE);
            fireControlButton.setText("CLEAR FIRE");
            fireControlButton.setStyle(fireControlButton.getStyle().replaceFirst("-fx-background-color: #.*?;", fireBtnColor_ON));

            // Command all elevators to move to floor 1 and open doors
            for (ElevatorPanel elevator : elevators) {
                elevator.forceMoveAndOpen(1);
            }
            System.out.println("Action: Fire Test - ALL elevators at F1, Doors OPEN.");
        }
    }

    /**
     * Clears the fire emergency mode.
     * Sets the system to INDEPENDENT and allows elevators to resume normal operation.
     */
    private void clearFireLogic() {
        system.setSystemMode("INDEPENDENT");
        modeDisplay.setText("INDEPENDENT");
        modeDisplay.setStyle(modeDisplayBaseStyle + modeDisplayColor_IND);
        fireControlButton.setText("TEST FIRE");
        fireControlButton.setStyle(fireControlButton.getStyle().replaceFirst("-fx-background-color: #.*?;", fireBtnColor_OFF));

        // Release all elevators from fire mode
        for (ElevatorPanel elevator : elevators) {
            elevator.releaseAndClose();
        }
        System.out.println("Action: Clear Fire - Doors CLOSED, Independent Operation Restored.");
    }

    /**
     * Toggles the system operation mode between CENTRALIZED (all calls go to the
     * system) and INDEPENDENT (all calls are handled by the elevators individually).
     * This action is ignored if the system is in FIRE mode.
     */
    private void autoToggleAction() {
        if (system.getSystemMode().equals("INDEPENDENT")) {
            system.setSystemMode("CENTRALIZED");
            modeDisplay.setText("CENTRALIZED");
            modeDisplay.setStyle(modeDisplayBaseStyle + modeDisplayColor_CEN);
            autoButton.setStyle(autoButton.getStyle().replaceFirst("-fx-background-color: #.*?;", autoBtnColor_ON));
        } else if (system.getSystemMode().equals("CENTRALIZED")) {
            system.setSystemMode("INDEPENDENT");
            modeDisplay.setText("INDEPENDENT");
            modeDisplay.setStyle(modeDisplayBaseStyle + modeDisplayColor_IND);
            autoButton.setStyle(autoButton.getStyle().replaceFirst("-fx-background-color: #.*?;", autoBtnColor_OFF));
        }
        // Note: No 'else' block, so clicking this button during FIRE mode does nothing.
    }
}