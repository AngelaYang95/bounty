/*
 * WaterHeuristic.java
 *
 * 18/05/2016
 */

 /**
  * Calculates the cost of a state by only the number of stones it uses in its
  * path.
  */
public class WaterHeuristic implements IStrategy{

  public WaterHeuristic() {}

  public int calcHCost(State child) {
    return child.getNumWaterCrossings();
  }
}
