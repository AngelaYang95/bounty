/*
 * State.java
 *
 * 06/05/2016
 */

/**
 * Keeps x and y map coordinates.
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

	/**
	 * Changes the current coordinate so that it is one unit in a direction.
	 * @param direction 0-3, representing North to West
	 */
	public void takeStep(int direction) {
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
		   other.getY() == this.y) {
			return true;
		}
		return false;
	}

	@Override
	public int hashCode() {
	 return (x * 31) ^ y;
 	}
}
