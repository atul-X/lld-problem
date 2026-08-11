package elevatordesign.services;

import elevatordesign.models.Directions;
import elevatordesign.models.Elevator;

import java.util.List;

// Picks *which car* answers a hall call — the sibling of SchedulingStrategy,
// which only decides where an already-chosen car goes next. See
// elevator-selection-strategy-suggestion.md for why these are separate concerns.
public interface ElevatorSelectionStrategy {
    Elevator selectElevator(List<Elevator> elevators, Directions directions, int floor);
}
