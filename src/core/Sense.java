package core;

import java.io.Serializable;

/**
 * A sense gives sensory input to our neuron model.
 */
public interface Sense extends Serializable {
	/**
	 * Returns the sensory input in the form of 
	 * a boolean array.
	 */
	boolean[] getInput ();
	
	/**
	 * Returns the length of the sensory input array.
	 */
	int getInputLength ();
}
