package senses.basic;

import core.Sense;

/**
 * Builds up a simple hierarchy
 */
public class BuildingSense implements Sense {
	/**
	 * 
	 */
	private static final long serialVersionUID = -4384842164985440037L;
	int cnt = 0;
	
	public boolean[] getInput() {
		if (cnt++ % 5 == 0)
			return new boolean[] {false};
		return new boolean[] {true};
	}

	public int getInputLength() {
		return 1;
	}
	
}
