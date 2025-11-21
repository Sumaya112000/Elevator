package CommandCenter.Messages;

public class Message {
    private int topic;
    private int subTopic;
    //private String body;
    private int body;


    /**
     * Message constructor
     *
     * @param topic    Topic
     * @param subTopic Subtopic
     * @param body     Body
     */
    public Message(int topic, int subTopic, int body) {
        this.topic = topic;
        this.subTopic = subTopic;
        this.body = body;
    }

    /**
     * Topic-SubTopic-Body
     *
     * @return String
     */
    @Override
    public String toString() {
        return topic + "-" + subTopic + "-" + body;
    }

    /**
     * Get topic of message
     *
     * @return Topic
     */
    public int getTopic() {
        return topic;
    }

    /**
     * Get subtopic of message
     *
     * @return Subtopic
     */
    public int getSubTopic() {
        return subTopic;
    }

    /**
     * Get body of message
     *
     * @return Body
     */
    public int getBody() {
        return body;
    }

    /**
     * Turn string into a message object
     *
     * @param line String
     * @return Message
     */
    public static Message parseStringToMsg(String line) {
        String[] parts = line.split("-", 3);
        int t = Integer.parseInt(parts[0]);
        int st = Integer.parseInt(parts[1]);
        int body = Integer.parseInt(parts[2]);
        return new Message(t, st, body);
    }
}