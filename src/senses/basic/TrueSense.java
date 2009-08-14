package senses.basic;

import core.Sense;

public class TrueSense implements Sense {
	private static final long serialVersionUID = -785533035958263007L;
	boolean on = true;
	int count = 0;
	int blackout = 0;
	
	public boolean[] getInput() {
		on = !on;
		count++;
		if (count == 10) {
			count = 0;
			blackout = 0;
		}
		while (blackout++ < 10) {
			count = 0;
			return new boolean[]{false};
		}
		return new boolean[] {on};
	}

	public int getInputLength() {
		return 1;
	}
	
}
