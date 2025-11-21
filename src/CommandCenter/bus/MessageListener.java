package CommandCenter.bus;

import CommandCenter.Messages.Message;

public interface MessageListener {
    void onMessage(Message message);
}