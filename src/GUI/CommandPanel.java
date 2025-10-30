package GUI;
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
 * CommandPanel represents the master control panel for the entire elevator system.
 * It provides system-wide controls like START, STOP, RESET, Fire Mode, and
 * toggling between Centralized and Independent operation modes.
 * It does NOT control individual elevator cars (that's ElevatorPanel).
 */
public class CommandPanel extends GridPane {

    private final ElevatorControlSystem system;
    private final ElevatorPanel[] elevators;
    private final Label modeDisplay;
    private final Button autoButton;
    private final Button fireControlButton;

    private final Button startButton;
    private final Button stopButton;
    private final Button resetButton;

    private final String modeDisplayBaseStyle = "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-alignment: center; -fx-background-radius: 10;";

    private final String buttonBaseStyle = "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-background-radius: 0;";

    private final String autoBtnStyle_ON = "-fx-background-color: #00008B; -fx-border-color: #B8860B; -fx-border-width: 3; " + buttonBaseStyle;
    private final String autoBtnStyle_OFF = "-fx-background-color: #00008B; -fx-border-color: transparent; -fx-border-width: 3; " + buttonBaseStyle;


    private final String fireBtnColor_ON = "-fx-background-color: #FF8C00;"; // Orange
    private final String fireBtnColor_OFF = "-fx-background-color: #B22222;"; // Red
    private final String modeDisplayColor_CEN = "-fx-background-color: #006400;"; // Dark Green
    private final String modeDisplayColor_IND = "-fx-background-color: #505050;"; // Dark Gray
    private final String modeDisplayColor_FIRE = "-fx-background-color: red;";

    /**
     * Constructs the main command panel and lays out all its UI controls.
     */
    public CommandPanel(ElevatorControlSystem system, ElevatorPanel[] elevators) {
        this.system = system;
        this.elevators = elevators;

        setStyle("-fx-background-color: #333333;");
        setPadding(new Insets(10, 20, 10, 20));
        setHgap(10);
        setVgap(3);

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

        modeDisplay = new Label("CENTRALIZED");
        modeDisplay.setPrefSize(150, 65);
        modeDisplay.setStyle(modeDisplayBaseStyle + modeDisplayColor_CEN);
        add(modeDisplay, 0, 2, 1, 2);

        fireControlButton = createButton("TEST FIRE", Color.web("#B22222"), 63, e -> fireAction());
        add(fireControlButton, 0, 4, 1, 2);

        autoButton = createButton("AUTO", Color.web("#00008B"), 63, e -> autoToggleAction());
        autoButton.setStyle(autoBtnStyle_OFF);
        add(autoButton, 0, 6, 1, 2);

        startButton = createButton("START", Color.web("#228B22"), 63, e -> {
            system.setSystemRunning(true);
        });
        add(startButton, 0, 8, 1, 2);

        stopButton = createButton("STOP", Color.web("#B22222"), 63, e -> {
            system.setSystemRunning(false);
        });
        add(stopButton, 0, 10, 1, 2);

        resetButton = createButton("RESET", Color.BLACK, 63, e -> {
            system.resetSystemToInitialState();
        });
        add(resetButton, 0, 12, 1, 2);
    }

    /**
     * A helper factory method to create and style the command buttons consistently.
     */
    private Button createButton(String text, Color bgColor, double height, EventHandler<ActionEvent> listener) {
        Button button = new Button(text);
        String hexColor = String.format("#%02X%02X%02X",
                (int) (bgColor.getRed() * 255),
                (int) (bgColor.getGreen() * 255),
                (int) (bgColor.getBlue() * 255));

        button.setStyle(
                "-fx-background-color: " + hexColor + ";" +
                        buttonBaseStyle
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
     */
    public void updateForReset() {
        modeDisplay.setText("CENTRALIZED");
        modeDisplay.setStyle(modeDisplayBaseStyle + modeDisplayColor_CEN);

        fireControlButton.setText("TEST FIRE");
        fireControlButton.setStyle(fireControlButton.getStyle().replaceFirst("-fx-background-color: #.*?;", fireBtnColor_OFF));

        autoButton.setStyle(autoBtnStyle_ON);

        updateButtonStates(true);
    }

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
     */
    private void testFireLogic() {
        if (!system.getSystemMode().equals("FIRE")) {
            system.setSystemMode("FIRE");
            modeDisplay.setText("FIRE");
            modeDisplay.setStyle(modeDisplayBaseStyle + modeDisplayColor_FIRE);
            fireControlButton.setText("CLEAR FIRE");
            fireControlButton.setStyle(fireControlButton.getStyle().replaceFirst("-fx-background-color: #.*?;", fireBtnColor_ON));

            for (ElevatorPanel elevator : elevators) {
                elevator.forceMoveAndOpen(1);
            }
            System.out.println("Action: Fire Test - ALL elevators at F1, Doors OPEN.");
        }
    }

    /**
     * Clears the fire emergency mode.
     */
    private void clearFireLogic() {
        system.setSystemMode("INDEPENDENT");
        modeDisplay.setText("INDEPENDENT");
        modeDisplay.setStyle(modeDisplayBaseStyle + modeDisplayColor_IND);
        fireControlButton.setText("TEST FIRE");
        fireControlButton.setStyle(fireControlButton.getStyle().replaceFirst("-fx-background-color: #.*?;", fireBtnColor_OFF));

        for (ElevatorPanel elevator : elevators) {
            elevator.releaseAndClose();
        }
        System.out.println("Action: Clear Fire - Doors CLOSED, Independent Operation Restored.");
    }

    /**
     * Toggles the system operation mode between CENTRALIZED and INDEPENDENT.
     */
    private void autoToggleAction() {
        if (system.getSystemMode().equals("INDEPENDENT")) {
            system.setSystemMode("CENTRALIZED");
            modeDisplay.setText("CENTRALIZED");
            modeDisplay.setStyle(modeDisplayBaseStyle + modeDisplayColor_CEN);

            autoButton.setStyle(autoBtnStyle_ON);

        } else if (system.getSystemMode().equals("CENTRALIZED")) {
            system.setSystemMode("INDEPENDENT");
            modeDisplay.setText("INDEPENDENT");
            modeDisplay.setStyle(modeDisplayBaseStyle + modeDisplayColor_IND);

            autoButton.setStyle(autoBtnStyle_OFF);
        }
    }
}