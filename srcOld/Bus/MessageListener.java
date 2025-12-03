package Bus;

import Message.*;

public interface MessageListener {
    void onMessage(Message message);
}

