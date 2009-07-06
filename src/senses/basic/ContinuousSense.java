package senses.basic;

import core.Sense;

public class ContinuousSense implements Sense {
	/**
	 * 
	 */
	private static final long serialVersionUID = 3444418014863445155L;
	
	int cnt = 0;
	boolean mod = false;
	
	public boolean[] getInput() {
		if (cnt++ % 4 == 0) {
			mod = !mod;
			return new boolean[] {false, mod, !mod};
		}else
			return new boolean[] {true, false, false};
	}

	public int getInputLength() {
		return 3;
	}
}
