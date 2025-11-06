package TestCode;

import bus.SoftwareBus;
import Message.Message;
import Message.Commands;
import Message.Channels;

/**
 * ScratchSender: a tiny no-UI publisher that connects to the running BUS server
 * and sends a few demo commands. Run your Command Center (ElevatorControlSystem)
 * first so the BUS server is up, then run this main().
 */
public class ScratchSender {
    public static void main(String[] args) throws Exception {
        // Connect as a BUS client (server must already be running)
        SoftwareBus bus = new SoftwareBus(false);

        // --- Examples ---

        // 1) START all elevators (system-wide topic 0)
        bus.publish(new Message(Channels.SYSTEM, 0, Commands.encode(Commands.START, 0)));
        System.out.println("Sent: START (broadcast)");

        // 2) Targeted move: Elevator 3 → floor 7 (topic = 3)
        bus.publish(new Message(3, 0, Commands.encode(Commands.GOTO, 7)));
        System.out.println("Sent: E3 GOTO 7");
        Thread.sleep(2200);

        // 3) FIRE ON (everyone recalls to 1 and opens)
        bus.publish(new Message(Channels.SYSTEM, 0, Commands.encode(Commands.FIRE_ON, 0)));
        System.out.println("Sent: FIRE_ON");
        Thread.sleep(2500);

        // 4) Clear FIRE
        bus.publish(new Message(Channels.SYSTEM, 0, Commands.encode(Commands.FIRE_CLEAR, 0)));
        System.out.println("Sent: FIRE_CLEAR");
        Thread.sleep(2000);

        // 5) Move ALL elevators to floor 5 (separate messages to topics 1–4)
        for (int i = 1; i <= 4; i++) {
            bus.publish(new Message(i, 0, Commands.encode(Commands.GOTO, 5)));
            System.out.println("Sent: Elevator " + i + " GOTO 5");
            Thread.sleep(500);

        System.out.println("ScratchSender finished.");
    }
}