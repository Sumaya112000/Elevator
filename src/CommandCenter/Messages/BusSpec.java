package CommandCenter.Messages;
/**
 * Message triplet: (topic:String, subTopic:int, body:int).
 * Topics (Strings):
 *   "SYSTEM_STOP" = System Stop        (all)
 *   "SYSTEM_START" = System Start       (all)
 *   "SYSTEM_RESET" = System Reset       (all)
 *   "CLEAR_FIRE" = Clear Fire         (all)
 *   "MODE" = Mode               (all; body used)
 *   "START_ONE" = Start one          (subTopic = 1..4)
 *   "STOP_ONE" = Stop  one          (subTopic = 1..4)
 *
 * Bodies (4-digit) for Topic "MODE":
 *   1000 = Centralized
 *   1100 = Independent
 *   1110 = Test Fire
 *   0000 = Unused/default elsewhere
 */
public final class BusSpec {
    // topics
    public static final String T_SYSTEM_STOP  = "SYSTEM_STOP";
    public static final String T_SYSTEM_START = "SYSTEM_START";
    public static final String T_SYSTEM_RESET = "SYSTEM_RESET";
    public static final String T_CLEAR_FIRE   = "CLEAR_FIRE";
    public static final String T_MODE         = "MODE";
    public static final String T_START_ONE    = "START_ONE";
    public static final String T_STOP_ONE     = "STOP_ONE";

    // bodies for MODE
    public static final int B_UNUSED   = 0000;
    public static final int B_MODE_CEN = 1000;
    public static final int B_MODE_IND = 1100;
    public static final int B_MODE_TF  = 1110;

    /** printer for logs: TSBBBB (T=topic, S=subtopic, BBBB=body) */
    public static String tsbbbb(String topic, int subTopic, int body4) {
        return String.format("%s%d%04d", topic, subTopic, body4);
    }

    private BusSpec() {}
}