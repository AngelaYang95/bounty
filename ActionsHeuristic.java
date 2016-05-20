public class ActionsHeuristic implements IStrategy{

  public ActionsHeuristic() {
  }

  public int calcHCost(State child) {
    return child.getNumActions();
  }
}
