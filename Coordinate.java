import java.util.Map;
/**
 * Structure to hold a potential position in map.
 */
public class Coordinate {

	private int x;
	private int y;
	private int direction;
	
	public Coordinate(int x, int y, int direction) {
		this.x = x;
		this.y = y;
		this.direction = direction;
	}

	public int getX() {
		return x;
	}
	
	public int getY() {
		return y;
	}
	
	public int getDirection() {
		return direction;
	}
	
	public int getDir() {
		return direction;
	}
	
	public void turnLeft() {
		direction--;
		direction %= 4;
	}
	
	public void turnRight() {
		direction++;
		direction %= 4;
	}
	
	public void moveForward() {
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
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj == null) {
			return false;
		}
		
		if(obj.getClass() != this.getClass()) {
			return false;
		}
		Coordinate other = (Coordinate) obj;
		if(other.getX() == this.x && 
		   other.getY() == this.y && 
		   other.getDirection() == this.direction) {
			return true;
		}
		return false;
	}
	
}