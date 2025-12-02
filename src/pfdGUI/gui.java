package pfdGUI;

import java.util.ArrayList;

import Bus.SoftwareBus;
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

public class gui extends Application {
    private int numElevators = 4;
    private int numFloors = 10;
    private imageLoader loader;

    public GUIControl internalState = new GUIControl();
    private mux.ElevatorMultiplexor[] elevatorMuxes = new mux.ElevatorMultiplexor[numElevators];

    private Panel[] panels = new Panel[numElevators];
    private Door[] doors = new Door[numElevators];
    private Display[] displays = new Display[numElevators];
    private WeighScale[] weighScales = new WeighScale[numElevators];
    private CallButton[] callButtons = new CallButton[numFloors];
    private FireAlarm fireAlarm;

    private static gui instance;

    public gui() {
        instance = this;
    }

    public static gui getInstance() {
        return instance;
    }

    public class GUIControl {
        public GUIControl() {
            for (int i = 0; i < numElevators; i++) {
                pressedFloors[i] = new ArrayList<>();
            }
        }

        private ArrayList<Integer>[] pressedFloors = new ArrayList[numElevators];
        private boolean[] doorObstructions = new boolean[numElevators];
        private boolean[] cabinOverloads = new boolean[numElevators];
        private boolean[] fireKeys = new boolean[numElevators];
        private boolean fireAlarmActive;
        private boolean callButtonsDisabled = false;
        private boolean[] panelButtonsDisabled = new boolean[numElevators];
        private boolean[] singleSelection = new boolean[numElevators];
        private int lastSelected = 0;

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

        public boolean getFireAlarm() {
            return fireAlarmActive;
        }

        public boolean getIsFireKeyActive(int ID) {
            int panelIndex = ID - 1;
            return (panelIndex >= 0 && panelIndex < numElevators) ? fireKeys[panelIndex] : false;
        }

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

        public void pressPanelButton(int ID, int floorNumber) {
            Platform.runLater(() -> {
                for (Node n : panels[ID - 1].panelOverlay.getChildren()) {
                    if (n instanceof Label lbl && lbl.getText().equals(String.valueOf(floorNumber))) {
                        if (n == panels[ID - 1].digitalLabel) continue;
                        lbl.setStyle("-fx-text-fill: white;");
                        pressedFloors[ID - 1].add(floorNumber);
                    }
                }
            });
        }

        public void resetPanelButton(int ID, int floorNumber) {
            Platform.runLater(() -> {
                for (Node n : panels[ID - 1].panelOverlay.getChildren()) {
                    if (n == panels[ID - 1].digitalLabel) continue;
                    if (n instanceof Label lbl && lbl.getText().equals(String.valueOf(floorNumber))) {
                        lbl.setStyle("-fx-text-fill: black;");
                        pressedFloors[ID - 1].remove(Integer.valueOf(floorNumber));
                    }
                }
                if (floorNumber == lastSelected) {
                    lastSelected = 0;
                }
            });
        }

        public void resetPanel(int ID) {
            Platform.runLater(() -> {
                for (Node n : panels[ID - 1].panelOverlay.getChildren()) {
                    if (n == panels[ID - 1].digitalLabel) continue;
                    if (n instanceof Label lbl) {
                        lbl.setStyle("-fx-text-fill: black;");
                    }
                }
                pressedFloors[ID - 1].clear();
            });
        }

        public void setDoorObstruction(int ID, boolean isObstructed) {
            Platform.runLater(() -> {
                doorObstructions[ID - 1] = isObstructed;
                ImageView doorImg = doors[ID - 1].elevDoorsImg;

                if (loader.imageList.get(6).equals(doorImg.getImage()) ||
                        loader.imageList.get(7).equals(doorImg.getImage())) {
                    doorImg.setImage(isObstructed ? loader.imageList.get(7) : loader.imageList.get(6));
                }
            });
        }

