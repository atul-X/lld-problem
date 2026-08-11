package elevatordesign;

import elevatordesign.models.Building;
import elevatordesign.models.Directions;
import elevatordesign.models.Elevator;
import elevatordesign.models.ElevatorState;
import elevatordesign.models.Floor;
import elevatordesign.services.ElevatorDisplay;
import elevatordesign.services.ElevatorManager;
import elevatordesign.services.NearestIdleSelectionStrategy;
import elevatordesign.services.SchedulingScan;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Simulation {
    public static void main(String[] args) {
        int totalFloors = 10;
        int totalElevators = 3;

        List<Floor> floors = new ArrayList<>();
        for (int i = 0; i < totalFloors; i++) {
            floors.add(new Floor());
        }

        List<Elevator> elevators = new ArrayList<>();
        for (int id = 1; id <= totalElevators; id++) {
            Elevator elevator = new Elevator(id, ElevatorState.IDEAL, Directions.IDLE, new LinkedList<>(), 0);
            elevator.addObserver(new ElevatorDisplay("Car " + id));
            elevators.add(elevator);
        }

        ElevatorManager manager = new ElevatorManager(elevators, floors, new SchedulingScan(), new NearestIdleSelectionStrategy());
        Building building = new Building("HQ Tower", totalElevators, totalFloors, manager);

        System.out.println("=== " + building.getName() + " simulation: "
                + building.getTotalElevator() + " elevators, " + building.getTotalFloors() + " floors ===");

        // hall calls: someone on a floor presses up/down
        manager.requestElevator(Directions.UP, 3);
        manager.requestElevator(Directions.UP, 7);
        manager.requestElevator(Directions.DOWN, 5);

        // car calls: rider already inside a specific car presses a floor button
        manager.requestFloor(9, 1);
        manager.requestFloor(2, 2);

        int step = 0;
        int maxSteps = 100;
        while (!allIdle(elevators) && step < maxSteps) {
            step++;
            System.out.println("--- step " + step + " ---");
            manager.step();
        }

        System.out.println("=== simulation finished after " + step + " steps ===");
    }

    private static boolean allIdle(List<Elevator> elevators) {
        for (Elevator elevator : elevators) {
            if (!elevator.getRequests().isEmpty()) {
                return false;
            }
        }
        return true;
    }
}
