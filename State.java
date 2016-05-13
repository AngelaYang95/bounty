/**
 * Structure to hold data in search finding algorithms.
 */
public class State implements Comparable<State>{
  private Coordinate location;
  private int direction;
  //private List<Coordinate> path = new LinkedList<>();
  private String actionSequence;
  private int numActions;

  public State(Coordinate start, int startDir, int numActions, String actions) {
    this.direction = startDir;
    location = start;
    this.numActions = numActions;
    actionSequence = actions;
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

  public Coordinate getCoordinate() {
    return location;
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
