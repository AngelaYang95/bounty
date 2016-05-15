import java.util.Map;
/**
 * Structure to hold a position in map.
 */
public class Coordinate {

	private int x;
	private int y;

	public Coordinate(int x, int y) {
		this.x = x;
		this.y = y;
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public void takeStep(int currDir) {
		switch (currDir) {
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
		   other.getY() == this.y) {
			return true;
		}
		return false;
	}

}
