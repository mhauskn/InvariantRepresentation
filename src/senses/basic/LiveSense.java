package senses.basic;

import core.Sense;

/**
 * This sense will never go "dead" because it
 * has an extra neuron which fires whenever all
 * normal neurons go dead.
 *
 */
public class LiveSense implements Sense {
	private static final long serialVersionUID = 4899754387293132478L;
	public static final int size = 3;

	public boolean[] getInput() {
		boolean alive = false;
		boolean[] arr = new boolean[size];
		for (int i = 0; i < size-1; i++) {
			int num = (int)(Math.random() * 2);
			if (num == 0) {
				arr[i] = true;
				alive = true;
			} else
				arr[i] = false;
		}
		
		arr[arr.length-1] = !alive;
		return arr;
	}

	public int getInputLength() {
		return size;
	}
}
