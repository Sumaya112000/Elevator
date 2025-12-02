package motion.Simulation;

import java.util.HashMap;

import motion.Hardware.Elevator;
import motion.Hardware.Motor;
import motion.Hardware.Sensor;
import motion.Util.Constants;
import motion.Util.Direction;
import motion.Util.Observable;
import motion.Util.Observer;

/**
 * This class simulates motion, for demonstration purposes
 * will be deleted later and replaced with working software
 */

public class MotionSimulation implements Runnable, Observer {
    // Convert floor indicator to Sensor
    private HashMap<Integer, Sensor> sensor_HashMap =new HashMap<>();
    // Convert Sensors to y positions
    private final HashMap<Integer, Double>  sensor_pos_Map = new HashMap<>();

    // The motor object (to be replaced by Hardware)
    private final Motor motor;

    // The elevator object (for simulation purposes) also hardware
    private final Elevator elevator;
    // Top Level
    private final int MAX_SENSOR_IDX = 19;
    // The elevator's current speed
    private double current_speed = 0.0;
    // 1 if accelerating, -1 if decelerating,
    private int accelerating_indicator = 0;
    // Which direction the elevator is going
    private Direction direction = Direction.NULL;

    // The floor number associated (-1 when unset)
    private volatile int top_idx = 1;
    private volatile int bottom_idx = 0;

    //How fast the sim goes, relative to the ticks
    private double speedFactor=1;

    // Sensor tolerance
    private final double TOLERANCE = 0.5;

    /**
     * Makes a motion simulation
     */
    public MotionSimulation(double speedFactor, Motor motor,HashMap<Integer, Sensor> sensor_HashMap){
        this.motor =motor;
        elevator = new Elevator();
        this.speedFactor=speedFactor;
        this.sensor_HashMap=sensor_HashMap;
        motor.subscribe(this);

        // Initializing the Hash Maps
        double y_pos = 0;
        for (int i = 0; i <= MAX_SENSOR_IDX; i++) {
            //sensor_HashMap.put(i, new Sensor());

            if (i % 2 == 0){
                // Lower level sensor
                y_pos += Constants.FLOOR_THICKNESS;
            } else {
                // Upper level sensor
                y_pos += Constants.HEIGHT;
            }
            sensor_pos_Map.put(i, y_pos);
        }

        //this.start();


    }

