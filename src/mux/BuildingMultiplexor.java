package mux;

import Bus.*;
import Message.*;
import static java.lang.Math.abs;
import java.net.URL;
import java.util.Arrays;
import javafx.application.Platform;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import pfdAPI.Building;
import pfdAPI.FloorCallButtons;

/**
 * BuildingMultiplexor with looping fire alarm audio.
 *
 * HALL CALL ENCODING:
 * - UP calls: body = floor + 100
 * - DOWN calls: body = floor
 */
public class BuildingMultiplexor {

    private final SoftwareBus bus = new SoftwareBus(false);
    private final Building bldg = new Building(10);
    boolean[][] lastCallState = new boolean[bldg.totalFloors][3];
    private boolean lastFireState = false;
    int[] elevatorPos = new int[4];

    // Fire alarm audio player - loops until stopped
    private MediaPlayer fireAlarmPlayer = null;

    int DIR_UP = 0;
    int DIR_DOWN = 1;
    int FIRE_OFF = 0;
    int FIRE_ON = 1;

    public BuildingMultiplexor(){
        initialize();
    }

    public void initialize() {
        bus.subscribe(SoftwareBusCodes.fireAlarm, 5);
        bus.subscribe(SoftwareBusCodes.resetCall, 5);
        bus.subscribe(SoftwareBusCodes.callsEnable, 5);

        bus.subscribe(SoftwareBusCodes.cabinPosition, 1);
        bus.subscribe(SoftwareBusCodes.cabinPosition, 2);
        bus.subscribe(SoftwareBusCodes.cabinPosition, 3);
        bus.subscribe(SoftwareBusCodes.cabinPosition, 4);
        Arrays.fill(elevatorPos, 1);

        System.out.println("BuildingMUX initialized and subscribed");
        startBusPoller();
        startStatePoller();
    }

