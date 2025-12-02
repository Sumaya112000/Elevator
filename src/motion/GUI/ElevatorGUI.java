package motion.GUI;

import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import motion.MotionAPI;
import motion.Hardware.*;
import motion.Util.*;

import java.util.HashMap;

public class ElevatorGUI {


    /**
     * JOEL'S Util.Constants
     * <p>
     * Height between two sensors on the same floor: 3.0 meters
     * Floor thickness: 1.0 meters
     * Maximum speed of the elevator: 1.0 meters per second
     * The distance the cabin travels while accelerating (from max speed to stop or vice versa): 1.5 meters
     * Time it takes for the motor to fully accelerate the cabin from max speed to stop or vice versa: 3.0 seconds
     * Acceleration of the motor (same for start and stop): 0.3333333333333333 meters per second^2
     */

    private MotionAPI motion_API = new MotionAPI();
    //private Label direction_label = new Label("direction:null");
    // private Label top_alignment_label = new Label("top_Aligned:null");
    // private Label bottom_alignment_label = new Label("bottom_Aligned:null");
    private Rectangle elevator_car;
    private Rectangle floor;
    public static final double SENSOR_HEIGHT = Constants.HEIGHT;
    private static final double FLOOR_THICKNESS = Constants.FLOOR_THICKNESS;
    private static final double MAX_SPEED = Constants.MAX_SPEED;
    private static final double ACCLERATION = Constants.ACCELERATION;
    private static final int NUM_FLOORS = 10;
    public static final double SHAFT_HEIGHT = (SENSOR_HEIGHT + FLOOR_THICKNESS) * NUM_FLOORS;
    public static final double CAR_HEIGHT = Constants.HEIGHT;
    private static final int SHAFT_WIDTH = 100;
    private static final double CAR_WIDTH = CAR_HEIGHT;
    private static final double initialY = SHAFT_HEIGHT-1;

    private static final double MOTORRAD = 1.5;
    private Stage primaryStage;
    private HashMap<Integer, Sensor> sensor_HashMap;
    private Elevator elevator;
    private Motor motor;
    private ElevatorFX elevatorFX;
    private MotorFX motorFX;
    private HashMap<Integer, SensorFX> sensorsFX;
    private HashMap<Integer, Double> sensor_pos_HashMap;
    //    private AnchorPane anchorPane;
    private Pane shaftPane;

    public ElevatorGUI(HashMap<Integer, Sensor> sensor_HashMap, HashMap<Integer, Double> sensor_pos_HashMap,
                       Elevator elevator, Motor motor) {
        this.sensor_HashMap = sensor_HashMap;
        this.motor = motor;
        this.elevator = elevator;
        this.sensorsFX = new HashMap<>();
        this.sensor_pos_HashMap = sensor_pos_HashMap;
    }

//    public static void main(String[] args) {
//        launch(args);
//    }

