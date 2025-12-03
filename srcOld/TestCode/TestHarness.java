package TestCode;

import Bus.*;
import Message.*;
import java.util.Random;

public class TestHarness {

    public static void main(String[] args) throws Exception {

        SoftwareBus bus = new SoftwareBus(false);
        Random rand = new Random();

        Thread.sleep(2000);

        System.out.println("START ALL");
        System.out.println("START ALL ELEVATORS");
        bus.publish(new Message(SoftwareBusCodes.systemStart, 0, 0));
        Thread.sleep(1000);

        // TEST HALL CALLS BEFORE FIRE
        System.out.println("\nTEST 1: HALL CALLS BEFORE FIRE");
        bus.publish(new Message(SoftwareBusCodes.hallCall, 1, 5));  // Call E1 to floor 5 (UP from floor 10)
        bus.publish(new Message(SoftwareBusCodes.hallCall, 2, 3));  // Call E2 to floor 3 (DOWN from floor 10)
        bus.publish(new Message(SoftwareBusCodes.hallCall, 3, 8));  // Call E3 to floor 8 (DOWN from floor 10)
        bus.publish(new Message(SoftwareBusCodes.hallCall, 4, 6));  // Call E4 to floor 6 (DOWN from floor 10)
        System.out.println("Hall calls sent - indicators should light up");
        Thread.sleep(3000);

        // FIRE MODE TEST
        System.out.println("\nTEST 2: FIRE MODE");
        bus.publish(new Message(SoftwareBusCodes.setMode, 0, SoftwareBusCodes.fire));  // enter FIRE
        Thread.sleep(300);

        System.out.println("All elevators recalling to floor 1 … (simultaneous)");
        System.out.println("All hall call indicators should clear during fire mode");

        Thread[] recallThreads = new Thread[4];

        for (int id = 1; id <= 4; id++) {
            final int carId = id;

            recallThreads[id-1] = new Thread(() -> {
                int currentFloor = 10;
                int targetFloor = 1;

                // Tell GUI: moving down
                bus.publish(new Message(SoftwareBusCodes.displayDirection, carId, 1)); // down

                while (currentFloor > targetFloor) {
                    currentFloor--;
                    bus.publish(new Message(SoftwareBusCodes.cabinPosition, carId, currentFloor));
                    bus.publish(new Message(SoftwareBusCodes.displayFloor, carId, currentFloor));
                    try { Thread.sleep(500); } catch (InterruptedException ignored) {}
                }

                // Arrived at floor 1
                bus.publish(new Message(SoftwareBusCodes.displayDirection, carId, 2)); // idle
                bus.publish(new Message(SoftwareBusCodes.doorStatus, carId, 0)); // open doors
                System.out.println("Elevator " + carId + " arrived at floor 1 (fire recall)");
            });
            recallThreads[id-1].start();
        }

        // wait for all to finish
        for (Thread t : recallThreads) t.join();

        System.out.println("Holding fire recall for 3 seconds…");
        Thread.sleep(3000);

        // CLEAR FIRE AND TEST HALL CALLS AFTER FIRE
        System.out.println("\nTEST 3: CLEAR FIRE AND TEST HALL CALLS");
        bus.publish(new Message(SoftwareBusCodes.clearFire, 0, 0)); // Clear Fire
        Thread.sleep(1000);

        // Close doors after fire clear
        for (int id = 1; id <= 4; id++) {
            bus.publish(new Message(SoftwareBusCodes.doorStatus, id, 1)); // close doors
        }
        Thread.sleep(1000);

        // TEST HALL CALLS AFTER FIRE
        System.out.println("All elevators should now be at floor 1 and responsive to hall calls");
        System.out.println("Sending hall calls after fire clear...");

        // Test various hall calls - these should work now!
        bus.publish(new Message(SoftwareBusCodes.hallCall, 1, 7));  // Call E1 to floor 7 (UP from floor 1)
        Thread.sleep(500);
        bus.publish(new Message(SoftwareBusCodes.hallCall, 2, 4));  // Call E2 to floor 4 (UP from floor 1)
        Thread.sleep(500);
        bus.publish(new Message(SoftwareBusCodes.hallCall, 3, 9));  // Call E3 to floor 9 (UP from floor 1)
        Thread.sleep(500);
        bus.publish(new Message(SoftwareBusCodes.hallCall, 4, 5));  // Call E4 to floor 5 (UP from floor 1)

        System.out.println("Hall calls sent after fire clear - indicators should light up again!");
        Thread.sleep(3000);

        // Simulate elevator responses to show indicators clearing
        System.out.println("\nTEST 4: ELEVATOR RESPONSES AFTER FIRE");

        for (int elevatorId = 1; elevatorId <= 4; elevatorId++) {
            final int carId = elevatorId;
            int targetFloor = 5 + elevatorId; // E1->6, E2->7, E3->8, E4->9

            new Thread(() -> {
                try {
                    int currentFloor = 1;

                    // Moving up to target
                    bus.publish(new Message(SoftwareBusCodes.displayDirection, carId, 0)); // up

                    while (currentFloor < targetFloor) {
                        currentFloor++;
                        bus.publish(new Message(SoftwareBusCodes.cabinPosition, carId, currentFloor));
                        bus.publish(new Message(SoftwareBusCodes.displayFloor, carId, currentFloor));
                        Thread.sleep(600);
                    }

                    // Arrived at target floor
                    bus.publish(new Message(SoftwareBusCodes.displayDirection, carId, 2)); // idle
                    bus.publish(new Message(SoftwareBusCodes.doorStatus, carId, 0)); // open doors
                    System.out.println("Elevator " + carId + " arrived at floor " + targetFloor + " - indicator should clear");

                    Thread.sleep(2000);

                    // Close doors
                    bus.publish(new Message(SoftwareBusCodes.doorStatus, carId, 1)); // close doors

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();

            Thread.sleep(1000); // Stagger the starts
        }

        Thread.sleep(8000);

        // Final test - more hall calls to ensure system is fully functional
        System.out.println("\nTEST 5: FINAL HALL CALL VERIFICATION");
        bus.publish(new Message(SoftwareBusCodes.hallCall, 1, 3));
        bus.publish(new Message(SoftwareBusCodes.hallCall, 2, 10));
        bus.publish(new Message(SoftwareBusCodes.hallCall, 3, 2));
        bus.publish(new Message(SoftwareBusCodes.hallCall, 4, 8));

        System.out.println("Final hall calls sent all indicators should work normally after fire mode");
        System.out.println("Buttoms should light up now, the fix is working!");
        Thread.sleep(5000);

        System.out.println("\nTEST COMPLETE");
        System.out.println("Hall calls should work before, during, and after fire mode");
    }
}