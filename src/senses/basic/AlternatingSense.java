package senses.basic;

import core.Sense;

public class AlternatingSense implements Sense {
	private static final long serialVersionUID = 4675077384942212609L;
	boolean a = false;
	
	public boolean[] getInput() {
		a = !a;
		return new boolean[] {a};
	}

	public int getInputLength() {
		return 1;
	}

}
