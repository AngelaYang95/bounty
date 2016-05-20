import java.util.*;
/**
 * Structure to hold data in search finding algorithms.
 */
public class State implements Comparable<State>{
  private Coordinate location;
  private int direction;
  private StringBuilder actionSequence;
  private int numActions;
  private int numStones;
  private List<Coordinate> stoneLocations = new LinkedList<>();
  private List<Coordinate> stonesHeld = new LinkedList<>();
  private boolean hasKey;
  private boolean hasAxe;
  private int totalCost;
  private int numWaterWays;

  public State(Coordinate start, int startDir, int numActions, String actions, boolean hasAxe, boolean hasKey, int numStones) {
    this.direction = startDir;
    location = start;
    this.numActions = numActions;
    actionSequence = new StringBuilder(actions);
    this.numStones = numStones;
    this.hasAxe = hasAxe;
    this.hasKey = hasKey;
    totalCost = 0;
    numWaterWays = 0;
  }

  public void addWaterWay() {
    numWaterWays++;
  }

  public void updateWaterWay(int numWater) {
    this.numWaterWays = numWater;
  }

  public int getNumWaterWays() {
    return numWaterWays;
  }

  public void updateCost(int cost) {
      totalCost = cost;
  }

  /**
   * Updates entire state with new action.
   */
  public void addMove(char action) {
    actionSequence.append(action);
    numActions++;
    switch(action) {
      case Agent.TURN_LEFT:
        turnLeft();
        break;
      case Agent.TURN_RIGHT:
        turnRight();
        break;
      case Agent.MOVE_FORWARD:
        location.takeStep(direction);
        break;
    }
  }

  public void addStonesHeld(List<Coordinate> stonePoints) {
    stonesHeld.addAll(stonePoints);
  }

  public void updateHeldStone() {
    Coordinate stoneLocation = new Coordinate(location.getX(), location.getY());
    stonesHeld.add(stoneLocation);
  }

  public List<Coordinate> getStonesHeld() {
    return stonesHeld;
  }

  public int getNumStones() {
    return numStones;
  }

  public void addStone() {
    numStones++;
  }

  public void addStoneLocations(List<Coordinate> points) {
    stoneLocations.addAll(points);
  }

  public List<Coordinate> getStoneLocations() {
    return stoneLocations;
  }

  public void placeStoneInFront() {
    numStones--;
    addWaterWay();
    Coordinate stoneLocation = new Coordinate(location.getX(), location.getY());
    stoneLocation.takeStep(direction);
    stoneLocations.add(stoneLocation);
  }

  public boolean hasStoneInFront() {
    Coordinate stoneLocation = new Coordinate(location.getX(), location.getY());
    stoneLocation.takeStep(direction);
    return stoneLocations.contains(stoneLocation);
  }

  public void addAxe() {
    hasAxe = true;
  }

  public boolean hasAxe(){
    return hasAxe;
  }

  public void addKey(){
    hasKey = true;
  }

  public boolean hasKey() {
    return hasKey;
  }

  public Coordinate getCoordinate() {
    return location;
  }

  public int getX() {
    return location.getX();
  }

  public int getY() {
    return location.getY();
  }

  public int getDirection() {
    return direction;
  }

  public String getSequence() {
    return actionSequence.toString();
  }

  public int getNumActions() {
    return numActions;
  }

  // Used to prioritise States in search.
  @Override
  public int compareTo(State toCompare) {
    return (this.getCost() - toCompare.getCost());
  }

  // Best by number of actions it takes.
  private int getCost(){
    return totalCost;
  }

  private void turnLeft() {
    direction--;
    direction = (direction + 8) % 4;
  }

  private void turnRight() {
    direction = direction + 1 + 4;
    direction %= 4;
  }
}
