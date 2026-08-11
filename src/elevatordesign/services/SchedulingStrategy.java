package elevatordesign.services;

import elevatordesign.models.Elevator;

public interface SchedulingStrategy {
    int getNextStop(Elevator elevator);
}
