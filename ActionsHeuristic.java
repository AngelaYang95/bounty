/**
 * Calculates the cost of a state according to number of actions it has taken.
 */
public class ActionsHeuristic implements IStrategy{

  public ActionsHeuristic() {
  }

  public int calcHCost(State child) {
    return child.getNumActions();
  }
}
