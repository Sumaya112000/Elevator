package pfdGUI;

import java.util.ArrayList;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;
import utils.imageLoader;

/** MUX calls api, api modifies the gui. MUX needs to also poll the internal state.
 * Simple JavaFX GUI that listens to model classes via a nested listener
 * interface and swaps images in response to events.
 */
public class gui extends Application {
    private int numElevators = 4; // Total number of elevators
    private int numFloors = 10; // Total number of floors
    private imageLoader loader; // Utility image loader

    // GUI Control/Query Interface (Contains internal state & control methods)
    public GUIControl internalState = new GUIControl();
    private mux.ElevatorMultiplexor[] elevatorMuxes = new mux.ElevatorMultiplexor[numElevators];

    // Internal State Devices
    private Panel[] panels = new Panel[numElevators];
    private Door[] doors = new Door[numElevators];
    private Display[] displays = new Display[numElevators];
    private WeighScale[] weighScales = new WeighScale[numElevators];
    private CallButton[] callButtons = new CallButton[numFloors];
    private FireAlarm fireAlarm;

    // Singleton instance for API access
    private static gui instance;
    public gui() { instance = this; }
    public static gui getInstance() { return instance; }


    /**************************************************
     * GUI Control & State Query Interface
     */

    public class GUIControl {
        public GUIControl() {
            for (int i = 0; i < numElevators; i++) {
                pressedFloors[i] = new ArrayList<>();
            }
        }

        // Internal State Variables
        private ArrayList<Integer>[] pressedFloors = new ArrayList[numElevators];
        private boolean[] doorObstructions = new boolean[numElevators];
        private boolean[] cabinOverloads = new boolean[numElevators];
        private boolean[] fireKeys = new boolean[numElevators];
        private boolean fireAlarmActive;
        private boolean callButtonsDisabled = false;
        private boolean[] panelButtonsDisabled = new boolean[numElevators];
        private boolean[] singleSelection = new boolean[numElevators];
        private int lastSelected = 0;

        // Getters for internal state variables
        public ArrayList<Integer> getPressedFloors(int ID) {
            int panelIndex = ID - 1;
            return (panelIndex >= 0 && panelIndex < numElevators) ? pressedFloors[panelIndex] : new ArrayList<>();
        }
        public boolean getIsDoorObstructed(int ID) {
            int panelIndex = ID - 1;
            return (panelIndex >= 0 && panelIndex < numElevators) ? doorObstructions[panelIndex] : false;
        }
        public boolean getIsCabinOverloaded(int ID) {
            int panelIndex = ID - 1;
            return (panelIndex >= 0 && panelIndex < numElevators) ? cabinOverloads[panelIndex] : false;
        }
        public boolean getFireAlarm() { return fireAlarmActive; }

        public boolean getIsFireKeyActive(int ID){
            int panelIndex = ID - 1;
            return (panelIndex >= 0 && panelIndex < numElevators) ? fireKeys[panelIndex] : false;
        }

        // Setters for internal state variables
        public void setCallButtonsDisabled(boolean disabled) {
            callButtonsDisabled = disabled;
        }

        public void setPanelButtonsDisabled(int ID, boolean disabled) {
            int panelIndex = ID - 1;
            panelButtonsDisabled[panelIndex] = disabled;
        }

        public void setSingleSelection(int ID, boolean single) {
            int panelIndex = ID - 1;
            lastSelected = 0;
            singleSelection[panelIndex] = single;
        }

        // Press panel button
        public void pressPanelButton(int ID, int floorNumber) {
            Platform.runLater(() -> {
                for (Node n : panels[ID-1].panelOverlay.getChildren()) {
                    if (n instanceof Label lbl && lbl.getText().equals(String.valueOf(floorNumber))) {
                        if (n == panels[ID-1].digitalLabel) continue;
                        lbl.setStyle("-fx-text-fill: white;");
                        pressedFloors[ID-1].add(floorNumber);
                    }
                }
            });
        }

        // Reset panel button
        public void resetPanelButton(int ID, int floorNumber) {
            Platform.runLater(() -> {
                for (Node n : panels[ID-1].panelOverlay.getChildren()) {
                    if (n == panels[ID-1].digitalLabel) continue;
                    if (n instanceof Label lbl && lbl.getText().equals(String.valueOf(floorNumber))) {
                        lbl.setStyle("-fx-text-fill: black;");
                        pressedFloors[ID-1].remove(Integer.valueOf(floorNumber));
                    }
                }
                if(floorNumber == lastSelected){
                    lastSelected = 0;
                }
            });
        }

