package motion.GUI;

import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import motion.Hardware.*;
import motion.Util.*;

/**
 * Author: Youssef Amin, Danny Phantom, VallyPally
 * This class is used to update the circle that represents the motor
 * in the JavaFX simulation
 */
public class MotorFX implements Observer {
    private Circle motor;

    public MotorFX(Circle motor) {
        this.motor = motor;
    }

    @Override
    public void update(Observable viewee) {
        if (viewee instanceof Motor) {
            Direction dir = (((Motor) viewee).get_direction());
            //change colors to be pretty
            if (dir == Direction.UP) {
                motor.setFill(Color.AQUA);
            } else if (dir == Direction.DOWN) {
                motor.setFill(Color.MEDIUMPURPLE);
            } else {
                motor.setFill(Color.GRAY);
            }
        }
    }
}