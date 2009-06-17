package senses.basic;

import core.Sense;

public class RandomSense implements Sense {
	private static final long serialVersionUID = -1740707931956018399L;
	public static final int size = 2;

	public boolean[] getInput() {
		boolean[] arr = new boolean[size];
		for (int i = 0; i < size; i++) {
			int num = (int)(Math.random() * 2);
			if (num == 0)
				arr[i] = true;
			else
				arr[i] = false;
		}
		return arr;
	}

	public int getInputLength() {
		return size;
	}

}
