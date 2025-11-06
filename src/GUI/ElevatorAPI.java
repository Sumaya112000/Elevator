package GUI;

import bus.SoftwareBus;
import Message.Message;
import Message.Channels;
import Message.Commands;

/**
 * The API for the whole app with Software Bus integration.
 * All other parts talk to this class.
 */
public class ElevatorAPI {
    private SoftwareBus softwareBus;
    private boolean systemRunning = true;
    private String systemMode = "CENTRALIZED";

    private ElevatorPanel[] elevators;
    private CommandPanel commandPanel;

    public ElevatorAPI(SoftwareBus bus) {
        this.softwareBus = bus;
        setupSubscriptions();
    }

    /**
     * Set up message subscriptions for the command center
     */
    private void setupSubscriptions() {
        // Subscribe to system-wide messages
        softwareBus.subscribe(Channels.SYSTEM, 0);

        // Subscribe to individual elevator channels for status updates
        for (int i = 1; i <= 4; i++) {
            softwareBus.subscribe(i, 0);
        }

        // Subscribe to individual elevator control commands
        softwareBus.subscribe(Channels.ELEVATOR_START, 0); // Listen to all individual start commands
        softwareBus.subscribe(Channels.ELEVATOR_STOP, 0);  // Listen to all individual stop commands

        // Start message processing thread
        startMessageProcessing();
    }