        public synchronized void changeDoorState(int ID, boolean open) {
            Platform.runLater(() -> {
                if (open) {
                    if (doorObstructions[ID - 1]) {
                        doors[ID - 1].elevDoorsImg.setImage(loader.imageList.get(5));
                    } else {
                        doors[ID - 1].elevDoorsImg.setImage(loader.imageList.get(4));
                    }

                    new Thread(() -> {
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        Platform.runLater(() -> {
                            if (doorObstructions[ID - 1]) {
                                doors[ID - 1].elevDoorsImg.setImage(loader.imageList.get(7));
                            } else {
                                doors[ID - 1].elevDoorsImg.setImage(loader.imageList.get(6));
                            }
                        });
                    }).start();
                } else {
                    if (doorObstructions[ID - 1]) {
                        doors[ID - 1].elevDoorsImg.setImage(loader.imageList.get(5));
                    } else {
                        doors[ID - 1].elevDoorsImg.setImage(loader.imageList.get(4));
                    }
                    new Thread(() -> {
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        Platform.runLater(() -> {
                            if (doorObstructions[ID - 1]) {
                                System.out.println("Obstruction detected - reopening doors for elevator " + ID);
                                if (doorObstructions[ID - 1]) {
                                    doors[ID - 1].elevDoorsImg.setImage(loader.imageList.get(7));
                                } else {
                                    doors[ID - 1].elevDoorsImg.setImage(loader.imageList.get(6));
                                }
                            } else {
                                doors[ID - 1].elevDoorsImg.setImage(loader.imageList.get(3));
                            }
                        });
                    }).start();
                }
            });
        }

        public void setCabinOverload(int ID, boolean isOverloaded) {
            Platform.runLater(() -> {
                cabinOverloads[ID - 1] = isOverloaded;
                if (isOverloaded) {
                    weighScales[ID - 1].weightTriggerButton.setStyle("-fx-background-color: #684b4bff; -fx-text-fill: black;");
                } else {
                    weighScales[ID - 1].weightTriggerButton.setStyle("-fx-background-color: #bdbdbdff; -fx-text-fill: black;");
                }
            });
        }

        public void setDisplay(int carId, int floorNumber, String direction) {
            Platform.runLater(() -> {
                displays[carId - 1].digitalLabel.setText(String.valueOf(floorNumber));
                panels[carId - 1].digitalLabel.setText(String.valueOf(floorNumber));
                if (direction.contains("UP")) {
                    displays[carId - 1].floorDispImg.setImage(loader.imageList.get(10));
                    panels[carId - 1].elevPanelImg.setImage(loader.imageList.get(2));
                } else if (direction.contains("DOWN")) {
                    displays[carId - 1].floorDispImg.setImage(loader.imageList.get(9));
                    panels[carId - 1].elevPanelImg.setImage(loader.imageList.get(1));
                } else {
                    displays[carId - 1].floorDispImg.setImage(loader.imageList.get(8));
                    panels[carId - 1].elevPanelImg.setImage(loader.imageList.get(0));
                }
            });
        }

