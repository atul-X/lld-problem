package elevatordesign.services;

import elevatordesign.models.Directions;
import elevatordesign.models.Elevator;

import java.util.Objects;

public class ElevatorRequest implements  Command{
    private int floor;
    private Directions directions;
    private int elevatorId;
    private boolean isInternalCommand;
    private Elevator elevator;

    public ElevatorRequest(int floor, Directions directions, int elevatorId, boolean isInternalCommand, Elevator elevator) {
        this.floor = floor;
        this.directions = directions;
        this.elevatorId = elevatorId;
        this.isInternalCommand = isInternalCommand;
        this.elevator = elevator;
    }

    public int getFloor() {
        return floor;
    }

    public Directions getDirections() {
        return directions;
    }

    public int getElevatorId() {
        return elevatorId;
    }

    public boolean isInternalCommand() {
        return isInternalCommand;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ElevatorRequest that = (ElevatorRequest) o;
        return floor == that.floor && elevatorId == that.elevatorId && isInternalCommand == that.isInternalCommand && directions == that.directions && Objects.equals(elevator, that.elevator);
    }

    @Override
    public int hashCode() {
        return Objects.hash(floor, directions, elevatorId, isInternalCommand, elevator);
    }

    @Override
    public void execute() {
        elevator.addRequest(this);
    }
}
