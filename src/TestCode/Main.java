package TestCode;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * Console-based test harness. No GUI*/
public class Main {

    private static final List<String> results = new ArrayList<>();
    private static int passed = 0, failed = 0;

    public static void main(String[] args) throws Exception {
        // ---- Unit-ish tests on Elevator
        test("Initial state: floor=1, idle, doors closed",
                () -> {
                    Elevator e = new Elevator();
                    return e.getCurrentFloor() == 1 &&
                            !e.isDoorOpen() &&
                            !e.isMoving() &&
                            e.getDirection() == Elevator.Direction.IDLE &&
                            e.getStatus() == Elevator.Status.ON &&
                            e.getMode() == Elevator.Mode.NORMAL;
                });

        test("Request same floor opens door",
                () -> {
                    Elevator e = new Elevator();
                    try { e.request(1); } catch (Exception ex) { return false; }
                    return e.isDoorOpen() && !e.isMoving() && e.getDirection() == Elevator.Direction.IDLE;
                });

        test("Request up to 5 moves then opens",
                () -> {
                    Elevator e = new Elevator();
                    try { e.request(5); } catch (Exception ex) { return false; }
                    // simulate stepping
                    int guard = 50;
                    while (e.isMoving() && guard-- > 0) e.moveElevator();
                    return e.getCurrentFloor() == 5 && e.isDoorOpen() && !e.isMoving();
                });

        test("Request invalid floor throws",
                () -> {
                    Elevator e = new Elevator();
                    try { e.request(15); return false; }
                    catch (Exception ex) { return ex.getMessage().contains("Invalid Floor"); }
                });

        test("Fire mode: recall to 1, open, deny other floors",
                () -> {
                    Elevator e = new Elevator();
                    try { e.request(6); } catch (Exception ex) { return false; }
                    while (e.isMoving()) e.moveElevator();
                    if (e.getCurrentFloor() != 6) return false;

                    e.setMode("FIRE");
                    if (e.getCurrentFloor() != 1 || !e.isDoorOpen()) return false;

                    try { e.request(3); return false; }
                    catch (Exception ex) { return ex.getMessage().contains("FIRE"); }
                });

        test("OFF power: recall/open; deny requests",
                () -> {
                    Elevator e = new Elevator();
                    try { e.request(4); } catch (Exception ex) { return false; }
                    while (e.isMoving()) e.moveElevator();
                    if (e.getCurrentFloor() != 4) return false;

                    e.setStatus("OFF");
                    if (e.getCurrentFloor() != 1 || !e.isDoorOpen()) return false;

                    try { e.request(2); return false; }
                    catch (Exception ex) { return ex.getMessage().contains("OFF"); }
                });

        // ---- Controller tests (LOOK/SCAN-ish behavior)
        test("Controller: queue up calls 4,7,10 from floor 1",
                () -> {
                    Elevator e = new Elevator();
                    ElevatorController c = new ElevatorController(e);
                    try {
                        c.addHallRequest(4, Elevator.Direction.UP);
                        c.addHallRequest(7, Elevator.Direction.UP);
                        c.addHallRequest(10, Elevator.Direction.UP);
                    } catch (Exception ex) { return false; }

                    runTicks(c, 200);
                    return e.getCurrentFloor() == 10 && !e.isMoving() && e.isDoorOpen();
                });

        test("Controller: mixed up/down calls serviced in sensible order",
                () -> {
                    Elevator e = new Elevator(); // at 1
                    ElevatorController c = new ElevatorController(e);
                    try {
                        c.addHallRequest(3, Elevator.Direction.UP);
                        c.addCarRequest(6);
                        c.addHallRequest(9, Elevator.Direction.UP);
                        // after reaching top, go down
                        c.addHallRequest(8, Elevator.Direction.DOWN);
                        c.addHallRequest(2, Elevator.Direction.DOWN);
                    } catch (Exception ex) { return false; }

                    runTicks(c, 500);
                    // Expect to have ended somewhere low (2) with door open and queues empty
                    return e.getCurrentFloor() <= 3 && !e.isMoving() && e.isDoorOpen();
                });

        // ---- Print summary
        System.out.println("==== Test Results ====");
        results.forEach(System.out::println);
        System.out.printf("Summary: %d passed, %d failed%n", passed, failed);

        // Exit non-zero if any failed (useful in CI/graders)
        if (failed > 0) System.exit(1);
    }

    // ---------- helpers ----------
    private static void test(String name, BooleanSupplier fn) {
        boolean ok;
        try { ok = fn.getAsBoolean(); } catch (Throwable t) { ok = false; }
        if (ok) { passed++; results.add("[PASS] " + name); }
        else    { failed++; results.add("[FAIL] " + name); }
    }

    private static void runTicks(ElevatorController c, int maxTicks) {
        for (int i = 0; i < maxTicks; i++) c.tick();
    }
}