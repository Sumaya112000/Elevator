package elevatorController.HigherLevel;

import Bus.*;
import elevatorController.LowerLevel.*;

public class ElevatorMain {
    private Buttons buttons;
    private Cabin cabin;
    private DoorAssembly doorAssembly;
    private Notifier notifier;
    private Mode mode;
    private Fire fire;
    private Normal normal;
    private Control control;
    private ElevatorController controller;

    public ElevatorMain(int elevatorID, SoftwareBus softwareBus){
        // 1. Instantiate Lower Level Objects
        buttons = new Buttons(elevatorID, softwareBus);
        cabin = new Cabin(elevatorID, softwareBus);
        doorAssembly = new DoorAssembly(elevatorID, softwareBus);
        notifier = new Notifier(elevatorID, softwareBus);
        mode = new Mode(elevatorID, softwareBus);

        // 2. Instantiate Higher Level Mode Procedures
        fire = new Fire(mode,buttons,cabin,doorAssembly,notifier);
        normal = new Normal(mode,buttons,cabin,doorAssembly,notifier);
        control = new Control(mode,buttons,cabin,doorAssembly,notifier);

        // 3. Instantiate the Controller and start the main loop
        controller = new ElevatorController(normal, fire, control);

        Thread controllerThread = new Thread(() -> {
            controller.elevatorController();
        });
        controllerThread.setName("ElevatorController-" + elevatorID);
        controllerThread.start();

        System.out.println("Elevator " + elevatorID + " fully initialized.");
    }
}