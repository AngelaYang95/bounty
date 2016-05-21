/*********************************************
 *  COMP3411 Artificial Intelligence
 *  UNSW Session 1, 2016
 *  Angela Yang z5019922 & Tom(Jinyuan) He z3484364
 *  Group 84
 */

/**
 * QUESTION ANSWER:
 * The agent keeps information of the game state in the following structures:
 *  - Agent keeps it's own copy of the game terrain in worldMap[][] which is 4x
 *    the max game size. The agent always starts in the middle of the world map.
 *    All locations where the agent hasn't seen yet are marked as UNKNOWN. Each
 *    5 x 5 views is mapped onto worldMap[][] relative to agent's current
 *    direction and position.
 *  - Map<> Inventory stores the tools it currently has excluding stones.
 *  - Agent keeps count of the number of stones it has picked up.
 *  - Coordinate location of spotted axes, keys or stones; each in their own Set.
 *  - Whether the gold has been seen & whether we have the gold in hand.
 *  - The agent's initial location which will always be the middle of the
 *    worldMap.
 *
 *
 *  Logic Flow:
 *  The agent first scans in the 5 x 5 view of the game and updates itself with
 *  new information.
 *  The agent keeps a List<>journey of actions it should take. The agent takes
 *  the first action off that list. The agent has a series of search methods to
 *  populate the list if it is empty. All the searches are variations of A*
 *  searches or Uniform-cost search.
 *  They are prioritise and called as follows:
 *    1. We're holding GOLD -> search for a journey back to original location.
 *       This is an astar using Manhattan Distance Heuristic.
 *    2. GOLD has been seen -> try search for a journey to the gold without
 *       using stones. This is an A* search with Manhattan Distance Heuristic.
 *    3. Tools have been seen -> try search a journey to one of these tools
 *       without using stones. This is an A* search with Manhattan Distance
 *       Heuristic.
 *    4. Explore new territory A. This search will result in AI going deep into
 *       an area. It only looks at the what is reachable in front of it.
 *       It picks least number of moves to spot UNKNOWN, but also has tendency
 *       to move in straight line and cover more ground. It is a uniform cost
 *       search.
 *    5. Gold has been seen & we're holding stones -> Search for a journey to
 *       GOLD using the stones we have. Stones are precious so we will only
 *       deploy them if we can find a path to GOLD. This search is recursive.
 *       The waterHeuristic calculates the state cost by number of stones used
 *       since for every location, we want to take the path to get there with
 *       least number of stones. Everytime a tool is picked up, the search
 *       recurses as a means to forward check that.
 *    6. Explore new territory B. This search will result in AI doing a shallow
 *       but thorough search. Search B looks for any UNKNOWNS within the area
 *       of the 5 x 5 view.
 *    7. If none of the above works, the agent chooses a random action that
 *       won't result in it dying.
 */

import java.util.*;
import java.io.*;
import java.net.*;

public class Agent {

   final static int NORTH  = 0;
   final static int EAST   = 1;
   final static int SOUTH  = 2;
   final static int WEST   = 3;

   final static int WORLD_MAP_LENGTH = 159;
   final static int ENV_MAX_LENGTH = 80;
   final static int WINDOW_SIZE = 5;
   final static int AGENT_START_INDEX = 79;
   final static int VIEW_OFFSET = 2;

   final static char UNKNOWN = '?';
   final static char MOVE_FORWARD = 'f';
   final static char TURN_LEFT = 'l';
   final static char TURN_RIGHT = 'r';
   final static char CUT_DOWN = 'c';
   final static char OPEN_DOOR = 'u';
   final static char[] choiceOfMoves = {MOVE_FORWARD, TURN_LEFT, TURN_RIGHT};

   final static char STEPPING_STONE = 'o';
   final static char AXE = 'a';
   final static char KEY = 'k';
   final static char GOLD = 'g';

   final static char TREE = 'T';
   final static char WALL = '*';
   final static char WATER = '~';
   final static char SPACE = ' ';
   final static char DOOR = '-';
   final static char OUT = '.';