    public void startBusPoller() {
        Thread t = new Thread(() -> {
            while (true) {
                Message msg;
                msg = bus.get(SoftwareBusCodes.fireAlarm, 5);
                if (msg != null) {
                    handleFireAlarm(msg);
                }
                msg = bus.get(SoftwareBusCodes.resetCall, 5);
                if (msg != null) {
                    handleCallReset(msg);
                }
                msg = bus.get(SoftwareBusCodes.callsEnable, 5);
                if (msg != null) {
                    handleCallEnable(msg);
                }
                msg = bus.get(SoftwareBusCodes.cabinPosition, 1);
                if (msg != null) {
                    handleElevatorPos(msg);
                }
                msg = bus.get(SoftwareBusCodes.cabinPosition, 2);
                if (msg != null) {
                    handleElevatorPos(msg);
                }
                msg = bus.get(SoftwareBusCodes.cabinPosition, 3);
                if (msg != null) {
                    handleElevatorPos(msg);
                }
                msg = bus.get(SoftwareBusCodes.cabinPosition, 4);
                if (msg != null) {
                    handleElevatorPos(msg);
                }
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        t.start();
    }

    private void startStatePoller() {
        Thread statePoller = new Thread(() -> {
            while (true) {
                pollCallButtons();
                pollFireAlarm();

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        statePoller.start();
    }

    private void pollCallButtons() {
        for (int floor = 0; floor < bldg.callButtons.length; floor++) {
            int elevator = bestElevator(floor);

            // UP call: body = floor + 100
            if (bldg.callButtons[floor].isUpCallPressed() && !lastCallState[floor][0]) {
                System.out.println("Closest elev is " + (elevator+1) + ", floor is " + (floor+1));
                bus.publish(new Message(SoftwareBusCodes.hallCall, elevator + 1, floor + 1 + 100));
                lastCallState[floor][0] = true;
            }

            // DOWN call: body = floor
            if (bldg.callButtons[floor].isDownCallPressed() && !lastCallState[floor][1]) {
                System.out.println("Closest elev is " + (elevator+1) + ", floor is " + (floor+1));
                bus.publish(new Message(SoftwareBusCodes.hallCall, elevator + 1, floor + 1));
                lastCallState[floor][1] = true;
            }
        }
    }

    private void pollFireAlarm() {
        boolean state = bldg.callButtons[0].getFireAlarmStatus();
        if (state != lastFireState) {
            bus.publish(new Message(SoftwareBusCodes.fireAlarmActive, 5, state ? FIRE_ON : FIRE_OFF));
            lastFireState = state;
            if(state){
                fireAlarmResets(true);
                playFireAlarm();
            } else {
                stopFireAlarm();
            }
        }
    }

    public void handleFireAlarm(Message msg) {
        int modeCode = msg.getBody();
        if ((modeCode == FIRE_ON) && (!lastFireState)) {
            bldg.callButtons[0].setFireAlarm(true);
            lastFireState = true;
            fireAlarmResets(false);
            playFireAlarm();
        } else if(modeCode == FIRE_OFF){
            bldg.callButtons[0].setFireAlarm(false);
            lastFireState = false;
            stopFireAlarm();
        }
    }

    public void handleCallReset(Message msg) {
        int floor = msg.getBody()/10;
        int directionCode = msg.getBody()%10;
        if (directionCode == DIR_UP) {
            bldg.callButtons[floor].resetCallButton("UP");
            lastCallState[floor][0] = false;
        }
        else if (directionCode == DIR_DOWN) {
            bldg.callButtons[floor].resetCallButton("DOWN");
            lastCallState[floor][1] = false;
        }
    }

    public void handleCallEnable(Message msg){
        int body = msg.getBody();
        fireAlarmResets(false);
        bldg.callButtons[1].setButtonsEnabled(body);
    }

    private void handleElevatorPos(Message msg){
        int elevator = msg.getSubTopic()-1;
        int floor =  msg.getBody();
        elevatorPos[elevator] = floor;
    }

    private int bestElevator(int floor){
        int choose = 0;
        int bestDistance = 1000;
        for(int i = 0; i < 4; i++){
            if(bestDistance > abs(elevatorPos[i]-floor)){
                bestDistance = abs(elevatorPos[i]-floor);
                choose = i;
            }
        }
        return choose;
    }

    private void fireAlarmResets(boolean sendMsg){
        for(FloorCallButtons buttons : bldg.callButtons){
            buttons.resetCallButton("DOWN");
            buttons.resetCallButton("UP");
        }
        if(sendMsg){
            bus.publish(new Message(SoftwareBusCodes.fireAlarm, 1, 1));
            bus.publish(new Message(SoftwareBusCodes.fireAlarm, 2, 1));
            bus.publish(new Message(SoftwareBusCodes.fireAlarm, 3, 1));
            bus.publish(new Message(SoftwareBusCodes.fireAlarm, 4, 1));
        }
    }

    /**
     * Start looping fire alarm sound.
     */
    private void playFireAlarm(){
        System.out.println("FIRE! - Starting looping alarm");
        try {
            Platform.runLater(() -> {
                try {
                    // Stop any existing alarm
                    if (fireAlarmPlayer != null) {
                        fireAlarmPlayer.stop();
                        fireAlarmPlayer.dispose();
                    }

                    URL sound = getClass().getResource("/sounds/firealarm.mp3");
                    if (sound == null) {
                        System.err.println("Fire alarm sound file not found at /sounds/firealarm.mp3");
                        return;
                    }

                    Media media = new Media(sound.toExternalForm());
                    fireAlarmPlayer = new MediaPlayer(media);

                    // Loop indefinitely
                    fireAlarmPlayer.setCycleCount(MediaPlayer.INDEFINITE);

                    fireAlarmPlayer.play();
                    System.out.println("Fire alarm playing (looping indefinitely)");
                } catch (Exception e) {
                    System.err.println("Error playing fire alarm: " + e.getMessage());
                    e.printStackTrace();
                }
            });
        } catch (IllegalStateException e) {
            System.out.println("FIRE! (JavaFX not initialized - skipping audio)");
        }
    }

    /**
     * Stop looping fire alarm sound.
     */
    private void stopFireAlarm() {
        System.out.println("Fire alarm cleared - stopping sound");
        try {
            Platform.runLater(() -> {
                if (fireAlarmPlayer != null) {
                    fireAlarmPlayer.stop();
                    fireAlarmPlayer.dispose();
                    fireAlarmPlayer = null;
                    System.out.println("Fire alarm sound stopped");
                }
            });
        } catch (IllegalStateException e) {
            System.out.println("Fire alarm stopped (no JavaFX)");
        }
    }
}