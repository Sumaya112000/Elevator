package Message;

/** Integer command protocol for Message.body
 *
 * Encoding:
 *   body = (opcode << 16) | (arg & 0xFFFF)
 * Decoding:
 *   opcode = (body >>> 16) & 0xFFFF
 *   arg    =  body        & 0xFFFF    (signedness not important for our small args)
 *
 * Opcodes (small ints):
 *   1 START        (no arg)
 *   2 STOP         (no arg)
 *   3 RESET        (no arg)
 *   4 FIRE_ON      (no arg)
 *   5 FIRE_CLEAR   (no arg)
 *   6 GOTO         (arg = target floor 1..10)
 *   7 OPEN         (no arg)
 *   8 CLOSE        (no arg)
 *   9 ENABLE       (no arg)
 *  10 DISABLE      (no arg)
 *  11 STATUS       (arg packs a few flags; optional – for logging/diagnostics)
 *  12 ELEV_START   (no arg) - Individual elevator start
 *  13 ELEV_STOP    (no arg) - Individual elevator stop
 *
 * STATUS arg packing (optional):
 *   bits  0..7   = floor (0..255)
 *   bit       8  = doorOpen (1=yes)
 *   bit       9  = isMoving (1=yes)
 *   bits 10..11  = direction (0=IDLE,1=UP,2=DOWN)
 */
public final class Commands {
    // opcodes
    public static final int START      = 1;
    public static final int STOP       = 2;
    public static final int RESET      = 3;
    public static final int FIRE_ON    = 4;
    public static final int FIRE_CLEAR = 5;
    public static final int GOTO       = 6;
    public static final int OPEN       = 7;
    public static final int CLOSE      = 8;
    public static final int ENABLE     = 9;
    public static final int DISABLE    = 10;
    public static final int STATUS     = 11;
    public static final int ELEV_START = 12;  // Individual elevator start
    public static final int ELEV_STOP  = 13;  // Individual elevator stop

    // encode/decode helpers
    public static int encode(int opcode, int arg) {
        return (opcode << 16) | (arg & 0xFFFF);
    }
    public static int opcode(int body) {
        return (body >>> 16) & 0xFFFF;
    }
    public static int arg(int body) {
        return body & 0xFFFF;
    }

    // helpers for STATUS arg packing
    public static int statusArg(int floor, boolean doorOpen, boolean moving, int dirCode) {
        int a = (floor & 0xFF);
        if (doorOpen) a |= (1 << 8);
        if (moving)   a |= (1 << 9);
        a |= ((dirCode & 0x3) << 10);
        return a;
    }
    public static int statusFloor(int a) { return a & 0xFF; }
    public static boolean statusDoorOpen(int a) { return ((a >>> 8) & 1) == 1; }
    public static boolean statusMoving(int a)   { return ((a >>> 9) & 1) == 1; }
    public static int statusDirCode(int a)      { return (a >>> 10) & 0x3; }

    private Commands() {}
}