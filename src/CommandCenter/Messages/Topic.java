package CommandCenter.Messages;

/**
 * Topic codes matching the spreadsheet spec.
 *
 * Message class still carries raw ints (topic, subTopic, body).
 * This enum is just a typed helper so you can write Topic.SYSTEM_START.code()
 * instead of sprinkling magic numbers (1..7) everywhere.
 */
public enum Topic {
    SYSTEM_STOP   (1),  // all elevators
    SYSTEM_START  (2),  // all elevators
    SYSTEM_RESET  (3),  // all elevators
    CLEAR_FIRE    (4),  // all elevators
    MODE          (5),  // all elevators (body = 1000/1100/1110)
    START_ONE     (6),  // subTopic = 1..4
    STOP_ONE      (7),  // subTopic = 1..4
    FIRE          (120), // all elevators (body = 0 for on and 1 for off)
    DISPATCH      (102), // subTopic = 1..4 (body = destination floor number)
    POSITION      (202), // subTopic = 1..4 (body = current floor number)
    DOOR          (204), // subTopic = 1..4 (body = 0 for open and 1 for closed)
    DIRECTION     (112), // subTopic = 1..4 (body = 0 for up, 1 for down, 2 for none)
    FLOOR         (111); // subTopic = 1..4 (body = floor number)


    private final int code;
    Topic(int code) { this.code = code; }
    public int code() { return code; }

    // Reverse lookup
    public static Topic fromCode(int code) {
        for (Topic t : values()) if (t.code == code) return t;
        return null;
    }
}