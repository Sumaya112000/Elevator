package GUI;
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
 * This is the main class that starts the whole application.
 * It builds the window and acts like the "boss" for all the
 * elevator panels and control buttons.
 */
public class ElevatorControlSystem extends Application {

    private String systemMode = "CENTRALIZED";
    private boolean systemRunning = true;

    private ElevatorPanel[] elevators = new ElevatorPanel[4];
    private CommandPanel commandPanel;

    /**
     * This method sets up and shows the main window when the app starts.
     */
    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Command Center");

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #333333;"); // Dark Gray

        Label titleLabel = new Label("Command Center");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: white;");
        StackPane titlePane = new StackPane(titleLabel);
        titlePane.setPadding(new Insets(10));
        root.setTop(titleLabel);

        HBox elevatorContainer = new HBox(15);
        elevatorContainer.setAlignment(Pos.TOP_CENTER);
        elevatorContainer.setPadding(new Insets(10));

        for (int i = 0; i < 4; i++) {
            elevators[i] = new ElevatorPanel(i + 1, this);
            elevatorContainer.getChildren().add(elevators[i]);
        }

        StackPane centerWrapper = new StackPane(elevatorContainer);
        centerWrapper.setStyle("-fx-background-color: #333333;");
        root.setCenter(centerWrapper);

        commandPanel = new CommandPanel(this, elevators);
        root.setRight(commandPanel);

        // Set up the buttons and labels for the first time
        setInitialUIState();

        // Give each elevator its list of floors to visit
        int[] seq1 = {1, 8, 10, 4, 7, 1};
        int[] seq2 = {1, 9, 1, 6, 10, 2};
        int[] seq3 = {8, 1, 4, 7, 2, 9};
        int[] seq4 = {10, 5, 2, 6, 3, 1};

        // Tell the elevators to start their automatic loops
        elevators[0].startAutomatedLoop(seq1);
        elevators[1].startAutomatedLoop(seq2);
        elevators[2].startAutomatedLoop(seq3);
        elevators[3].startAutomatedLoop(seq4);

        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * Changes the system's main mode (like "CENTRALIZED" or "FIRE").
     */
    public void setSystemMode(String mode) {
        this.systemMode = mode;
        for (ElevatorPanel elevator : elevators) {
            elevator.onSystemModeChange(mode);
        }
        System.out.println("System Mode Changed to: " + mode);
    }

    /**
     * Gets the system's current mode.
     */
    public String getSystemMode() {
        return systemMode;
    }

    /**
     * Turns the whole system "ON" (running) or "OFF" (stopped).
     */
    public void setSystemRunning(boolean running) {
        this.systemRunning = running;
        if (commandPanel != null) {
            commandPanel.updateButtonStates(running);
        }
        System.out.println("System Power State: " + (running ? "ON" : "OFF"));
    }

    /**
     * Checks if the system is currently "ON".
     */
    public boolean isSystemRunning() {
        return systemRunning;
    }

    /**
     * Sets up the control buttons and labels to their starting look.
     */
    public void setInitialUIState() {
        setSystemMode("CENTRALIZED");
        setSystemRunning(true);

        if (commandPanel != null) {
            commandPanel.updateForReset();
        }
    }


    /**
     * This is called when the user clicks the "RESET" button.
     * It resets the buttons and tells all elevators to go to floor 1.
     */
    public void resetSystemToInitialState() {
        setInitialUIState();

        for (ElevatorPanel elevator : elevators) {
            elevator.forceReset();
        }
    }

    /**
     * This is the main starting point that launches the application.
     */
    public static void main(String[] args) {
        launch(args);
    }
}