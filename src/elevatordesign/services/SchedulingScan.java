package elevatordesign.services;

import elevatordesign.models.Directions;
import elevatordesign.models.Elevator;

import java.util.Queue;

// SCAN: keep moving in the current direction, serving every pending request
// along the way, until nothing is left ahead — then reverse. Unlike FIFO,
// order of arrival doesn't matter; only position relative to current floor
// and direction of travel does, which is what keeps a real elevator from
// zig-zagging between floors it's already passed.
public class SchedulingScan implements SchedulingStrategy {

    @Override
    public int getNextStop(Elevator elevator) {
        if (elevator == null) {
            throw new IllegalStateException();
        }
        Queue<ElevatorRequest> requests = elevator.getRequests();
        int currentFloor = elevator.getCurrentFloor();
        Directions direction = elevator.getDirections();

        Integer nextInCurrentDirection = nearestMatch(requests, currentFloor, direction);
        if (nextInCurrentDirection != null) {
            return nextInCurrentDirection;
        }

        // nothing left ahead — turn around, same as a real car reversing at
        // the last call instead of running empty to the end of the shaft
        Directions reversed = direction == Directions.UP ? Directions.DOWN : Directions.UP;
        Integer nextAfterReversal = nearestMatch(requests, currentFloor, reversed);
        if (nextAfterReversal != null) {
            elevator.setDirections(reversed);
            return nextAfterReversal;
        }

        // direction was IDLE (elevator just went idle) or requests don't
        // align with either direction check above — fall back to nearest
        return nearestOverall(requests, currentFloor, elevator);
    }

    private Integer nearestMatch(Queue<ElevatorRequest> requests, int currentFloor, Directions direction) {
        Integer best = null;
        for (ElevatorRequest request : requests) {
            int floor = request.getFloor();
            boolean aheadInDirection = direction == Directions.UP ? floor >= currentFloor : floor <= currentFloor;
            if (!aheadInDirection) {
                continue;
            }
            if (best == null || Math.abs(floor - currentFloor) < Math.abs(best - currentFloor)) {
                best = floor;
            }
        }
        return best;
    }

    private int nearestOverall(Queue<ElevatorRequest> requests, int currentFloor, Elevator elevator) {
        Integer best = null;
        for (ElevatorRequest request : requests) {
            int floor = request.getFloor();
            if (best == null || Math.abs(floor - currentFloor) < Math.abs(best - currentFloor)) {
                best = floor;
            }
        }
        if (best == null) {
            return currentFloor;
        }
        if (best != currentFloor) {
            elevator.setDirections(best > currentFloor ? Directions.UP : Directions.DOWN);
        }
        return best;
    }
}
