package elevatordesign.services;

import elevatordesign.models.Elevator;

public interface ElevatorObserver {
    void onElevatorUpdate(Elevator elevator);
}
