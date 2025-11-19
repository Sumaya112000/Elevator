package CommandCenter.Messages;

public class Message {
    private String topic;
    private int subTopic;
    private int body;

    public Message(String topic, int subTopic, int body) {
        this.topic = topic;
        this.subTopic = subTopic;
        this.body = body;
    }

    @Override
    public String toString() {
        return topic + "-" + subTopic + "-" + body;
    }

    public String getTopic() { return topic; }
    public int getSubTopic() { return subTopic; }
    public int getBody() { return body; }

    public static Message parseStringToMsg(String line) {
        String[] parts = line.split("-", 3);
        String t = parts[0];
        int st = Integer.parseInt(parts[1]);
        int body = Integer.parseInt(parts[2]);
        return new Message(t, st, body);
    }
}