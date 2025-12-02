package elevatorController.HigherLevel;

import elevatorController.Util.State;

public class ElevatorController {
    private Normal normal;
    private Fire fire;
    private Control control;

    public ElevatorController(Normal normal, Fire fire, Control control) {
        this.normal = normal;
        this.fire = fire;
        this.control = control;
    }

    public void elevatorController(){
        State currentState = State.NORMAL;

        System.out.println("ElevatorController started in NORMAL mode");

        while (true) {
            switch (currentState) {
                case NORMAL:
                    currentState = normal.normal();
                    break;
                case FIRE:
                    currentState = fire.fire();
                    break;
                case CONTROL:
                    currentState = control.control();
                    break;
                default:
                    System.err.println("Unknown state: " + currentState);
                    currentState = State.NORMAL;
            }
        }
    }
}