   private final Coordinate initialLocation = new Coordinate(AGENT_START_INDEX,
                                                             AGENT_START_INDEX);
   private char[][] worldMap = new char[WORLD_MAP_LENGTH][WORLD_MAP_LENGTH];

   // Current position and direction of the agent.
   private int currRow,currCol,currDir;

   // Equipment the agent is currently holding.
   private Map<Character, Boolean> inventory = new TreeMap<Character, Boolean>();
   private int numStones;
   private Map<Character, Coordinate> spottedTools = new HashMap<>();
   private Coordinate goldPosition;
   private boolean goldSeen;

   private LinkedList<Character> journey = new LinkedList<>();

   public Agent() {
	   currRow = AGENT_START_INDEX;
	   currCol = AGENT_START_INDEX;
	   currDir = NORTH;
     inventory.put(AXE, false);
	   inventory.put(KEY, false);
     inventory.put(GOLD, false);
	   numStones = 0;
     goldSeen = false;
	   for(char[] row: worldMap) { // Initialize the World Map to be all UNKNOWNs.
		   Arrays.fill(row, UNKNOWN);
	   }
   }

   /**
    * Given a view of it's surroundings, returns an action.
    */
   public char get_action( char view[][] ) {
	  scanView(view);
    char action = TURN_LEFT;
    if(journey.isEmpty()) {
      Coordinate currentLocation = new Coordinate(currRow, currCol);
      String path = "";

      // Look for path back to initialLocation.
      if(inventory.get(GOLD)) {
        path = findPathToCoordinate(currentLocation, initialLocation);
      }

      // Look for path to GOLD.
      if(path.isEmpty() && goldSeen) {
        path = findPathToCoordinate(currentLocation, goldPosition);
      }

      // Look for path to tools.
      if (path.isEmpty() && !spottedTools.keySet().isEmpty()) {
        for(Map.Entry<Character, Coordinate> entry : spottedTools.entrySet()) {
          path = findPathToCoordinate(currentLocation, entry.getValue());
          if(!path.isEmpty()) {
            break;
          }
        }
      }

      // Look for path to UNKNOWN using strategy A.
      if(path.isEmpty()) {
        path = exploreA(currentLocation);
      }

      // Try to find path to gold that uses stones.
      if(path.isEmpty() && goldSeen && numStones > 0) {
        path = pathToWin(currentLocation, currDir, numStones,
                         inventory.get(AXE), inventory.get(KEY),
                         new LinkedList<>(), new LinkedList<>());
      }

      // Look for path to UNKNOWN using strategy B.
      if(path.isEmpty()) {
        path = exploreB(currentLocation);
      }

      // Choose random action if no paths are found above.
      if(path.isEmpty()) {
        action = getRandomAction();
      } else {
        createJourney(path);
        action = journey.removeFirst();
      }
    } else {
      action = journey.removeFirst();
    }
	  updateNewPosition(action);
	  return action;
   }

   /**
    * Sets AI to follow the actions given in an actionSequence.
    */
   private void createJourney(String actionSequence) {
     char[] path = actionSequence.toCharArray();
     for(char action: path) {
       journey.add(action);
     }
   }

   /**
    * Updates agent with it's new location after it makes the next action.
    * AI attempting to walk through walls will break it's worldMap.
    */
   private void updateNewPosition(char action) {
      if(action == TURN_LEFT) {
         currDir = ((currDir - 1) + 8) % 4;  // Java's % gives only remainder.
      } else if (action == TURN_RIGHT) {
        currDir = (currDir + 1) % 4;
      } else if (action == MOVE_FORWARD) {
        if(currDir == NORTH) {
          currRow--;
        } else if(currDir == EAST) {
          currCol++;
        } else if(currDir == SOUTH) {
          currRow++;
        } else {
          currCol--;
        }
      }

     // Updates inventory with any items it has collected.
     char obj = getObjectAtPoint(new Coordinate(currRow, currCol));
     switch(obj) {
     case GOLD: case AXE: case KEY:
        spottedTools.remove(obj);
        inventory.put(obj,true);
        break;
     case STEPPING_STONE:
        spottedTools.remove(obj);
        numStones++;
        break;
     case WATER:
        numStones--;
        break;
     }
   }

