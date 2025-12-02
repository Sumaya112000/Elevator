package motion.Util;

/**
 * File for computing motor constants
 */

public class Constants {
    // Distance between sensors on the same level (in meters)
    public static final double HEIGHT = 3;
//    public static final double HEIGHT = 2.875;
    // check if all constants are correct


    // Distance between sensors on distinct levels (in meters)
    public static final double FLOOR_THICKNESS = 1;

    // Maximum speed of elevator (meters per second)
    public static final double MAX_SPEED = 1; // ~ 200 ft/minute

    /* The distance the cabin travels while accelerating (from max speed to stop
     * or vice versa) in meters */
    public static final double ACCELERATION_DIST = 1.5;

    /* Time it takes for the motor to fully accelerate the cabin from max speed
     * to stop or vice versa (seconds) */
    public static final double ACCELERATION_TIME = 2 * ACCELERATION_DIST
            / MAX_SPEED;

    // Acceleration of the motor (same for start and stop) - meters per second^2
    public static final double ACCELERATION = MAX_SPEED / ACCELERATION_TIME;

    // How much time between each simulation update, in milliseconds
    public static final double SIM_SLEEP_TIME = 100;

    // Number of ticks per second
    public static final double TICKS_PER_SECOND = 1 / millis_to_seconds(SIM_SLEEP_TIME);

    // The constant speed in meters per tick
    public static final double MAX_SPEED_TICK = MAX_SPEED / TICKS_PER_SECOND;

    // The acceleration in meters per tick^2
    public static final double ACCELERATION_TICK = ACCELERATION / (TICKS_PER_SECOND * TICKS_PER_SECOND);

    /**
     * Acceleration needs to be large enough to  ensure two things:
     * (1) that cabin will always be traveling at constant speed when stop is
     *     called
     * (2) that there is enough time to call stop after sensing a significant
     *     sensor
     * @return True if the acceleration is sufficiently large
     *         False if the acceleration is too small, and things will not work
     */
    public static boolean is_acceleration_sufficient() {
        /* Is the cabin guaranteed to be traveling at max speed when stop is
         * called */
        boolean condition_one = HEIGHT + FLOOR_THICKNESS > ACCELERATION_DIST * 2;

        // The maximum time the cabin can take to stop (without latency)
        double t_max = 2 * HEIGHT / MAX_SPEED;

        // The minimum acceleration required to stop the cabin for condition (2)
        double a_min = MAX_SPEED / t_max;

        /* Is there guaranteed to be enough time to call stop after sensing a
         * significant sensor */
        boolean condition_two = ACCELERATION > a_min;

        // Are both conditions met?
        return condition_one && condition_two;
    }

    /**
     *  Handy converter
     * @param milliseconds the number of milliseconds
     * @return converted to seconds
     */
    private static double millis_to_seconds(double milliseconds){
        return milliseconds/1000;
    }

    public static void main(String[] args) {
        System.out.println("Is acceleration valid: " + is_acceleration_sufficient());
        System.out.println("Height between two sensors on the same floor: " + HEIGHT + " meters");
        System.out.println("Thickness of the floors: " + FLOOR_THICKNESS + " meters");
        System.out.println("Maximum speed of the elevator: " + MAX_SPEED + " meters per second");
        System.out.println("The distance the cabin travels while accelerating (from max speed to stop or vice versa): " +  ACCELERATION_DIST + " meters");
        System.out.println("Time it takes for the motor to fully accelerate the cabin from max speed to stop or vice versa: " + ACCELERATION_TIME + " seconds");
        System.out.println("Acceleration of the motor (same for start and stop): " + ACCELERATION + " meters per second^2");
        System.out.println("Max speed: " + MAX_SPEED_TICK + " meters per tick");
        System.out.println("Acceleration: " + ACCELERATION_TICK + " meters per tick^2");
    }
}