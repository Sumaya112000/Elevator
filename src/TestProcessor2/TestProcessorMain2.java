package TestProcessor2;

import Bus.SoftwareBus;
import TestProcessor1.TestProcessorDisplay1;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class TestProcessorMain2 extends Application {
    private int topic = 5;
    private int subtopic = 5;

    @Override
    public void start(Stage primaryStage) {
        SoftwareBus softwareBus = new SoftwareBus(false);
        TestProcessorDisplay2 display = new TestProcessorDisplay2(softwareBus, topic, subtopic);
        softwareBus.subscribe(topic, subtopic);

        primaryStage.setTitle("Test Processor 2, subscribed to t" + topic + ":s" + subtopic);

        Scene scene = new Scene(display.getPane());
        primaryStage.setScene(scene);

        primaryStage.setScene(scene);
        primaryStage.setMinWidth(500);
        primaryStage.setX(1100);
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