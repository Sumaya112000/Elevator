package GUI;

/**
 * The API for the whole app.
 * All other parts talk to this class.
 */
public class ElevatorAPI {

    private boolean systemRunning = true;
    private String systemMode = "CENTRALIZED";

    private ElevatorPanel[] elevators;
    private CommandPanel commandPanel;

    public ElevatorAPI() {
        // Constructor
    }

    /**
     * Access Command Panel
     */
    public void registerCommandPanel(CommandPanel panel) {
        this.commandPanel = panel;
    }

    /**
     * Status of each elevator location.
     */
    public void registerElevators(ElevatorPanel[] panels) {
        this.elevators = panels;
    }

    /**
     * Start Button
     */
    public void sendStartCommand() {
        this.systemRunning = true;
        if (commandPanel != null) {
            commandPanel.updateButtonStates(true);
        }
    }

    /**
     * STOP Button
     */
    public void sendStopCommand() {
        this.systemRunning = false;
        if (commandPanel != null) {
            commandPanel.updateButtonStates(false);
        }
    }

    /**
     * Setting UI for buttons when program starts
     */
    public void setInitialUIState() {
        this.systemRunning = true;
        this.systemMode = "CENTRALIZED";

        if (commandPanel != null) {
            commandPanel.updateForReset();
        }
    }

    /**
     * Reset Button
     */
    public void sendResetCommand() {
        setInitialUIState(); // Reset the buttons
        if (elevators != null) {
            for (ElevatorPanel elevator : elevators) {
                elevator.forceReset();
            }
        }
    }

    /**
     * Test Fire Button
     */
    public void sendFireCommand() {
        this.systemMode = "FIRE";
        if (commandPanel != null) {
            commandPanel.updateForFireMode(true);
        }
        // Tell every elevator to go to floor 1
        if (elevators != null) {
            for (ElevatorPanel elevator : elevators) {
                elevator.forceMoveAndOpen(1);
            }
        }
    }

    /**
     * Clear Fire Button
     */
    public void sendClearFireCommand() {
        this.systemMode = "INDEPENDENT"; // Fire clear puts system in Independent
        if (commandPanel != null) {
            commandPanel.updateForFireMode(false);
        }
        // Tell every elevator to resume
        if (elevators != null) {
            for (ElevatorPanel elevator : elevators) {
                elevator.releaseAndClose();
            }
        }
    }

    /**
     * Auto Button function
     */
    public void toggleAutoMode() {
        // Changes between CENTRALIZED and INDEPENDENT
        if (this.systemMode.equals("INDEPENDENT")) {
            this.systemMode = "CENTRALIZED";
            if (commandPanel != null) {
                commandPanel.updateForAutoMode("CENTRALIZED");
            }
        } else if (this.systemMode.equals("CENTRALIZED")) {
            this.systemMode = "INDEPENDENT";
            if (commandPanel != null) {
                commandPanel.updateForAutoMode("INDEPENDENT");
            }
        }
    }

    /**
     * Sending specific elevator to reach specific floor.
     */
    public void sendFloorRequest(int elevatorId, int targetFloor) {
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
}