package core;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.PriorityQueue;

import core.memory.Memory;

/**
 * Represents a hierarchically arranged group of neurons.
 * 
 * Specific functionality includes the ability to update given new 
 * input data or to add new neurons.
 *
 */
public class NeuronHierarchy implements Serializable {
	private static final long serialVersionUID = -5099893866679640130L;
	public static final int MIN_SCORE = 2;
	Neuron[] neuronBase;
	Neuron[] levelZero;
	ArrayList<LinkedList<Neuron>> hierarchy = new ArrayList<LinkedList<Neuron>>();
	Hashtable<Long,Neuron> neuronRegistry = new Hashtable<Long,Neuron>();
	Memory mem;
	long neuronCount = 0;
	long currentNeurons = 0;
	
	PriorityQueue<Neuron> q = new PriorityQueue<Neuron>(10, new Neuron.LevelComparator());
	Hashtable<Neuron,Boolean> seen = new Hashtable<Neuron,Boolean>();
	
	void setMemory (Memory memory) {
		mem = memory;
	}
	
	/**
	 * This method is called rarely and will search through the full hierarchy of 
	 * neurons, removing those who have not been used.
	 */
	public void deleteUnusedNeurons () {
		ListIterator<LinkedList<Neuron>> it = hierarchy.listIterator();
		while (it.hasNext()) {
			ListIterator<Neuron> level = it.next().listIterator();
			while (level.hasNext()) {
				Neuron n = level.next();
				if (n.getHeight() <= 0)
					continue;
				if (n.dead || n.getScore() < MIN_SCORE && n.lastFiringTime < mem.getCurrentTimeStep() - Memory.DELETE_INTERVAL) {
					n.foundation = null;
					for (Neuron parent : n.parents)
						parent.dead = true;
					n.parents = null;
					n.firingQueue = null;
					
					mem.remove(n);
					
					level.remove();
					neuronRegistry.remove(n);
					currentNeurons--;
				} 
				//else n.setScore(n.getScore() - MIN_SCORE);
			}
		}
	}
	
	/**
	 * The method for updating the hierarchy. This only updates
	 * the neurons who are firing, thus saving lots of time when dealing
	 * with large networks.
	 */
	void updateHierarchy () {		
		for (Neuron base : neuronBase)
			if (base.firing())
				q.add(base);
		
		while (!q.isEmpty()) {
			Neuron n = q.remove();
			n.update();
			if (n.firing())
				for (Neuron parent : n.getParents())
					if (!seen.containsKey(parent)) {
						q.add(parent);
						seen.put(parent, true);
					}
		}
		mem.endStep();
		seen.clear();
	}
	
	/**
	 * Adds a new neuron to our hierarchy at a height
	 * specified by its height field.
	 */
	public void addNeuron (Neuron n) {
		int height = n.height;
		if (hierarchy.size() <= height)
			hierarchy.add(new LinkedList<Neuron>());
		hierarchy.get(height).add(n);
		neuronRegistry.put(n.id,n);
		currentNeurons++;
	}
	
	/**
	 * Creates a new neuron.
	 * @param height The height or level of neuron on the hierarchy
	 * @param foundation The neurons below this neuron which feed and activate it
	 * @param delays The amount of delay between each of the foundation neurons
	 * @return A new neuron ready for use
	 */
	public Neuron createNeuron (Neuron[] foundation, Integer[] delays) {
		int height = findMaxHeight(foundation) + 1;
		Neuron newN = new Neuron (neuronCount++, height, foundation, delays, mem);
		return newN;
	}
	
	/**
	 * Finds the max Height of the foundation neurons
	 */
	int findMaxHeight(Neuron[] foundation) {
		if (foundation == null || foundation.length == 0) return -2;
		int max = Integer.MIN_VALUE;
		for (Neuron n : foundation) {
			if (n.height > max)
				max = n.height;
		}
		return max;
	}
	
	/**
	 * Creates a two-level neuron array:
	 * 
	 * o o o o -- First level of neurons controlled by NeuronHierarchy
	 * | | | |
	 * D D D D -- Base sensory array controlled by Sensory Relay
	 * {Sense}
	 * 
	 * We pump the base directly with data from the sense. The higher
	 * level is fed directly with zero delay from the base array.
	 */
	public Neuron[] createInitialNeurons (int num) {
		int inputlen = num;
		neuronBase = new Neuron[num];
		
		createBaseNeurons(inputlen);
		
		// Create Zero Level Neurons
		for (int i = 0; i < inputlen; i++) {
			Neuron realNeuron = createNeuron(new Neuron[] {neuronBase[i]}, new Integer[] {0});
			addNeuron(realNeuron);
		}
		return neuronBase;
	}
	
	/**
	 * Creates the base level of neurons meant to reside below level
	 * zero and be manually controlled by the sensory relay.
	 * 
	 * We do not wish to count these neurons and thus the 
	 * counts are all reset.
	 */
	void createBaseNeurons (int num) {
		long initNC = neuronCount;
		long initCN = currentNeurons;
		
		// Create Base Leve Neurons
		for (int i = 0; i < num; i++) {
			Neuron baseNeuron = createNeuron(new Neuron[0], new Integer[0]);
			neuronBase[i] = baseNeuron;
		}
		
		// Ignore the base neurons created
		neuronCount = initNC;
		currentNeurons = initCN;
	}
	
	public Neuron[] getBase () {
		return neuronBase;
	}
	
	public ArrayList<LinkedList<Neuron>> getHierarchy () {
		return hierarchy;
	}
	
	public long getNeuronCount () {
		return currentNeurons;
	}
	
	public Neuron getNeuronByID (long id) {
		if (neuronRegistry.containsKey(id))
			return neuronRegistry.get(id);
		return null;
	}
}
