/*********************************************
 *  Agent.java
 *  Sample Agent for Text-Based Adventure Game
 *  COMP3411 Artificial Intelligence
 *  UNSW Session 1, 2016
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

   private char[][] worldMap = new char[WORLD_MAP_LENGTH][WORLD_MAP_LENGTH];
   private Coordinate initialLocation;
   char[] choiceOfMoves = {MOVE_FORWARD, TURN_LEFT, TURN_RIGHT};

   // Current position and direction of the agent.
   private int currRow,currCol,currDir;

   // Equipment the agent is currently holding.
   Map<Character, Boolean> inventory = new TreeMap<Character, Boolean>();
   private int numStones;
   // Currently only tracks location of 1 of each type.
   private Map<Character, Coordinate> spottedTools = new HashMap<>();

   private Coordinate goldPosition;
   private boolean goldSeen;
   private boolean goldIsReachable;
   private boolean hasWonGame;
   private LinkedList<Character> journey = new LinkedList<>();

   private int turn;

   private int numShown;

   public Agent() {
     initialLocation = new Coordinate(AGENT_START_INDEX, AGENT_START_INDEX);
	   currRow = AGENT_START_INDEX;
	   currCol = AGENT_START_INDEX;
	   currDir = NORTH;
     inventory.put(AXE, false);
	   inventory.put(KEY, false);
     inventory.put(GOLD, false);
	   numStones = 0;
     goldIsReachable = false;
     goldSeen = false;
     hasWonGame = false;
     turn = 0;

     numShown = 0;
	   // Initialize the World Map to be all UNKNOWNs.
	   for(char[] row: worldMap) {
		   Arrays.fill(row, UNKNOWN);
	   }
   }

   /**
    * Given a view of it's surroundings, returns an action.
    */
   public char get_action( char view[][] ) {
     turn++;
     if(numShown == 25) {
       numShown = 0;
       System.out.print("Next 25 steps");
       try {
       	 System.in.read();
       }
       catch (IOException e) {
          System.out.println ("IO error:" + e );
       }
     }

	  scanView(view);
    numShown++;
	  /* Decision making for what action to take.
    Priority:
    1. Current journey
    2. If we have gold, find path back to initial position.
    3. Makes an attempt to get to Gold.
    4. Tries to get the tools it has seen.
    5. Tries to explore new spaces.
    5. Reverts to random movement if nothing else is good.
    */
    char action = TURN_LEFT;
    if(journey.isEmpty()) {
      Coordinate currentLocation = new Coordinate(currRow, currCol);
      String path = "";

      // Look for path back to initialLocation.
      if(inventory.get(GOLD)) {
        path = findPathToCoordinate(currentLocation, initialLocation);
        createJourney(path);
      }

      // Look for path to GOLD.
      if(goldSeen && path == "") {
        path = findPathToCoordinate(currentLocation, goldPosition);
        createJourney(path);
      }

      // Look for path to tools.
      if (path == "" && !spottedTools.keySet().isEmpty()) {
        for(Map.Entry<Character, Coordinate> entry : spottedTools.entrySet()) {
          path = findPathToCoordinate(currentLocation, entry.getValue());
          if(path != "") {
            createJourney(path);
            break;
          }
        }
      }

      // Look for path to UNKNOWN using strategy A.
      if(path == "") {
        path = exploreA(currentLocation);
        createJourney(path);
      }

      // Try to find path to gold that uses stones.
      if(path == "" && goldSeen && numStones > 0) {
        path = pathToWin(currentLocation, currDir, numStones, inventory.get(AXE), inventory.get(KEY), new LinkedList<>(), new LinkedList<>());
        createJourney(path);
      }

      // Look for path to UNKNOWN using strategy B.
      if(path == "") {
        path = exploreB(currentLocation);
        createJourney(path);
      }

      // Choose random action if no paths are found above.
      if(path == "") {
        action = getRandomAction();
      } else {
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
     System.out.println("NEXT MOVE IS " + action);
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
    } else {
       // Catching actions for cutting tree etc.
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
			   } else if ((inventory.containsKey(value) && !inventory.get(value) &&
                    (value == KEY || value == AXE)) || value == STEPPING_STONE) {
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
      Map<Integer, Map<Integer, String>> visited = new HashMap<>();
      Queue<State> toVisit = new PriorityQueue<State>();
      String path = "";

      // Add initial states.
      State currentState = new State(currentPoint, currDir, 0, "",
                                     inventory.get(AXE), inventory.get(KEY), 0);
      toVisit.add(currentState);
      currentState = new State(currentPoint, (currDir + 2) % 4, 2, "rr",
                               inventory.get(AXE), inventory.get(KEY), 0);
      toVisit.add(currentState);

      while(!toVisit.isEmpty()) {
          currentState = toVisit.poll();
          int currX = currentState.getX();
          int currY = currentState.getY();
          String actionSequence = currentState.getSequence();

          if(getObjectInFront(currentState.getX(), currentState.getY(),
                      currentState.getDirection(), 2) == UNKNOWN ||
             getObjectInFront(currentState.getX(), currentState.getY(),
                      currentState.getDirection(), 1) == UNKNOWN) {
              path = actionSequence;
              break;
          }
          toVisit = considerChoices(currentState, visited, toVisit, costCalc);
          visited = addToVisitedMap(visited, currentState);
      }
  	  return path;
   }

   /**
    * Search for UNKNOWN to uncover new areas of the environment.
    * This search is shallow and slowly widens the scope of seen.
    * Returns path as list of moves.
    */
   private String exploreB(Coordinate currentPoint) {
      IStrategy costCalc = new ActionsHeuristic();
      Map<Integer, Map<Integer, String>> visited = new HashMap<>();
      Queue<State> toVisit = new PriorityQueue<State>();
      String path = "";

      // Add initial states.
      State currentState = new State(currentPoint, currDir, 0, "",
                                     inventory.get(AXE), inventory.get(KEY), 0);
      toVisit.add(currentState);
      currentState = new State(currentPoint, (currDir + 2) % 4, 2, "rr",
                               inventory.get(AXE), inventory.get(KEY), 0);
      toVisit.add(currentState);

      while(!toVisit.isEmpty()) {
          currentState = toVisit.poll();
          int currX = currentState.getX();
          int currY = currentState.getY();
          String actionSequence = currentState.getSequence();
          // This search is like bfs.
          for(int i = 0; i < WINDOW_SIZE; i++) {
            for(int j = 0; j < WINDOW_SIZE; j++) {
              int viewX = i - VIEW_OFFSET + currX;
              int viewY = j - VIEW_OFFSET + currY;
              if(getObjectAtPoint(new Coordinate(viewX, viewY)) == UNKNOWN) {
                path = actionSequence;
                break;
              }
            }
          }
          toVisit = considerChoices(currentState, visited, toVisit, costCalc);
          visited = addToVisitedMap(visited, currentState);
      }
     return path;
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
     Map<Integer, Map<Integer, String>> visitedByAgent = new HashMap<>();
     Queue<State> agentToVisit = new PriorityQueue<State>();
     String agentPath = "";

     State currAgentState = new State(from, dir, 0, "", hasAxe, hasKey, stones);
     currAgentState.addStoneLocations(stoneLocations);
     currAgentState.addStonesHeld(stonesHeld);
     agentToVisit.add(currAgentState);

     currAgentState = new State(from, (dir + 2) % 4, 2, "rr", hasAxe, hasKey, stones);
     currAgentState.addStoneLocations(stoneLocations);
     currAgentState.addStonesHeld(stonesHeld);
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
           agentPath = currSequence;
           break;
       } else if (currItem == STEPPING_STONE && !currStonesHeld.contains(currLocation)) {
         currAgentState.updateHeldStone();
         forwardCheckPath = pathToWin(currLocation, currDirection, currNumStones + 1, currHasAxe, currHasKey, currStoneLocations, currStonesHeld);
       } else if (currItem == AXE && !currHasAxe) {
         forwardCheckPath = pathToWin(currLocation, currDirection, currNumStones, true, currHasKey, currStoneLocations, currStonesHeld);
       } else if (currItem == KEY && !currHasKey) {
         forwardCheckPath = pathToWin(currLocation, currDirection, currNumStones, currHasAxe, true, currStoneLocations, currStonesHeld);
       } else {
         considerChoices(currAgentState, visitedByAgent, agentToVisit, waterStrat);
         addToVisitedMap(visitedByAgent, currAgentState);
       }
       if(forwardCheckPath != "") {
         agentPath = currSequence + forwardCheckPath; // Use string builder.
         break;
       }
      }
      return agentPath;
   }

   /**
    * Finds path from one location to a destination.
    */
   private String findPathToCoordinate(Coordinate from, Coordinate destination) {
     IStrategy costCalc = new ManhattanHeuristic(destination);
     Map<Integer, Map<Integer, String>> visitedByAgent = new HashMap<>();
     Queue<State> agentToVisit = new PriorityQueue<State>();
     String agentPath = "";

     // Add initial states.
     State currAgentState = new State(from, currDir, 0, "", inventory.get(AXE),
                                      inventory.get(KEY), 0);
     agentToVisit.add(currAgentState);
     currAgentState = new State(from, (currDir + 2) % 4, 2, "rr",
                                inventory.get(AXE), inventory.get(KEY), 0);
     agentToVisit.add(currAgentState);

     while(!agentToVisit.isEmpty()) {
       /*} else if(obj == KEY) {
         nextState.addKey();
       } else if(obj == AXE) {
         nextState.addAxe();*/
       currAgentState = agentToVisit.poll();
       if(currAgentState.getCoordinate().equals(destination)) {
           agentPath = currAgentState.getSequence();
           break;
       }
       considerChoices(currAgentState, visitedByAgent, agentToVisit, costCalc);
       addToVisitedMap(visitedByAgent, currAgentState);
      }
      return agentPath;
   }

   /*_____________________________________________
      Below are helper functions for searches.
     _____________________________________________
   */
   // TODO: make coordinate map a private class.
   /**
    * Adds new valid states generated from either TURN_LEFT, MOVE_FORWARD,
    * TURN_RIGHT to the toVisit list.
    */
   private Queue<State> considerChoices(State currentState, Map<Integer, Map<Integer,
                                String>> visited, Queue<State>toVisit, IStrategy costCalc) {
     String actionSequence = currentState.getSequence();
     int prevDirection = currentState.getDirection();
     int numActions = currentState.getNumActions();
     boolean hasAxe = currentState.hasAxe();
     boolean hasKey = currentState.hasKey();
     int numSteppingStones = currentState.getNumStones();
     int x = currentState.getX();
     int y = currentState.getY();
     char obj = getObjectInFront(x, y, prevDirection, 1);
     for(char action: choiceOfMoves) {
       if(action == MOVE_FORWARD && (obj == UNKNOWN || obj == WALL ||
                                     obj == OUT     ||
                                     (obj == WATER && numSteppingStones == 0 && !currentState.hasStoneInFront()) ||
                                     (obj == TREE && !currentState.hasAxe())||
                                     (obj == DOOR && !currentState.hasKey()))) {
         continue;
       }

       Coordinate stateLoc = new Coordinate(x, y);
       State nextState = new State(stateLoc, prevDirection, numActions,
                             actionSequence, hasAxe, hasKey, numSteppingStones);
       nextState.addStoneLocations(currentState.getStoneLocations());
       nextState.addStonesHeld(currentState.getStonesHeld());
       nextState.updateWaterWay(currentState.getNumWaterWays());
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
       if(!isVisited(visited, nextState.getCoordinate())) {
         toVisit.add(nextState);
       }
     }
     return toVisit;
   }

   /**
    * Checks if a Coordinate point exists in a map.
    */
   private boolean isVisited(Map<Integer, Map<Integer, String>> map, Coordinate point) {
     int x = point.getX();
     int y = point.getY();
     if(map.containsKey(x)) {
       if(map.get(x).containsKey(y)) {
         return true;
       }
     }
     return false;
   }

   /**
    * Adds new State to Coordinate map.
    */
   private Map<Integer, Map<Integer, String>> addToVisitedMap(Map<Integer, Map<Integer, String>>visited,
                                State searchState) {
          int x = searchState.getX();
          int y = searchState.getY();
          String path = searchState.getSequence();
          if(visited.containsKey(x)) {
            if(!visited.get(x).containsKey(y)) {
              visited.get(x).put(y, path);
            }
          } else {
            Map<Integer, String> yToPath = new HashMap<>();
            yToPath.put(y, path);
            visited.put(x, yToPath);
          }
          return visited;
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

            agent.print_view(view); // COMMENT THIS OUT BEFORE SUBMISSION
            agent.scanView(view);
            agent.show_world(); // COMMENT THIS OUT BEFORE SUBMISSION
            action = agent.get_action(view);
            out.write( action );
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