   /**
    * Scans the given 5x5 view and updates AI's map with information.
    */
   public void scanView(char view[][]) {
	   for(int i = 0; i < WINDOW_SIZE; i++) {
		   for(int j = 0; j < WINDOW_SIZE; j++) {
			   char value = view[i][j];
			   if(i == 2 && j==2) {
			   		updateWorld(i, j, SPACE);
			   } else {
				    updateWorld(i, j, value);
			   }
			   // Store position of gold and tools.
			   if(value == GOLD && !goldSeen) {
			   		goldPosition = convertToWorldCoordinate(i,j);
            goldSeen = true;
			   } else if (((inventory.containsKey(value)) &&
                     (!inventory.get(value))        &&
                     ((value == KEY) || (value == AXE)))) {
           Coordinate location = convertToWorldCoordinate(i, j);
           spottedTools.put(value, location);
         } else if(value == STEPPING_STONE) {
           Coordinate location = convertToWorldCoordinate(i, j);
           spottedTools.put(value, location);
         }
		   }
	   }
   }

   /**
    * Updates the world map with new information on the environment.
    */
   private void updateWorld(int xInView, int yInView, char value) {
     Coordinate pointInWorld = convertToWorldCoordinate(xInView, yInView);
     int rowInWorld = pointInWorld.getX();
     int colInWorld = pointInWorld.getY();
     worldMap[rowInWorld][colInWorld] = value;
   }

   /**
    * Converts a x and y coordinate of the 5 x 5 window into a coordinate on
    * the AI's worldMap.
    */
   private Coordinate convertToWorldCoordinate(int xInView, int yInView) {
     int temp = xInView;
	   // Rotate the view and its indices to face NORTH.
	   switch(currDir) {
	   		case EAST:
          xInView = yInView;
          yInView = Math.abs(temp - 4);
	   			break;
	   		case SOUTH:
	   			xInView = Math.abs(xInView - 4);
	   			yInView = Math.abs(yInView - 4);
	   			break;
	   		case WEST:
	   			xInView = Math.abs(yInView - 4);
          yInView = temp;
	   }

	   // Calculate the corresponding indices in world map.
	   int rowInWorld = xInView - VIEW_OFFSET + currRow;
	   int colInWorld = yInView - VIEW_OFFSET + currCol;
     return new Coordinate(rowInWorld, colInWorld);
   }

   /**
    * Given a valid coordinate, return what is known to be at that point.
    */
   private char getObjectAtPoint(Coordinate point) {
     int x = point.getX();
     int y = point.getY();
     return worldMap[x][y];
   }

   /*_________________________________________
      BELOW ARE FUNCTIONS THAT CHOOSE ACTIONS.
     _________________________________________
    */

   /**
    * Gives a random action that won't result in agent dying.
    */
   private char getRandomAction() {
     Random rn = new Random();
     char action = choiceOfMoves[rn.nextInt(3)];
     if(action == MOVE_FORWARD) {
         char itemInFront = getObjectInFront(currRow, currCol, currDir, 1);
         if(itemInFront != SPACE) { // BE CAREFUL DON'T DIE.
            action = TURN_LEFT;
            if(itemInFront == TREE && inventory.get(AXE)) {
                action = CUT_DOWN;
            } else if(itemInFront == DOOR && inventory.get(KEY)) {
                action = OPEN_DOOR;
            }
         }
     }
     return action;
   }

