package CommandCenter;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.geometry.Pos;
import javafx.geometry.Insets;

import Bus.*;

public class ElevatorControlSystem extends Application {

    private SoftwareBus busServer;
    private SoftwareBus ccClient;
    private ElevatorPanel[] elevators;
    private CommandPanel commandPanel;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Command Center");

        busServer = new SoftwareBus(true);
        ccClient = new SoftwareBus(false);

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #333333;");

        Label title = new Label("Command Center");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: white;");
        root.setTop(title);
        BorderPane.setMargin(title, new Insets(10));

        HBox elevatorContainer = new HBox(15);
        elevatorContainer.setAlignment(Pos.TOP_CENTER);
        elevatorContainer.setPadding(new Insets(10));

        elevators = new ElevatorPanel[4];
        for (int i = 0; i < 4; i++) {
            elevators[i] = new ElevatorPanel(i + 1);
            elevatorContainer.getChildren().add(elevators[i]);
        }
        root.setCenter(elevatorContainer);

        commandPanel = new CommandPanel(ccClient);
        root.setRight(commandPanel);

        Scene scene = new Scene(root, 1200, 720);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) { launch(args); }
}