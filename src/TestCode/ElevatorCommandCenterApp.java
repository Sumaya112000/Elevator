package TestCode;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.*;
import javafx.scene.shape.Polygon;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

/**
 * Command Center GUI matching the provided mockup.
 * - 4 elevators side-by-side
 * - Per-elevator START/STOP, current floor display, direction chevrons
 * - 10-floor stack with door-open = white outline, door-closed = black outline
 * - Global controls: CENTRALIZED (indicator), TEST FIRE, AUTO, START, STOP, RESET
 *
 * Requires:
 *   - Elevator.java (enum-based)
 *   - ElevatorController.java
 *
 * Notes:
 *   - "TEST FIRE" puts ALL cars in FIRE mode (recall to 1).
 *   - "RESET" returns ALL cars to NORMAL.
 *   - "AUTO" sets AUTO mode for all.
 *   - Global START turns all to ON; STOP turns all to OFF.
 */
public class ElevatorCommandCenterApp extends Application {

    private static final int FLOORS = 10;

    private static class CarWidgets {
        final Elevator car;
        final ElevatorController ctl;

        final Label title;
        final Button btnPower; // toggles STOP/START
        final Label txtCurrent; // small rounded "7 / 8 / etc" under STOP/START
        final Polygon upChevron, downChevron;
        final List<FloorCell> floorCells;

        CarWidgets(Elevator car, ElevatorController ctl, Label title, Button btnPower,
                   Label txtCurrent, Polygon upChevron, Polygon downChevron, List<FloorCell> cells) {
            this.car = car; this.ctl = ctl; this.title = title; this.btnPower = btnPower;
            this.txtCurrent = txtCurrent; this.upChevron = upChevron; this.downChevron = downChevron;
            this.floorCells = cells;
        }
    }

    /** One floor box with a label and an outline that turns white when door is open. */
    private static class FloorCell extends StackPane {
        final Label lbl;
        final Region outline;
        final Region sideLamp; // small round lamp at left (for looks)

        FloorCell(String text) {
            setPrefSize(54, 34);
            setMinSize(54, 34);
            setMaxSize(54, 34);

            // interior
            lbl = new Label(text);
            lbl.setStyle("-fx-text-fill: #eaeaea; -fx-font-size: 14px; -fx-font-weight: bold;");

            // outline (door state look)
            outline = new Region();
            outline.setStyle("-fx-border-color: black; -fx-border-width: 2; -fx-background-color: #6f6f6f; -fx-background-radius: 3; -fx-border-radius: 3;");
            outline.setPrefSize(54, 34);

            // side lamp (small circle)
            sideLamp = new Region();
            sideLamp.setStyle("-fx-background-color: #cfd9e3; -fx-background-radius: 100;");
            sideLamp.setPrefSize(8, 8);
            StackPane.setAlignment(sideLamp, Pos.CENTER_LEFT);
            StackPane.setMargin(sideLamp, new Insets(0,0,0, -14));

            getChildren().addAll(outline, lbl, sideLamp);
        }

        void setDoorOpen(boolean open) {
            // white outline if open, black if closed
            if (open) {
                outline.setStyle("-fx-border-color: white; -fx-border-width: 2; -fx-background-color: #6f6f6f; -fx-background-radius: 3; -fx-border-radius: 3;");
            } else {
                outline.setStyle("-fx-border-color: black; -fx-border-width: 2; -fx-background-color: #6f6f6f; -fx-background-radius: 3; -fx-border-radius: 3;");
            }
        }

        void highlight(boolean active) {
            // shade active floor a bit darker
            if (active) {
                lbl.setStyle("-fx-text-fill: #eaeaea; -fx-font-size: 14px; -fx-font-weight: bold;");
                outline.setStyle(outline.getStyle() + "; -fx-background-color: #5f5f5f;");
            }
        }
    }

    private final List<CarWidgets> cars = new ArrayList<>();

    // Global right-side buttons
    private Label centralIndicator;

