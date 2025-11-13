package TestCode;

import bus.SoftwareBus;
import Message.Message;

public class ScratchSender {

    public static void main(String[] args) throws Exception {

        SoftwareBus bus = new SoftwareBus(false);

        Thread.sleep(2000);

        System.out.println("START ALL");
        bus.publish(new Message(2,0,0));
        Thread.sleep(1000);

        System.out.println("MOVE E1 TO 5");
        bus.publish(new Message(9,1,5));
        Thread.sleep(6000);

        System.out.println("MOVE E2 TO 3");
        bus.publish(new Message(9,2,3));
        Thread.sleep(6000);

        System.out.println("STOP E1");
        bus.publish(new Message(7,1,0));
        Thread.sleep(2000);

        System.out.println("START E1");
        bus.publish(new Message(6,1,0));
        Thread.sleep(2000);

        System.out.println("TEST FIRE ALL");
        bus.publish(new Message(5,0,1110));
        Thread.sleep(6000);

        System.out.println("CLEAR FIRE");
        bus.publish(new Message(4,0,0));
        Thread.sleep(3000);

        System.out.println("RESET ALL");
        bus.publish(new Message(3,0,0));
    }
}