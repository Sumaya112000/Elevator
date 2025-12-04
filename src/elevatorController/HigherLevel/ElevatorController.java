package elevatorController.HigherLevel;

import elevatorController.Util.State;

public class ElevatorController {
    private Normal normal;
    private Fire fire;
    private Control control;
    private State currentState;

    public ElevatorController(Normal normal, Fire fire, Control control) {
        this.normal = normal;
        this.fire = fire;
        this.control = control;
        this.currentState = State.NORMAL;
    }

    public void elevatorController(){
        // The main state machine loop
        while (true) {
            State nextState = State.NULL;

            if (currentState == State.NORMAL) {
                nextState = normal.normal();
            } else if (currentState == State.FIRE) {
                nextState = fire.fire();
            } else if (currentState == State.CONTROL) {
                nextState = control.control();
            }

            if (nextState != State.NULL && nextState != currentState) {
                System.out.println("Elevator transitioning from " + currentState + " to " + nextState);
                currentState = nextState;
            }

            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}