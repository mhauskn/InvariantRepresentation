package core;

import java.io.Serializable;

import core.memory.Memory;

/**
 * This class will manage and organize our 
 * neurons. It is responsible for keeping track
 * of the flow of time and updating all of our
 * neurons.
 */
public class SensoryRelay implements Serializable {
	private static final long serialVersionUID = 139637820107177372L;
	Sense sense; // Sensory data source
	NeuronHierarchy hier;
	Neuron[] base; // Stores neurons linked only to sensory signal 
	
	/**
	 * Constructs a new neuron hierarchy to accompany the sense and scorer.
	 */
	public SensoryRelay (Sense _sense) {
		sense = _sense;
		hier = new NeuronHierarchy();
		Memory mem = new Memory();
		hier.setMemory(mem);
		mem.setHierarchy(hier);
		
		base = hier.createInitialNeurons(sense.getInputLength());
	}
	
	/**
	 * Uses the specified neuron hierarchy as well as the specified sense
	 * and scorer. 
	 */
	public SensoryRelay (Sense _sense, NeuronHierarchy _hier) {
		sense = _sense;
		hier = _hier;
		
		base = hier.getBase();
	}
		
	/**
	 * Move through another time slice:
	 * 
	 * 1. Get sensory input data
	 * 2. Update our base array
	 * 3. Cue NeuronHierarchy to update all neurons
	 */
	public void step () {
		boolean[] input = sense.getInput();
		for (int i = 0; i < input.length; i++)
			if (input[i])
				base[i].setFiring();
		hier.updateHierarchy();
	}
}
