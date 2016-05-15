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
   private boolean hasAxe;
   private boolean hasKey;
   private boolean hasGold;
   private int numSteppingStones;
   // Only tracks location of 1 of each type atm.
   private Map<Character, Coordinate> spottedEquipment = new HashMap<>();

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
	   hasAxe = false;
	   hasKey = false;
	   hasGold = false;
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

	  // Decision making for what action to take.
	  // Currently explores new territory trying not to die.
    // Reverts to random movement if no new terriory reachable.
    char action;
    if(journey.isEmpty()) {
      String path;
      if(hasGold) {
        path = findPathToCoordinate(initialLocation);
      } else if (goldSeen) {
        path = findPathToCoordinate(goldPosition);
        System.out.println("GOLD");
      } else {
        path = conquerTerritory();
        System.out.println("GOnquering");
      }

      if(path == "") {
        System.out.println("RANDOM");
        action = getRandomAction();
      } else {
        char[] pathArray = path.toCharArray();
        for(char pathAction: pathArray) {
          journey.add(pathAction);
          System.out.print(pathAction);
        }
        if(!goldSeen) {
          journey.removeLast();
        }
        action = journey.removeFirst();
      }
    } else {
      System.out.println("CONTINUING JOURNEY");
      action = journey.removeFirst();
    }
	  updateNewPosition(action);
	  return action;
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
            if(itemInFront == TREE && hasAxe) {
                action = CUT_DOWN;
            } else if(itemInFront == DOOR && hasKey) {
                action = OPEN_DOOR;
            }
         }
     }
     return action;
   }

   /**
    * Scans the given 5x5 view and updates AI's map with information.
    * TODO: Change so it doesn't constantly rescan already know information.
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
           spottedEquipment.put(value, location);
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
    currentState = new State(currentPoint, (currDir + 2) % 4, 0, "rr");
    toVisit.add(currentState);
    String path = "";
    // BFS
    while(!toVisit.isEmpty()) {
        currentState = toVisit.poll();
        Coordinate location = currentState.getCoordinate();
        String actionSequence = currentState.getSequence();
        if(getObjectAtPoint(location) == UNKNOWN) {
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
    */
   private String findPathToCoordinate(Coordinate destination) {
     // Structures needed for Agent's side of search.
     Map<Integer, Map<Integer, String>> visitedByAgent = new HashMap<>();
     Queue<State> agentToVisit = new PriorityQueue<State>();
     Coordinate agentCurrPoint = new Coordinate(currRow, currCol);
     State currAgentState = new State(agentCurrPoint, currDir, 0, "");
     agentToVisit.add(currAgentState);
     currAgentState = new State(agentCurrPoint, (currDir + 2) % 4, 0, "rr");
     agentToVisit.add(currAgentState);
     String agentPath = "";

     // Structures needed for Goal side of search.
     Map<Integer, Map<Integer, String>> visitedByGoal = new HashMap<>();
     Queue<State> goalToVisit = new PriorityQueue<State>();
     // TODO: Fix "direction" for goal.
     State currGoalState = new State(destination, NORTH, 0, "");
     goalToVisit.add(currGoalState);
     String goalPath = "";

     while(!agentToVisit.isEmpty() && !goalToVisit.isEmpty()) {
       if(!agentToVisit.isEmpty()) {
         currAgentState = agentToVisit.poll();
         if(isVisited(visitedByGoal, currAgentState.getCoordinate()) ||
            sameLocation(currAgentState, currGoalState)) {
             int agentXCo = currAgentState.getX();
             int agentYCo = currAgentState.getY();
             goalPath = visitedByGoal.get(agentXCo).get(agentYCo);
             agentPath = currAgentState.getSequence();
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
           break;
         }
         considerChoices(currGoalState, visitedByGoal, goalToVisit);
         addToVisitedMap(visitedByGoal, currGoalState);
       }
     }
    System.out.println("agent path is " + agentPath);
    System.out.println("goal path is " + goalPath);
     return combinePaths(agentPath, goalPath);
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
       if((action == MOVE_FORWARD) && (obj == WATER || obj == WALL ||
                                       obj == TREE  || obj == DOOR ||
                                       obj == OUT)) {
         //check it doesn't kill agent.
         // TODO: modify so agent can check against tools it owns.
       } else {
         Coordinate stateLoc = new Coordinate(x, y);
         State nextState = new State(stateLoc, prevDirection, numActions,
                                        actionSequence);
         nextState.addMove(action);
         if(!isVisited(visited, nextState.getCoordinate())) {
           toVisit.add(nextState);
         }
       }
     }
     return toVisit;
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
            visited.get(x).put(y, path);
          } else {
            Map<Integer, String> yToPath = new HashMap<>();
            yToPath.put(y, path);
            visited.put(x, yToPath);
          }
          return visited;
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
    * Converts the search path from the goal state and combines with path from
    * agent.
    */
   private String combinePaths(String agentPath, String goalPath) {
     char[] goalActions = goalPath.toCharArray();
     StringBuilder pathBuilder = new StringBuilder(agentPath);
     // Don't add first action which will overlap with agentPath.
     for(int i = goalActions.length - 1; i >= 0; i--) {
       char action = goalActions[i];
       switch(action) {
       case TURN_LEFT:
        action = TURN_RIGHT;
        break;
       case TURN_RIGHT:
        action = TURN_LEFT;
        break;
       }
       pathBuilder.append(action);
     }
     return pathBuilder.toString();
   }

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
     Coordinate pointInFront = getCoordinateNextdoor(xInWorld, yInWorld, direction);
     xInWorld = pointInFront.getX();
     yInWorld = pointInFront.getY();
  		if(xInWorld < 0 || xInWorld >= WORLD_MAP_LENGTH ||
         yInWorld < 0 || yInWorld >= WORLD_MAP_LENGTH) {
  			return WALL;
  		} else {
  			return worldMap[xInWorld][yInWorld];
  		}
   }

   /**
    * Gives coordinate next to point in a given direction.
    */
   private Coordinate getCoordinateNextdoor(int xInWorld, int yInWorld, int direction) {
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
     return new Coordinate(xInWorld, yInWorld);
   }

   /**
    * Updates agent with it's own location for the move it's about to make.
    * Trusting that AI doesn't make any false moves.
    * AI attempting to walk through wall will break it's worldMap.
    */
   private void updateNewPosition(char action) {
	   if(action == TURN_LEFT) {
       currDir = ((currDir - 1) + 8) % 4;  // Java's % gives only remainder.
     } else if (action == TURN_RIGHT) {
		   currDir = ((currDir + 1) + 4) % 4;
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
       // YOU GOT GOLD.
       char obj = getObjectInFront(currRow, currCol, currDir);
       if(obj == GOLD) {
         hasGold = true;
         System.out.println("YOU GOT THE GOLD");
       }
	   } else {
       // Catching actions for cutting tree etc.
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
         System.out.println("Could not bind to port: "+port);
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
