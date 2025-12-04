package elevatorController.Util;

public class ConstantsElevatorControl {

    // SoftwareBusCodes Mappings (Inferred/Assumed from Test Code)
    public static final int hallCall = 101;
    public static final int startElevator = 102;
    public static final int stopElevator = 103;
    public static final int setMode = 104; // Treated as a system-level topic
    public static final int clearFire = 105; // Treated as a system-level topic
    public static final int allElevators = 0; // For broadcast messages

    // Message Topics (Specific to Elevator Controller components)
    public static final int BUTTON = 300;
    public static final int CABIN = 301;
    public static final int DOORASSEMBLY = 302;
    public static final int MODE = 303;
    public static final int NOTIFIER = 304;

    // Message Body (Door Assembly)
    public static final int CLOSE = 1;
    public static final int OPEN = 2;
    public static final int OBSTRUCTED = 3;
    public static final int FULLYCLOSED = 4;
    public static final int FULLYOPEN = 5;
    public static final int OVERCAPACITY = 6;


    // Message Body (MODES)
    public static final int NORMAL = 100;
    public static final int FIRE = 200;
    public static final int CONTROLL = 300; // Renamed to CONTROL to match usage

    /* Message Body (Floor and Direction Codes) */
    public static final int UP = 1;
    public static final int DOWN = 2;

    // Message Body (Floors)
    public static final int FLOOR_ONE = 1;
    public static final int FLOOR_TWO = 2;
    public static final int FLOOR_THREE = 3;
    public static final int FLOOR_FOUR= 4;
    public static final int FLOOR_FIVE = 5;
    public static final int FLOOR_SIX = 6;
    public static final int FLOOR_SEVEN = 7;
    public static final int FLOOR_EIGHT = 8;
    public static final int FLOOR_NINE = 9;
    public static final int FLOOR_TEN = 10;

    // Motor Messages (Used by Cabin to tell MUX to move)
    public static final int MOTOR_MOVE_UP = 601;
    public static final int MOTOR_MOVE_DOWN = 602;
    public static final int MOTOR_STOP = 603;

    public static final int FIREKEY = 505; // Fire key inserted/removed
    //Message Body (NOTIFIER)
    // Notifier Outgoing (Notifier -> Software Bus) - for capacity
    public static final int LIGHT_OFF = 5;
    public static final int CAPON_ONE = 111;
    public static final int CAPON_TWO = 211;
    public static final int CAPON_THREE = 311;
    public static final int CAPON_FOUR= 411;
    public static final int CAPON_FIVE = 511;
    public static final int CAPON_SIX = 611;
    public static final int CAPON_SEVEN = 711;
    public static final int CAPON_EIGHT = 811;
    public static final int CAPON_NINE = 911;
    public static final int CAPON_TEN = 1011;

    public static final int CAPOFF_ONE = 122;
    public static final int CAPOFF_TWO = 222;
    public static final int CAPOFF_THREE = 322;
    public static final int CAPOFF_FOUR= 422;
    public static final int CAPOFF_FIVE = 522;
    public static final int CAPOFF_SIX = 622;
    public static final int CAPOFF_SEVEN = 722;
    public static final int CAPOFF_EIGHT = 822;
    public static final int CAPOFF_NINE = 922;
    public static final int CAPOFF_TEN = 1022;

    /* Message Body (BUTTONS) Outgoing Req one means turn off request buttons */
    public static final int REQ_ONE = CAPON_ONE;
    public static final int REQ_TWO = CAPON_TWO;
    public static final int REQ_THREE = CAPON_THREE;
    public static final int REQ_FOUR= CAPON_FOUR;
    public static final int REQ_FIVE = CAPON_FIVE;
    public static final int REQ_SIX = CAPON_SIX;
    public static final int REQ_SEVEN = CAPON_SEVEN;
    public static final int REQ_EIGHT = CAPON_EIGHT;
    public static final int REQ_NINE = CAPON_NINE;
    public static final int REQ_TEN = CAPON_TEN;

    public static final int CALLUPOFF_ONE = CAPOFF_ONE;
    public static final int CALLUPOFF_TWO = CAPOFF_TWO;
    public static final int CALLUPOFF_THREE = CAPOFF_THREE;
    public static final int CALLUPOFF_FOUR= CAPOFF_FOUR;
    public static final int CALLUPOFF_FIVE = CAPOFF_FIVE;
    public static final int CALLUPOFF_SIX = CAPOFF_SIX;
    public static final int CALLUPOFF_SEVEN = CAPOFF_SEVEN;
    public static final int CALLUPOFF_EIGHT = CAPOFF_EIGHT;
    public static final int CALLUPOFF_NINE = CAPOFF_NINE;
    public static final int CALLUPOFF_TEN = CAPOFF_TEN;

    public static final int CALLDOWNOFF_ONE = 1222;
    public static final int CALLDOWNOFF_TWO = 2222;
    public static final int CALLDOWNOFF_THREE = 3222;
    public static final int CALLDOWNOFF_FOUR= 4222;
    public static final int CALLDOWNOFF_FIVE = 5222;
    public static final int CALLDOWNOFF_SIX = 6222;
    public static final int CALLDOWNOFF_SEVEN = 7222;
    public static final int CALLDOWNOFF_EIGHT = 8222;
    public static final int CALLDOWNOFF_NINE = 9222;
    public static final int CALLDOWNOFF_TEN = 10222;
    // TODO: CABIN Messages
}