        // Reset all panel buttons
        public void resetPanel(int ID) {
            Platform.runLater(() -> {
                for (Node n : panels[ID-1].panelOverlay.getChildren()) {
                    if (n == panels[ID-1].digitalLabel) continue;
                    if (n instanceof Label lbl) {
                        lbl.setStyle("-fx-text-fill: black;");
                    }
                }
                pressedFloors[ID-1].clear();
            });
        }

        // Set the door obstruction state of a given elevator
        public void setDoorObstruction(int ID, boolean isObstructed) {
            Platform.runLater(() -> {
                doorObstructions[ID-1] = isObstructed;
                ImageView doorImg = doors[ID-1].elevDoorsImg;

                // Update image based on current door state + new obstruction state
                if (loader.imageList.get(6).equals(doorImg.getImage()) ||
                        loader.imageList.get(7).equals(doorImg.getImage())) {
                    // Door is open - update open state
                    doorImg.setImage(isObstructed ? loader.imageList.get(7) : loader.imageList.get(6));
                }
                // If door is closed or transitioning, obstruction state is stored but image doesn't change
                // until next open/close operation
            });
        }

        // Change the door state of a given elevator
        public synchronized void changeDoorState(int ID, boolean open) {
            Platform.runLater(() -> {
                if (open) {
                    // Opening doors, show midway transition then fully open
                    if (doorObstructions[ID-1]) {
                        doors[ID-1].elevDoorsImg.setImage(loader.imageList.get(5));
                    } else {
                        doors[ID-1].elevDoorsImg.setImage(loader.imageList.get(4));
                    }

                    new Thread(() -> {
                        try { Thread.sleep(2000); } catch (InterruptedException e) { e.printStackTrace(); }
                        Platform.runLater(() -> {
                            if (doorObstructions[ID-1]) {
                                doors[ID-1].elevDoorsImg.setImage(loader.imageList.get(7));
                            } else {
                                doors[ID-1].elevDoorsImg.setImage(loader.imageList.get(6));
                            }
                        });
                    }).start();
                } else {
                    // Closing doors, show midway transition
                    if (doorObstructions[ID-1]) {
                        doors[ID-1].elevDoorsImg.setImage(loader.imageList.get(5));
                    } else {
                        doors[ID-1].elevDoorsImg.setImage(loader.imageList.get(4));
                    }

                    new Thread(() -> {
                        try { Thread.sleep(2000); } catch (InterruptedException e) { e.printStackTrace(); }
                        Platform.runLater(() -> {
                            if (doorObstructions[ID-1]) {
                                // reopen doors, Obstruction detected
                                System.out.println("Obstruction detected - reopening doors for elevator " + ID);
                                if (doorObstructions[ID-1]) {
                                    doors[ID-1].elevDoorsImg.setImage(loader.imageList.get(7));
                                } else {
                                    doors[ID-1].elevDoorsImg.setImage(loader.imageList.get(6));
                                }
                            } else {
                                // No obstruction, close fully
                                doors[ID-1].elevDoorsImg.setImage(loader.imageList.get(3));
                            }
                        });
                    }).start();
                }
            });
        }


        // Set the cabin overload state of a given elevator
        public void setCabinOverload(int ID, boolean isOverloaded) {
            Platform.runLater(() -> {
                cabinOverloads[ID-1] = isOverloaded;
                if (isOverloaded) {
                    weighScales[ID-1].weightTriggerButton.setStyle("-fx-background-color: #684b4bff; -fx-text-fill: black;");
                } else {
                    weighScales[ID-1].weightTriggerButton.setStyle("-fx-background-color: #bdbdbdff; -fx-text-fill: black;");
                }
            });
        }

