package elevatordesign.services;

import elevatordesign.models.Directions;
import elevatordesign.models.Elevator;
import elevatordesign.models.ElevatorState;

import java.util.List;

// 1) prefer an idle/stopped car nearest the caller — nobody else is waiting on it.
// 2) otherwise prefer a car already moving the requested direction that hasn't
//    passed the floor yet — the "on the way" case, core to real collective control.
// 3) otherwise fall back to the nearest car regardless of state.
// MAINTENANCE/EMERGENCY cars are never selected.
public class NearestIdleSelectionStrategy implements ElevatorSelectionStrategy {

    @Override
    public Elevator selectElevator(List<Elevator> elevators, Directions directions, int floor) {
        Elevator idle = nearest(elevators, floor, elevator -> {
            ElevatorState state = elevator.getElevatorState();
            return state == ElevatorState.IDEAL || state == ElevatorState.STOPPED;
        });
        if (idle != null) {
            return idle;
        }

        Elevator onTheWay = nearest(elevators, floor, elevator ->
                elevator.getDirections() == directions && isAheadOf(elevator, directions, floor));
        if (onTheWay != null) {
            return onTheWay;
        }

        return nearest(elevators, floor, elevator -> true);
    }

    private boolean isAheadOf(Elevator elevator, Directions directions, int floor) {
        return directions == Directions.UP
                ? elevator.getCurrentFloor() <= floor
                : elevator.getCurrentFloor() >= floor;
    }

    private Elevator nearest(List<Elevator> elevators, int floor, java.util.function.Predicate<Elevator> eligible) {
        Elevator best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (Elevator elevator : elevators) {
            if (isOutOfService(elevator) || !eligible.test(elevator)) {
                continue;
            }
            int distance = Math.abs(elevator.getCurrentFloor() - floor);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = elevator;
            }
        }
        return best;
    }

    private boolean isOutOfService(Elevator elevator) {
        ElevatorState state = elevator.getElevatorState();
        return state == ElevatorState.MAINTENANCE || state == ElevatorState.EMERGENCY;
    }
}
