public class ManhattanHeuristic implements IStrategy{

  private Coordinate destination;

  public ManhattanHeuristic(Coordinate destination) {
    this.destination = destination;
  }

  public int calcHCost(State child) {
    int manhattan = calculateManhattan(child.getCoordinate());
    return child.getNumActions() + manhattan;
  }

  /**
   * Manhattan distance from agent to coordinate.
   */
  private int calculateManhattan (Coordinate start) {
    int startX = start.getX();
    int startY = start.getY();
    int destX = destination.getX();
    int destY = destination.getY();
    return Math.abs(startX - destX) + Math.abs(startY - destY);
  }
}