        // Set the floor display of a given elevator
        public void setDisplay(int carId, int floorNumber, String direction) {
            Platform.runLater(() -> {
                displays[carId-1].digitalLabel.setText(String.valueOf(floorNumber));
                panels[carId-1].digitalLabel.setText(String.valueOf(floorNumber));
                if (direction.contains("UP")) {
                    displays[carId-1].floorDispImg.setImage(loader.imageList.get(10));
                    panels[carId-1].elevPanelImg.setImage(loader.imageList.get(2));
                } else if (direction.contains("DOWN")) {
                    displays[carId-1].floorDispImg.setImage(loader.imageList.get(9));
                    panels[carId-1].elevPanelImg.setImage(loader.imageList.get(1));
                } else {
                    displays[carId-1].floorDispImg.setImage(loader.imageList.get(8));
                    panels[carId-1].elevPanelImg.setImage(loader.imageList.get(0));
                }
            });
        }

        // Set the floor call button state
        public void setCallButton(int floorNumber, String direction) {
            int buttonIndex = floorNumber - 1;  // Convert floor number (1-10) to array index (0-9)
            if (buttonIndex >= 0 && buttonIndex < numFloors) {
                Platform.runLater(() -> {
                    if (direction.contains("UP")) {
                        callButtons[buttonIndex].elevCallButtonsImg.setImage(loader.imageList.get(15));
                        System.out.println("UP button set on floor " + floorNumber);
                    } else if (direction.contains("DOWN")) {
                        callButtons[buttonIndex].elevCallButtonsImg.setImage(loader.imageList.get(14));
                        System.out.println("DOWN button set on floor " + floorNumber);
                    }
                });
            }
        }

        // Reset the floor call button state
        public void resetCallButton(int floorNumber, String direction) {
            int buttonIndex = floorNumber - 1;  // Convert floor number (1-10) to array index (0-9)
            if (buttonIndex >= 0 && buttonIndex < numFloors) {
                Platform.runLater(() -> {
                    if(callButtons[buttonIndex].direction.equals("BOTH")) {
                        if(direction.equalsIgnoreCase("UP")) {
                            callButtons[buttonIndex].setDirection("DOWN");
                            callButtons[buttonIndex].elevCallButtonsImg.setImage(loader.imageList.get(14));
                        } else {
                            callButtons[buttonIndex].setDirection("UP");
                            callButtons[buttonIndex].elevCallButtonsImg.setImage(loader.imageList.get(15));
                        }
                    } else {
                        callButtons[buttonIndex].setDirection("IDLE");
                        callButtons[buttonIndex].elevCallButtonsImg.setImage(loader.imageList.get(13));
                    }
                });
            }
        }

        // Set the fire alarm state
        public void setFireAlarm(boolean isActive) {
            Platform.runLater(() -> {
                fireAlarmActive = isActive;
                if (isActive) {
                    fireAlarm.fireAlarmImg.setImage(loader.imageList.get(12)); // Active image
                } else {
                    fireAlarm.fireAlarmImg.setImage(loader.imageList.get(11)); // Inactive image
                }
            });
        }

        // Query whether the floor call button is active or not
        public boolean isCallButtonActive(int floorNumber, String direction) {
            int buttonIndex = floorNumber - 1;
            if (buttonIndex < 0 || buttonIndex >= numFloors) {
                return false;
            }
            String currentDirection = callButtons[buttonIndex].direction;
            if(currentDirection.equalsIgnoreCase("BOTH")) {
                return true;
            }
            return currentDirection.equalsIgnoreCase(direction);
        }
    }

    /**************************************************
     * JavaFX Application & UI
     */


    private double scale = 0.8;