    /**
     * Process incoming messages from the software bus
     */
    private void startMessageProcessing() {
        Thread messageThread = new Thread(() -> {
            while (true) {
                // Check for system messages
                Message systemMsg = softwareBus.get(Channels.SYSTEM, 0);
                if (systemMsg != null) {
                    handleSystemMessage(systemMsg);
                }

                // Check for elevator-specific messages
                for (int elevatorId = 1; elevatorId <= 4; elevatorId++) {
                    Message elevatorMsg = softwareBus.get(elevatorId, 0);
                    if (elevatorMsg != null) {
                        handleElevatorMessage(elevatorId, elevatorMsg);
                    }
                }

                // Check for individual elevator control messages
                Message startMsg = softwareBus.get(Channels.ELEVATOR_START, 0);
                if (startMsg != null) {
                    handleIndividualStartCommand(startMsg);
                }

                Message stopMsg = softwareBus.get(Channels.ELEVATOR_STOP, 0);
                if (stopMsg != null) {
                    handleIndividualStopCommand(stopMsg);
                }

                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        messageThread.setDaemon(true);
        messageThread.start();
    }

    /**
     * Handle system-wide messages (Topic 1-6 from protocol)
     */
    private void handleSystemMessage(Message message) {
        int topic = message.getTopic();
        int body = message.getBody();

        switch (topic) {
            case 1: // System Stop
                sendStopCommand();
                break;
            case 2: // System Start
                sendStartCommand();
                break;
            case 3: // System Reset
                sendResetCommand();
                break;
            case 4: // Test Fire
                sendFireCommand();
                break;
            case 5: // Clear Fire
                sendClearFireCommand();
                break;
            case 6: // Mode
                handleModeChange(body);
                break;
        }
    }

    /**
     * Handle individual elevator start commands (Topic 7)
     */
    private void handleIndividualStartCommand(Message message) {
        int subtopic = message.getSubTopic();
        if (subtopic >= 1 && subtopic <= 4) {
            startIndividualElevator(subtopic);
        }
    }

    /**
     * Handle individual elevator stop commands (Topic 8)
     */
    private void handleIndividualStopCommand(Message message) {
        int subtopic = message.getSubTopic();
        if (subtopic >= 1 && subtopic <= 4) {
            stopIndividualElevator(subtopic);
        }
    }

    /**
     * Handle elevator-specific messages
     */
    private void handleElevatorMessage(int elevatorId, Message message) {
        int body = message.getBody();
        int opcode = Commands.opcode(body);
        int arg = Commands.arg(body);

        switch (opcode) {
            case Commands.STATUS:
                updateElevatorStatus(elevatorId, arg);
                break;
            case Commands.GOTO:
                sendFloorRequest(elevatorId, arg);
                break;
            case Commands.ENABLE:
                startIndividualElevator(elevatorId);
                break;
            case Commands.DISABLE:
                stopIndividualElevator(elevatorId);
                break;
        }
    }

    /**
     * Handle mode change messages
     */
    private void handleModeChange(int body) {
        switch (body) {
            case 1: // Centralized mode
                this.systemMode = "CENTRALIZED";
                if (commandPanel != null) {
                    commandPanel.updateForAutoMode("CENTRALIZED");
                }
                break;
            case 2: // Independent mode
                this.systemMode = "INDEPENDENT";
                if (commandPanel != null) {
                    commandPanel.updateForAutoMode("INDEPENDENT");
                }
                break;
            case 3: // Test Fire mode
                sendFireCommand();
                break;
        }
    }

    /**
     * Update elevator status from received STATUS message
     */
    private void updateElevatorStatus(int elevatorId, int statusArg) {
        int floor = Commands.statusFloor(statusArg);
        boolean doorOpen = Commands.statusDoorOpen(statusArg);
        boolean moving = Commands.statusMoving(statusArg);
        int dirCode = Commands.statusDirCode(statusArg);

        // Convert direction code to ElevatorPanel.Direction
        ElevatorPanel.Direction direction = ElevatorPanel.Direction.IDLE;
        if (dirCode == 1) direction = ElevatorPanel.Direction.UP;
        else if (dirCode == 2) direction = ElevatorPanel.Direction.DOWN;

        // Update the specific elevator panel
        int index = elevatorId - 1;
        if (elevators != null && index >= 0 && index < elevators.length) {
            elevators[index].updateStatusFromBus(floor, doorOpen, moving, direction);
        }
    }

    /**
     * Send system commands via software bus
     */
    public void sendSystemCommand(int topic) {
        Message message = new Message(topic, 0, 0);
        softwareBus.publish(message);
    }

    /**
     * Send elevator-specific commands
     */
    public void sendElevatorCommand(int elevatorId, int opcode, int arg) {
        int body = Commands.encode(opcode, arg);
        Message message = new Message(elevatorId, 0, body);
        softwareBus.publish(message);
    }

    /**
     * Start an individual elevator
     */
    public void startIndividualElevator(int elevatorId) {
        int index = elevatorId - 1;
        if (elevators != null && index >= 0 && index < elevators.length) {
            // Enable the elevator locally
            elevators[index].setEnabledState(true);

            // Send enable command via bus
            sendElevatorCommand(elevatorId, Commands.ENABLE, 0);

            System.out.println("Elevator " + elevatorId + " started individually");
        }
    }

    /**
     * Stop an individual elevator
     */
    public void stopIndividualElevator(int elevatorId) {
        int index = elevatorId - 1;
        if (elevators != null && index >= 0 && index < elevators.length) {
            // Disable the elevator locally
            elevators[index].setEnabledState(false);

            // Send disable command via bus
            sendElevatorCommand(elevatorId, Commands.DISABLE, 0);

            System.out.println("Elevator " + elevatorId + " stopped individually");
        }
    }

    /**
     * Send individual elevator start command via bus
     */
    public void sendIndividualStartCommand(int elevatorId) {
        Message message = new Message(Channels.ELEVATOR_START, elevatorId, 0);
        softwareBus.publish(message);
        startIndividualElevator(elevatorId); // Also handle locally
    }

    /**
     * Send individual elevator stop command via bus
     */
    public void sendIndividualStopCommand(int elevatorId) {
        Message message = new Message(Channels.ELEVATOR_STOP, elevatorId, 0);
        softwareBus.publish(message);
        stopIndividualElevator(elevatorId); // Also handle locally
    }

    // Modified existing methods to use software bus:

    public void sendStartCommand() {
        this.systemRunning = true;
        sendSystemCommand(2); // Topic 2: System Start
        if (commandPanel != null) {
            commandPanel.updateButtonStates(true);
        }
    }

    public void sendStopCommand() {
        this.systemRunning = false;
        sendSystemCommand(1); // Topic 1: System Stop
        if (commandPanel != null) {
            commandPanel.updateButtonStates(false);
        }
    }

    public void sendResetCommand() {
        sendSystemCommand(3); // Topic 3: System Reset
        setInitialUIState();
        if (elevators != null) {
            for (ElevatorPanel elevator : elevators) {
                elevator.forceReset();
            }
        }
    }

    public void sendFireCommand() {
        this.systemMode = "FIRE";
        sendSystemCommand(4); // Topic 4: Test Fire
        if (commandPanel != null) {
            commandPanel.updateForFireMode(true);
        }
        // Send fire command to all elevators
        for (int i = 1; i <= 4; i++) {
            sendElevatorCommand(i, Commands.GOTO, 1); // Go to floor 1
        }
    }

    public void sendClearFireCommand() {
        this.systemMode = "INDEPENDENT";
        sendSystemCommand(5); // Topic 5: Clear Fire
        if (commandPanel != null) {
            commandPanel.updateForFireMode(false);
        }
    }

    public void toggleAutoMode() {
        if (this.systemMode.equals("INDEPENDENT")) {
            this.systemMode = "CENTRALIZED";
            sendSystemCommand(6); // Topic 6: Mode with body 1 for Centralized
            if (commandPanel != null) {
                commandPanel.updateForAutoMode("CENTRALIZED");
            }
        } else if (this.systemMode.equals("CENTRALIZED")) {
            this.systemMode = "INDEPENDENT";
            sendSystemCommand(6); // Topic 6: Mode with body 2 for Independent
            if (commandPanel != null) {
                commandPanel.updateForAutoMode("INDEPENDENT");
            }
        }
    }

    public void sendFloorRequest(int elevatorId, int targetFloor) {
        if (!systemRunning || systemMode.equals("FIRE")) {
            return;
        }

        sendElevatorCommand(elevatorId, Commands.GOTO, targetFloor);

        int index = elevatorId - 1;
        if (elevators != null && index >= 0 && index < elevators.length) {
            elevators[index].requestFloor(targetFloor);
        }
    }

    // Getter Methods
    public boolean isSystemRunning() {
        return this.systemRunning;
    }

    public String getSystemMode() {
        return this.systemMode;
    }

    public int getElevatorFloor(int elevatorId) {
        int index = elevatorId - 1;
        if (elevators != null && index >= 0 && index < elevators.length) {
            return elevators[index].getCurrentFloor();
        }
        return -1;
    }

    public boolean isElevatorMoving(int elevatorId) {
        int index = elevatorId - 1;
        if (elevators != null && index >= 0 && index < elevators.length) {
            return elevators[index].isMoving();
        }
        return false;
    }

    public boolean isElevatorDoorOpen(int elevatorId) {
        int index = elevatorId - 1;
        if (elevators != null && index >= 0 && index < elevators.length) {
            return elevators[index].isDoorOpen();
        }
        return false;
    }

    public ElevatorPanel.Direction getElevatorDirection(int elevatorId) {
        int index = elevatorId - 1;
        if (elevators != null && index >= 0 && index < elevators.length) {
            return elevators[index].getCurrentDirection();
        }
        return ElevatorPanel.Direction.IDLE;
    }

    public boolean isElevatorEnabled(int elevatorId) {
        int index = elevatorId - 1;
        if (elevators != null && index >= 0 && index < elevators.length) {
            return elevators[index].isEnabled();
        }
        return false;
    }

    // Registration methods
    public void registerCommandPanel(CommandPanel panel) {
        this.commandPanel = panel;
    }

    public void registerElevators(ElevatorPanel[] panels) {
        this.elevators = panels;
    }

    public void setInitialUIState() {
        this.systemRunning = true;
        this.systemMode = "CENTRALIZED";
        if (commandPanel != null) {
            commandPanel.updateForReset();
        }
    }
}