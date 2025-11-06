package Message;

/** Bus channels (topic numbers):
 *  0 = system-wide broadcast (all elevators listen)
 *  1 = Elevator 1 only
 *  2 = Elevator 2 only
 *  3 = Elevator 3 only
 *  4 = Elevator 4 only
 *  7 = Elevator Start (individual)
 *  8 = Elevator Stop (individual)
 *
 * Subtopic: elevator ID (1-4) for individual commands, 0 for system-wide
 */
public final class Channels {
    public static final int SYSTEM = 0;
    public static final int E1 = 1;
    public static final int E2 = 2;
    public static final int E3 = 3;
    public static final int E4 = 4;
    public static final int ELEVATOR_START = 7;  // Topic for individual elevator start
    public static final int ELEVATOR_STOP = 8;   // Topic for individual elevator stop
    private Channels() {}
}