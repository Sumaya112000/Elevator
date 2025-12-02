package mux;

/**
 * HALL CALL ENCODING CONVENTION:
 * ================================
 * This encoding is used consistently across the entire system.
 *
 * UP calls:   body = floor + 100
 *   Examples:
 *     - Floor 1 UP  → body = 101
 *     - Floor 5 UP  → body = 105
 *     - Floor 9 UP  → body = 109
 *
 * DOWN calls: body = floor
 *   Examples:
 *     - Floor 2 DOWN → body = 2
 *     - Floor 7 DOWN → body = 7
 *     - Floor 10 DOWN → body = 10
 *
 * Decoding (in receivers like ElevatorPanel, SystemController):
 *   if (body > 100) {
 *       floor = body - 100;
 *       direction = UP;
 *   } else {
 *       floor = body;
 *       direction = DOWN;
 *   }
 *
 * This encoding is already implemented correctly in pollCallButtons() below.
 */

import Bus.*;
import Message.*;
import javafx.application.Platform;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import pfdAPI.Building;
import pfdAPI.FloorCallButtons;

import java.net.URL;
import java.util.Arrays;

import static java.lang.Math.abs;

/**
 * Class that defines the BuildingMultiplexor, which coordinates communication from the Elevator
 * Command Center to the relevant devices. Communication is accomplished via the software bus,
 * and both the PFDs and the motion devices are subject to control.
 *
 * Note: car and elevator are used interchangeably in this context.
 */
public class BuildingMultiplexor {

    // Constructor
    public BuildingMultiplexor(){
        initialize();
    }

    // Listener for GUI/API integration
    private final SoftwareBus bus = new SoftwareBus(false);
    private final Building bldg = new Building(10);;
    boolean[][] lastCallState = new boolean[bldg.totalFloors][3]; // Up/Down/Null
    private boolean lastFireState = false;
    int[] elevatorPos = new int[4];

    int DIR_UP = 0;
    int DIR_DOWN = 1;

    int FIRE_OFF = 0;
    int FIRE_ON = 1;

    // Initialize the MUX
    public void initialize() {
        bus.subscribe(SoftwareBusCodes.fireAlarm, 5);
        bus.subscribe(SoftwareBusCodes.resetCall, 5);
        bus.subscribe(SoftwareBusCodes.callsEnable, 5);

        //NOTE: MUX EATS THESE MESSAGES FOR OPTIMAL CALL DISPATCHING
        bus.subscribe(SoftwareBusCodes.cabinPosition, 1);
        bus.subscribe(SoftwareBusCodes.cabinPosition, 2);
        bus.subscribe(SoftwareBusCodes.cabinPosition, 3);
        bus.subscribe(SoftwareBusCodes.cabinPosition, 4);
        Arrays.fill(elevatorPos, 1);

        System.out.println("BuildingMUX initialized and subscribed");
        startBusPoller();
        startStatePoller();
    }

    /**
     * Incoming Message Polling
     */

    // Polls the software bus for messages and handles them accordingly
    public void startBusPoller() {
        Thread t = new Thread(() -> {
            // keep polling
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

    /**
     * Internal State Polling Functions
     */

    // Polls the bldg state periodically and publishes updates to the bus
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

    // Poll all call buttons
    private void pollCallButtons() {
        for (int floor = 0; floor < bldg.callButtons.length; floor++) {
            int elevator = bestElevator(floor);
            if (bldg.callButtons[floor].isUpCallPressed() && !lastCallState[floor][0]) {
                System.out.println("Closest elev is " + (elevator+1) + ", floor is " + (floor+1));
                bus.publish(new Message(SoftwareBusCodes.hallCall, elevator + 1, floor + 1 + 100));
                lastCallState[floor][0] = true;
            }

            if (bldg.callButtons[floor].isDownCallPressed() && !lastCallState[floor][1]) {
                System.out.println("Closest elev is " + (elevator+1) + ", floor is " + (floor+1));
                bus.publish(new Message(SoftwareBusCodes.hallCall, elevator + 1, floor + 1));
                lastCallState[floor][1] = true;
            }
        }
    }

    // Poll fire alarm state
    private void pollFireAlarm() {
        boolean state = bldg.callButtons[0].getFireAlarmStatus();
        if (state != lastFireState) {
            bus.publish(new Message(SoftwareBusCodes.fireAlarmActive, 5, state ? FIRE_ON : FIRE_OFF));
            lastFireState = state;
            if(state){
                fireAlarmResets(true);
                playFireAlarm();
            }
        }
    }

    /**
     * Incoming Message Handlers
     */

    // Handle Fire Alarm Message
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
        }
    }

    // Handle Call Reset Message
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

    // Handle Call Enable/Disable Message
    public void handleCallEnable(Message msg){
        int body = msg.getBody();
        fireAlarmResets(false);
        bldg.callButtons[1].setButtonsEnabled(body);
    }

    // Handle all elevator position (for call button servicing)
    private void handleElevatorPos(Message msg){
        int elevator = msg.getSubTopic()-1;
        int floor =  msg.getBody();
        elevatorPos[elevator] = floor;
    }

    /**
     * Util
     */

    // Choose the closet elevator to give the hall call to
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

    private void playFireAlarm(){
        System.out.println("FIRE!");
        Platform.runLater(() -> {
            try {
                URL sound = getClass().getResource("../res/sounds/firealarm.mp3");
                if (sound == null) {
                    System.err.println("Sound file not found.");
                    return;
                }

                Media media = new Media(sound.toExternalForm());
                MediaPlayer player = new MediaPlayer(media);
                player.play();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}