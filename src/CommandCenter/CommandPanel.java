package CommandCenter;

import Bus.*;
import Message.*;

import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;
import javafx.scene.paint.Color;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.VPos;

/**
 * CommandPanel
 * Uses the BUS commands:
 *
 *  Topic  Subtopic   Body(4 ints)  Meaning
 *    1       0       {0,0,0,0}    System Stop        (all elevators)
 *    2       0       {0,0,0,0}    System Start       (all elevators)
 *    3       0       {0,0,0,0}    System Reset       (all elevators)
 *    4       0       {0,0,0,0}    Clear Fire         (all elevators)
 *    5       0       {1,0,0,0}    Mode = Centralized
 *    5       0       {1,1,0,0}    Mode = Independent
 *    5       0       {1,1,1,0}    Mode = Test Fire
 *    6     1..4      {0,0,0,0}    Start individual elevator
 *    7     1..4      {0,0,0,0}    Stop individual elevator
 *
 * Here we encode Body(4 ints) as a simple 4-digit int:
 *   {1,0,0,0} to 1000
 *   {1,1,0,0} to 1100 and
 *   {1,1,1,0} to 1110
 */
public class CommandPanel extends GridPane {

    private final SoftwareBus bus; // Command Center's BUS client

    // UI controls
    private final Label  modeDisplay;
    private final Button autoButton;
    private final Button fireControlButton;
    private final Button startButton;
    private final Button stopButton;


    // Local UI state (purely visual)
    private boolean systemRunning = true;
    private String systemMode = "CENTRALIZED"; // CENTRALIZED | INDEPENDENT | FIRE


    // Styling
    private final String modeDisplayBaseStyle =
            "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16px; " +
                    "-fx-alignment: center; -fx-background-radius: 14; -fx-padding: 6 10 6 10;";


    private final String buttonBaseStyle =
            "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16px; " +
                    "-fx-background-radius: 14; -fx-padding: 6 10 6 10; " +
                    "-fx-border-radius: 14; -fx-border-width: 1; -fx-border-color: rgba(255,255,255,0.15);";




    private final String colorModeCentral = "-fx-background-color: #00695C;";
    private final String colorModeIndependent = "-fx-background-color: #424242;";
    private final String colorModeFire = "-fx-background-color: #D32F2F;";
    private final String colorFire = "-fx-background-color: #C62828;";
    private final String colorAuto = "-fx-background-color: #283593;";
    private final String colorStart = "-fx-background-color: #2E7D32;";
    private final String colorStop = "-fx-background-color: #B71C1C;";
    private final String autoBorderOn  = "-fx-border-color: #FFEB3B;";
    private final String autoBorderOff = "-fx-border-color: rgba(255,255,255,0.10);";
    private final String fireBtnGlowOn  = "-fx-effect: dropshadow(gaussian, rgba(255,193,7,0.7), 18, 0.4, 0, 0);";
    private final String fireBtnGlowOff = "-fx-effect: none;";



    private static final int B_MODE_CEN = 1000;
    private static final int B_MODE_IND = 1100;
    private static final int B_MODE_TF  = 1110;

    public CommandPanel(SoftwareBus bus) {
        this.bus = bus;
        // LISTEN FOR MODE UPDATES FROM THE BUS
        bus.subscribe(TopicCodes.MODE.code(), 0);

        // Layout grid
        setStyle("-fx-background-color: #333333;");
        setPadding(new Insets(20, 24, 20, 24));
        setHgap(10);
        setVgap(10);

        RowConstraints row0 = rc(10);
        RowConstraints row1 = rc(30);
        RowConstraints row2 = rc(30);
        RowConstraints row3 = rc(32);
        getRowConstraints().addAll(row0, row1, row2, row3);
        RowConstraints buttonRow = rc(70);
        for (int i = 0; i < 10; i++) {
            getRowConstraints().add(buttonRow);
        }

        // Mode display badge
        modeDisplay = new Label("CENTRALIZED");
        modeDisplay.setPrefSize(170, 60);
        modeDisplay.setStyle(modeDisplayBaseStyle + colorModeCentral);
        add(modeDisplay, 0, 2, 1, 2);



        // Buttons  publish messages
        fireControlButton = createButton("TEST FIRE", Color.web("#C62828"), 70,
                e -> onFirePressed());
        add(fireControlButton, 0, 4, 1, 2);



        autoButton = createButton("AUTO", Color.web("#283593"), 70,
                e -> onAutoPressed());
        autoButton.setStyle(colorAuto + " " + buttonBaseStyle + " " + autoBorderOn);
        add(autoButton, 0, 6, 1, 2);



        startButton = createButton("START", Color.web("#2E7D32"), 70,
                e -> onStart());
        add(startButton, 0, 8, 1, 2);



        stopButton = createButton("STOP", Color.web("#B71C1C"), 70,
                e -> onStop());
        add(stopButton, 0, 10, 1, 2);

        updateButtonStates(true);

        startBusListener();
    }

