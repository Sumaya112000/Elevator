package motion;

import java.util.HashMap;
import motion.Hardware.Motor;
import motion.Hardware.Sensor;
import motion.Simulation.MotionSimulation;
import motion.Util.Direction;

public class MotionAPI {
    //The direction the elevator is moving in
    public Direction direction=null;
    //Sensors mapped to floor numbers
    private HashMap<Integer, Sensor> sensor_HashMap;
    //Max amount of sensors (starting from 0)
    private int MAX_SENSOR_IDX=19;
    //The hardware representative
    private Motor motor;
    //Informs the hardware
    private MotionSimulation motionSimulation;

    /**
     * Creates a new motionController object, creates motor, sensor map, and
     * motion simulation
     *
     */
    public MotionAPI(){
        motor=new Motor();
        sensor_HashMap=new HashMap<>();
        for (int i = 0; i <= MAX_SENSOR_IDX; i++) {
            sensor_HashMap.put(i, new Sensor());
        }
        motionSimulation= new MotionSimulation(1,motor,sensor_HashMap);
        Thread simThread = new Thread(motionSimulation);
        simThread.setDaemon(true);
        simThread.start();

    }

    /**
     * Set directions of elevator
     * @param direction Up, down or null
     */
    public void set_direction(Direction direction){
        motor.set_direction(direction);
    }

    /**
     * Returns the floor that the top of elevator is aligned with
     * @return A floor number or null
     */
    public Integer top_alignment(){
        for(int index: sensor_HashMap.keySet()){
            if (sensor_HashMap.get(index).is_triggered()&& index%2==1){
                return index;
            }
        }
        return null;
    }

    /**
     * Returns the floor that the bottom of elevator is aligned with
     * @return A floor number or null
     */
    public Integer bottom_alignment(){
        for(int index: sensor_HashMap.keySet()){
            if (sensor_HashMap.get(index).is_triggered()&& index%2==0){
                return index;
            }
        }
        return null;
    }

    /**
     *Starts the motor and the elevator’s
     * movement at constant_speed either up or down
     */
    public void start(){
        motor.start();
    }

    /**
     *Starts the motor and the elevator’s
     * movement from constant_speed to zero with a decay of deeleration
     */
    public void stop(){
        motor.stop();
    }



}