    @Override
    public void start(Stage primaryStage) {

        // load images via utility
        loader = new imageLoader();
        loader.loadImages();

        HBox mainContainer = new HBox(20);

        ScrollPane floorScroll = new ScrollPane();
        VBox floorVBox = new VBox(8);
        for (int i = 0; i < numFloors; i++) {
            callButtons[i] = new CallButton(i);
            Label floorLabel = new Label("Floor " + (i + 1));
            floorLabel.setFont(Font.font("Times New Roman", FontWeight.BOLD, 14));
            floorLabel.setStyle("-fx-text-fill: black;");
            floorVBox.getChildren().addAll(floorLabel, callButtons[i].callButtonOverlay);
        }
        Label floorLabel = new Label("Fire Alarm");
        floorLabel.setFont(Font.font("Times New Roman", FontWeight.BOLD, 14));
        floorLabel.setStyle("-fx-text-fill: black;");
        fireAlarm = new FireAlarm();
        floorVBox.getChildren().addAll(floorLabel, fireAlarm.fireAlarmOverlay);
        floorScroll.setContent(floorVBox);
        floorScroll.setPrefViewportWidth(180);
        mainContainer.getChildren().add(floorScroll);

        HBox elevatorsContainer = new HBox(15);
        for (int i = 0; i < numElevators; i++) {
            VBox elevatorColumn = new VBox(10);
            elevatorColumn.setAlignment(Pos.CENTER);

            panels[i] = new Panel(i);
            doors[i] = new Door(i);
            displays[i] = new Display(i);
            weighScales[i] = new WeighScale(i);

            elevatorColumn.getChildren().addAll(panels[i].panelOverlay, displays[i].displayOverlay,
                    doors[i].doorOverlay, weighScales[i].weightTriggerButton);
            elevatorsContainer.getChildren().add(elevatorColumn);
        }

        mainContainer.getChildren().add(elevatorsContainer);

        double windowWidth = 1160;
        double windowHeight = 572;

        Scene scene = new Scene(mainContainer, windowWidth, windowHeight, Color.web("#c0bfbbff"));
        primaryStage.setTitle("Elevator Passenger Devices");
        primaryStage.setScene(scene);
        primaryStage.show();

        // Initialize multiplexors AFTER GUI is fully set up
        new mux.BuildingMultiplexor();
        for (int i = 0; i < numElevators; i++) {
            elevatorMuxes[i] = new mux.ElevatorMultiplexor(i + 1);  // Store the reference
        }
        System.out.println("All multiplexors initialized after GUI setup");
    }

    private class Panel{
        public  ImageView elevPanelImg = new ImageView();
        public StackPane panelOverlay = new StackPane(elevPanelImg);
        public Label digitalLabel;
        private double scaleFactor = 0.7;
        private int yTranslation = -40; // adjust as needed for non 1:1 scales
        private int offset = 18;
        private int carId;

        private Panel(int index){
            this.carId = index;
            makePanel();
        }

        private void makePanel(){
            elevPanelImg.setPreserveRatio(true);
            elevPanelImg.setFitWidth(250 * scaleFactor);
            elevPanelImg.setImage(loader.imageList.get(0)); // 0-2 indices are cabin panels

            // Digital Display label
            digitalLabel = new Label("1");
            digitalLabel.setStyle("-fx-text-fill: white;");
            digitalLabel.setFont(Font.font("Verdana", FontWeight.BOLD, 28));
            digitalLabel.setTranslateY(-130*scaleFactor);
            panelOverlay.getChildren().add(digitalLabel);

            // Button overlays
            for (int i = 1; i < 10; i += 2) {
                Label left = new Label(String.valueOf(i));
                left.setStyle("-fx-text-fill: black;");
                left.setFont(Font.font("Verdana", FontWeight.BOLD, 14  * scaleFactor));
                left.setTranslateX(-20 * scaleFactor);
                left.setTranslateY(yTranslation + (offset * i) * scaleFactor);

                // Capture final reference for lambda
                final int leftFloorNumber = i;
                left.setOnMouseClicked(event -> {
                    Platform.runLater(() -> {
                        if(!internalState.panelButtonsDisabled[carId]) {
                            if(!internalState.singleSelection[carId] || internalState.lastSelected == 0) {
                                if (elevatorMuxes != null && carId < elevatorMuxes.length && elevatorMuxes[carId] != null) {
                                    elevatorMuxes[carId].getElevator().panel.pressFloorButton(leftFloorNumber);
                                    internalState.lastSelected = leftFloorNumber;
                                }
                                left.setStyle("-fx-text-fill: #ffffffff;");
                            }
                        }
                    });
                });

                panelOverlay.getChildren().add(left);

                Label right = new Label(String.valueOf(i + 1));
                right.setStyle("-fx-text-fill: black;");
                right.setFont(Font.font("Verdana", FontWeight.BOLD, 14  * scaleFactor));
                right.setTranslateX(21  * scaleFactor);
                right.setTranslateY(yTranslation + (offset * i) * scaleFactor);

                final int rightFloorNumber = i + 1;
                right.setOnMouseClicked(event -> {
                    Platform.runLater(() -> {
                        if(!internalState.panelButtonsDisabled[carId]) {
                            if(!internalState.singleSelection[carId] || internalState.lastSelected == 0) {
                                if (elevatorMuxes != null && carId < elevatorMuxes.length && elevatorMuxes[carId] != null) {
                                    elevatorMuxes[carId].getElevator().panel.pressFloorButton(rightFloorNumber);
                                    internalState.lastSelected = rightFloorNumber;
                                }
                                right.setStyle("-fx-text-fill: #ffffffff;");
                            }
                        }
                    });
                });

                panelOverlay.getChildren().add(right);
            }

            Button keyButton = new Button();
            keyButton.setPrefSize(25, 25);
            keyButton.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");

            double keyX = 18;
            double keyY = 130;

            keyButton.setTranslateX(keyX);
            keyButton.setTranslateY(keyY);

            keyButton.setOnAction(event -> {
                internalState.fireKeys[carId] = !internalState.fireKeys[carId];
            });

            panelOverlay.getChildren().add(keyButton);

        }
    }