        public void setCallButton(int floorNumber, String direction) {
            int buttonIndex = floorNumber - 1;
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

        public void resetCallButton(int floorNumber, String direction) {
            int buttonIndex = floorNumber - 1;
            if (buttonIndex >= 0 && buttonIndex < numFloors) {
                Platform.runLater(() -> {
                    if (callButtons[buttonIndex].direction.equals("BOTH")) {
                        if (direction.equalsIgnoreCase("UP")) {
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

        public void setFireAlarm(boolean isActive) {
            Platform.runLater(() -> {
                fireAlarmActive = isActive;
                if (isActive) {
                    fireAlarm.fireAlarmImg.setImage(loader.imageList.get(12));
                } else {
                    fireAlarm.fireAlarmImg.setImage(loader.imageList.get(11));
                }
            });
        }

        public boolean isCallButtonActive(int floorNumber, String direction) {
            int buttonIndex = floorNumber - 1;
            if (buttonIndex < 0 || buttonIndex >= numFloors) {
                return false;
            }
            String currentDirection = callButtons[buttonIndex].direction;
            if (currentDirection.equalsIgnoreCase("BOTH")) {
                return true;
            }
            return currentDirection.equalsIgnoreCase(direction);
        }
    }

    private double scale = 0.8;

    @Override
    public void start(Stage primaryStage) {

        loader = new imageLoader();
        loader.loadImages();

        ScrollPane scrollPane = new ScrollPane();
        VBox vbox = new VBox(10);
        HBox hbox = new HBox();

        for (int i = 0; i < numFloors; i++) {
            callButtons[i] = new CallButton(i);
            Label floorLabel = new Label("Floor " + (i + 1));
            floorLabel.setFont(Font.font("Times New Roman", FontWeight.BOLD, 16));
            floorLabel.setStyle("-fx-text-fill: black;");
            vbox.getChildren().addAll(floorLabel, callButtons[i].callButtonOverlay);
        }
        Label floorLabel = new Label("Fire Alarm");
        floorLabel.setFont(Font.font("Times New Roman", FontWeight.BOLD, 16));
        floorLabel.setStyle("-fx-text-fill: black;");
        fireAlarm = new FireAlarm();
        vbox.getChildren().addAll(floorLabel, fireAlarm.fireAlarmOverlay);
        scrollPane.setContent(vbox);
        hbox.getChildren().add(scrollPane);

        for (int i = 0; i < numElevators; i++) {
            VBox v = new VBox();
            panels[i] = new Panel(i);
            doors[i] = new Door(i);
            displays[i] = new Display(i);
            weighScales[i] = new WeighScale(i);

            v.getChildren().addAll(panels[i].panelOverlay, displays[i].displayOverlay,
                    doors[i].doorOverlay, weighScales[i].weightTriggerButton);

            hbox.getChildren().add(v);
            v.setAlignment(Pos.CENTER);
        }

        double width = Screen.getPrimary().getBounds().getWidth() * (scale - 0.1);
        double height = Screen.getPrimary().getBounds().getHeight() * (scale - 0.2);

        Scene scene = new Scene(hbox, width, height, Color.web("#c0bfbbff"));
        primaryStage.setTitle("Elevator Passenger Devices");
        primaryStage.setScene(scene);
        primaryStage.show();

        System.out.println("\n\n");
        System.out.println("================================================");
        System.out.println("         INITIALIZING BUILDING SYSTEMS          ");
        System.out.println("================================================");

        new mux.BuildingMultiplexor();

        for (int i = 0; i < numElevators; i++) {
            elevatorMuxes[i] = new mux.ElevatorMultiplexor(i + 1);
        }

        for (int i = 1; i <= numElevators; i++) {
            new elevatorController.HigherLevel.ElevatorMain(i);
        }

        System.out.println("================================================");
        System.out.println("     ALL SYSTEMS INITIALIZED SUCCESSFULLY!      ");
        System.out.println("================================================\n\n");
    }

    private class Panel {
        public ImageView elevPanelImg = new ImageView();
        public StackPane panelOverlay = new StackPane(elevPanelImg);
        public Label digitalLabel;
        private double scaleFactor = scale + 0.1;
        private int yTranslation = -53;
        private int offset = 20;
        private int carId;

        private Panel(int index) {
            this.carId = index;
            makePanel();
        }

        private void makePanel() {
            elevPanelImg.setPreserveRatio(true);
            elevPanelImg.setFitWidth(300 * scaleFactor);
            elevPanelImg.setImage(loader.imageList.get(0));

            digitalLabel = new Label("1");
            digitalLabel.setStyle("-fx-text-fill: white;");
            digitalLabel.setFont(Font.font("Verdana", FontWeight.BOLD, 32));
            digitalLabel.setTranslateY(-145 * scaleFactor);
            panelOverlay.getChildren().add(digitalLabel);

            for (int i = 1; i < 10; i += 2) {
                Label left = new Label(String.valueOf(i));
                left.setStyle("-fx-text-fill: black;");
                left.setFont(Font.font("Verdana", FontWeight.BOLD, 16 * scaleFactor));
                left.setTranslateX(-23 * scaleFactor);
                left.setTranslateY(yTranslation + (offset * i) * scaleFactor);

                final int leftFloorNumber = i;
                left.setOnMouseClicked(event -> {
                    Platform.runLater(() -> {
                        if (!internalState.panelButtonsDisabled[carId]) {
                            if (!internalState.singleSelection[carId] || internalState.lastSelected == 0) {
                                if (elevatorMuxes != null && carId < elevatorMuxes.length && elevatorMuxes[carId] != null) {
                                    elevatorMuxes[carId].getElevator().panel.pressFloorButton(leftFloorNumber);
                                    internalState.lastSelected = leftFloorNumber;
                                    System.out.println("GUI: Floor " + leftFloorNumber + " button pressed in elevator " + (carId + 1));
                                }
                                left.setStyle("-fx-text-fill: #ffffffff;");
                            }
                        }
                    });
                });

                panelOverlay.getChildren().add(left);

                Label right = new Label(String.valueOf(i + 1));
                right.setStyle("-fx-text-fill: black;");
                right.setFont(Font.font("Verdana", FontWeight.BOLD, 16 * scaleFactor));
                right.setTranslateX(24 * scaleFactor);
                right.setTranslateY(yTranslation + (offset * i) * scaleFactor);

                final int rightFloorNumber = i + 1;
                right.setOnMouseClicked(event -> {
                    Platform.runLater(() -> {
                        if (!internalState.panelButtonsDisabled[carId]) {
                            if (!internalState.singleSelection[carId] || internalState.lastSelected == 0) {
                                if (elevatorMuxes != null && carId < elevatorMuxes.length && elevatorMuxes[carId] != null) {
                                    elevatorMuxes[carId].getElevator().panel.pressFloorButton(rightFloorNumber);
                                    internalState.lastSelected = rightFloorNumber;
                                    System.out.println("GUI: Floor " + rightFloorNumber + " button pressed in elevator " + (carId + 1));
                                }
                                right.setStyle("-fx-text-fill: #ffffffff;");
                            }
                        }
                    });
                });

                panelOverlay.getChildren().add(right);
            }

            Button keyButton = new Button();
            keyButton.setPrefSize(30, 30);
            keyButton.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");

            double keyX = 20;
            double keyY = 150;

            keyButton.setTranslateX(keyX);
            keyButton.setTranslateY(keyY);

            keyButton.setOnAction(event -> {
                internalState.fireKeys[carId] = !internalState.fireKeys[carId];
            });

            panelOverlay.getChildren().add(keyButton);

        }
    }

    private class Door {
        public ImageView elevDoorsImg = new ImageView();
        public StackPane doorOverlay = new StackPane(elevDoorsImg);
        private int carId;

        private Door(int index) {
            this.carId = index;
            makeDoor();
        }

        private void makeDoor() {
            elevDoorsImg.setPreserveRatio(true);
            elevDoorsImg.setFitWidth(300);
            elevDoorsImg.setImage(loader.imageList.get(6));

            internalState.doorObstructions[carId] = false;

            elevDoorsImg.setOnMouseClicked(event -> {
                if (loader.imageList.get(6).equals(elevDoorsImg.getImage())) {
                    Platform.runLater(() -> {
                        internalState.doorObstructions[carId] = true;
                        elevDoorsImg.setImage(loader.imageList.get(7));
                    });
                    return;
                } else if (loader.imageList.get(7).equals(elevDoorsImg.getImage())) {
                    Platform.runLater(() -> {
                        internalState.doorObstructions[carId] = false;
                        elevDoorsImg.setImage(loader.imageList.get(6));
                    });
                    return;
                }
            });
        }
    }

    private class Display {
        public ImageView floorDispImg = new ImageView();
        public StackPane displayOverlay = new StackPane(floorDispImg);
        public Label digitalLabel;
        private int displayIndex;

        private Display(int index) {
            this.displayIndex = index;
            makeDisplay();
        }

        private void makeDisplay() {
            floorDispImg.setPreserveRatio(true);
            floorDispImg.setFitWidth(120);
            floorDispImg.setImage(loader.imageList.get(8));

            digitalLabel = new Label("1");
            digitalLabel.setStyle("-fx-text-fill: white;");
            digitalLabel.setFont(Font.font("Verdana", FontWeight.BOLD, 18));
            digitalLabel.setTranslateY(-5);
            displayOverlay.getChildren().add(digitalLabel);

            Label floorLabel = new Label(String.valueOf(displayIndex + 1));
            floorLabel.setStyle("-fx-text-fill: black;");
            floorLabel.setFont(Font.font("Times New Roman", FontWeight.BOLD, 16));
            floorLabel.setTranslateY(55);
            displayOverlay.getChildren().add(floorLabel);
        }
    }

    private class WeighScale {
        public Button weightTriggerButton = new Button("Overload");
        private int buttonIndex;

        public WeighScale(int index) {
            this.buttonIndex = index;
            makeTrigger();
        }

        private void makeTrigger() {
            weightTriggerButton.setPrefWidth(150);
            weightTriggerButton.setPrefHeight(50);
            weightTriggerButton.setStyle("-fx-background-color: #bdbdbdff; -fx-text-fill: black;");
            weightTriggerButton.setFont(Font.font("Times New Roman", FontWeight.BOLD, 22));

            weightTriggerButton.setOnMouseClicked(event -> {

                Platform.runLater(() -> {
                    String style = weightTriggerButton.getStyle();
                    boolean isOverloaded = style.contains("#684b4bff");

                    if (isOverloaded) {
                        weightTriggerButton.setStyle("-fx-background-color: #bdbdbdff; -fx-text-fill: black;");
                        internalState.cabinOverloads[buttonIndex] = false;
                    } else {
                        weightTriggerButton.setStyle("-fx-background-color: #684b4bff; -fx-text-fill: black;");
                        internalState.cabinOverloads[buttonIndex] = true;
                    }
                });
            });
        }
    }

    private class CallButton {
        public ImageView elevCallButtonsImg = new ImageView();
        public StackPane callButtonOverlay = new StackPane(elevCallButtonsImg);
        private int buttonIndex;
        private String direction;

        private CallButton(int index) {
            this.buttonIndex = index;
            this.direction = "IDLE";
            makeCallButton();
        }

        private void makeCallButton() {
            elevCallButtonsImg.setPreserveRatio(true);
            elevCallButtonsImg.setFitWidth(100);
            elevCallButtonsImg.setFitHeight(100);
            elevCallButtonsImg.setImage(loader.imageList.get(13));

            elevCallButtonsImg.setOnMouseClicked(event -> {
                if (!internalState.callButtonsDisabled) {
                    double clickX = event.getX();
                    double clickY = event.getY();
                    double width = elevCallButtonsImg.getBoundsInLocal().getWidth();
                    double height = elevCallButtonsImg.getBoundsInLocal().getHeight();

                    double centerX = width / 2;
                    double centerY = height / 2;
                    double offsetY = 20;
                    double radius = 15;

                    double distToUp = Math.hypot(clickX - centerX, clickY - (centerY - offsetY));
                    double distToDown = Math.hypot(clickX - centerX, clickY - (centerY + offsetY));

                    if (distToUp <= radius) {
                        Platform.runLater(() -> {
                            if (buttonIndex != 9) {
                                if (callButtons[buttonIndex].direction.equals("DOWN")) {
                                    callButtons[buttonIndex].direction = "BOTH";
                                    elevCallButtonsImg.setImage(loader.imageList.get(16));
                                } else if (!callButtons[buttonIndex].direction.equals("BOTH")) {
                                    callButtons[buttonIndex].direction = "UP";
                                    elevCallButtonsImg.setImage(loader.imageList.get(15));
                                }
                                System.out.println("GUI: UP call button pressed on floor " + (buttonIndex + 1));
                            }
                        });
                    } else if (distToDown <= radius) {
                        Platform.runLater(() -> {
                            if (buttonIndex != 0) {
                                if (callButtons[buttonIndex].direction.equals("UP")) {
                                    callButtons[buttonIndex].direction = "BOTH";
                                    elevCallButtonsImg.setImage(loader.imageList.get(16));
                                } else if (!callButtons[buttonIndex].direction.equals("BOTH")) {
                                    callButtons[buttonIndex].direction = "DOWN";
                                    elevCallButtonsImg.setImage(loader.imageList.get(14));
                                }
                                System.out.println("GUI: DOWN call button pressed on floor " + (buttonIndex + 1));
                            }
                        });
                    }
                }
            });
        }

        public void setDirection(String direction) {
            this.direction = direction;
        }
    }

    private class FireAlarm {
        public ImageView fireAlarmImg = new ImageView();
        public StackPane fireAlarmOverlay = new StackPane(fireAlarmImg);

        private FireAlarm() {
            makeFireAlarm();
        }

        private void makeFireAlarm() {
            fireAlarmImg.setPreserveRatio(true);
            fireAlarmImg.setFitWidth(70);
            fireAlarmImg.setImage(loader.imageList.get(11));

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

    public static void main(String[] args) {
        launch(args);
    }
}