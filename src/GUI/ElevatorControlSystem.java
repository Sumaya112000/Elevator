package GUI;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import bus.SoftwareBus;

/**
 * Starts the whole application with Software Bus integration.
 */
public class ElevatorControlSystem extends Application {

    private ElevatorAPI api;
    private ElevatorPanel[] elevators = new ElevatorPanel[4];
    private CommandPanel commandPanel;
    private SoftwareBus busServer;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Command Center");

        // Create software bus in server mode (command center is the server)
        this.busServer = new SoftwareBus(true);

        // Create API with bus integration
        this.api = new ElevatorAPI(busServer);

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #333333;");

        Label titleLabel = new Label("Command Center");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: white;");
        StackPane titlePane = new StackPane(titleLabel);
        titlePane.setPadding(new Insets(10));
        root.setTop(titleLabel);

        HBox elevatorContainer = new HBox(15);
        elevatorContainer.setAlignment(Pos.TOP_CENTER);
        elevatorContainer.setPadding(new Insets(10));

        for (int i = 0; i < 4; i++) {
            elevators[i] = new ElevatorPanel(i + 1, api);
            elevatorContainer.getChildren().add(elevators[i]);
        }

        StackPane centerWrapper = new StackPane(elevatorContainer);
        centerWrapper.setStyle("-fx-background-color: #333333;");
        root.setCenter(centerWrapper);

        commandPanel = new CommandPanel(api);
        root.setRight(commandPanel);

        api.registerElevators(elevators);
        api.registerCommandPanel(commandPanel);

        // Start the background logging thread
        startLoggingThread(api);

        // Start the enhanced test harness
        startTestHarness(api);

        // Set the initial UI state
        api.setInitialUIState();

        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * Enhanced test harness with individual elevator control
     */
    public void startTestHarness(ElevatorAPI api) {
        Runnable testTask = () -> {
            try {
                // Wait for 5 seconds for the app to start
                Thread.sleep(5000);

                // Test individual elevator control
                System.out.println("Testing individual elevator control...");

                // Stop elevator 2 individually
                Platform.runLater(() -> api.sendIndividualStopCommand(2));
                Thread.sleep(2000);

                // Start elevator 2 individually
                Platform.runLater(() -> api.sendIndividualStartCommand(2));
                Thread.sleep(2000);

                // Stop elevator 3 individually
                Platform.runLater(() -> api.sendIndividualStopCommand(3));
                Thread.sleep(2000);

                // Normal floor requests
                Platform.runLater(() -> api.sendFloorRequest(1, 10));
                Thread.sleep(2000);
                Platform.runLater(() -> api.sendFloorRequest(2, 4));
                Thread.sleep(2000);
                Platform.runLater(() -> api.sendFloorRequest(3, 6)); // This should be ignored (elevator 3 is stopped)
                Thread.sleep(2000);
                Platform.runLater(() -> api.sendFloorRequest(4, 9));

                Thread.sleep(7000);

                // Start elevator 3 again
                Platform.runLater(() -> api.sendIndividualStartCommand(3));
                Thread.sleep(2000);

                // Test fire mode
                Platform.runLater(() -> api.sendFireCommand());
                Thread.sleep(7000);

                // Clear fire mode
                Platform.runLater(() -> api.sendClearFireCommand());
                Thread.sleep(1000);

                // System stop
                Platform.runLater(() -> api.sendStopCommand());
                Thread.sleep(2000);

                // System start
                Platform.runLater(() -> api.sendStartCommand());
                Thread.sleep(1000);

            } catch (InterruptedException e) {
                System.out.println("[Test harness interrupted.]");
            }
        };
        Thread testThread = new Thread(testTask);
        testThread.setDaemon(true);
        testThread.start();
    }

    /**
     * Printing Status of Elevators using API.
     */
    public void startLoggingThread(ElevatorAPI api) {
        Runnable loggingTask = () -> {
            try {
                while (true) {
                    StringBuilder log = new StringBuilder();
                    log.append("\nELEVATOR STATUS LOG \n");
                    log.append(String.format("System Mode: %s | System Running: %s\n",
                            api.getSystemMode(), api.isSystemRunning()));

                    for (int i = 1; i <= 4; i++) {
                        log.append(String.format(
                                "  [Elev %d] Floor: %-2d | Door: %-6s | Moving: %-5s | Dir: %-4s | Enabled: %-5s\n",
                                i,
                                api.getElevatorFloor(i),
                                api.isElevatorDoorOpen(i) ? "OPEN" : "CLOSED",
                                api.isElevatorMoving(i) ? "Yes" : "No",
                                api.getElevatorDirection(i),
                                api.isElevatorEnabled(i) ? "Yes" : "No"
                        ));
                    }

                    System.out.println(log.toString());
                    Thread.sleep(5000); // Log every 5 seconds
                }
            } catch (InterruptedException e) {
                System.out.println("Logging thread interrupted.");
            }
        };

        Thread loggingThread = new Thread(loggingTask);
        loggingThread.setDaemon(true);
        loggingThread.start();
    }

    /**
     * Launching the application.
     */
    public static void main(String[] args) {
        launch(args);
    }
}