   /**
    * Search for UNKNOWN to uncover new areas of the environment.
    * This search moves deep into an area before looking around.
    * Returns path as list of moves.
    */
   private String exploreA(Coordinate currentPoint) {
      IStrategy costCalc = new ActionsHeuristic();
      List<Coordinate> visited = new LinkedList<>();
      Queue<State> toVisit = new PriorityQueue<State>();

      // Add initial states.
      State currentState = new State(currentPoint, currDir, "",
                                     inventory.get(AXE), inventory.get(KEY), 0);
      toVisit.add(currentState);
      currentState = new State(currentPoint, (currDir + 2) % 4, "rr",
                               inventory.get(AXE), inventory.get(KEY), 0);
      toVisit.add(currentState);

      while(!toVisit.isEmpty()) {
          currentState = toVisit.poll();
          int currX = currentState.getX();
          int currY = currentState.getY();
          int currDirection = currentState.getDirection();

          if(getObjectInFront(currX, currY, currDirection, 2) == UNKNOWN ||
             getObjectInFront(currX, currY, currDirection, 1) == UNKNOWN) {
              return currentState.getSequence();
          }
          toVisit = considerChoices(currentState, visited, toVisit, costCalc);
          visited.add(currentState.getCoordinate());
      }
  	  return "";
   }

   /**
    * Search for UNKNOWN to uncover new areas of the environment.
    * This search is shallow and slowly widens the scope of seen.
    * Returns path as list of moves.
    */
   private String exploreB(Coordinate currentPoint) {
      IStrategy costCalc = new ActionsHeuristic();
      List<Coordinate> visited = new LinkedList<>();
      Queue<State> toVisit = new PriorityQueue<State>();

      // Add initial states.
      State currentState = new State(currentPoint, currDir, "",
                                     inventory.get(AXE), inventory.get(KEY), 0);
      toVisit.add(currentState);
      currentState = new State(currentPoint, (currDir + 2) % 4, "rr",
                               inventory.get(AXE), inventory.get(KEY), 0);
      toVisit.add(currentState);

      while(!toVisit.isEmpty()) {
          currentState = toVisit.poll();
          int currX = currentState.getX();
          int currY = currentState.getY();

          for(int i = 0; i < WINDOW_SIZE; i++) {
            for(int j = 0; j < WINDOW_SIZE; j++) {
              int viewX = i - VIEW_OFFSET + currX;
              int viewY = j - VIEW_OFFSET + currY;
              if(getObjectAtPoint(new Coordinate(viewX, viewY)) == UNKNOWN) {
                return currentState.getSequence();
              }
            }
          }
          considerChoices(currentState, visited, toVisit, costCalc);
          visited.add(currentState.getCoordinate());
      }
      return "";
   }

   /**
    * Searches for a path that can will arrive at gold using stones.
    * Everytime item is picked up, the search recurses and forward checks
    * whether there is a path to win.
    */
   private String pathToWin(Coordinate from, int dir, int stones, boolean hasAxe,
                            boolean hasKey, List<Coordinate> stoneLocations,
                            List<Coordinate> stonesHeld) {
     IStrategy waterStrat = new WaterHeuristic();
     List<Coordinate> visitedByAgent = new LinkedList<>();
     Queue<State> agentToVisit = new PriorityQueue<State>();
     StringBuilder path = new StringBuilder();

     State currAgentState = new State(from, dir, "", hasAxe, hasKey, stones);
     currAgentState.setStoneLocations(stoneLocations);
     currAgentState.setStonesHeld(stonesHeld);
     agentToVisit.add(currAgentState);

     currAgentState = new State(from, (dir + 2) % 4, "rr", hasAxe, hasKey, stones);
     currAgentState.setStoneLocations(stoneLocations);
     currAgentState.setStonesHeld(stonesHeld);
     agentToVisit.add(currAgentState);

     while(!agentToVisit.isEmpty()) {
       currAgentState = agentToVisit.poll();

       Coordinate currLocation = currAgentState.getCoordinate();
       int currDirection = currAgentState.getDirection();
       int currNumStones = currAgentState.getNumStones();
       boolean currHasAxe = currAgentState.hasAxe();
       boolean currHasKey = currAgentState.hasKey();
       List<Coordinate> currStoneLocations = currAgentState.getStoneLocations();
       List<Coordinate> currStonesHeld = currAgentState.getStonesHeld();
       String currSequence = currAgentState.getSequence();
       String forwardCheckPath = "";

       char currItem = getObjectAtPoint(currLocation);
       if(currItem == GOLD) {
           return currSequence;
       } else if ((currItem == STEPPING_STONE) &&
                  (!currStonesHeld.contains(currLocation))) {
         currAgentState.updateHeldStone();
         forwardCheckPath = pathToWin(currLocation, currDirection,
                                      currNumStones + 1, currHasAxe, currHasKey,
                                      currStoneLocations, currStonesHeld);
       } else if ((currItem == AXE) && (!currHasAxe)) {
         forwardCheckPath = pathToWin(currLocation, currDirection, currNumStones,
                                      true, currHasKey, currStoneLocations,
                                      currStonesHeld);
       } else if ((currItem == KEY) && (!currHasKey)) {
         forwardCheckPath = pathToWin(currLocation, currDirection, currNumStones,
                                      currHasAxe, true, currStoneLocations,
                                      currStonesHeld);
       } else {
         considerChoices(currAgentState, visitedByAgent, agentToVisit, waterStrat);
         visitedByAgent.add(currLocation);
       }
       if(!forwardCheckPath.isEmpty()) {
         path.append(currSequence);
         path.append(forwardCheckPath);
         return path.toString();
       }
      }
      return path.toString();
   }

