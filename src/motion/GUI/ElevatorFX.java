package motion.GUI;

import javafx.scene.shape.Rectangle;
import motion.Hardware.*;
import motion.Util.*;


public class ElevatorFX implements Observer{

    private Rectangle elly;
    private double joelsConstant =460;


    public ElevatorFX(Rectangle elly){
        this.elly = elly;

    }
    @Override
    public void update(Observable viewee) {


        if(viewee instanceof Elevator){
            elly.setLayoutY(ElevatorGUI.joel_to_java(ElevatorGUI.SENSOR_HEIGHT + ElevatorGUI.SHAFT_HEIGHT -1/2 - ElevatorGUI.CAR_HEIGHT - ((Elevator) viewee).getY_position()));
        }else {
            System.out.println("Observable error from elevator");
        }
    }
}