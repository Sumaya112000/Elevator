package TestCommandCenter;


import Bus.SoftwareBus;
import Message.Message;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

public class TestCommandCenterDisplay {
    private SoftwareBus softwareBus;

    private final BorderPane display;

    private Label messageStatus;
    private TextField messageToBeSent;
    private Button submitMessage;

    int currentTopic;
    int currentSubtopic;

    /**
     * Constructor for Command Center Display, creates the user interface
     *
     * @param softwareBus Software bus for handling messages
     * @param topic       Topic Command center is subscribed to
     * @param subtopic    Subtopic Command center is subscribed to
     */
    public TestCommandCenterDisplay(SoftwareBus softwareBus, int topic, int subtopic) {
        this.softwareBus = softwareBus;

        currentTopic = topic;
        currentSubtopic = subtopic;

        display = new BorderPane();
        messageStatus = new Label("");
        StackPane messagePane = new StackPane();
        messagePane.setMinSize(100, 100);
        messagePane.getChildren().add(messageStatus);
        display.setTop(messagePane);


        messageToBeSent = new TextField();
        messageToBeSent.setMaxSize(250, 20);
        StackPane textPane = new StackPane();
        textPane.setMinSize(350, 150);
        textPane.getChildren().add(messageToBeSent);
        textPane.setBackground(new Background(new BackgroundFill(Color.LIGHTBLUE,
                CornerRadii.EMPTY, Insets.EMPTY
        )));
        display.setCenter(textPane);

        submitMessage = new Button("Submit Message");
        submitMessage.setOnAction(event -> handleSubmit());
        StackPane buttonPane = new StackPane();
        buttonPane.getChildren().add(submitMessage);
        buttonPane.setAlignment(Pos.CENTER);
        display.setRight(buttonPane);

        checkForIncomingMessage();
    }

    /**
     * Check for messages in the software bus
     */
    private void checkForIncomingMessage() {
        Thread thread = new Thread(() -> {
            while (true) {
                Message message = softwareBus.get(currentTopic, currentSubtopic);
                if (message != null) {
                    Platform.runLater(() -> {
                        handleNewMessage(message);
                    });

                }
            }
        });
        thread.start();
    }

    /**
     * Handles messages that need to be sent to the software bus
     */
    private void handleSubmit() {
        String messageString = messageToBeSent.getText();

        if (messageString.isEmpty()) {
            return;
        }

        if (!messageString.matches("\\d+-\\d+-\\d+")) {
            invalidMessage();
            System.out.println("Invalid format. Expected: <topic>-<subtopic>-<body>");
            return;
        }

        Message messageToBeSent = Message.parseStringToMsg(messageString);
        handleSendMessage(messageToBeSent);

        softwareBus.publish(messageToBeSent);
    }

    /**
     * Update label to show that a message to be sent was invalid
     */
    private void invalidMessage() {
        messageStatus.setText("Invalid format!\nExpected: <topic>-<subtopic>-<body>");
    }

    /**
     * Update label to indicate that a message was received
     *
     * @param message Message received
     */
    public void handleNewMessage(Message message) {
        messageStatus.setText("Message Received!\n" + message.toString());
    }

    /**
     * Update label to indicate that a message was sent to the software bus
     *
     * @param message Message sent to software bus
     */
    public void handleSendMessage(Message message) {
        messageStatus.setText("Message Sent!\n" + message.toString());
    }

    /**
     * Border Pane
     *
     * @return Pane
     */
    public BorderPane getPane() {
        return display;
    }


}