package senses.basic;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import core.Sense;
import haus.io.FileReader;

/**
 * Looks for patterns in text at a letter by letter level
 */
public class WordSense implements Sense {
	private static final long serialVersionUID = -3135178809617441516L;
	transient FileReader freader = new FileReader("ap");
	String line = freader.getNextLine();
	int lineIndex = 0;
	
	public boolean[] getInput() {
		if (lineIndex >= line.length()) {
			line = freader.getNextLine();
			lineIndex = 0;
			if (line == null) {
				System.out.println("Done");

				return new boolean[getInputLength()];
			}
		}
		
		char c = line.charAt(lineIndex++);
		int i = (int) c;
		
		boolean[] out = new boolean[getInputLength()];
		out[i] = true;
		
		return out;
	}

	public int getInputLength() {
		return 128;
	}
	
	private void writeObject (ObjectOutputStream out) throws IOException {
		out.defaultWriteObject();
	}
	
	private void readObject (ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		freader = new FileReader("ap");
	}
}
