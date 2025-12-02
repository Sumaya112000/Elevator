package elevatorController.HigherLevel;

import elevatorController.LowerLevel.*;
import elevatorController.Util.State;

public class Fire {
    private Mode mode;
    private Buttons buttons;
    private Cabin cabin;
    private DoorAssembly doorAssembly;
    private Notifier notifier;

    public Fire(Mode mode, Buttons buttons, Cabin cabin,
                DoorAssembly doorAssembly, Notifier notifier) {
        this.mode = mode;
        this.buttons = buttons;
        this.cabin = cabin;
        this.doorAssembly = doorAssembly;
        this.notifier = notifier;
    }

    public State fire(){
        System.out.println("Entering FIRE mode - recalling to floor 1");

        buttons.disableCalls();
        buttons.enableSingleRequest();

        cabin.gotoFloor(1);

        while (!cabin.arrived() || cabin.getTargetFloor() != 1) {
            try { Thread.sleep(100); } catch (InterruptedException e) {}
        }

        doorAssembly.open();

        while (!doorAssembly.fullyOpen()) {
            try { Thread.sleep(100); } catch (InterruptedException e) {}
        }

        System.out.println("Fire recall complete - doors open at floor 1");

        while (true) {
            State newMode = mode.getMode();
            if (newMode != State.FIRE) {
                System.out.println("Exiting FIRE mode to: " + newMode);

                buttons.enableCalls();
                buttons.enableAllRequests();

                return newMode;
            }

            try { Thread.sleep(100); }
            catch (InterruptedException e) { break; }
        }

        return State.NORMAL;
    }
}