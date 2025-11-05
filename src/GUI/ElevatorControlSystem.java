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
import bus.SoftwareBus;


/**
 * Starts the whole application.
 * It builds the window and communicates with elevator panels and control buttons.
 */
public class ElevatorControlSystem extends Application {

    private ElevatorAPI api;
    private ElevatorPanel[] elevators = new ElevatorPanel[4];
    private CommandPanel commandPanel;

    /**
     * sets up and shows the main window when the app starts.
     */
    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Command Center");
        SoftwareBus busServer = new SoftwareBus(true);
        this.api = new ElevatorAPI();

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

        api.setInitialUIState();

        int[] seq1 = {1, 8, 10, 4, 7, 1};
        int[] seq2 = {1, 9, 1, 6, 10, 2};
        int[] seq3 = {8, 1, 4, 7, 2, 9};
        int[] seq4 = {10, 5, 2, 6, 3, 1};

        api.setAndStartElevatorSequence(1, seq1);
        api.setAndStartElevatorSequence(2, seq2);
        api.setAndStartElevatorSequence(3, seq3);
        api.setAndStartElevatorSequence(4, seq4);

        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * Printing Status of Elevators using API.
     */
    public void startLoggingThread(ElevatorAPI api) {
        Runnable loggingTask = () -> {
            try {
                while (true) {
                    StringBuilder log = new StringBuilder();
                    log.append("\n--- ELEVATOR STATUS LOG ---\n");
                    log.append(String.format("System Mode: %s | System Running: %s\n",
                            api.getSystemMode(), api.isSystemRunning()));

                    for (int i = 1; i <= 4; i++) {
                        log.append(String.format(
                                "  [Elev %d] Floor: %-2d | Door: %-6s | Moving: %-5s | Dir: %-4s\n",
                                i,
                                api.getElevatorFloor(i),
                                api.isElevatorDoorOpen(i) ? "OPEN" : "CLOSED",
                                api.isElevatorMoving(i) ? "Yes" : "No",
                                api.getElevatorDirection(i)
                        ));
                    }
                    log.append("-----------------------------\n");

                    System.out.println(log.toString());
                    Thread.sleep(5000);
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

    private SoftwareBus busServer;
}