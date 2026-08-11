package elevatordesign.services;

import elevatordesign.models.Directions;
import elevatordesign.models.Elevator;

public interface ElevatorController {
     // hall call — rider presses up/down on a floor, system picks the car
     boolean requestElevator(Directions directions, int floor);
     // car call — rider is already inside a specific car, so elevatorId is legitimate context
     boolean requestFloor(int floor,int elevatorId);
     Elevator getElevatorById(int id);
}
