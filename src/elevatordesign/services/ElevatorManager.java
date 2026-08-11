package elevatordesign.services;

import elevatordesign.models.Directions;
import elevatordesign.models.Elevator;
import elevatordesign.models.ElevatorState;
import elevatordesign.models.Floor;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class ElevatorManager implements ElevatorController{
    private final List<Elevator> elevators;
    private final List<Floor> floors;
    private final SchedulingStrategy schedulingStrategy;
    private final ElevatorSelectionStrategy selectionStrategy;

    public ElevatorManager(List<Elevator> elevators, List<Floor> floors, SchedulingStrategy schedulingStrategy, ElevatorSelectionStrategy selectionStrategy) {
        this.elevators = elevators;
        this.floors = floors;
        this.schedulingStrategy = schedulingStrategy;
        this.selectionStrategy = selectionStrategy;
    }

    // unmodifiable views: callers can inspect elevators/floors but can't
    // mutate the manager's list out from under it without going through the
    // manager's own methods
    public List<Elevator> getElevators() {
        return Collections.unmodifiableList(elevators);
    }

    public List<Floor> getFloors() {
        return Collections.unmodifiableList(floors);
    }

    @Override
    public boolean requestElevator(Directions directions, int floor) {
        // best-effort pick: selectionStrategy reads each candidate's state/floor
        // one call at a time, not as one atomic snapshot across all elevators, so
        // under concurrent load the chosen car could change state a moment later.
        // That's fine — addRequest/step handle any state on arrival safely; worst
        // case is a slightly suboptimal choice, never a corrupted one.
        Elevator elevator = selectionStrategy.selectElevator(elevators, directions, floor);
        if (elevator == null) {
            System.out.println("No elevator available");
            return false;
        }
        dispatch(new ElevatorRequest(floor,directions,elevator.getId(),false,elevator));
        return true;
    }

    @Override
    public boolean requestFloor(int floor, int elevatorId) {
        Elevator elevator=getElevatorById(elevatorId);
        if (elevator==null){
            System.out.println("No elevator found");
            throw new IllegalStateException();
        }
        if (isOutOfService(elevator)) {
            System.out.println("Elevator " + elevatorId + " is out of service, request refused");
            return false;
        }
        Directions directions=floor>elevator.getCurrentFloor()?Directions.UP:Directions.DOWN;
        dispatch(new ElevatorRequest(floor,directions,elevatorId,true,elevator));
        return true;
    }

    private boolean isOutOfService(Elevator elevator) {
        ElevatorState state = elevator.getElevatorState();
        return state == ElevatorState.MAINTENANCE || state == ElevatorState.EMERGENCY;
    }

    private void dispatch(Command command) {
        command.execute();
    }

    public void step(){
        // each elevator's step() takes its own lock and runs scheduling +
        // move atomically, so no coordination is needed at the manager level
        for (Elevator elevator:elevators){
            elevator.step(schedulingStrategy);
        }
    }

    @Override
    public Elevator getElevatorById(int id) {
       Optional<Elevator> elevator=elevators.stream().filter(e->e.getId()==id).findFirst();
       if (!elevator.isPresent()){
           return null;
       }
       return elevator.get();
    }
}
