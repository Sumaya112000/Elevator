//package GUI;
//
//
//public class ElevatorAPI{
//    private boolean systemRunning = true;
//    private String systemMode = "CENTRALIZED";
//
//    private ElevatorPanel[] elevators;
//    private CommandPanel commandPanel;
//
//    public ElevatorAPI() {
//
//    }
//    public void registerCommandPanel(CommandPanel panel) {
//        this.commandPanel = panel;
//    }
//
//    public void registerElevators(ElevatorPanel[] panels) {
//        this.elevators = panels;
//    }
//
//
//    public void sendStartCommand(){
//        this.systemRunning = true;
//        if(commandPanel != null){
//            commandPanel.updateButtonStates(true);
//        }
//    }
//
//    public void sendStopCommand(){
//        this.systemRunning = false;
//        if(commandPanel != null){
//            commandPanel.updateButtonStates(false);
//        }
//    }
//
//    public void sendResetCommand(){
//        this.systemRunning = true;
//        this.systemMode = "CENTRALIZED";
//        // Resets all command panel UI
//        if(commandPanel != null){
//            commandPanel.updateForReset();
//        }
//        //Tells each elevator to reset.
//        if (elevators != null) {
//            for(ElevatorPanel elevator : elevators){
//                elevator.forceReset();
//            }
//        }
//    }
//
//    public void sendFireCommand(){
//        this.systemMode = "FIRE";
//
//        if(commandPanel != null){
//            commandPanel.updateForFireMode(true);
//        }
//        if(elevators != null){
//            for(ElevatorPanel elevator : elevators){
//                elevator.forceMoveAndOpen(1);
//            }
//        }
//    }
//    public void sendClearFireCommand() {
//        this.systemMode = "CENTRALIZED";
//
//        //Tell UI to clear fire
//        if (commandPanel != null) {
//            commandPanel.updateForFireMode(false);
//        }
//
//        if (elevators != null) {
//            for (ElevatorPanel elevator : elevators) {
//                elevator.releaseAndClose();
//            }
//        }
//    }
//
//    /**
//     * Auto Button Function. Switching from/to Centralized?/ Independent.
//     */
//    public void toggleAutoMode() {
//        // This button only works if NOT in fire mode
//        if (this.systemMode.equals("INDEPENDENT")) {
//            this.systemMode = "CENTRALIZED";
//            if (commandPanel != null) {
//                commandPanel.updateForAutoMode("CENTRALIZED");
//            }
//        } else if (this.systemMode.equals("CENTRALIZED")) {
//            this.systemMode = "INDEPENDENT";
//            if (commandPanel != null) {
//                commandPanel.updateForAutoMode("INDEPENDENT");
//            }
//        }
//    }
//
//    /** API Methods for ElevatorPanel **/
//
//    /**
//     *  For checking system on/off status
//     * @return system status
//     */
//    public boolean isSystemRunning() {
//        return this.systemRunning;
//    }
//
//    /**
//     * Checks mode of CC(Fire, Independent, Centralized)
//     * @return system mode
//     */
//    public String getSystemMode(){
//        return this.systemMode;
//    }
//
//    /**
//     * Gets the current floor of  elevator.
//     * @param elevatorId The elevator number (1-4)
//     * @return The floor number (1-10), or -1 if the ID is invalid.
//     */
//    public int getElevatorFloor(int elevatorId){
//        int index = elevatorId - 1;
//        if(elevators != null && index >=0 && index <elevators.length){
//            return elevators[index].getCurrentFloor();
//        }
//        return -1;
//    }
//
//
//    /**
//     * Sets the UI to its default state
//     */
//    public void setInitialUIState() {
//        this.systemRunning = true;
//        this.systemMode = "CENTRALIZED";
//
//        if (commandPanel != null) {
//            commandPanel.updateForReset();
//        }
//    }
//
//    /**
//     * Checks if a specific elevator is currently moving.
//     * @param elevatorId The elevator number (1-4)
//     * @return true if the elevator is moving.
//     */
//    public boolean isElevatorMoving(int elevatorId) {
//        int index = elevatorId - 1;
//        if (elevators != null && index >= 0 && index < elevators.length) {
//            return elevators[index].isMoving();
//        }
//        return false;
//    }
//
//    /**
//     * Checks if a specific elevator's door is open.
//     * @param elevatorId The elevator number (1-4)
//     * @return true if the door is open.
//     */
//    public boolean isElevatorDoorOpen(int elevatorId) {
//        int index = elevatorId - 1;
//        if (elevators != null && index >= 0 && index < elevators.length) {
//            return elevators[index].isDoorOpen();
//        }
//        return false;
//    }
//
//    /**
//     * Gets the current direction of a specific elevator.
//     * @param elevatorId The elevator number (1-4)
//     * @return The direction (UP, DOWN, IDLE), or IDLE if invalid.
//     */
//    public ElevatorPanel.Direction getElevatorDirection(int elevatorId) {
//        int index = elevatorId - 1;
//        if (elevators != null && index >= 0 && index < elevators.length) {
//            return elevators[index].getCurrentDirection();
//        }
//        return ElevatorPanel.Direction.IDLE;
//    }
//
//    /**
//     * Running elevator in a sequeunce to show elevators communicate with CC.
//     *
//     * @param elevatorId The elevator number (1-4)
//     * @param sequence   The array of floors to visit.
//     */
//    public void setAndStartElevatorSequence(int elevatorId, int[] sequence) {
//        int index = elevatorId - 1;
//
//        if (elevators != null && index >= 0 && index < elevators.length) {
//            elevators[index].startAutomatedLoop(sequence);
//        }
//    }
//}