   /**
    * Finds path from one location to a destination.
    */
   private String findPathToCoordinate(Coordinate from, Coordinate destination) {
     IStrategy costCalc = new ManhattanHeuristic(destination);
     List<Coordinate> visited = new LinkedList<>();
     Queue<State> toVisit = new PriorityQueue<State>();

     // Add initial states.
     State currentState = new State(from, currDir, "", inventory.get(AXE),
                                    inventory.get(KEY), 0);
     toVisit.add(currentState);
     currentState = new State(from, (currDir + 2) % 4, "rr", inventory.get(AXE),
                              inventory.get(KEY), 0);
     toVisit.add(currentState);

     while(!toVisit.isEmpty()) {
       currentState = toVisit.poll();
       Coordinate location = currentState.getCoordinate();
       if(location.equals(destination)) {
           return currentState.getSequence();
       } else if(getObjectAtPoint(location) == KEY) {
          currentState.addKey();
       } else if(getObjectAtPoint(location) == AXE) {
         currentState.addAxe();
       }
       considerChoices(currentState, visited, toVisit, costCalc);
       visited.add(location);
      }
      return "";
   }

   /*_____________________________________________
      Below are helper functions for searches.
     _____________________________________________
   */
   /**
    * Adds new valid states generated from either TURN_LEFT, MOVE_FORWARD,
    * TURN_RIGHT to the toVisit list.
    */
   private Queue<State> considerChoices(State currentState, List<Coordinate> visited,
                                        Queue<State>toVisit, IStrategy costCalc) {
     String actionSequence = currentState.getSequence();
     int prevDirection = currentState.getDirection();
     boolean hasAxe = currentState.hasAxe();
     boolean hasKey = currentState.hasKey();
     int numSteppingStones = currentState.getNumStones();
     int x = currentState.getX();
     int y = currentState.getY();
     char obj = getObjectInFront(x, y, prevDirection, 1);
     for(char action: choiceOfMoves) {
       if((action == MOVE_FORWARD) &&
         ((obj == UNKNOWN) ||
          (obj == WALL) ||
          (obj == OUT) ||
          (obj == WATER && numSteppingStones == 0 && !currentState.hasStoneInFront()) ||
          (obj == TREE && !hasAxe)  ||
          (obj == DOOR && !hasKey))) {

       } else {
         Coordinate stateLoc = new Coordinate(x, y);
         State nextState = new State(stateLoc, prevDirection, actionSequence,
                                       hasAxe, hasKey, numSteppingStones);
         nextState.setStoneLocations(currentState.getStoneLocations());
         nextState.setStonesHeld(currentState.getStonesHeld());
         if(action == MOVE_FORWARD) {
           if (obj == TREE) {
              nextState.addMove(CUT_DOWN);
           } else if (obj == DOOR) {
             nextState.addMove(OPEN_DOOR);
           } else if (obj == WATER && !currentState.hasStoneInFront()) {
             nextState.placeStoneInFront();
           }
         }
         nextState.addMove(action);
         int cost = costCalc.calcHCost(nextState);
         nextState.updateCost(cost);
         if(!visited.contains(nextState.getCoordinate())) {
           toVisit.add(nextState);
         }
       }
     }
     return toVisit;
   }

