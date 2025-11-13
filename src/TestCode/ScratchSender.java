package TestCode;

import bus.SoftwareBus;
import Message.Message;
import Message.Topic;

/**
 * ScratchSender: no-UI publisher that talks to the running BUS server.
 *
 * Run your Command Center (ElevatorControlSystem) first so the server is up.
 * Then run this main(). It sends a few spaced-out commands that match the
 * spreadsheet protocol (topic, subtopic, body).
 *
 * Bodies for MODE (Topic 5):
 *   1000 = Centralized
 *   1100 = Independent
 *   1110 = Test Fire
 * Elsewhere use 0000.
 */
public class ScratchSender {

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }

    public static void main(String[] args) {
        // Connect as a BUS client
        SoftwareBus bus = new SoftwareBus(false);

        // 1) START all (topic=2, sub=0, body=0000)
        bus.publish(new Message(Topic.SYSTEM_START.code(), 0, 0000));
        System.out.println("Sent: START (20 0000)");
        sleep(800);

        // 2) MODE -> Centralized (topic=5, sub=0, body=1000)
        bus.publish(new Message(Topic.MODE.code(), 0, 1000));
        System.out.println("Sent: MODE CENTRALIZED (50 1000)");
        sleep(800);

        // 3) MODE -> Independent (topic=5, sub=0, body=1100)
        bus.publish(new Message(Topic.MODE.code(), 0, 1100));
        System.out.println("Sent: MODE INDEPENDENT (50 1100)");
        sleep(800);

        // 4) MODE -> Test Fire (topic=5, sub=0, body=1110)
        bus.publish(new Message(Topic.MODE.code(), 0, 1110));
        System.out.println("Sent: MODE TEST FIRE (50 1110)");
        sleep(1500);

        // 5) Clear Fire (topic=4, sub=0, body=0000)
        bus.publish(new Message(Topic.CLEAR_FIRE.code(), 0, 0000));
        System.out.println("Sent: CLEAR FIRE (40 0000)");
        sleep(800);

        // 6) Start only Elevator 3 (topic=6, sub=3, body=0000)
        bus.publish(new Message(Topic.START_ONE.code(), 3, 0000));
        System.out.println("Sent: START E3 (63 0000)");
        sleep(800);

        // 7) Stop only Elevator 1 (topic=7, sub=1, body=0000)
        bus.publish(new Message(Topic.STOP_ONE.code(), 1, 0000));
        System.out.println("Sent: STOP E1 (71 0000)");
        sleep(800);

        // 8) System STOP all (topic=1, sub=0, body=0000)
        bus.publish(new Message(Topic.SYSTEM_STOP.code(), 0, 0000));
        System.out.println("Sent: SYSTEM STOP (10 0000)");

        System.out.println("ScratchSender finished.");
    }
}