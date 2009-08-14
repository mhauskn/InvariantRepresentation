package core;

import java.io.Serializable;

/**
 * The TimeKeeper keeps track of time
 */
public class TimeKeeper implements Serializable {
	private static final long serialVersionUID = 2910682726745688029L;
	
	/**
	 * The current time step
	 */
	private int time = -1;
	
	/**
	 * Returns the current time step
	 */
	public int getTime () {
		return time;
	}
	
	/**
	 * Increments time
	 */
	public void step () {
		time++;
	}
	
	public String toString () {
		return "CurrentTime: " + time;
	}
}
