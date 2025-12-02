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

    public ElevatorMain(int elevatorID){
        System.out.println("========================================");
        System.out.println("Initializing Elevator " + elevatorID + " controller...");
        System.out.println("========================================");

        SoftwareBus softwareBus = new SoftwareBus(false);

        buttons = new Buttons(elevatorID, softwareBus);
        cabin = new Cabin(elevatorID, softwareBus);
        doorAssembly = new DoorAssembly(elevatorID, softwareBus);
        notifier = new Notifier(elevatorID, softwareBus);
        mode = new Mode(elevatorID, softwareBus);

        fire = new Fire(mode, buttons, cabin, doorAssembly, notifier);
        normal = new Normal(mode, buttons, cabin, doorAssembly, notifier);
        control = new Control(mode, buttons, cabin, doorAssembly, notifier);

        controller = new ElevatorController(normal, fire, control);

        Thread controllerThread = new Thread(() -> controller.elevatorController());
        controllerThread.setDaemon(true);
        controllerThread.start();

        System.out.println("Elevator " + elevatorID + " controller started successfully!");
        System.out.println("========================================\n");
    }
}