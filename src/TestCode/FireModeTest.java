package TestCode;

import Bus.*;
import Message.*;

/**
 * Test harness to trigger FIRE mode and CLEAR FIRE from the bus
 * for testing fire recall behavior.
 */
public class FireModeTest {
    public static void main(String[] args) {
        SoftwareBus bus = new SoftwareBus(false);
        
        System.out.println("[FireModeTest] Starting fire mode test...");
        
        try {
            // Wait a moment for system to stabilize
            Thread.sleep(2000);
            
            // Trigger FIRE MODE (affects all elevators)
            System.out.println("[FireModeTest] Publishing setMode = FIRE");
            bus.publish(new Message(SoftwareBusCodes.setMode, 0, SoftwareBusCodes.fire));
            
            // Wait 15 seconds to observe recall
            System.out.println("[FireModeTest] Waiting 15 seconds for elevators to recall to floor 1...");
            Thread.sleep(15000);
            
            // Trigger CLEAR FIRE
            System.out.println("[FireModeTest] Publishing clearFire");
            bus.publish(new Message(SoftwareBusCodes.clearFire, 0, 0));
            
            System.out.println("[FireModeTest] Test complete.");
            Thread.sleep(5000);
            
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
