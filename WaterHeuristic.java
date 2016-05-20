public class WaterHeuristic implements IStrategy{

  public WaterHeuristic() {

  }

  public int calcHCost(State child) {
    return child.getNumWaterWays();
  }
}
