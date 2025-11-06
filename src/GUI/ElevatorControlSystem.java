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
 * Hosts the Command Center window and starts the BUS server.
 * - Starts one SoftwareBus(true) server.
 * - Creates one client for the CommandPanel to publish system-wide commands.
 * - Each ElevatorPanel creates its own client and subscribes to topics.
 */
public class ElevatorControlSystem extends Application {

    private SoftwareBus busServer;   // server socket
    private SoftwareBus ccClient;    // command panel's publisher client
    private ElevatorPanel[] elevators;
    private CommandPanel commandPanel;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Command Center");

        // Start BUS server
        busServer = new SoftwareBus(true);

        // Client for the right-side command panel
        ccClient = new SoftwareBus(false);

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #333333;");

        Label titleLabel = new Label("Command Center");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: white;");
        StackPane titlePane = new StackPane(titleLabel);
        titlePane.setPadding(new Insets(10));
        root.setTop(titleLabel);

        // Center: 4 elevators
        HBox elevatorContainer = new HBox(15);
        elevatorContainer.setAlignment(Pos.TOP_CENTER);
        elevatorContainer.setPadding(new Insets(10));

        elevators = new ElevatorPanel[4];
        for (int i = 0; i < 4; i++) {
            elevators[i] = new ElevatorPanel(i + 1); // each panel makes its own client + subscriptions
            elevatorContainer.getChildren().add(elevators[i]);
        }
        root.setCenter(new StackPane(elevatorContainer));

        // Right: command stack (publishes over bus)
        commandPanel = new CommandPanel(ccClient);
        root.setRight(commandPanel);

        Scene scene = new Scene(root, 1200, 720);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) { launch(args); }
}