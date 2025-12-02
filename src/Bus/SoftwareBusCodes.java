package Bus;

public class SoftwareBusCodes {
    //List of topics
    public static int systemStop = 1;
    public static int systemStart = 2;
    public static int clearFire = 4;
    public static int elevatorMode = 5;
    public static int startElevator = 6;
    public static int stopElevator = 7;

    //Device relevant control

    public static int doorControl = 100;
    public static int carDispatch = 102;
    public static int resetFloorSelection = 109;
    public static int resetCall = 110;
    public static int displayFloor = 111;
    public static int displayDirection = 112;
    public static final int callsEnable = 113;
    public static final int selectionsEnable = 114;
    public static final int selectionsType = 115;
    public static final int playSound = 116;

    public static final int carStop = 117;

    public static int fireAlarm = 120;

    public static int hallCall = 200;

    public static int cabinSelect = 201;
    public static int cabinPosition = 202;
    public static int doorSensor = 203;
    public static int doorStatus = 204;
    public static int cabinLoad = 205;
    public static int fireKey = 206;
    public static final int currDirection = 207;
    public static final int currMovement = 208;
    public static final int fireAlarmActive = 209;
    public static final int topSensor = 210;
    public static final int bottomSensor = 211;


    //Control devices
    public static final int setMode = 300;
    public static final int setDestination = 301;
    public static final int elevatorState = 303; // if on or off


    //List of Subtopics
    public static int allElevators = 0;
    public static int elevatorOne = 1;
    public static int elevatorTwo = 2;
    public static int elevatorThree = 3;
    public static int elevatorFour = 4;

    public static int floorOne = 1;
    public static int floorTwo = 2;
    public static int floorThree = 3;
    public static int floorFour = 4;
    public static int floorFive = 5;
    public static int floorSix = 6;
    public static int floorSeven = 7;
    public static int floorEight = 8;
    public static int floorNine = 9;
    public static int floorTen = 10;

    //List of bodies
    public static int emptyBody = 0;
    public static int centralized = 1000;
    public static int independent = 1100;
    public static int fire = 1110;

    public static int doorOpen = 0;
    public static int doorClose = 1;

    public static int deviceFire = 0;
    public static int deviceIndependent = 1;
    public static int deviceCentralized = 2;

    public static int deviceFloorOne = 1;
    public static int deviceFloorTwo = 2;
    public static int deviceFloorThree = 3;
    public static int deviceFloorFour = 4;
    public static int deviceFloorFive = 5;
    public static int deviceFloorSix = 6;
    public static int deviceFloorSeven = 7;
    public static int deviceFloorEight = 8;
    public static int deviceFloorNine = 9;
    public static int deviceFloorTen = 10;

    public static int up = 0;
    public static int down = 1;
    public static int none = 2;

    public static int obstructed = 0;
    public static int clear = 1;

    public static int normal = 0;
    public static int overloaded = 1;


}