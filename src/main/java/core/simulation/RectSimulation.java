package main.java.core.simulation;

import main.java.core.interfaces.Simulation;
import main.java.core.rect.RectGrid;

// util
import java.util.Objects;
import java.util.function.UnaryOperator;

public class RectSimulation implements Simulation<RectGrid> {

    // saved grid states
    private final RectGrid initialGrid;
    private RectGrid currentGrid;

    // function that calculates the next grid state from a current grid state
    private final UnaryOperator<RectGrid> calculateNextGrid;
    private long stepCount = 0;

    /*
     * Creates a simulation with a supplied update rule
     */
    public RectSimulation(
        RectGrid grid,
        UnaryOperator<RectGrid> calculateNextGrid
    ) {
        Objects.requireNonNull(grid, "Grid cannot be null.");
        this.calculateNextGrid = Objects.requireNonNull(
            calculateNextGrid, "Update rule cannot be null."
        );

        initialGrid = grid.copy();
        currentGrid = initialGrid.copy();
    }

    /*
     * Returns a deep copy of the current grid
     */
    @Override 
    public RectGrid getCurrentGrid() { return currentGrid.copy(); }

    /*
     * Returns a deep copy of the initial grid
     */
    @Override 
    public RectGrid getInitialGrid() { return initialGrid.copy(); }
    
    @Override
    public void step() {

        // calculate using a copy so a failed rule cannot partially change the current state
        RectGrid workingGrid = currentGrid.copy();
        RectGrid nextGrid = Objects.requireNonNull(
            calculateNextGrid.apply(workingGrid),
            "Update rule cannot return null."
        );

        // own the completed result independently of any grid held by the rule
        currentGrid = nextGrid;
        stepCount++;
    }

    /*
     * Restores the initial state and restarts the step counter
     */
    @Override
    public void reset() {
        currentGrid = initialGrid.copy();
        stepCount = 0;
    }

    @Override
    public long getStepCount() {
        return stepCount;
    }
}