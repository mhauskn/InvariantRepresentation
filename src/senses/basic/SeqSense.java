package senses.basic;

import core.Sense;

public class SeqSense implements Sense {
	private static final long serialVersionUID = -5511636181351143871L;
	long count = 0;
	
	public boolean[] getInput() {
		count++;
		if (count %3 == 0) {
			return new boolean[] {true,false,false};
		} else if (count %3 == 1) {
			return new boolean[] {false,true,false};
		} else {
			return new boolean[] {false,false,true};
		}
	}

	public int getInputLength() {
		return 3;
	}

}
