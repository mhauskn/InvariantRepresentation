package senses.basic;

import core.Sense;

public class PatternSense implements Sense {
	private static final long serialVersionUID = -8866842912295852826L;
	long count = 0;
	
	/**
	 * Delivers a simple pattern
	 */
	public boolean[] getInput() {
		if (count++ % 2 == 0)
			return new boolean[] { true, true, false, false};
		else
			return new boolean[] { false, false, true, true};
	}
	
	public int getInputLength () {
		return 4;
	}
}
