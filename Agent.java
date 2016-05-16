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
   private int numSteppingStones;
   // Only tracks location of 1 of each type atm.
   private Map<Character, Coordinate> spottedTools = new HashMap<>();

   private Coordinate goldPosition;
   private boolean goldSeen;
   private boolean goldIsReachable;
   private boolean hasWonGame;
   private LinkedList<Character> journey = new LinkedList<>();

   public Agent() {
     initialLocation = new Coordinate(AGENT_START_INDEX, AGENT_START_INDEX);
	   currRow = AGENT_START_INDEX;
	   currCol = AGENT_START_INDEX;
	   currDir = NORTH;
     inventory.put(AXE, false);
	   inventory.put(KEY, false);
     inventory.put(GOLD, false);
	   numSteppingStones = 0;
     goldIsReachable = false;
     goldSeen = false;
     hasWonGame = false;

	   // Initialize the World Map to be all unknowns.
	   for(char[] row: worldMap) {
		   Arrays.fill(row, UNKNOWN);
	   }
   }

   /**
    * Given a view of it's surroundings, returns an action.
    */
   public char get_action( char view[][] ) {
      System.out.print("Next Step");
      try {
      	 System.in.read();
      }
      catch (IOException e) {
         System.out.println ("IO error:" + e );
      }
	  scanView(view);

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
      String path = "";
      if(inventory.get(GOLD)) {
        path = findPathToCoordinate(initialLocation);
        createJourney(path);
      }

      // Look for path to tools.
      if (path == "" && !spottedTools.keySet().isEmpty()) {
        Iterator<Map.Entry<Character, Coordinate>> it = spottedTools.entrySet().iterator();
        while(it.hasNext()) {
          Map.Entry<Character, Coordinate> entry = it.next();
          path = findPathToCoordinate(entry.getValue());
          if(path != "") {
            System.out.println("Trying to get to " + entry.getKey());
            createJourney(path);
            break;
          }
        }
      }
      // fflffff-r-fflffurfflffr
      // Look for path to Gold.
      if(goldSeen && path == "") {
        path = findPathToCoordinate(goldPosition);
        createJourney(path);
        System.out.println("Gold search " + path);
      }

      // Look for path to UNKNOWN.
      if(path == "") {
        path = conquerTerritory(); // TODO: has issues, can go out of world.
        createJourney(path);
        if(path == "") {
          System.out.println("Couldn't find unknown");
        } else {
          System.out.println("territory search " + path);
        }
      }

      if(path == "") {
        action = getRandomAction();
        System.out.println("RANDOM");
      } else {
        action = journey.removeFirst();
      }
    } else {
      action = journey.removeFirst();
    }
	  updateNewPosition(action);
	  return action;
   }

   private void createJourney(String path) {
     char[] pathArray = path.toCharArray();
     for(char pathAction: pathArray) {
       journey.add(pathAction);
     }
   }

   /**
    * Gives a random action that won't result in agent dying.
    */
   private char getRandomAction() {
     Random rn = new Random();
     char action = choiceOfMoves[rn.nextInt(3)];
     if(action == MOVE_FORWARD) {
         char itemInFront = getObjectInFront(currRow, currCol, currDir);
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
    * Scans the given 5x5 view and updates AI's map with information.
    * TODO: Change so it doesn't constantly rescan already know information.
    * Lowercase all roman letters.
    */
   public void scanView( char view[][]) {
	   for(int i = 0; i < WINDOW_SIZE; i++) {
		   for(int j = 0; j < WINDOW_SIZE; j++) {
			   char value = view[i][j];
			   if(i == 2 && j==2) {
			   		updateWorld(i, j, SPACE);
			   } else {
				   updateWorld(i, j, value);
			   }

			   // Store position of gold.
			   if(value == GOLD && !goldSeen) {
			   		goldPosition = convertToWorldCoordinate(i,j);
            goldSeen = true;
			   } else if (value == STEPPING_STONE || value == KEY || value == AXE) {
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
    * Updates the world map with new information on the environment.
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
    * Basic strategy to get AI to explore new areas of the environment.
    * Currently Uni-cost/bfs for UNKNOWN.
    * Maybe change to iterative deepening.
    * Returns path as list of moves.
    */
   private String conquerTerritory() {
    // Speed up by changing to 2D array means immediate lookup. But space.
    Map<Integer, Map<Integer, String>> visited = new HashMap<>();
    // Can be priority queue later.
    Queue<State> toVisit = new PriorityQueue<State>();
    Coordinate currentPoint = new Coordinate(currRow, currCol);
    State currentState = new State(currentPoint, currDir, 0, "");
    toVisit.add(currentState);
    currentState = new State(currentPoint, (currDir + 2) % 4, 2, "rr");
    toVisit.add(currentState);
    String path = "";
    // BFS
    while(!toVisit.isEmpty()) {
        currentState = toVisit.poll();
        String actionSequence = currentState.getSequence();
        if(getObjectInFront(currentState.getX(), currentState.getY(),
                            currentState.getDirection()) == UNKNOWN) {
            path = actionSequence;
            break;
        }
        toVisit = considerChoices(currentState, visited, toVisit);
        // Added afterwards to allow states facing left and right to be added.
        visited = addToVisitedMap(visited, currentState);
    }
	  return path;
   }

   /**
    * Given a valid coordinate, return what is known to be at that point.
    */
   private char getObjectAtPoint(Coordinate point) {
     int x = point.getX();
     int y = point.getY();
     return worldMap[x][y];
   }

   /**
    * Given an x and y point returns a path from the agent's current location to
    * that coordinate.
    * TODO: Bi-directional search with A*. BFS first then update to A*.
    * Learn to unlock doors and cut trees etc.
    * TODO: Changing states to store coordinates instead of moves.
    */
   private String findPathToCoordinate(Coordinate destination) {
     // Structures needed for Agent's side of search.
     Map<Integer, Map<Integer, String>> visitedByAgent = new HashMap<>();
     Queue<State> agentToVisit = new PriorityQueue<State>();
     Coordinate agentCurrPoint = new Coordinate(currRow, currCol);
     State currAgentState = new State(agentCurrPoint, currDir, 0, "");
     agentToVisit.add(currAgentState);
     int a = (currDir + 2) % 4;
     currAgentState = new State(agentCurrPoint, (currDir + 2) % 4, 2, "rr");
     agentToVisit.add(currAgentState);
     String agentPath = "";
     int agentFinalDir = 5;

     // Structures needed for Goal side of search.
     Map<Integer, Map<Integer, String>> visitedByGoal = new HashMap<>();
     Queue<State> goalToVisit = new PriorityQueue<State>();
     State currGoalState = new State(destination, NORTH, 0, "");
     goalToVisit.add(currGoalState);
     String goalPath = "";
     int goalFinalDir = 5;

     while(!agentToVisit.isEmpty() && !goalToVisit.isEmpty()) {
       if(!agentToVisit.isEmpty()) {
         currAgentState = agentToVisit.poll();
         System.out.println("Agent path choice is " + currAgentState.getSequence());
         if(isVisited(visitedByGoal, currAgentState.getCoordinate()) ||
            sameLocation(currAgentState, currGoalState)) {
             int agentXCo = currAgentState.getX();
             int agentYCo = currAgentState.getY();
             goalPath = visitedByGoal.get(agentXCo).get(agentYCo);
             agentPath = currAgentState.getSequence();
             agentFinalDir = currAgentState.getDirection();
             break;
         }
         considerChoices(currAgentState, visitedByAgent, agentToVisit);
         addToVisitedMap(visitedByAgent, currAgentState);
       }

       if(!goalToVisit.isEmpty()) {
         currGoalState = goalToVisit.poll();
         if(isVisited(visitedByAgent, currGoalState.getCoordinate()) ||
         sameLocation(currGoalState, currAgentState)) {
           int goalXCo = currGoalState.getX();
           int goalYCo = currGoalState.getY();
           agentPath = visitedByAgent.get(goalXCo).get(goalYCo);
           goalPath = currGoalState.getSequence();
           goalFinalDir = currGoalState.getDirection();
           System.out.println("Found by goal");
           break;
         }
         considerChoices(currGoalState, visitedByGoal, goalToVisit);
         addToVisitedMap(visitedByGoal, currGoalState);
       }
     }
    System.out.println("Agent path is " + agentPath);
    System.out.println("Goal path is " + goalPath);
     return combinePaths(agentPath, agentFinalDir, goalPath, goalFinalDir);
   }

   /**
    * Adds new states to the toVisit list.
    */
   private Queue<State> considerChoices(State currentState, Map<Integer, Map<Integer,
                                String>> visited, Queue<State>toVisit) {
     String actionSequence = currentState.getSequence();
     int prevDirection = currentState.getDirection();
     int numActions = currentState.getNumActions();
     int x = currentState.getX();
     int y = currentState.getY();
     char obj = getObjectInFront(x, y, prevDirection);
     for(char action: choiceOfMoves) {
       if(action == MOVE_FORWARD && (obj == UNKNOWN || obj == WATER ||
                                     obj == WALL     || obj == OUT   ||
                                     (obj == TREE && !inventory.get(AXE)) ||
                                     (obj == DOOR && !inventory.get(KEY)))) {
         continue;
       }
       Coordinate stateLoc = new Coordinate(x, y);
       State nextState = new State(stateLoc, prevDirection, numActions,
                                      actionSequence);
       if (obj == TREE) {
          nextState.addMove(CUT_DOWN);
       } else if (obj == DOOR) {
         nextState.addMove(OPEN_DOOR);
         // TODO: Stepping stone, crossing water.
       }
       nextState.addMove(action);
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
    * Adds new State to map.
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
    * Converts the search path from the goal state and combines with path from
    * agent.
    */
   private String combinePaths(String agentPath, int agentFinalDir,
                          String goalPath, int goalFinalDir) {
     char[] agentActions = agentPath.toCharArray();
     char[] goalActions = goalPath.toCharArray();
     if(agentActions.length == 0 && goalActions.length == 0) {
       return agentPath;
     }

     if(agentFinalDir == 5) {
       agentFinalDir = currDir;
       for(char action :agentActions) {
         if(action == TURN_LEFT) {
           agentFinalDir--;
           agentFinalDir = (agentFinalDir + 8) % 4;
         } else if (action == TURN_RIGHT) {
           agentFinalDir = agentFinalDir + 1 + 4;
           agentFinalDir %= 4;
         }
       }
     } else {
       goalFinalDir = NORTH;
       for(char action: goalActions) {
           if(action == TURN_LEFT) {
             goalFinalDir--;
             goalFinalDir = (goalFinalDir + 8) % 4;
           } else if (action == TURN_RIGHT) {
             goalFinalDir = goalFinalDir + 1 + 4;
             goalFinalDir %= 4;
           }
       }
     }
     StringBuilder pathBuilder = new StringBuilder(agentPath);
     // Create a joiner
     if((agentFinalDir + 2) % 4 != goalFinalDir) {
       if((agentFinalDir + 1) % 4 == goalFinalDir) {
         pathBuilder.append(TURN_LEFT);
       } else if((agentFinalDir - 1 + 4) % 4 == goalFinalDir) {
         pathBuilder.append(TURN_RIGHT);
       } else {
         System.out.println("WARNING: goal and path search error");
       }
     }
     StringBuilder goalReversed = new StringBuilder();
     for(int i = 0; i < goalActions.length; i++) {
       char action = goalActions[i];
       if(action == OPEN_DOOR || action == CUT_DOWN) {
         int numforwards = 0;
         StringBuilder toolUse = new StringBuilder();
         while(numforwards != 2 && i < goalActions.length) {
           action = goalActions[i];
           if(action == TURN_LEFT) {
             action = TURN_RIGHT;
           } else if(action == TURN_RIGHT) {
             action = TURN_LEFT;
           } else if(action == MOVE_FORWARD) {
             numforwards++;
           }
           toolUse.append(action);
           i++;
         }
         i--;
         goalReversed.insert(0, toolUse.toString());
       } else {
         if(action == TURN_LEFT) {
           action = TURN_RIGHT;
         } else if (action == TURN_RIGHT) {
           action = TURN_LEFT;
         }
         goalReversed.insert(0, action);
       }
     }
     pathBuilder.append(goalReversed);
     // Reverse string.
     return pathBuilder.toString();
   }

   /**
    * TODO: Move to be function in State.java.
    */
   private boolean sameLocation(State a, State b) {
     return a.getX() == b.getX() && a.getY() == b.getY();
   }

   /**
    * Manhattan distance from agent to coordinate.
    * Ignores obstacles along the way.
    * Can modify to include agent turn moves.
    */
   private int calculateManhattan (Coordinate destination) {
     int destX = destination.getX();
     int destY = destination.getY();
     return Math.abs(currRow - destX) + Math.abs(currCol - destY);
   }

   /**
    * Given two coordinates and a direction, finds what is in front of this
    * position.
    */
   private char getObjectInFront(int xInWorld, int yInWorld, int direction) {
       switch (direction) {
         case Agent.NORTH:
           xInWorld--;
           break;
         case Agent.EAST:
           yInWorld++;
           break;
         case Agent.SOUTH:
           xInWorld++;
           break;
         case Agent.WEST:
           yInWorld--;
           break;
       }
  		if(xInWorld < 0 || xInWorld >= WORLD_MAP_LENGTH ||
         yInWorld < 0 || yInWorld >= WORLD_MAP_LENGTH) {
  			return WALL;
  		} else {
  			return worldMap[xInWorld][yInWorld];
  		}
   }

   /**
    * Updates agent with it's own location for the move it's about to make.
    * Trusting that AI doesn't make any false moves.
    * AI attempting to walk through wall will break it's worldMap.
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
     // YOU GOT AN ITEM.
     char obj = getObjectAtPoint(new Coordinate(currRow, currCol));
     switch(obj) {
      case GOLD: case AXE: case KEY:
      spottedTools.remove(obj);
      inventory.put(obj,true);
      break;
      case STEPPING_STONE:
      numSteppingStones++;
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
