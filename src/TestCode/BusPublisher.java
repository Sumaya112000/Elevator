package TestCode;

import Bus.*;
import Message.*;

public class BusPublisher {
    public static void main(String[] args) throws Exception {
        SoftwareBus bus = new SoftwareBus(false);
        System.out.println("BusPublisher started");

        // Small helper to publish and sleep
        Runnable sleep = () -> {
            try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
        };

        // 1) Ensure elevator 1 doors are closed initially
        System.out.println("Publishing stopElevator -> elevator 1");
        bus.publish(new Message(SoftwareBusCodes.stopElevator, 1, 0));
        Thread.sleep(1000);

        // 2) Try to send a hall call to elevator 1 (should be ignored while stopped)
        System.out.println("Publishing hallCall -> elevator 1 floor 5 (should be ignored if stopped)");
        bus.publish(new Message(SoftwareBusCodes.hallCall, 1, 5));
        Thread.sleep(3000);

        // 3) Start elevator 1 and send hall call again
        System.out.println("Publishing startElevator -> elevator 1");
        bus.publish(new Message(SoftwareBusCodes.startElevator, 1, 0));
        Thread.sleep(1000);
        System.out.println("Publishing hallCall -> elevator 1 floor 5");
        bus.publish(new Message(SoftwareBusCodes.hallCall, 1, 5));
        Thread.sleep(8000);

        // 4) Simulate TEST FIRE via setMode broadcast
        System.out.println("Publishing setMode = FIRE (broadcast)");
        bus.publish(new Message(SoftwareBusCodes.setMode, SoftwareBusCodes.allElevators, SoftwareBusCodes.fire));
        Thread.sleep(8000);

        // 5) Clear fire
        System.out.println("Publishing clearFire (broadcast)");
        bus.publish(new Message(SoftwareBusCodes.clearFire, SoftwareBusCodes.allElevators, 0));
        Thread.sleep(4000);

        System.out.println("BusPublisher finished");
        System.exit(0);
    }
}
