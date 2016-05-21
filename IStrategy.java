/*
 * IStrategy.java
 *
 * 18/05/2016
 */

/**
 * Interface for a class of calculators that give a State's costs.
 */
public interface IStrategy {
	public int calcHCost(State child);
}