    //bus publisher helpers
    private void publishAll(int topic, int body) {
        bus.publish(new Message(topic, 0, body));
    }

    public void publishToCar(int topic, int elevatorId, int body) {
        if (elevatorId < 1 || elevatorId > 4) return;
        bus.publish(new Message(topic, elevatorId, body));
    }

    //button handlers
    private void onStart() {
        systemRunning = true;
        publishAll(TopicCodes.SYSTEM_START.code(), 0); // System Start
        updateButtonStates(true);
    }

    private void onStop() {
        systemRunning = false;
        publishAll(TopicCodes.SYSTEM_STOP.code(), 0); // System Stop
        updateButtonStates(false);
    }

    private void onReset() {
        systemRunning = true;
        systemMode = "CENTRALIZED";
        publishAll(TopicCodes.SYSTEM_RESET.code(), 0); // System Reset
        updateForReset();
    }

    private void onFirePressed() {
        if ("FIRE".equals(systemMode)) {
            systemMode = "CENTRALIZED";
            publishAll(TopicCodes.CLEAR_FIRE.code(), 0);    // Clear Fire
            updateForFireMode(false);
        } else {
            systemMode = "FIRE";
            publishAll(TopicCodes.MODE.code(), B_MODE_TF); // Test Fire
            updateForFireMode(true);
        }
    }

    private void onAutoPressed() {
        if ("FIRE".equals(systemMode)) return; // ignore during FIRE

        if ("CENTRALIZED".equals(systemMode)) {
            systemMode = "INDEPENDENT";
            publishAll(TopicCodes.MODE.code(), B_MODE_IND); // Mode: Independent
            updateForAutoMode("INDEPENDENT");
        } else {
            systemMode = "CENTRALIZED";
            publishAll(TopicCodes.MODE.code(), B_MODE_CEN); // Mode: Centralized
            updateForAutoMode("CENTRALIZED");
        }
    }

    //LOCAL ui HELPERS

    private Button createButton(String text, Color bgColor, double height,
                                EventHandler<ActionEvent> listener) {
        Button button = new Button(text);
        button.setStyle(buttonBaseStyle +
                String.format(" -fx-background-color: #%02X%02X%02X;",
                        (int) (bgColor.getRed() * 255),
                        (int) (bgColor.getGreen() * 255),
                        (int) (bgColor.getBlue() * 255)));
        button.setPrefSize(170, height);
        button.setMaxHeight(height);
        // keep text visible even when disabled
        button.setStyle(button.getStyle() + " -fx-opacity: 1.0;");
        if (listener != null) button.setOnAction(listener);
        return button;
    }


    private RowConstraints rc(double h) {
        RowConstraints rc = new RowConstraints(h);
        rc.setValignment(VPos.CENTER);
        return rc;
    }


    public void updateButtonStates(boolean isRunning) {
        startButton.setDisable(isRunning);
        stopButton.setDisable(!isRunning);
        fireControlButton.setDisable(!isRunning);
        autoButton.setDisable(!isRunning);

        // keep labels readable even when disabled
        String keepOpacity = " -fx-opacity: 1.0;";
        startButton.setStyle(startButton.getStyle() + keepOpacity);
        stopButton.setStyle(stopButton.getStyle() + keepOpacity);
        fireControlButton.setStyle(fireControlButton.getStyle() + keepOpacity);
        autoButton.setStyle(autoButton.getStyle() + keepOpacity);
    }


    public void updateForReset() {
        modeDisplay.setText("CENTRALIZED");
        modeDisplay.setStyle(modeDisplayBaseStyle + colorModeCentral);
        fireControlButton.setText("TEST FIRE");
        fireControlButton.setStyle(colorFire + " " + buttonBaseStyle + " " + fireBtnGlowOff);
        autoButton.setStyle(colorAuto + " " + buttonBaseStyle + " " + autoBorderOn + " -fx-opacity: 1.0;");
        updateButtonStates(true);
    }


