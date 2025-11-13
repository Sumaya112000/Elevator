package TestCode;

import bus.SoftwareBus;
import Message.Message;

/**
 * ScratchSender: scripted BUS test client.
 *
 * Run steps:
 *  1. In IntelliJ, run GUI.ElevatorControlSystem (so the BUS server + GUI are up).
 *  2. In a second run configuration, run TestCode.ScratchSender.
 */
public class ScratchSender {

    private static void send(SoftwareBus bus, int topic, int sub, int body, String label,
                             long delayMs) throws InterruptedException {
        System.out.printf(">>> %-50s  (%d-%d-%d)%n", label, topic, sub, body);
        bus.publish(new Message(topic, sub, body));
        Thread.sleep(delayMs);
    }

    public static void main(String[] args) throws Exception {
        System.out.println("ScratchSender: make sure ElevatorControlSystem is already running.");
        System.out.println("Connecting as BUS client...");

        // Connect as a BUS client (server already running in ElevatorControlSystem)
        SoftwareBus bus = new SoftwareBus(false);

        // Small wait so subscriptions in ElevatorPanels have time to come up
        Thread.sleep(1000);

        // ===== System-wide commands =====
        send(bus, 2, 0, 0,    "System START (Topic 2, all elevators)", 2000);
        send(bus, 1, 0, 0,    "System STOP  (Topic 1, all elevators)", 2000);
        send(bus, 3, 0, 0,    "System RESET (Topic 3, all elevators)", 2500);

        // ===== Mode commands (Topic 5, body flags) =====
        send(bus, 5, 0, 1000, "Mode = CENTRALIZED (body 1000)",       2000);
        send(bus, 5, 0, 1100, "Mode = INDEPENDENT (body 1100)",       2000);

        // Test fire: cars should recall to floor 1 and open door
        send(bus, 5, 0, 1110, "Mode = TEST FIRE (body 1110)",         3000);

        // Clear fire: doors should close, fire flag off
        send(bus, 4, 0, 0,    "CLEAR FIRE (Topic 4)",                 2500);

        // ===== Individual elevator start/stop =====
        // Start each elevator one by one
        for (int i = 1; i <= 4; i++) {
            send(bus, 6, i, 0,
                    "START elevator " + i + "  (Topic 6, sub=" + i + ")", 1200);
        }

        // Stop each elevator one by one
        for (int i = 1; i <= 4; i++) {
            send(bus, 7, i, 0,
                    "STOP elevator " + i + "   (Topic 7, sub=" + i + ")", 1200);
        }

        System.out.println("ScratchSender: demo sequence finished.");
    }
}