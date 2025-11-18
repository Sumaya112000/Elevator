package TestCode;

import bus.SoftwareBus;
import CommandCenter.Messages.Message;
import java.util.Random;

public class TestHarness {

    public static void main(String[] args) throws Exception {

        SoftwareBus bus = new SoftwareBus(false);
        Random rand = new Random();

        Thread.sleep(2000);

        System.out.println("START ALL");
        bus.publish(new Message(2,0,0));
        Thread.sleep(1000);

        System.out.println("STOP E1");
        bus.publish(new Message(7,1,0));
        Thread.sleep(2000);

        System.out.println("START E1");
        bus.publish(new Message(6,1,0));
        Thread.sleep(2000);


        // fire
        System.out.println("\n=== TEST FIRE MODE ===");
        bus.publish(new Message(5,0,1110));  // enter FIRE
        Thread.sleep(300);

        System.out.println("All elevators recalling to floor 1 … (simultaneous)");

        Thread[] recallThreads = new Thread[4];

        for (int id = 1; id <= 4; id++) {
            final int carId = id;

            recallThreads[id-1] = new Thread(() -> {

                int currentFloor = 10;
                int targetFloor = 1;

                // Tell GUI: moving down
                bus.publish(new Message(112, carId, 1));

                while (currentFloor > targetFloor) {
                    currentFloor--;

                    bus.publish(new Message(202, carId, currentFloor)); // position
                    bus.publish(new Message(111, carId, currentFloor)); // floor display

                    try { Thread.sleep(500); } catch (InterruptedException ignored) {}
                }

                // Arrived at floor 1
                bus.publish(new Message(112, carId, 2)); // idle
                bus.publish(new Message(204, carId, 0)); // open doors
            });

            recallThreads[id-1].start();
        }

        // wait for all to finish
        for (Thread t : recallThreads) t.join();

        System.out.println("Holding fire recall for 3 seconds…");
        Thread.sleep(3000);

        System.out.println("AUTO CLEAR FIRE");
        bus.publish(new Message(4,0,0)); // Clear Fire
        Thread.sleep(400);


        // Close doors and refresh GUI
        for (int id = 1; id <= 4; id++) {
            bus.publish(new Message(112, id, 2));
            bus.publish(new Message(111, id, 1));
            bus.publish(new Message(204, id, 1));
            Thread.sleep(200);
        }


        System.out.println("MOVING ALL 4 ELEVATORS TO RANDOM FLOORS WITH DOORS...");

        for (int cycle = 1; cycle <= 5; cycle++) {
            System.out.println("---- Cycle " + cycle + " ----");

            for (int elevatorId = 1; elevatorId <= 4; elevatorId++) {

                int targetFloor = 1 + rand.nextInt(10);
                System.out.println("Dispatch E" + elevatorId + " -> " + targetFloor);

                int currentFloor = 5;  // or track if you want
                int step = (targetFloor > currentFloor) ? 1 : -1;

                bus.publish(new Message(112, elevatorId,
                        step > 0 ? 0 : 1)); // up/down

                while (currentFloor != targetFloor) {
                    currentFloor += step;

                    bus.publish(new Message(202, elevatorId, currentFloor));
                    bus.publish(new Message(111, elevatorId, currentFloor));

                    Thread.sleep(600);
                }

                bus.publish(new Message(112, elevatorId, 2));

                // doors
                bus.publish(new Message(204, elevatorId, 0));
                Thread.sleep(2000);

                bus.publish(new Message(204, elevatorId, 1));
                Thread.sleep(1000);
            }

            Thread.sleep(3000);
        }

        System.out.println("DONE.");
    }
}
