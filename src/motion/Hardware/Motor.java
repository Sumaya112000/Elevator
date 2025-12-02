package motion.Hardware;
import java.util.ArrayList;
import java.util.List;

import motion.Util.*;

/**
 * Authors: Natalie Runyan, Valerie Barker, Youssef Amin
 */
public class Motor implements Observable{
    //If the motor is on or not
    private boolean on;
    //The direction the motor is running
    private Direction direction;
    //List of objects observing this object
    List<Observer> observers = new ArrayList<>();

    public Motor(){
        this.on = false;
        this.direction = Direction.NULL;
    }
    /**
     * Start API for the motor, starts the motor and notifies observers
     */
    public synchronized void start(){
        on=true;
        for (Observer o : observers) o.update(this);
    }

    /**
     * Stop API for the motor, stops the motor and notifies observers
     */
    public synchronized void stop(){
        on=false;
        for (Observer o : observers) o.update(this);
    }

    /**
     * Set direction API for the motor, sets direction and notifies observers
     * @param direction
     */
    public synchronized void set_direction(Direction direction){
        this.direction=direction;
        for (Observer o : observers) o.update(this);
    }

    /**
     * Subscribe API for observers to observe this motor
     * @param subscriber an Observer that wishes to observe this object
     */
    @Override
    public void subscribe(Observer subscriber) {
        observers.add(subscriber);
    }

    /**
     * Gets the direction of the motor
     * @return
     */
    public Direction get_direction(){
        return this.direction;
    }

    /**
     * Lets us check if the motor is on or off.
     * Used to snap into place in motion sim
     * @return true if the motor is on
     */
    public boolean is_off(){
        return !on;
    }
}