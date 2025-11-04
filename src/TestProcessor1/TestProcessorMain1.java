package TestProcessor1;

import Bus.SoftwareBus;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class TestProcessorMain1 extends Application {
    private int topic = 4;
    private int subtopic = 2;

    @Override
    public void start(Stage primaryStage) {
        SoftwareBus softwareBus = new SoftwareBus(false);
        TestProcessorDisplay1 display = new TestProcessorDisplay1(softwareBus, topic, subtopic);
        softwareBus.subscribe(topic, subtopic);

        primaryStage.setTitle("Test Processor 1, subscribed t:" + topic + ":s" + subtopic);
        Scene scene = new Scene(display.getPane());
        primaryStage.setScene(scene);

        primaryStage.setScene(scene);
        primaryStage.setMinWidth(500);
        primaryStage.setX(500);
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