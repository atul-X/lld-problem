package elevatordesign.models;

import elevatordesign.services.ElevatorObserver;
import elevatordesign.services.ElevatorRequest;
import elevatordesign.services.SchedulingStrategy;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

public class Elevator {
    private final Object lock = new Object();
    private final List<ElevatorObserver> observers = new CopyOnWriteArrayList<>();
    private int id;
    private ElevatorState elevatorState;
    private Directions directions;
    private final Queue<ElevatorRequest> requests;
    private int currentFloor;

    public Queue<ElevatorRequest> getRequests() {
        synchronized (lock) {
            return requests;
        }
    }

    public int getCurrentFloor() {
        synchronized (lock) {
            return currentFloor;
        }
    }

    public void setCurrentFloor(int currentFloor) {
        synchronized (lock) {
            this.currentFloor = currentFloor;
        }
    }

    public Elevator(int id, ElevatorState elevatorState, Directions directions, Queue<ElevatorRequest> requests, int currentFloor) {
        this.id = id;
        this.elevatorState = elevatorState;
        this.directions = directions;
        // copy into a queue that's safe for concurrent add/peek/removeIf, regardless
        // of what the caller passed in (see Example 1, sde3-lld-review-and-thread-safety.md)
        this.requests = new ConcurrentLinkedQueue<>(requests);
        this.currentFloor = currentFloor;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public ElevatorState getElevatorState() {
        synchronized (lock) {
            return elevatorState;
        }
    }

    public void setElevatorState(ElevatorState elevatorState) {
        synchronized (lock) {
            this.elevatorState = elevatorState;
        }
    }

    public Directions getDirections() {
        synchronized (lock) {
            return directions;
        }
    }

    public void setDirections(Directions directions) {
        synchronized (lock) {
            this.directions = directions;
        }
    }

    public void addObserver(ElevatorObserver observer) {
        observers.add(observer);
    }

    public void removeObserver(ElevatorObserver observer) {
        observers.remove(observer);
    }

    // Only ever called after `lock` has been released — observers are arbitrary
    // code (e.g. a display) and must never be able to block a movement/request
    // thread by holding this elevator's monitor.
    private void notifyObservers() {
        for (ElevatorObserver observer : observers) {
            observer.onElevatorUpdate(this);
        }
    }

    public void addRequest(ElevatorRequest elevatorRequest){
        synchronized (lock) {
            if(!requests.contains(elevatorRequest)){
                requests.add(elevatorRequest);
            }
            if(elevatorRequest!=null && elevatorState.equals(ElevatorState.STOPPED)){
                if (elevatorRequest.getFloor()>currentFloor){
                    directions = Directions.UP;
                }else{
                    directions = Directions.DOWN;
                }
            }
        }
        notifyObservers();
    }

    // Runs one scheduling decision + move for this elevator as a single atomic
    // unit: getNextStop() and moveToNextFloor() used to be two separate lock
    // acquisitions from ElevatorManager.step(), leaving a window where another
    // thread's addRequest could land between "decide direction" and "move,"
    // producing a stale decision. Holding the lock across both closes that gap.
    public void step(SchedulingStrategy strategy) {
        boolean moved;
        synchronized (lock) {
            if (isOutOfService() || requests.isEmpty()) {
                return;
            }
            int nextFloor = strategy.getNextStop(this);
            moved = currentFloor != nextFloor;
            if (moved) {
                moveToNextFloorLocked(nextFloor);
            }
        }
        if (moved) {
            notifyObservers();
        }
    }

    public void moveToNextFloor(int nextFloor){
        synchronized (lock) {
            if (isOutOfService()){
                System.out.println("Elevator " + id + " is out of service (" + elevatorState + ")");
                return;
            }
            moveToNextFloorLocked(nextFloor);
        }
        notifyObservers();
    }

    // caller must already hold `lock`
    private void moveToNextFloorLocked(int nextFloor) {
        while (currentFloor!=nextFloor){
            if (currentFloor>nextFloor){
                directions = Directions.DOWN;
                currentFloor--;
            }else {
                directions = Directions.UP;
                currentFloor++;
            }
            if (currentFloor==nextFloor){
                completeArrival();
            }
        }
    }

    // caller must already hold `lock`
    private boolean isOutOfService() {
        return elevatorState == ElevatorState.MAINTENANCE || elevatorState == ElevatorState.EMERGENCY;
    }

    // caller must already hold `lock` — only invoked from moveToNextFloorLocked
    private void completeArrival(){
        elevatorState = ElevatorState.STOPPED;
        requests.removeIf(r->r.getFloor()==currentFloor);
        elevatorState = requests.isEmpty() ? ElevatorState.IDEAL : ElevatorState.RUNNING;
    }

    // Real elevators drop everything and stop serving hall/car calls the
    // instant an emergency is triggered — pending requests are discarded
    // rather than completed, since the car is no longer safe to route.
    public void triggerEmergency() {
        synchronized (lock) {
            elevatorState = ElevatorState.EMERGENCY;
            requests.clear();
        }
        notifyObservers();
    }

    public void resolveEmergency() {
        synchronized (lock) {
            if (elevatorState == ElevatorState.EMERGENCY) {
                elevatorState = requests.isEmpty() ? ElevatorState.IDEAL : ElevatorState.RUNNING;
            }
        }
        notifyObservers();
    }
}
