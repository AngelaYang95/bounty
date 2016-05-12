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

   final static int EAST   = 0;
   final static int NORTH  = 1;
   final static int WEST   = 2;
   final static int SOUTH  = 3;
   
   final static int WORLD_MAP_LENGTH = 159;
   final static int ENV_MAX_LENGTH = 80;
   final static int WINDOW_SIZE = 5;
   final static int AGENT_START_INDEX = 79;
   final static int VIEW_OFFSET = 2;
   
   final static char UNKNOWN = '?';
   final static char MOVE_FORWARD = 'f';
   final static char TURN_LEFT = 'l';
   final static char TURN_RIGHT = 'r';
   final static char STEPPING_STONE = 'o';
   final static char GOLD = 'g';
   final static char TREE = 'T';
   final static char WALL = '*';
   final static char WATER = '~';
   final static char SPACE = ' ';
      
   private char[][] worldMap = new char[WORLD_MAP_LENGTH][WORLD_MAP_LENGTH]; 
   private int initialRow, initialCol;
   char[] choiceOfMoves = {MOVE_FORWARD, TURN_LEFT, TURN_RIGHT};
   
   // Current position and direction of the agent.
   private int currRow,currCol,currDir;
  
   // Equipment the agent is currently holding.
   private boolean hasAxe;
   private boolean hasKey;
   private boolean hasGold;
   private int numSteppingStones; 
   
   private Coordinate goldPosition;
   private Queue<Character> journey = new LinkedList<>();
   
   public Agent() {
	   initialRow = currRow = AGENT_START_INDEX;
	   initialCol = currCol = AGENT_START_INDEX;
	   currDir = NORTH;
	   hasAxe = false;
	   hasKey = false;
	   hasGold = false;
	   numSteppingStones = 0;
	   
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
	  // Currently just randomly looks around trying not to die.
	  char action;
	  action = conquerTerritory();
	  updateNewPosition(action);
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
			   if(value == GOLD && goldPosition == null) {
			   		// INCORRECT. NEDEDS TO BE TURNED INTO WORLD MAP INDEX.
			   		goldPosition = new Coordinate(i,j, NORTH); 
			   }
		   }
	   }
   }
   
   /**
    * Updates the world map with new information on the environment.
    */
   private void updateWorld(int xInView, int yInView, char value) {
	   // Rotate the view and its indices to face NORTH.
	   switch(currDir) {
	   		case EAST:
	   			xInView = WINDOW_SIZE - xInView;
	   			break;
	   		case SOUTH:
	   			xInView = WINDOW_SIZE - xInView;
	   			yInView = WINDOW_SIZE - yInView;
	   			break;
	   		case WEST:
	   			yInView = WINDOW_SIZE - yInView;
	   }
	   
	   // Calculate the corresponding indices in world map.
	   int rowInWorld = xInView - VIEW_OFFSET + currRow;
	   int colInWorld = yInView - VIEW_OFFSET + currCol;
	   worldMap[rowInWorld][colInWorld] = value;
   }
   
   /**
    * Basic strategy to get AI to explore new areas of the environment.
    * TODO: change to iterative Deepening Search/ BFS for UNKNOWN.
    */
   private char conquerTerritory() {
	  Random rn = new Random();
	  char action = choiceOfMoves[rn.nextInt(3)];
 	  if(action == MOVE_FORWARD) {
		  char itemInFront = getObjectInFront(currRow, currCol, currDir);
		  if(itemInFront == WATER) {	// DON'T DIE.
		  	action = TURN_LEFT;
		  }
	  }
	  return action;
   }
   
   /**
    * Given two coordinates and a direction, finds what is in front of this position.
    */
   private char getObjectInFront(int x, int y, int direction) {
		switch (direction) {
		case Agent.NORTH:
			x--;
			break;
		case Agent.EAST:
			y++;
			break;
		case Agent.SOUTH:
			x++;
			break;
		case Agent.WEST:
			y--;
			break;
		}
		
		if(x < 0 || x >= WORLD_MAP_LENGTH || y < 0 || y >= WORLD_MAP_LENGTH) {
			return WATER;
		} else {
			return worldMap[x][y];
		}
   }
   
   /**
    * Updates agent with it's own location for the move it's about to make.
    */
   private void updateNewPosition(char action) {
	   if(action == TURN_LEFT || action == TURN_RIGHT) {
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
            agent.show_world();
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
   		for(char[] row: worldMap) {
   			for(char c: row) {
   				System.out.print(c);
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
