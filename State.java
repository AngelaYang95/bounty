/*
 * State.java
 *
 * 06/05/2016
 */
import java.util.*;

/**
 * Encapsulates the game state. Used for search finding algorithms.
 */
public class State implements Comparable<State>{
  private Coordinate location;
  private int direction;
  private int totalCost;
  private StringBuilder actionSequence;
  private boolean hasKey;
  private boolean hasAxe;
  private int numStones;
  private int numWaterWays;
  private List<Coordinate> stoneLocations = new LinkedList<>();
  private List<Coordinate> stonesHeld = new LinkedList<>();

  public State(Coordinate location, int direction, String actions,
                boolean hasAxe, boolean hasKey, int numStones) {
    this.location = location;
    this.direction = direction;
    this.totalCost = 0;
    this.actionSequence = new StringBuilder(actions);
    this.hasAxe = hasAxe;
    this.hasKey = hasKey;
    this.numStones = numStones;
    this.numWaterWays = 0;
  }

  /**
   * Updates entire state with new a action.
   */
  public void addMove(char action) {
    actionSequence.append(action);
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

  /**
   * Changes the direction by 90 degrees clockwise.
   */
  private void turnLeft() {
    direction--;
    direction = (direction + 8) % 4;
  }

  /**
   * Changes the direction by 90 degrees anti-clockwise.
   */
  private void turnRight() {
    direction = direction + 1 + 4;
    direction %= 4;
  }

  /**
   * Returns the current direction of the state.
   */
  public int getDirection() {
    return direction;
  }

  /**
   * Updates the state's totalCost with new cost.
   */
  public void updateCost(int cost) {
      totalCost = cost;
  }

  /**
   * Returns the current cost of the State.
   */
  private int getCost(){
    return totalCost;
  }

  @Override
  public int compareTo(State toCompare) {
    return (this.getCost() - toCompare.getCost());
  }

  /**
   * Returns the current location of agaent in this state.
   */
  public Coordinate getCoordinate() {
    return location;
  }

  /**
   * Returns the x co-ordinate of the agent's location in this game state.
   */
  public int getX() {
    return location.getX();
  }

  /**
   * Returns the y co-ordinate of the agent's location in this game state.
   */
  public int getY() {
    return location.getY();
  }

  /**
   * Returns the sequence of actions the agent took to get to its location in
   * this game state.
   */
  public String getSequence() {
    return actionSequence.toString();
  }

  /**
   * Returns the number of actions the agent took to get to its location in this
   * game state.
   */
  public int getNumActions() {
    return actionSequence.length();
  }

  /**
   * Updates game state so that agent is holding an axe.
   */
  public void addAxe() {
    hasAxe = true;
  }

  /**
   * Checks if agent is has an axe.
   */
  public boolean hasAxe(){
    return hasAxe;
  }

  /**
   * Updates game state so that agent is holding an key.
   */
  public void addKey(){
    hasKey = true;
  }

  /**
   * Checks if agent is has an axe.
   */
  public boolean hasKey() {
    return hasKey;
  }

  /**
   * Adds to the game state a list of stones the agent is holding by their
   * Coordinate locations.
   */
  public void setStonesHeld(List<Coordinate> stonePoints) {
    stonesHeld.addAll(stonePoints);
  }

  /**
   * Sets the current location as a Coordinate of a stone held by the agent.
   */
  public void updateHeldStone() {
    Coordinate stoneLocation = new Coordinate(location.getX(), location.getY());
    stonesHeld.add(stoneLocation);
    numStones++;
  }

  /**
   * Returns the list of stones the agent has picked up since the beginning
   * of this game state. Stones are identitified by their original coordinate
   * locations on the map.
   */
  public List<Coordinate> getStonesHeld() {
    return stonesHeld;
  }

  /**
   * Returns the number of stones the agent is holding in this game state.
   */
  public int getNumStones() {
    return numStones;
  }

  /**
   * Adds a list of coordinates where the agent has put down stones in water.
   */
  public void setStoneLocations(List<Coordinate> points) {
    stoneLocations.addAll(points);
  }

  /**
   * Gets a list of coordinates where the agent has put down stones in water.
   */
  public List<Coordinate> getStoneLocations() {
    return stoneLocations;
  }

  /**
   * Places a stone in the water in front of the agent.
   */
  public void placeStoneInFront() {
    numStones--;
    Coordinate stoneLocation = new Coordinate(location.getX(), location.getY());
    stoneLocation.takeStep(direction);
    stoneLocations.add(stoneLocation);
  }

  /**
   * Checks if the agent has put down a stone in front of it's current location.
   */
  public boolean hasStoneInFront() {
    Coordinate stoneLocation = new Coordinate(location.getX(), location.getY());
    stoneLocation.takeStep(direction);
    return stoneLocations.contains(stoneLocation);
  }

  /**
   * Returns the number of times the agent has crossed water.
   */
  public int getNumWaterCrossings() {
    return stoneLocations.size();
  }
}
