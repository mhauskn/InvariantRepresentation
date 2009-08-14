package senses.basic;

import core.Sense;

/**
 * Randomly sets one of the inputs to be true
 */
public class SingleRandomSense implements Sense {
	private static final long serialVersionUID = -5277550946761612351L;
	public static final int size = 128;

	public boolean[] getInput() {
		boolean[] arr = new boolean[size];
		int num = (int)(Math.random() * size);
		arr[num] = true;
		return arr;
	}

	public int getInputLength() {
		return size;
	}

}
