package CommandCenter.Messages;

/**
 * Topic codes matching the spreadsheet spec.
 *
 * Message class now carries String topics.
 */
public enum Topic {
    SYSTEM_STOP   ("SYSTEM_STOP"),  // all elevators
    SYSTEM_START  ("SYSTEM_START"),  // all elevators
    SYSTEM_RESET  ("SYSTEM_RESET"),  // all elevators
    CLEAR_FIRE    ("CLEAR_FIRE"),  // all elevators
    MODE          ("MODE"),  // all elevators (body = 1000/1100/1110)
    START_ONE     ("START_ONE"),  // subTopic = 1..4
    STOP_ONE      ("STOP_ONE"),  // subTopic = 1..4
    FIRE          ("FIRE"), // all elevators (body = 0 for on and 1 for off)
    DISPATCH      ("DISPATCH"), // subTopic = 1..4 (body = destination floor number)
    POSITION      ("POSITION"), // subTopic = 1..4 (body = current floor number)
    DOOR          ("DOOR"), // subTopic = 1..4 (body = 0 for open and 1 for closed)
    DIRECTION     ("DIRECTION"), // subTopic = 1..4 (body = 0 for up, 1 for down, 2 for none)
    FLOOR         ("FLOOR"); // subTopic = 1..4 (body = floor number)


    private final String code;
    Topic(String code) { this.code = code; }
    public String code() { return code; }

    // Reverse lookup
    public static String fromCode(String code) {
        for (Topic t : values()) if (t.code.equals(code)) return t.toString();
        return null;
    }
}