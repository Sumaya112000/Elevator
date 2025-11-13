package Message;

/**
 * Spreadsheet-aligned BUS constants.
 * We keep your Message triplet: (topic:int, subTopic:int, body:int).
 *
 * Topics (ints):
 *   1 = System Stop        (all)
 *   2 = System Start       (all)
 *   3 = System Reset       (all)
 *   4 = Clear Fire         (all)
 *   5 = Mode               (all; body used)
 *   6 = Start one          (subTopic = 1..4)
 *   7 = Stop  one          (subTopic = 1..4)
 *
 * Bodies (4-digit) for Topic 5 (Mode):
 *   1000 = Centralized
 *   1100 = Independent
 *   1110 = Test Fire
 *   0000 = Unused/default elsewhere
 */
public final class BusSpec {
    // topics
    public static final int T_SYSTEM_STOP  = 1;
    public static final int T_SYSTEM_START = 2;
    public static final int T_SYSTEM_RESET = 3;
    public static final int T_CLEAR_FIRE   = 4;
    public static final int T_MODE         = 5;
    public static final int T_START_ONE    = 6;
    public static final int T_STOP_ONE     = 7;

    // bodies for MODE
    public static final int B_UNUSED   = 0000;
    public static final int B_MODE_CEN = 1000;
    public static final int B_MODE_IND = 1100;
    public static final int B_MODE_TF  = 1110;

    /** Pretty printer for logs: TSBBBB (T=topic, S=subtopic, BBBB=body) */
    public static String tsbbbb(int topic, int subTopic, int body4) {
        return String.format("%d%d%04d", topic, subTopic, body4);
    }

    private BusSpec() {}
}