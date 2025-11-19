package CommandCenter.Messages;

/**
 * Topic codes matching the spreadsheet spec.
 */
public enum Topic {
    SYSTEM_STOP   (1, "SYSTEM_STOP"),  // all elevators
    SYSTEM_START  (2, "SYSTEM_START"),  // all elevators
    SYSTEM_RESET  (3, "SYSTEM_RESET"),  // all elevators
    CLEAR_FIRE    (4, "CLEAR_FIRE"),  // all elevators
    MODE          (5, "MODE"),  // all elevators (body = 1000/1100/1110)
    START_ONE     (6, "START_ONE"),  // subTopic = 1..4
    STOP_ONE      (7, "STOP_ONE"),  // subTopic = 1..4
    FIRE          (120, "FIRE"), // all elevators (body = 0 for on and 1 for off)
    DISPATCH      (102, "DISPATCH"), // subTopic = 1..4 (body = destination floor number)
    POSITION      (202, "POSITION"), // subTopic = 1..4 (body = current floor number)
    DOOR          (204, "DOOR"), // subTopic = 1..4 (body = 0 for open and 1 for closed)
    DIRECTION     (112, "DIRECTION"), // subTopic = 1..4 (body = 0 for up, 1 for down, 2 for none)
    FLOOR         (111, "FLOOR"); // subTopic = 1..4 (body = floor number)

    private final int code;
    private final String name;

    Topic(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public int code() { return code; }
    public String getName() { return name; }

    // Reverse lookup from numeric code to string name
    public static String fromCode(int code) {
        for (Topic t : values()) {
            if (t.code == code) return t.getName();
        }
        return null;
    }

    // Reverse lookup from string name to Topic enum
    public static Topic fromName(String name) {
        for (Topic t : values()) {
            if (t.getName().equals(name)) return t;
        }
        return null;
    }
}