    //I added the stage as a parameter because it was always null.
    // We can change this I just wanted to make it run, so I could fix the GUI
    public Stage getPrimaryStage(Stage stage) {
        this.primaryStage = stage;
        primaryStage.setTitle("Group 7's super sick gui");
        shaftPane = new Pane();
        shaftPane.setPrefSize(joel_to_java(SHAFT_WIDTH), joel_to_java(SHAFT_HEIGHT) + joel_to_java(2 * SENSOR_HEIGHT));
        shaftPane.setStyle("-fx-border-color: black; -fx-background-color: lightgray;");

        // CHANGE TO CONCAT GUI VIEWS INTO ONE
        elevator_car = new Rectangle(joel_to_java(CAR_WIDTH), joel_to_java(CAR_HEIGHT), Color.DARKGRAY);
        elevator_car.setLayoutX(120);
        elevator_car.setLayoutY(joel_to_java(initialY));
        elevatorFX = new ElevatorFX(elevator_car);

        elevator.subscribe(elevatorFX);
        shaftPane.getChildren().add(elevator_car);

        //Drawing Sensors
//        anchorPane = new AnchorPane();
        draw_floors();
        drawSensor();

//        Button upButton = new Button("Up");
//        Button downButton = new Button("Down");
//        Button startButton = new Button("Start");
//        Button stopButton = new Button("Stop");

//        upButton.setOnAction(e -> {
//            motion_API.set_direction(Direction.UP);
//            redraw();
//        });
//
//        downButton.setOnAction(e -> {
//            motion_API.set_direction(Direction.DOWN);
//            redraw();
//        });
//
//        startButton.setOnAction(e -> {
//            motion_API.start();
//            move();
//        });
//
//        stopButton.setOnAction(e -> {
//            motion_API.stop();
//            redraw();
//        });

//        HBox buttonBox = new HBox(10, upButton, downButton, startButton, stopButton);
//        buttonBox.setAlignment(Pos.CENTER);
//        buttonBox.setPadding(new Insets(10));


        //VBox statusBox = new VBox(5, direction_label, top_alignment_label, bottom_alignment_label);
        // statusBox.setAlignment(Pos.CENTER_LEFT);
        // statusBox.setPadding(new Insets(10));

        Group root = new Group();
        BorderPane natPane = new BorderPane();
        natPane.setRight(shaftPane);
//        natPane.setTop(buttonBox);
        // natPane.setBottom(statusBox);
        natPane.setStyle("-fx-border-color: black; -fx-background-color: lightgray;");
        root.getChildren().add(natPane);
//        root.getChildren().add(anchorPane);

        Scene scene = new Scene(root, 300, 635);
        primaryStage.setScene(scene);
        primaryStage.show();

        return primaryStage;
    }

    public void draw_floors() {
        double x = 200;
        //floors
        for (Integer i = 0; i < sensor_HashMap.size(); i++) {
            double y = joel_to_java(SHAFT_HEIGHT + SENSOR_HEIGHT - sensor_pos_HashMap.get(i));
            if (i % 2 == 0) {
                Rectangle floor = new Rectangle(x-3, y - 3, 100, joel_to_java(FLOOR_THICKNESS) + SENSOR_HEIGHT * 2);
                floor.setFill(Color.GRAY);
                shaftPane.getChildren().add(floor);
            }

            //Top Floor
            if (i == sensor_HashMap.size() - 1) {
                Rectangle floor = new Rectangle(x-3, y - joel_to_java(FLOOR_THICKNESS) - 3, 100, joel_to_java(FLOOR_THICKNESS) + SENSOR_HEIGHT * 2);
                floor.setFill(Color.GRAY);
                shaftPane.getChildren().add(floor);
            }
        }
    }

    /**
     * Draws all the circles for the sensors and gives the circles to the sensorFXs
     * 1.
     */
    public void drawSensor() {
//        double y = SHAFT_HEIGHT;
        double x = 200;
//        double ys = 0;

        //draw sensors
        for (Integer i = 0; i < sensor_HashMap.size(); i++) {
            double y = joel_to_java(SHAFT_HEIGHT + SENSOR_HEIGHT - sensor_pos_HashMap.get(i));

            Circle sen = new Circle(x, y, SENSOR_HEIGHT, Color.RED);
            SensorFX sensorFX = new SensorFX(sen);
            sensor_HashMap.get(i).subscribe(sensorFX);
            sensorsFX.put(i, sensorFX);
            shaftPane.getChildren().add(sen);

        }

//        Circle moto = new Circle(150,100,joelToJava(MOTORRAD));
//        motorFX = new MotorFX(moto);
//        motor.subscribe(motorFX);
//        shaftPane.getChildren().add(moto);

    }


    /**
     * Any refresh actions here
     */
//    private void redraw() {
//
//        direction_label.setText("Util.Direction: " + motion_API.direction);
//        top_alignment_label.setText("Top Aligned: " + motion_API.top_alignment());
//        bottom_alignment_label.setText("Bottom Aligned: " + motion_API.bottom_alignment());
//    }

    public static double joel_to_java(double meters) {
        return meters * 12;
    }

}