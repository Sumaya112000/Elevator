package TestProcessor1;

import Bus.SoftwareBus;
import Message.Message;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

public class TestProcessorDisplay1 {
    private SoftwareBus softwareBus;
    private BorderPane pane;

    private GridPane buttonGrid;
    private Label messageStatus;

    int currentTopic;
    int currentSubtopic;

    /**
     * Constructor for Processor 1, creates the user interface
     *
     * @param softwareBus Software bus for handling messages
     * @param topic       Topic processor 1 is subscribed to
     * @param subtopic    Subtopic processor 1 is subscribed to
     */
    public TestProcessorDisplay1(SoftwareBus softwareBus, int topic, int subtopic) {
        this.softwareBus = softwareBus;

        this.currentTopic = topic;
        this.currentSubtopic = subtopic;

        pane = new BorderPane();
        buttonGrid = new GridPane();
        createButtons();
        buttonGrid.setHgap(5);
        buttonGrid.setVgap(5);
        buttonGrid.setAlignment(Pos.CENTER);
        StackPane buttonPane = new StackPane();
        buttonPane.setMinSize(200, 200);
        buttonPane.getChildren().addAll(buttonGrid);

        buttonPane.setBackground(new Background(new BackgroundFill(Color.GREY, CornerRadii.EMPTY, Insets.EMPTY)));
        pane.setCenter(buttonPane);

        messageStatus = new Label("");
        StackPane messagePane = new StackPane();
        messagePane.setMinSize(100, 100);
        messagePane.getChildren().add(messageStatus);
        pane.setTop(messagePane);

        checkForIncomingMessage();

    }

    /**
     * Create elevator floor buttons
     */
    private void createButtons() {
        int count = 1;
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 2; j++) {
                Button button = new Button();
                if (count != 10) {
                    button.setText(" Floor " + count + " ");
                } else {
                    button.setText("Floor " + count);
                }
                button.setAlignment(Pos.CENTER);
                int buttonNum = count;
                button.setOnAction(event -> handleButtonClick(buttonNum));
                buttonGrid.add(button, j, i);
                count++;
            }
        }
    }

    /**
     * Method that handles when a button is clicked on
     *
     * @param count Button number
     */
    private void handleButtonClick(int count) {
        //When button is clicked, we should have a message be sent, simulates what happens in the elevator
        //String buttonNum = String.valueOf(count);
        Message newMessage = new Message(2, 1, count);

        softwareBus.publish(newMessage);
        updateSendMessage(newMessage);
    }

    /**
     * Check for any incoming messages from the softwarebus
     */
    private void checkForIncomingMessage() {
        Thread thread = new Thread(() -> {
            while (true) {
                Message message = softwareBus.get(currentTopic, currentSubtopic);
                if (message != null) {
                    Platform.runLater(() -> {
                        updateReceiveMessage(message);
                    });
                }
            }
        });
        thread.start();
    }

    /**
     * Update label to indicate that a message was received
     *
     * @param message Message received
     */
    public void updateReceiveMessage(Message message) {
        messageStatus.setText("Message Received!\n" + message.toString());
    }

    /**
     * Update label to indicate that a message was sent out
     *
     * @param message Message sent out
     */
    public void updateSendMessage(Message message) {
        messageStatus.setText("Message Sent!\n" + message.toString());
    }

    /**
     * Border Pane
     *
     * @return Pane
     */
    public BorderPane getPane() {
        return pane;
    }


}