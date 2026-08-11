package elevatordesign.models;

import elevatordesign.services.ElevatorManager;

public class Building {
    private String name;
    private int totalElevator;
    private int totalFloors;
    private ElevatorManager elevatorManager;

    public Building(String name, int totalElevator, int totalFloors, ElevatorManager elevatorManager) {
        this.name = name;
        this.totalElevator = totalElevator;
        this.totalFloors = totalFloors;
        this.elevatorManager = elevatorManager;
    }

    public String getName() {
        return name;
    }

    public int getTotalElevator() {
        return totalElevator;
    }

    public int getTotalFloors() {
        return totalFloors;
    }

    public ElevatorManager getElevatorManager() {
        return elevatorManager;
    }
}
