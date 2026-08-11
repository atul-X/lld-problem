Design an elevator control system for a building with multiple floors.
The system should be able to handle multiple elevators, manage requests from users,
optimize elevator movements, and handle different operational modes (e.g., maintenance, emergency).

Building
    -List<Elevator> elevators
    -name
    -List<Floor> floors
Elevator
    -id
    -state
    -directions
Floor
    -id

Directions
    up
    down




Building can have multiple elevators.
Elevators can be stop on different floors by internal and external commands.||Command Pattern 
Elevators can scheduled in Different scheduling algorithm like FIFO and scan.|| Strategy Pattern
Showing elevator states on displays.|| Observer pattern
Elevator state management like Direction and Elevator state current_floor, Maintained other.
thread safe