    /**
     *  motion simulation is runnable
     */
    @Override
    public void run() {
        boolean running = true;
        update_sensors();

        while(running){
            // Update
            tick();
            try {
                // Sleep
                Thread.sleep((long) ((int)Constants.SIM_SLEEP_TIME*speedFactor));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                running = false;
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }



    /**
     * Update the elevator, then the sensors
     */
    private void tick() {
        update_elevator();
        update_sensors();
    }

    /**
     * Use the constants, to update the position, and velocity, according to
     * direction
     */
    private void update_elevator() {
        if (accelerating_indicator != 0) {

            current_speed += Constants.ACCELERATION_TICK * accelerating_indicator;
        } else {
            // No acceleration go towards zero, so this is sorta a janky reuse of accleration indicator
            if (current_speed != 0.0) {
                // Get the sign of the current speed (1) positive,  (-1) negative
                double steppre = Constants.ACCELERATION_TICK * Math.signum(current_speed);
                if (Math.abs(steppre) >= Math.abs(current_speed)) {
                    current_speed = 0.0;
                } else {
                    current_speed -= steppre;
                }
            }
        }

        //goes to max speed negative or positive account for going over so we
        // dont have to worry about rounding
        if (Math.abs(current_speed) > Constants.MAX_SPEED_TICK) {
            current_speed = Math.copySign(Constants.MAX_SPEED_TICK, current_speed);
        }

        // Change in y position, based on speed and
        if (current_speed != 0.0) {
            double delta_Y = current_speed;
            // Tell observers
            //elevator.set_y_position(elevator.getY_position() + delta_Y);
        } else {
            //If we've come to a full stop and previously were decelerating, reset indicator
            if (accelerating_indicator < 0) {
                accelerating_indicator = 0;
            }
        }
//        System.out.println("current speed: "+ current_speed);
        elevator.set_y_position(elevator.getY_position() + current_speed);
//        System.out.println(elevator.getY_position() + " + "+ current_speed);
    }

    /**
     * Updates the floor sensors
     */

    private synchronized void update_sensors() {
        int newBottom = -1;
        int newTop = -1;
        double yBottom = elevator.getY_position();
        double yTop = elevator.upper_bound();

        for (Integer idx : sensor_pos_Map.keySet()) {
            double sensorY = sensor_pos_Map.get(idx);

            if (sensorY+ TOLERANCE >= yBottom && sensorY- TOLERANCE <= yTop) {
                sensor_HashMap.get(idx).set_triggered(true);

                if(newBottom==-1){
                    newBottom=idx;

                }else{
                    newTop=idx;
                }

            } else {
                sensor_HashMap.get(idx).set_triggered(false);
            }
        }
        top_idx=newTop;
        bottom_idx=newBottom;

    }

    /**
     * @param floor_indicator 0 is bottom of first floor, 1 is top of first
     *                        floor, 2 is bottom of second floor, etc.
     * @return the Sensor object associated
     */
    public Sensor get_sensor(int floor_indicator) {
        return sensor_HashMap.get(floor_indicator);
    }

    /**
     * @return Gets the elevators position based on current speed
     */
    private double elevator_delta_y() {
        return  current_speed * millis_to_seconds(Constants.SIM_SLEEP_TIME);
    }
    /**
     *  Handy converter
     * @param milliseconds the number of milliseconds
     * @return converted to seconds
     */
    private double millis_to_seconds(double milliseconds){
        return milliseconds/1000;
    }

    /**
     * Getters for hashmap
     * @return The map of sensors the floor nums
     */
    public HashMap<Integer, Sensor> getSensors() {
        return sensor_HashMap;
    }

    /**
     * Getters for hashmap
     * @return The map of sensors to actual physical position
     */

    public HashMap<Integer, Double> get_sensor_pos_HashMap() {
        return sensor_pos_Map;
    }

    /**
     *
     * @return The elevator
     */

    public Elevator getElevator() {
        return elevator;
    }

    public Motor getMotor() {
        return motor;
    }


    public void setDirection(Direction direction){
        this.direction=direction;
    }

    /**
     * "so um basically we need to listen to the motor instead of telling it what to do"
     * -Valerie Barker
     * @param viewee the updated observable object (THIS SHOULD BE A MOTOR!!!1)
     */
    @Override
    public void update(Observable viewee) {
        Motor beloved = ((Motor) viewee);
        if (viewee instanceof Motor) {
            System.out.println("Motion sim being updated");
            if (beloved.is_off()) {
                accelerating_indicator = 0;
                direction = null;
            } else {
                if (beloved.get_direction() == Direction.UP) {
                    accelerating_indicator = 1;
                } else {
                    accelerating_indicator = -1;
                }
            }
        } else {
            System.err.println("HOLY CRAP THATS NOT A MOTOR!!1!");
        }
    }




















    private void tick(double cycle_time) {
        if (accelerating_indicator != 0) {

            current_speed += Constants.ACCELERATION * cycle_time * accelerating_indicator;
        } else {
            //no acceleration go towards zero, so this is sorta a janky reuse of accleration indicator

            if (current_speed != 0.0) {
                double steppre = Constants.ACCELERATION * cycle_time * Math.signum(current_speed); //get the sig nif number of the current speed
                if (Math.abs(steppre) >= Math.abs(current_speed)) {
                    current_speed = 0.0;
                } else {
                    current_speed -= steppre;
                }
            }
        }

        //goes to max speed negative or positive account for going over so we
        // don't have to worry about rounding
        if (Math.abs(current_speed) > Constants.MAX_SPEED) {
            current_speed = Math.copySign(Constants.MAX_SPEED, current_speed);
        }

        //positive->up, negative-> down
        if (current_speed != 0.0) {
            double delta_Y = current_speed * cycle_time;
            //tell obsetcvers
            elevator.set_y_position(elevator.getY_position() + delta_Y);
        } else {
            //If we've come to a full stop and previously were decelerating, reset indicator
            if (accelerating_indicator < 0) {

                accelerating_indicator = 0;
            }
        }
    }

    public synchronized int[] getAlignment() {
        return new int[]{ bottom_idx, top_idx };
    }




}