public class State implements Comparable<State>{
  private Coordinate location;
  private int direction;
  //private List<Coordinate> path = new LinkedList<>();
  private String actionSequence;
  private int numActions;

  public State(Coordinate start, int startDir) {
    this.direction = startDir;
    location = start;
    numActions = 0;
    actionSequence = "";
  }

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

  public int getDirection() {
    return direction;
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
    direction = (direction % 4 + 4) % 4;
  }

  private void turnRight() {
    direction++;
    direction %= 4;
  }
}