    @Override
    public void start(Stage stage) {
        // 4 cars
        for (int i = 0; i < 4; i++) {
            Elevator e = new Elevator();
            ElevatorController c = new ElevatorController(e);
            cars.add(buildCarColumn(i + 1, e, c));
        }

        // Title
        Label title = new Label("Command Center");
        title.setFont(Font.font("System", 34));
        title.setStyle("-fx-text-fill: #f0f0f0;");

        HBox columns = new HBox(30,
                makeCarPane(cars.get(0)),
                makeCarPane(cars.get(1)),
                makeCarPane(cars.get(2)),
                makeCarPane(cars.get(3)),
                rightSideControls()
        );
        columns.setAlignment(Pos.TOP_CENTER);
        columns.setPadding(new Insets(10,20,20,20));

        VBox root = new VBox(10, title, columns);
        root.setAlignment(Pos.TOP_CENTER);
        root.setStyle("-fx-background-color: #404040;");

        Scene scene = new Scene(root, 980, 560);
        stage.setTitle("Elevator Command Center");
        stage.setScene(scene);
        stage.show();

        // simulation clock
        Timeline tl = new Timeline(new KeyFrame(Duration.millis(250), e -> {
            for (CarWidgets cw : cars) {
                cw.ctl.tick();
            }
            refreshUI();
        }));
        tl.setCycleCount(Timeline.INDEFINITE);
        tl.play();

        refreshUI();
    }

    // Build one elevator column
    private CarWidgets buildCarColumn(int index, Elevator car, ElevatorController ctl) {
        // header "Elevator N"
        Label header = new Label("Elevator " + index);
        header.setStyle("-fx-text-fill: #e8e8e8; -fx-font-size: 16px; -fx-font-weight: bold;");

        // STOP/START button (per car power)
        Button btn = new Button("STOP");
        btn.setId("car" + index + "Power");
        styleStop(btn);
        btn.setOnAction(ev -> {
            if (car.getStatus() == Elevator.Status.ON) {
                car.setStatus("OFF");
                btn.setText("STOP"); styleStop(btn);
            } else {
                car.setStatus("ON");
                btn.setText("START"); styleStart(btn);
            }
        });

        // small rounded current-floor indicator
        Label smallBox = new Label(""); // shows current floor (e.g., 7 / 8 / 1)
        smallBox.setId("car" + index + "CurrentBox");
        smallBox.setMinSize(58, 26);
        smallBox.setAlignment(Pos.CENTER);
        smallBox.setStyle("-fx-background-color: #aeb5bb; -fx-text-fill: #1f1f1f; -fx-font-size: 16px; -fx-font-weight: bold; -fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: #5b646b;");

        // chevrons (up/down) under the box
        Polygon upCh = triangleUp();    upCh.setId("car" + index + "Up");
        Polygon dnCh = triangleDown();  dnCh.setId("car" + index + "Down");

        // floor stack (10→1)
        List<FloorCell> cells = new ArrayList<>();
        for (int f = FLOORS; f >= 1; f--) {
            FloorCell cell = new FloorCell(Integer.toString(f));
            cell.setId("car" + index + "F" + f);
            cells.add(cell);
        }

        return new CarWidgets(car, ctl, header, btn, smallBox, upCh, dnCh, cells);
    }

    // Pane for a single car (arrange nodes visually)
    private VBox makeCarPane(CarWidgets cw) {
        VBox floorStack = new VBox(6);
        floorStack.setAlignment(Pos.TOP_CENTER);
        for (FloorCell cell : cw.floorCells) floorStack.getChildren().add(cell);

        HBox arrows = new HBox(12, cw.upChevron, cw.downChevron);
        arrows.setAlignment(Pos.CENTER);

        VBox top = new VBox(8, cw.title, cw.btnPower, cw.txtCurrent, arrows);
        top.setAlignment(Pos.CENTER);

        VBox wrapper = new VBox(10, top, floorStack);
        wrapper.setAlignment(Pos.TOP_CENTER);
        wrapper.setPadding(new Insets(6, 0, 0, 0));

        return wrapper;
    }

