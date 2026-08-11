package elevatordesign.services;

import elevatordesign.models.Elevator;

// Stands in for a physical floor/car display panel — subscribes to one
// elevator and prints its state on every update. Multiple displays (one per
// floor, say) can subscribe to the same elevator independently.
public class ElevatorDisplay implements ElevatorObserver {
    private final String label;

    public ElevatorDisplay(String label) {
        this.label = label;
    }

    @Override
    public void onElevatorUpdate(Elevator elevator) {
        System.out.printf(
                "[%s] Elevator %d -> floor %d | state=%s | direction=%s%n",
                label,
                elevator.getId(),
                elevator.getCurrentFloor(),
                elevator.getElevatorState(),
                elevator.getDirections()
        );
    }
}
