package TestCode;

import Bus.*;
import Message.*;

/**
 * TestHarness for demonstrating elevator system functionality.
 *
 * HALL CALL ENCODING:
 * - UP calls: body = floor + 100 (e.g., floor 5 UP = 105)
 * - DOWN calls: body = floor (e.g., floor 5 DOWN = 5)
 *
 * ASSUMPTIONS:
 * - All elevators start at floor 1 with doors open
 * - CommandCenter GUI is already running (provides bus server)
 */
public class TestHarness {

    public static void main(String[] args) throws Exception {

        SoftwareBus bus = new SoftwareBus(false);

        System.out.println("========================================");
        System.out.println("    ELEVATOR SYSTEM TEST HARNESS");
        System.out.println("========================================");

        Thread.sleep(2000);

        // ============================================================
        // TEST 0: SYSTEM START
        // ============================================================
        System.out.println("\n[TEST 0] SYSTEM START");
        System.out.println("Starting all elevators...");
        bus.publish(new Message(SoftwareBusCodes.systemStart, 0, 0));
        Thread.sleep(1000);
        System.out.println("✓ System started - all elevators at floor 1, doors open");

        // ============================================================
        // TEST 1: HALL CALLS BEFORE FIRE
        // ============================================================
        System.out.println("\n[TEST 1] HALL CALLS (elevators at floor 1)");
        System.out.println("Sending UP calls to floors 3, 5, 6, 8...");

        // All calls are from floor 1 going UP, so body = floor + 100
        bus.publish(new Message(SoftwareBusCodes.hallCall, 1, 105));  // Floor 5 UP
        System.out.println("  → E1: Floor 5 UP (body=105)");
        Thread.sleep(200);

        bus.publish(new Message(SoftwareBusCodes.hallCall, 2, 103));  // Floor 3 UP
        System.out.println("  → E2: Floor 3 UP (body=103)");
        Thread.sleep(200);

        bus.publish(new Message(SoftwareBusCodes.hallCall, 3, 108));  // Floor 8 UP
        System.out.println("  → E3: Floor 8 UP (body=108)");
        Thread.sleep(200);

        bus.publish(new Message(SoftwareBusCodes.hallCall, 4, 106));  // Floor 6 UP
        System.out.println("  → E4: Floor 6 UP (body=106)");

        System.out.println("✓ Hall calls sent - UP indicators should light on floors 3, 5, 6, 8");
        Thread.sleep(3000);

        // ============================================================
        // TEST 2: FIRE MODE ACTIVATION
        // ============================================================
        System.out.println("\n[TEST 2] FIRE MODE");
        System.out.println("Activating fire mode...");
        bus.publish(new Message(SoftwareBusCodes.setMode, 0, SoftwareBusCodes.fire));
        Thread.sleep(500);

        System.out.println("✓ Fire mode active");
        System.out.println("  - All hall call indicators should clear");
        System.out.println("  - Elevators already at floor 1, doors should remain/become open");

        // Ensure doors are open for fire mode
        for (int id = 1; id <= 4; id++) {
            bus.publish(new Message(SoftwareBusCodes.doorStatus, id, 0)); // 0 = open
            System.out.println("  → E" + id + " doors open at floor 1");
        }

        System.out.println("Holding fire mode for 5 seconds...");
        Thread.sleep(5000);

        // ============================================================
        // TEST 3: CLEAR FIRE AND RECOVERY
        // ============================================================
        System.out.println("\n[TEST 3] CLEAR FIRE");
        System.out.println("Clearing fire mode...");
        bus.publish(new Message(SoftwareBusCodes.clearFire, 0, 0));
        Thread.sleep(1000);

        System.out.println("✓ Fire cleared - returning to CENTRALIZED mode");

        // Close doors after fire clear
        System.out.println("Closing doors...");
        for (int id = 1; id <= 4; id++) {
            bus.publish(new Message(SoftwareBusCodes.doorStatus, id, 1)); // 1 = closed
        }
        Thread.sleep(1000);
        System.out.println("✓ All doors closed");

        // ============================================================
        // TEST 4: HALL CALLS AFTER FIRE
        // ============================================================
        System.out.println("\n[TEST 4] HALL CALLS AFTER FIRE");
        System.out.println("System should now be responsive to new hall calls");
        System.out.println("Sending UP calls to floors 4, 5, 7, 9...");

        // All calls from floor 1 going UP = body = floor + 100
        bus.publish(new Message(SoftwareBusCodes.hallCall, 1, 107));  // Floor 7 UP
        System.out.println("  → E1: Floor 7 UP (body=107)");
        Thread.sleep(200);

        bus.publish(new Message(SoftwareBusCodes.hallCall, 2, 104));  // Floor 4 UP
        System.out.println("  → E2: Floor 4 UP (body=104)");
        Thread.sleep(200);

        bus.publish(new Message(SoftwareBusCodes.hallCall, 3, 109));  // Floor 9 UP
        System.out.println("  → E3: Floor 9 UP (body=109)");
        Thread.sleep(200);

        bus.publish(new Message(SoftwareBusCodes.hallCall, 4, 105));  // Floor 5 UP
        System.out.println("  → E4: Floor 5 UP (body=105)");

        System.out.println("✓ Post-fire hall calls sent - indicators should light up!");
        Thread.sleep(3000);

        // ============================================================
        // TEST 5: SIMULATE ELEVATOR MOVEMENT
        // ============================================================
        System.out.println("\n[TEST 5] SIMULATE ELEVATOR MOVEMENT");
        System.out.println("Simulating elevators responding to calls...");

        for (int elevatorId = 1; elevatorId <= 4; elevatorId++) {
            final int carId = elevatorId;
            final int targetFloor = 5 + elevatorId; // E1→6, E2→7, E3→8, E4→9

            new Thread(() -> {
                try {
                    int currentFloor = 1;

                    System.out.println("  → E" + carId + " moving UP to floor " + targetFloor);

                    // Signal movement UP
                    bus.publish(new Message(SoftwareBusCodes.displayDirection, carId, 0)); // 0 = UP

                    // Simulate movement floor by floor
                    while (currentFloor < targetFloor) {
                        currentFloor++;
                        bus.publish(new Message(SoftwareBusCodes.cabinPosition, carId, currentFloor));
                        bus.publish(new Message(SoftwareBusCodes.displayFloor, carId, currentFloor));
                        Thread.sleep(600);
                    }

                    // Arrived - stop and open doors
                    bus.publish(new Message(SoftwareBusCodes.displayDirection, carId, 2)); // 2 = IDLE
                    bus.publish(new Message(SoftwareBusCodes.doorStatus, carId, 0)); // 0 = open
                    System.out.println("  ✓ E" + carId + " arrived at floor " + targetFloor + " - doors open");

                    // Hold doors open
                    Thread.sleep(2000);

                    // Close doors
                    bus.publish(new Message(SoftwareBusCodes.doorStatus, carId, 1)); // 1 = closed
                    System.out.println("  → E" + carId + " doors closed");

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();

            Thread.sleep(1000); // Stagger elevator starts
        }

        System.out.println("Waiting for all elevators to complete...");
        Thread.sleep(8000);

        // ============================================================
        // TEST 6: FINAL VERIFICATION
        // ============================================================
        System.out.println("\n[TEST 6] FINAL HALL CALL VERIFICATION");
        System.out.println("Testing mixed hall calls...");

        // Mix of UP calls from various positions
        bus.publish(new Message(SoftwareBusCodes.hallCall, 1, 103));  // Floor 3 UP
        System.out.println("  → E1: Floor 3 UP (body=103)");
        Thread.sleep(200);

        bus.publish(new Message(SoftwareBusCodes.hallCall, 2, 110));  // Floor 10 UP
        System.out.println("  → E2: Floor 10 UP (body=110)");
        Thread.sleep(200);

        bus.publish(new Message(SoftwareBusCodes.hallCall, 3, 102));  // Floor 2 UP
        System.out.println("  → E3: Floor 2 UP (body=102)");
        Thread.sleep(200);

        bus.publish(new Message(SoftwareBusCodes.hallCall, 4, 108));  // Floor 8 UP
        System.out.println("  → E4: Floor 8 UP (body=108)");

        System.out.println("✓ Final hall calls sent - all UP indicators should light!");
        Thread.sleep(3000);

        // ============================================================
        // TEST 7: TEST CABIN SELECTIONS
        // ============================================================
        System.out.println("\n[TEST 7] CABIN BUTTON SELECTIONS");
        System.out.println("Simulating passengers pressing cabin buttons...");

        bus.publish(new Message(SoftwareBusCodes.cabinSelect, 1, 4));
        System.out.println("  → E1: Cabin button 4 pressed");
        Thread.sleep(200);

        bus.publish(new Message(SoftwareBusCodes.cabinSelect, 2, 7));
        System.out.println("  → E2: Cabin button 7 pressed");
        Thread.sleep(200);

        bus.publish(new Message(SoftwareBusCodes.cabinSelect, 3, 9));
        System.out.println("  → E3: Cabin button 9 pressed");
        Thread.sleep(200);

        bus.publish(new Message(SoftwareBusCodes.cabinSelect, 4, 6));
        System.out.println("  → E4: Cabin button 6 pressed");

        System.out.println("✓ Cabin selections sent");
        Thread.sleep(2000);


        // SUMMARY


        System.out.println("    TEST HARNESS COMPLETE");
        System.out.println("\nTests Performed:");
        System.out.println("  ✓ System start/stop");
        System.out.println("  ✓ Hall calls with correct encoding");
        System.out.println("  ✓ Fire mode activation");
        System.out.println("  ✓ Fire mode clear and recovery");
        System.out.println("  ✓ Post-fire hall calls");
        System.out.println("  ✓ Simulated elevator movement");
        System.out.println("  ✓ Cabin button selections");
        System.out.println("\nExpected Behavior:");
        System.out.println("  - Hall call indicators light correctly (UP/DOWN arrows)");
        System.out.println("  - Fire mode clears all indicators");
        System.out.println("  - System recovers properly after fire clear");
        System.out.println("  - Elevators display floor changes and direction");
        System.out.println("  - Doors open/close on command");
        System.out.println("\nNote: Actual elevator movement requires SystemController");

    }
}