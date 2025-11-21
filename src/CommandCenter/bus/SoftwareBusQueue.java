package CommandCenter.bus;

import CommandCenter.Messages.Message;

import java.util.Iterator;
import java.util.LinkedList;

public class SoftwareBusQueue {
    private final LinkedList<Message> queue;

    public SoftwareBusQueue() {
        queue = new LinkedList<>();
    }

    /**
     * Adds a message to the queue.
     * Synchronized to ensure thread-safe writes when the network listener receives messages.
     *
     * @param msg The message to be added.
     */
    public synchronized void add(Message msg) {
        queue.add(msg);
    }

    /**
     * Checks whether the queue is empty.
     *
     * @return true if queue has no messages, false otherwise.
     */
    public synchronized boolean isEmpty() {
        return queue.isEmpty();
    }

    /**
     * Retrieves and removes the first message in the queue matching the given topic/subtopic.
     * If subtopic = 0, matches all subtopics.
     * Returns null if no matching message is found.
     */
    public synchronized Message get(int topic, int subtopic) {
        Iterator<Message> it = queue.iterator();
        while (it.hasNext()) {
            Message m = it.next();
            if (m.getTopic() == topic && (subtopic == 0 || m.getSubTopic() == subtopic)) {
                it.remove();
                return m;
            }
        }
        return null;
    }
}