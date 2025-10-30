import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.geometry.Pos;
import javafx.geometry.Insets;

/**
 * The main entry point for the JavaFX application.
 * This class initializes the main window and holds the central state
 * (systemMode, systemRunning) that is shared between the CommandPanel
 * and all the individual ElevatorPanels.
 */
public class ElevatorControlSystem extends Application {

    private String systemMode = "CENTRALIZED";
    private boolean systemRunning = true;

    private ElevatorPanel[] elevators = new ElevatorPanel[4];
    private CommandPanel commandPanel;

    /**
     * Initializes and displays the primary application window (Stage).
     * It sets up the BorderPane layout, creates the title, the CommandPanel,
     * and initializes the four ElevatorPanel instances.
     */
    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Command Center");

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #333333;"); // Dark Gray

        // Top Panel for the Title
        Label titleLabel = new Label("Command Center");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: white;");
        StackPane titlePane = new StackPane(titleLabel);
        titlePane.setPadding(new Insets(10));
        root.setTop(titlePane);

        // Center Panel for Elevators
        HBox elevatorContainer = new HBox(15);
        elevatorContainer.setAlignment(Pos.TOP_CENTER);
        elevatorContainer.setPadding(new Insets(10));

        // Create the 4 elevator panels
        for (int i = 0; i < 4; i++) {
            elevators[i] = new ElevatorPanel(i + 1, this);
            elevatorContainer.getChildren().add(elevators[i]);
        }

        // Wrapper to keep elevators centered
        StackPane centerWrapper = new StackPane(elevatorContainer);
        centerWrapper.setStyle("-fx-background-color: #333333;");
        root.setCenter(centerWrapper);

        // Right Command Panel
        commandPanel = new CommandPanel(this, elevators);
        root.setRight(commandPanel);

        // Set the system to its default state on launch
        resetSystemToInitialState();

        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * Sets the global operation mode (CENTRALIZED, INDEPENDENT, FIRE).
     * This method also notifies all individual elevators of the mode change
     * so they can update their internal logic.
     *
     * @param mode The new system mode (e.g., "CENTRALIZED").
     */
    public void setSystemMode(String mode) {
        this.systemMode = mode;
        // Tell elevators the mode changed
        for (ElevatorPanel elevator : elevators) {
            elevator.onSystemModeChange(mode);
        }
        System.out.println("System Mode Changed to: " + mode);
    }

    /**
     * Returns the current global operation mode.
     * @return The current system mode string.
     */
    public String getSystemMode() {
        return systemMode;
    }

    /**
     * Sets the system's power state (START/STOP).
     * This updates the CommandPanel to enable/disable the appropriate buttons.
     *
     * @param running True if the system is running, false if stopped.
     */
    public void setSystemRunning(boolean running) {
        this.systemRunning = running;
        // Update CommandPanel buttons based on state
        if (commandPanel != null) {
            commandPanel.updateButtonStates(running);
        }
        System.out.println("System Power State: " + (running ? "ON" : "OFF"));
    }

    /**
     * Checks if the system is currently in a "running" (ON) state.
     * @return True if running, false if stopped.
     */
    public boolean isSystemRunning() {
        return systemRunning;
    }

    /**
     * Resets the entire simulation to its default starting state.
     * This sets the mode to CENTRALIZED, turns the system ON,
     * resets the CommandPanel visuals, and forces all elevators
     * back to their initial state (Floor 1, doors closed).
     */
    public void resetSystemToInitialState() {
        setSystemMode("CENTRALIZED");
        setSystemRunning(true);

        if (commandPanel != null) {
            commandPanel.updateForReset();
        }

        for (ElevatorPanel elevator : elevators) {
            elevator.forceReset();
        }
    }

    /**
     * The main method to launch the JavaFX application.
     */
    public static void main(String[] args) {
        launch(args);
    }
}