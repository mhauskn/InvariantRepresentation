package senses.basic;

import core.Sense;

public class TrueSense implements Sense {
	private static final long serialVersionUID = -785533035958263007L;

	public boolean[] getInput() {
		return new boolean[] {true,true,true,true};
	}

	public int getInputLength() {
		return 4;
	}
	
}
