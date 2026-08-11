package elevatordesign.services;

import elevatordesign.models.Directions;
import elevatordesign.models.Elevator;

import java.util.Queue;

public class SchedulingFifo implements  SchedulingStrategy{
    @Override
    public int getNextStop(Elevator elevator) {
        if (elevator==null){
            throw new IllegalStateException();
        }
        Queue<ElevatorRequest> requests=elevator.getRequests();
        int currentFloor=elevator.getCurrentFloor();
        int nextStop=requests.peek().getFloor();
        if(currentFloor==nextStop){
            return currentFloor;
        }
        elevator.setDirections(currentFloor>nextStop?Directions.DOWN:Directions.UP);
        return nextStop;
    }
}
