package integration;

import Bus.SoftwareBus;
import CommandCenter.ElevatorControlSystem;
import elevatorController.SystemController;

/**
 * Full system integration main.
 * Starts:
 * - CommandCenter GUI (includes bus server)
 * - SystemController (automated elevator control)
 *
 * Note: BuildingMultiplexor and ElevatorMultiplexors are started by pfdGUI.gui
 */
public class FullSystemMain {

    public static void main(String[] args) {
        System.out.println("   FULL ELEVATOR SYSTEM STARTUP");


        // 1. Start CommandCenter GUI in separate thread (includes bus server)
        Thread guiThread = new Thread(() -> {
            ElevatorControlSystem.main(args);
        });
        guiThread.setDaemon(false);
        guiThread.start();

        System.out.println("CommandCenter GUI starting...");

        // Wait for GUI and bus to initialize
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // 2. Create bus client for controller
        SoftwareBus controllerBus = new SoftwareBus(false);
        System.out.println("Controller connected to bus");

        // 3. Start SystemController
        SystemController controller = new SystemController(controllerBus, 4);
        Thread controllerThread = new Thread(controller);
        controllerThread.setDaemon(true);
        controllerThread.start();
        System.out.println("SystemController started");

        System.out.println("\n========================================");
        System.out.println("   SYSTEM READY");
        System.out.println("========================================");
        System.out.println("CommandCenter GUI: Running");
        System.out.println("SystemController: Running");
        System.out.println("\nNote: Start pfdGUI.gui separately for");
        System.out.println("      physical devices and motion simulation");
        System.out.println("========================================\n");
    }
}