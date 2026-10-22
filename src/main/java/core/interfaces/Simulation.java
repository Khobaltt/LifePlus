package main.java.core.interfaces;

public interface Simulation<G> {
    
    // returns the current grid; treat it as read-only
    G getCurrentGrid();

    // returns an independent copy of the initial grid
    G getInitialGrid();

    // calculates and installs the next grid state
    void step();

    // restores an independent copy of the initial grid
    void reset();

    // returns the number of completed steps since initialization or reset
    long getStepCount();

}