    private class Door{
        public ImageView elevDoorsImg = new ImageView();
        public StackPane doorOverlay = new StackPane(elevDoorsImg);
        private int carId;

        private Door(int index){
            this.carId = index;
            makeDoor();
        }

        private void makeDoor(){
            elevDoorsImg.setPreserveRatio(true);
            elevDoorsImg.setFitWidth(250);
            elevDoorsImg.setImage(loader.imageList.get(6));

            internalState.doorObstructions[carId] = false;

            elevDoorsImg.setOnMouseClicked(event -> {
                if(loader.imageList.get(6).equals(elevDoorsImg.getImage())) {
                    Platform.runLater(() -> {
                        internalState.doorObstructions[carId] = true;
                        elevDoorsImg.setImage(loader.imageList.get(7));
                    });
                    return;
                }
                else if(loader.imageList.get(7).equals(elevDoorsImg.getImage())) {
                    Platform.runLater(() -> {
                        internalState.doorObstructions[carId] = false;
                        elevDoorsImg.setImage(loader.imageList.get(6));
                    });
                    return;
                }
            });
        }
    }

    private class Display{
        public ImageView floorDispImg = new ImageView();
        public StackPane displayOverlay = new StackPane(floorDispImg);
        public Label digitalLabel;
        private int displayIndex;

        private Display(int index){
            this.displayIndex = index;
            makeDisplay();
        }

        private void makeDisplay(){
            floorDispImg.setPreserveRatio(true);
            floorDispImg.setFitWidth(100);
            floorDispImg.setImage(loader.imageList.get(8)); // 8-10 indices are floor displays

            // Digital Display label
            digitalLabel = new Label("1");
            digitalLabel.setStyle("-fx-text-fill: white;");
            digitalLabel.setFont(Font.font("Verdana", FontWeight.BOLD, 16));
            digitalLabel.setTranslateY(-5);
            displayOverlay.getChildren().add(digitalLabel);

            // Floor number label
            Label floorLabel = new Label(String.valueOf(displayIndex + 1));
            floorLabel.setStyle("-fx-text-fill: black;");
            floorLabel.setFont(Font.font("Times New Roman", FontWeight.BOLD, 14));
            floorLabel.setTranslateY(50);
            displayOverlay.getChildren().add(floorLabel);
        }
    }

    private class WeighScale{
        public Button weightTriggerButton = new Button("Overload");
        private int buttonIndex;

        public WeighScale(int index){
            this.buttonIndex = index;
            makeTrigger();
        }

        private void makeTrigger(){
            weightTriggerButton.setPrefWidth(200);
            weightTriggerButton.setPrefHeight(45);
            weightTriggerButton.setStyle("-fx-background-color: #bdbdbdff; -fx-text-fill: black;");
            weightTriggerButton.setFont(Font.font("Times New Roman", FontWeight.BOLD, 20));

            weightTriggerButton.setOnMouseClicked(event -> {

                // Notify the multiplexor of the overload weight click
                Platform.runLater(() -> {
                    String style = weightTriggerButton.getStyle();
                    boolean isOverloaded = style.contains("#684b4bff");

                    if (isOverloaded) {
                        // Toggle to NORMAL
                        weightTriggerButton.setStyle("-fx-background-color: #bdbdbdff; -fx-text-fill: black;");
                        internalState.cabinOverloads[buttonIndex] = false;
                    } else {
                        // Toggle to OVERLOAD
                        weightTriggerButton.setStyle("-fx-background-color: #684b4bff; -fx-text-fill: black;");
                        internalState.cabinOverloads[buttonIndex] = true;
                    }
                });
            });
        }
    }

