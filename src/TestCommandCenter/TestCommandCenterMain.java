package TestCommandCenter;

import Bus.SoftwareBus;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class TestCommandCenterMain extends Application {
    private int topic = 2;
    private int subtopic = 0;

    private int otherTopic = 3;
    private int otherSubtopic = 0;

    @Override
    public void start(Stage primaryStage) {
        SoftwareBus softwareBus = new SoftwareBus(true);
        softwareBus.subscribe(topic, subtopic);
        softwareBus.subscribe(otherTopic, otherSubtopic);

        TestCommandCenterDisplay display = new TestCommandCenterDisplay(softwareBus, topic, subtopic);

        primaryStage.setTitle("Test Command Center, subscribed to t" +
                topic + ":s" + subtopic + ", t" + otherTopic + "s:" + otherSubtopic);
        Scene scene = new Scene(display.getPane());
        primaryStage.setScene(scene);
        primaryStage.setX(0);
        primaryStage.setY(0);

        primaryStage.setOnCloseRequest(event -> {
            Platform.exit();
            System.exit(0);
        });
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}