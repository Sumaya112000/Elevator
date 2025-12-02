package elevatorController.HigherLevel;

import elevatorController.LowerLevel.*;
import elevatorController.Util.Direction;
import elevatorController.Util.FloorNDirection;
import elevatorController.Util.State;

public class Normal {
    private Mode mode;
    private Buttons buttons;
    private Cabin cabin;
    private DoorAssembly doorAssembly;
    private Notifier notifier;

    public Normal(Mode mode, Buttons buttons, Cabin cabin,
                  DoorAssembly doorAssembly, Notifier notifier) {
        this.mode = mode;
        this.buttons = buttons;
        this.cabin = cabin;
        this.doorAssembly = doorAssembly;
        this.notifier = notifier;
    }

    public State normal(){
        System.out.println("Entering NORMAL mode");

        if (!doorAssembly.fullyClosed()) {
            System.out.println("Startup: Closing doors at floor 1");
            doorAssembly.close();

            int timeout = 0;
            while (!doorAssembly.fullyClosed() && timeout < 50) {
                try { Thread.sleep(100); } catch (InterruptedException e) {}
                timeout++;
            }
            System.out.println("Doors closed, ready for operation");
        }

        while (true) {
            State newMode = mode.getMode();
            if (newMode != State.NORMAL) {
                System.out.println("Exiting NORMAL mode to: " + newMode);
                return newMode;
            }

            FloorNDirection currentStatus = cabin.currentStatus();

            if (cabin.arrived() && doorAssembly.fullyClosed()) {
                System.out.println("Arrived at destination, opening doors");
                doorAssembly.open();

                while (!doorAssembly.fullyOpen()) {
                    try { Thread.sleep(100); } catch (InterruptedException e) {}
                }

                notifier.arrivedAtFloor(currentStatus);

                int targetFloor = cabin.getTargetFloor();
                buttons.requestReset(targetFloor);

                try { Thread.sleep(5000); } catch (InterruptedException e) {}

                while (doorAssembly.overCapacity()) {
                    notifier.playCapacityNoise();
                    try { Thread.sleep(1000); } catch (InterruptedException e) {}
                }
                notifier.stopCapacityNoise();

                doorAssembly.close();

                while (!doorAssembly.fullyClosed()) {
                    if (doorAssembly.obstructed()) {
                        doorAssembly.open();
                        while (!doorAssembly.fullyOpen()) {
                            try { Thread.sleep(100); } catch (InterruptedException e) {}
                        }
                        try { Thread.sleep(3000); } catch (InterruptedException e) {}
                        doorAssembly.close();
                    }
                    try { Thread.sleep(100); } catch (InterruptedException e) {}
                }
            }

            if (cabin.arrived()) {
                FloorNDirection nextService = buttons.nextService(currentStatus);
                if (nextService != null) {
                    cabin.gotoFloor(nextService.floor());
                    notifier.elevatorStatus(new FloorNDirection(nextService.floor(),
                            nextService.floor() > currentStatus.floor() ? Direction.UP : Direction.DOWN));
                }
            }

            try { Thread.sleep(100); }
            catch (InterruptedException e) { break; }
        }
        return State.NORMAL;
    }
}