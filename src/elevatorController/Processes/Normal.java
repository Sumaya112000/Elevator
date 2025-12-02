package elevatorController.Processes;

import elevatorController.Util.State;

/**
 * Normal mode is the default mode that the system starts in.
 * The initial state of each elevator is on the first floor with the doors open.
 * In this mode, no messages from the supervisor are expected, other than a change in mode;
 * The movement is determined solely by button presses.
 * This mode provides the typical elevator functionality: handling requests and navigating floors.
 */
public class Normal {
    public State normal(){ return State.NULL;}
}