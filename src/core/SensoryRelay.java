package core;

import haus.util.WrappedList;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;

import core.memory.Memory;
import core.memory.MemoryManager;
import core.memory.PatternMatcher;

/**
 * The sensory relay transmits data between the sense
 * and the base level of the neuron hierarchy. It is 
 * also responsible for stepping through time and coordinating
 * the activities of different parts of the Core.
 */
public class SensoryRelay implements Serializable {
	private static final long serialVersionUID = 139637820107177372L;
	
	/**
	 * Every Delete_Interval turns, memory is pruned, deleting those 
	 * neurons which have not fired frequently enough.
	 */
	public static final int DELETE_INTERVAL = 5000;
	
	/**
	 * Source of sensory data.
	 */
	private Sense sense;
	
	/**
	 * The data structure storing the neurons
	 */
	private NeuronHierarchy hier;
	
	/**
	 * This class is responsible for managing the 
	 * memory to record neuron firings.
	 */
	private MemoryManager memoryManager;
	
	/**
	 * The Memory to be managed.
	 */
	private Memory memory;
	
	/**
	 * The pattern matcher will create new neurons
	 */
	private PatternMatcher patternMatcher;
	
	/**
	 * Stores neurons linked directly to the sensory signal
	 */
	private Neuron[] base;
	
	/**
	 * This time keeper is distributed to all others.
	 */
	private TimeKeeper timeKeeper = new TimeKeeper();
	
	
	//<><(8)><>//
	
	
	/**
	 * Constructs a new neuron hierarchy to accompany the sense and scorer.
	 */
	public SensoryRelay (Sense _sense) {
		sense = _sense;
		
		patternMatcher = new PatternMatcher(timeKeeper);
		memory = new Memory(timeKeeper, patternMatcher);
		memoryManager = new MemoryManager(timeKeeper, memory);
		hier = new NeuronHierarchy(timeKeeper, memory, memoryManager);
		
		patternMatcher.setMemory(memory);
		patternMatcher.setMemoryManager(memoryManager);
		patternMatcher.setHierarchy(hier);
		
		base = hier.createInitialNeurons(sense.getInputLength());
	}
		
	/**
	 * Move through another time slice:
	 * 
	 * 1. Get sensory input data
	 * 2. Update our base array
	 * 3. Cue NeuronHierarchy to update all neurons
	 */
	public void step () {
		timeKeeper.step();
		memoryManager.startStep();
		
		updateBaseLevel();
		hier.updateHierarchy();
		
		memoryManager.endStep();
		
		
		if (!hier.foundNeuronCap()) {
			patternMatcher.doPatternMatch();
		
			hier.checkForNeuronCap();
			
			if (timeKeeper.getTime() % DELETE_INTERVAL == 0)
				hier.deleteUnusedNeurons();
		}
	}
	
	private void updateBaseLevel () {
		boolean[] input = sense.getInput();
		for (int i = 0; i < input.length; i++)
			if (input[i])
				base[i].setFiring();
	}
	
	//<><(Methods forwarded for the sake of Core)><>//
	
	public ArrayList<LinkedList<Neuron>> getNeuronHierarchy() {
		return hier.getHierarchy();
	}
	
	public long getNeuronCount () {
		return hier.getNeuronCount();
	}
	
	public String getMemoryRepresentation () {
		return memory.toString();
	}
	
	public Neuron getNeuronByID (long id) {
		return hier.getNeuronByID(id);
	}
	
	public WrappedList<Integer> getNeuronFirings (Neuron n) {
		return memory.getNeuronFirings(n);
	}
	
	public int getTime () {
		return timeKeeper.getTime();
	}
}
