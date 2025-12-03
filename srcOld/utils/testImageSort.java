package utils;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.stage.Stage;

public class testImageSort extends Application {
    @Override
    public void start(Stage stage) {
        imageLoader loader = new imageLoader();
        loader.loadImages();

        FlowPane root = new FlowPane();

        for (int i = 0; i < loader.imageList.size(); i++) {
            var img = loader.imageList.get(i);
            int index = i;
            ImageView iv = new ImageView(img);
            iv.setFitWidth(150);
            iv.setPreserveRatio(true);
            iv.setOnMouseEntered(e -> stage.setTitle("Index: " + index));
            iv.setOnMouseExited(e -> stage.setTitle("Sorted Images Test"));
            root.getChildren().add(iv);
        }

        stage.setScene(new Scene(root, 800, 600));
        stage.setTitle("Sorted Images Test");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}