   /**
    * Given coordinates and a direction, finds what is in front of this
    * position.
    */
   private char getObjectInFront(int xInWorld, int yInWorld, int direction, int numSpots) {
       switch (direction) {
         case Agent.NORTH:
           xInWorld -= numSpots;
           break;
         case Agent.EAST:
           yInWorld += numSpots;
           break;
         case Agent.SOUTH:
           xInWorld += numSpots;
           break;
         case Agent.WEST:
           yInWorld -= numSpots;
           break;
       }
  		if(xInWorld < 0 || xInWorld >= WORLD_MAP_LENGTH ||
         yInWorld < 0 || yInWorld >= WORLD_MAP_LENGTH) {
  			return WALL;
  		} else {
  			return worldMap[xInWorld][yInWorld];
  		}
   }

   public static void main( String[] args )
   {
      InputStream in  = null;
      OutputStream out= null;
      Socket socket   = null;
      Agent  agent    = new Agent();
      char[][] view	  = new char[5][5];
      char   action   = 'F';
      int port;
      int ch;
      int i,j;

      if( args.length < 2 ) {
         System.out.println("Usage: java Agent -p <port>\n");
         System.exit(-1);
      }

      port = Integer.parseInt( args[1] );

      try { // open socket to Game Engine
         socket = new Socket( "localhost", port );
         in  = socket.getInputStream();
         out = socket.getOutputStream();
      }
      catch( IOException e ) {
         System.out.println("Could not bind to port: " + port);
         System.exit(-1);
      }

 	  // scan 5-by-5 window around current location
      try {
         while( true ) {
            for( i=0; i < 5; i++ ) {
               for( j=0; j < 5; j++ ) {
                  if( !(( i == 2 )&&( j == 2 ))) {
                     ch = in.read();
                     if( ch == -1 ) {
                        System.exit(-1);
                     }
                     view[i][j] = (char) ch;
                  }
               }
            }
            action = agent.get_action(view);
            out.write(action);
         }
      }
      catch( IOException e ) {
         System.out.println("Lost connection to port: "+ port );
         System.exit(-1);
      }
      finally {
         try {
            socket.close();
         }
         catch( IOException e ) {}
      }
   }

   //______________________________
   // PRINT FUNCTIONS TO BE REMOVED
   //______________________________
   /**
    * Prints to console everything the AI knows about its environment.
    */
   void show_world() {
      for(int i = 0; i < WORLD_MAP_LENGTH; i++) {
        for(int j = 0; j < WORLD_MAP_LENGTH; j++) {
          if(i == currRow && j == currCol) {
              switch(currDir) {
              case NORTH:
                System.out.print('^');
                break;
              case SOUTH:
                System.out.print('v');
                break;
              case EAST:
                System.out.print('>');
                break;
              case WEST:
                System.out.print('<');
                break;
              }
          } else {
             System.out.print(worldMap[i][j]);
          }
        }
        System.out.println();
      }
   }

   /**
    * Prints the current view to console.
    */
   void print_view(char[][] view) {
      int i,j;
      System.out.println("\n+-----+");
      for( i=0; i < 5; i++ ) {
         System.out.print("|");
         for( j=0; j < 5; j++ ) {
            if(( i == 2 )&&( j == 2 )) {
               System.out.print('^');
            }
            else {
               System.out.print( view[i][j] );
            }
         }
         System.out.println("|");
      }
      System.out.println("+-----+");
   }
}