    private class CallButton{
        public  ImageView elevCallButtonsImg = new ImageView();
        public StackPane callButtonOverlay = new StackPane(elevCallButtonsImg);
        private int buttonIndex;
        private String direction;

        private CallButton(int index){
            this.buttonIndex = index;
            this.direction = "IDLE";
            makeCallButton();
        }

        private void makeCallButton(){
            elevCallButtonsImg.setPreserveRatio(true);
            elevCallButtonsImg.setFitWidth(80);
            elevCallButtonsImg.setFitHeight(80);
            elevCallButtonsImg.setImage(loader.imageList.get(13)); // 13-16 indices are call buttons

            // Bound the click region with quick maths
            elevCallButtonsImg.setOnMouseClicked(event -> {
                if(!internalState.callButtonsDisabled) {
                    double clickX = event.getX();
                    double clickY = event.getY();
                    double width = elevCallButtonsImg.getBoundsInLocal().getWidth();
                    double height = elevCallButtonsImg.getBoundsInLocal().getHeight();

                    // Approximate centers of the upper and lower buttons
                    double centerX = width / 2;
                    double centerY = height / 2;
                    double offsetY = 18;
                    double radius = 12;

                    // Calculate distances from click point to each button center
                    double distToUp = Math.hypot(clickX - centerX, clickY - (centerY - offsetY));
                    double distToDown = Math.hypot(clickX - centerX, clickY - (centerY + offsetY));

                    if (distToUp <= radius) {
                        // Upper button clicked
                        Platform.runLater(() -> {
                            if(buttonIndex != 9) {
                                if (callButtons[buttonIndex].direction.equals("DOWN")) {
                                    callButtons[buttonIndex].direction = "BOTH";
                                    elevCallButtonsImg.setImage(loader.imageList.get(16));
                                } else if (!callButtons[buttonIndex].direction.equals("BOTH")) {
                                    callButtons[buttonIndex].direction = "UP";
                                    elevCallButtonsImg.setImage(loader.imageList.get(15));
                                }
                            }
                        });
                    } else if (distToDown <= radius) {
                        // Lower button clicked
                        Platform.runLater(() -> {
                            if(buttonIndex != 0) {
                                if (callButtons[buttonIndex].direction.equals("UP")) {
                                    callButtons[buttonIndex].direction = "BOTH";
                                    elevCallButtonsImg.setImage(loader.imageList.get(16));
                                } else if (!callButtons[buttonIndex].direction.equals("BOTH")) {
                                    callButtons[buttonIndex].direction = "DOWN";
                                    elevCallButtonsImg.setImage(loader.imageList.get(14));
                                }
                            }
                        });
                    } else {
                        // Clicked outside both button circles — ignore
                        System.out.println("Clicked outside call buttons");
                    }
                }
            });
        }

        public void setDirection(String direction) {
            this.direction = direction;
        }
    }

    private class FireAlarm{
        public ImageView fireAlarmImg = new ImageView();
        public StackPane fireAlarmOverlay = new StackPane(fireAlarmImg);

        private FireAlarm(){ makeFireAlarm(); }

        private void makeFireAlarm(){
            fireAlarmImg.setPreserveRatio(true);
            fireAlarmImg.setFitWidth(70);
            fireAlarmImg.setImage(loader.imageList.get(11)); // 11-12 indices are fire alarms

            fireAlarmImg.setOnMouseClicked(event -> {
                boolean isActive = fireAlarmImg.getImage() == loader.imageList.get(12);

                Platform.runLater(() -> {
                    if (isActive) {
                        internalState.fireAlarmActive = false;
                        fireAlarmImg.setImage(loader.imageList.get(11));
                    } else {
                        internalState.fireAlarmActive = true;
                        fireAlarmImg.setImage(loader.imageList.get(12));
                    }
                });
            });
        }
    }

    /**************************************************
     * Main Application Entry Point
     ****************************************
     */

    public static void main(String[] args) {
        launch(args);
    }


}