    public void updateForFireMode(boolean isFire) {
        if (isFire) {
            modeDisplay.setText("FIRE");
            modeDisplay.setStyle(modeDisplayBaseStyle + colorModeFire);
            fireControlButton.setText("CLEAR FIRE");
            fireControlButton.setStyle(colorFire + " " + buttonBaseStyle + " " + fireBtnGlowOn + " -fx-opacity: 1.0;");
        } else {
            modeDisplay.setText("CENTRALIZED");
            modeDisplay.setStyle(modeDisplayBaseStyle + colorModeCentral);
            fireControlButton.setText("TEST FIRE");
            fireControlButton.setStyle(colorFire + " " + buttonBaseStyle + " " + fireBtnGlowOff + " -fx-opacity: 1.0;");
            autoButton.setStyle(colorAuto + " " + buttonBaseStyle + " " + autoBorderOn + " -fx-opacity: 1.0;");
        }
    }


    public void updateForAutoMode(String mode) {
        if ("CENTRALIZED".equals(mode)) {
            modeDisplay.setText("CENTRALIZED");
            modeDisplay.setStyle(modeDisplayBaseStyle + colorModeCentral);
            autoButton.setStyle(colorAuto + " " + buttonBaseStyle + " " + autoBorderOn + " -fx-opacity: 1.0;");
        } else {
            modeDisplay.setText("INDEPENDENT");
            modeDisplay.setStyle(modeDisplayBaseStyle + colorModeIndependent);
            autoButton.setStyle(colorAuto + " " + buttonBaseStyle + " " + autoBorderOff + " -fx-opacity: 1.0;");
        }

    }

    /**
     * Background listener for CommandPanel.
     * Polls the bus
     */
    private void startBusListener() {
        Thread t = new Thread(() -> {
            while (true) {
                poll(TopicCodes.SYSTEM_STOP.code(), 0);   // System Stop
                poll(TopicCodes.SYSTEM_START.code(), 0);   // System Start
                poll(TopicCodes.SYSTEM_RESET.code(), 0);   // System Reset
                poll(TopicCodes.CLEAR_FIRE.code(), 0);   // Clear Fire
                poll(TopicCodes.MODE.code(), 0);   // Mode (1000/1100/1110)

                try {
                    Thread.sleep(10);
                } catch (InterruptedException ignored) {}
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private void poll(int topic, int subtopic) {
        Message m = bus.get(topic, subtopic);
        if (m != null) handleCommand(m);
    }

    private void handleCommand(Message m) {
        String t = TopicCodes.fromCode(m.getTopic());
        int body = m.getBody();

        switch (t) {
            case "SYSTEM_STOP" -> // System Stop
                    Platform.runLater(() -> {
                        systemRunning = false;
                        updateButtonStates(false);
                    });

            case "SYSTEM_START" -> // System Start
                    Platform.runLater(() -> {
                        systemRunning = true;
                        updateButtonStates(true);
                    });

            case "SYSTEM_RESET" -> // System Reset
                    Platform.runLater(() -> {
                        systemRunning = true;
                        systemMode = "CENTRALIZED";
                        updateForReset();
                    });

            case "CLEAR_FIRE" -> // Clear Fire
                    Platform.runLater(() -> {
                        systemMode = "CENTRALIZED";
                        updateForFireMode(false);        // remove fire styling
                        updateForAutoMode("CENTRALIZED"); // restore auto button border
                        modeDisplay.setText("CENTRALIZED");
                        modeDisplay.setStyle(modeDisplayBaseStyle + colorModeCentral);
                        updateButtonStates(true); // start/stop buttons re-enable
                    });

            case "MODE" -> // Mode change body: 1000/1100/1110
                    Platform.runLater(() -> {
                        switch (body) {
                            case B_MODE_CEN -> {                 // CENTRALIZED
                                systemMode = "CENTRALIZED";
                                updateForAutoMode("CENTRALIZED");
                            }

                            case B_MODE_IND -> {                // INDEPENDENT
                                systemMode = "INDEPENDENT";
                                updateForAutoMode("INDEPENDENT");
                            }

                            case B_MODE_TF -> {                // TEST FIRE
                                systemMode = "FIRE";
                                updateForFireMode(true);
                            }

                            default -> System.out.println("CommandPanel: Unknown mode: " + body);
                        }
                    });

            default -> {
                // Ignore unrelated topics
            }
        }
    }
}