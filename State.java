import java.util.*;
/**
 * Structure to hold data in search finding algorithms.
 */
public class State implements Comparable<State>{
  private Coordinate location;
  private int direction;
  private String actionSequence;
  private int numActions;
  private int numStones;
  private List<Coordinate> stoneLocations = new LinkedList<>();
  private boolean hasKey;
  private boolean hasAxe;

  public State(Coordinate start, int startDir, int numActions, String actions, boolean hasAxe, boolean hasKey) {
    this.direction = startDir;
    location = start;
    this.numActions = numActions;
    actionSequence = actions;
    this.numStones = 0;
    this.hasAxe = hasAxe;
    this.hasKey = hasKey;
  }

  /**
   * Updates entire state with new action.
   */
  public void addMove(char action) {
    actionSequence += action;
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

  public void showStones() {
    for(Coordinate point: stoneLocations) {
      System.out.println(point.getX() + "," + point.getY());

    }
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
    return actionSequence;
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
    return numActions;
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