    // Right-side global controls panel
    private VBox rightSideControls() {
        // CENTRALIZED (indicator label)
        centralIndicator = badge("CENTRALIZED");
        centralIndicator.setStyle("-fx-background-color: #d5dce3; -fx-text-fill: #1a361f; -fx-font-weight: bold; -fx-padding: 14 20; -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #22302a;");
        TitledPane central = block(" ", centralIndicator);
        central.setCollapsible(false);

        Button fire = wideBtn("TEST FIRE", "#aa1717"); fire.setId("btnFireAll");
        fire.setOnAction(e -> {
            for (CarWidgets cw : cars) cw.car.setMode("FIRE");
        });

        Button auto = wideBtn("AUTO", "#193c6b"); auto.setId("btnAutoAll");
        auto.setStyle(auto.getStyle() + "; -fx-border-color: #ffd84a; -fx-border-width: 2; -fx-border-radius: 10;");
        auto.setOnAction(e -> {
            for (CarWidgets cw : cars) cw.car.setMode("AUTO");
        });

        Button start = wideBtn("START", "#1f4a2d"); start.setId("btnStartAll");
        start.setOnAction(e -> {
            for (CarWidgets cw : cars) { cw.car.setStatus("ON"); }
        });

        Button stop = wideBtn("STOP", "#9f1919"); stop.setId("btnStopAll");
        stop.setOnAction(e -> {
            for (CarWidgets cw : cars) { cw.car.setStatus("OFF"); }
        });

        Button reset = wideBtn("RESET", "#5b646b"); reset.setId("btnResetAll");
        reset.setOnAction(e -> {
            for (CarWidgets cw : cars) { cw.car.setMode("NORMAL"); cw.car.setStatus("ON"); }
        });

        VBox group = new VBox(12,
                central,
                fire,
                auto,
                start,
                stop,
                reset
        );
        group.setAlignment(Pos.TOP_CENTER);
        group.setPadding(new Insets(6, 0, 0, 10));

        return group;
    }

    // Refresh visual state from model
    private void refreshUI() {
        for (int i = 0; i < cars.size(); i++) {
            CarWidgets cw = cars.get(i);
            Elevator car = cw.car;

            // update the small current-floor box
            cw.txtCurrent.setText(Integer.toString(car.getCurrentFloor()));

            // chevrons visibility
            cw.upChevron.setOpacity(car.getDirection() == Elevator.Direction.UP ? 1.0 : 0.25);
            cw.downChevron.setOpacity(car.getDirection() == Elevator.Direction.DOWN ? 1.0 : 0.25);

            // power button label/color
            if (car.getStatus() == Elevator.Status.ON) {
                cw.btnPower.setText("START");
                styleStart(cw.btnPower);
            } else {
                cw.btnPower.setText("STOP");
                styleStop(cw.btnPower);
            }

            // floor stack: clear, then set active & door outline
            for (FloorCell cell : cw.floorCells) {
                cell.setDoorOpen(false);
                cell.setOpacity(1);
            }
            int here = car.getCurrentFloor();
            // index in cells is (10→1), so 10 maps to idx0, 1 maps to idx9
            int idx = FLOORS - here;
            FloorCell active = cw.floorCells.get(idx);
            active.highlight(true);
            active.setDoorOpen(car.isDoorOpen());
        }
    }

    // ---------- small UI helpers & styles ----------
    private static void styleStop(Button b) {
        b.setMinSize(86, 36);
        b.setStyle("-fx-background-color: #c9302c; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-border-radius: 8;");
    }
    private static void styleStart(Button b) {
        b.setMinSize(86, 36);
        b.setStyle("-fx-background-color: #2d5b3b; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-border-radius: 8;");
    }
    private static Label badge(String text) {
        Label l = new Label(text);
        l.setAlignment(Pos.CENTER);
        return l;
    }
    private static TitledPane block(String title, Region child) {
        var pane = new TitledPane(title, new StackPane(child));
        pane.setPrefWidth(200);
        pane.setCollapsible(false);
        return pane;
    }
    private static Button wideBtn(String text, String bg) {
        Button b = new Button(text);
        b.setMinSize(180, 44);
        b.setStyle("-fx-background-color: " + bg + "; -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold; -fx-background-radius: 10; -fx-border-radius: 10;");
        return b;
    }
    private static Polygon triangleUp() {
        Polygon p = new Polygon(0.0, 14.0, 10.0, 0.0, 20.0, 14.0);
        p.setStyle("-fx-fill: #e6e6e6;");
        return p;
    }
    private static Polygon triangleDown() {
        Polygon p = new Polygon(0.0, 0.0, 10.0, 14.0, 20.0, 0.0);
        p.setStyle("-fx-fill: #1a1a1a;");
        return p;
    }

    public static void main(String[] args) { launch(args); }
}