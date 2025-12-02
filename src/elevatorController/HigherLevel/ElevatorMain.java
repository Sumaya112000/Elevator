package elevatorController.HigherLevel;

import bus.Bus.SoftwareBus;
import elevatorController.LowerLevel.*;

/**
 * Main is a lightweight object, which instantiates Elevator Controller, Mode,
 * Buttons, Cabin, Door Assembly and Notifier.
 */
public class ElevatorMain {
    /**
     * Instantiate Everything
     * @param elevatorID the number associated with this elevator
     * @param softwareBus the means of communication
     */
    private Buttons buttons;
    private Cabin cabin;
    private DoorAssembly doorAssembly;
    private Notifier notifier;
    private Mode mode;
    private Fire fire;
    private Normal normal;
    private Control control;

    public ElevatorMain(int elevatorID, SoftwareBus softwareBus){
        buttons = new Buttons(elevatorID, softwareBus);
        cabin = new Cabin(elevatorID, softwareBus);
        doorAssembly = new DoorAssembly(elevatorID, softwareBus);
        notifier = new Notifier(elevatorID, softwareBus);
        mode = new Mode(elevatorID, softwareBus);
        fire = new Fire(mode,buttons,cabin,doorAssembly,notifier);
        normal = new Normal(mode,buttons,cabin,doorAssembly,notifier);
        control = new Control(mode,buttons,cabin,doorAssembly,notifier);

    }
}