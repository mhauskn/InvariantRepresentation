package core;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.PriorityQueue;

import core.memory.Memory;
import core.memory.MemoryManager;

/**
 * Represents a hierarchically arranged group of neurons.
 * 
 * Specific functionality includes the ability to update given new 
 * input data or to add new neurons.
 *
 */
public class NeuronHierarchy implements Serializable {
	private static final long serialVersionUID = -5099893866679640130L;
	
	/**
	 * Minimum score to not be deleted
	 */
	private static final int MIN_SCORE = 2;
	
	/**
	 * The level of neurons fed directly by sensory input
	 */
	private Neuron[] neuronBase;
	
	/**
	 * The level based hierarchical structure of neurons
	 */
	private ArrayList<LinkedList<Neuron>> hierarchy = new ArrayList<LinkedList<Neuron>>();
	
	/**
	 * An index of neurons by thier id
	 */
	private Hashtable<Long,Neuron> neuronRegistry = new Hashtable<Long,Neuron>();
	
	/**
	 * The total number of neurons created. This is incremented 
	 * to create IDs for new neurons.
	 */
	private long neuronCount = 0;
	
	/**
	 * The number of active neurons. This is incremented as neurons
	 * are added to the network.
	 */
	private long currentNeurons = 0;
		
	private TimeKeeper timeKeeper = null;
	
	/**
	 * A memory unit linked to this neuron hierarchy
	 */
	private Memory memory;
	
	/**
	 * A memory manager to manage the memory linked to this hierarchy.
	 */
	private MemoryManager memoryManager;
	
	private boolean childFiring = false;
	
	private int childFiringCount = 0;
	
	private Neuron suspectedCap = null;
	
	private Neuron monitoredChild = null;
	
	private boolean foundNeuronCap = false;
	
	private static final int MIN_CYCLE_COUNT = 2;
	
	
	//<><(8)><>//
	
	
	public NeuronHierarchy (TimeKeeper _tk, Memory _mem, MemoryManager _memManager) {
		timeKeeper = _tk;
		memory = _mem;
		memoryManager = _memManager;
	}
	
	/**
	 * This method is called rarely and will search through the full hierarchy of 
	 * neurons, removing those who have not been used enough to surpass a given 
	 * threshold.
	 */
	public void deleteUnusedNeurons () {
		ListIterator<LinkedList<Neuron>> it = hierarchy.listIterator();
		while (it.hasNext()) {
			ListIterator<Neuron> level = it.next().listIterator();
			while (level.hasNext()) {
				Neuron n = level.next();
				if (n.getHeight() <= 0)
					continue;
				if (n.dead() || n.getScore() < MIN_SCORE && 
						n.getLastFiringTime() < timeKeeper.getTime() - SensoryRelay.DELETE_INTERVAL) {
					n.kill();
					memory.remove(n);
					level.remove();
					neuronRegistry.remove(n);
					currentNeurons--;
				} 
				//else n.setScore(n.getScore() - MIN_SCORE);
			}
		}
	}
	
	/**
	 * Traverse from top down checking for a neuron cap
	 */
	public void checkForNeuronCap () {
		if (suspectedCap != null)
			return;
		
		suspectedCap = findPerpetualFiringNeuron();
		
		if (suspectedCap == null)
			return;
							
		monitoredChild = findNonFiringChild(suspectedCap);
		
		if (monitoredChild == null) {
			suspectedCap = null;
			return;
		}
	}
	
	public boolean foundNeuronCap () {
		return foundNeuronCap;
	}
	
	private Neuron findNonFiringChild (Neuron suspectedCap) {
		Neuron child;
		
		while (suspectedCap.getHeight() > 0) {
			ArrayList<Neuron> children = suspectedCap.getChildren();
			
			if (!singleUniqueChild(children))
				return null;
			
			child = children.get(0);
			
			if (child.getLastNonFiringTime() >= 0) {
				assert child.getParents().size() == 1;
				childFiringCount = 0;
				return child;
			}
			
			suspectedCap = child;
		}
		return null;
	}
	
	private boolean singleUniqueChild (ArrayList<Neuron> children) {
		Neuron child = children.get(0);
		for (int i = 1; i < children.size(); i++) {
			Neuron otherChild = children.get(i);
			if (!child.equals(otherChild))
				return false;
		}
		return true;
	}
	
	private Neuron findPerpetualFiringNeuron () {
		for (int i = hierarchy.size() - 1; i >= 1; i--) {
			ListIterator<Neuron> level = hierarchy.get(i).listIterator();
			while (level.hasNext()) {
				Neuron n = level.next();
				if (n.hasNeverNotFired())
					return n;
			}
		}
		return null;
	}
	
	/**
	 * The method for updating the hierarchy. This only updates
	 * the neurons who are firing, thus saving lots of time when dealing
	 * with large networks.
	 */
	public void updateHierarchy () {
		//TODO: Establish how much faster we go with these two variables on a class scope
		PriorityQueue<Neuron> levelQueue = 
			new PriorityQueue<Neuron>(10, new Neuron.LevelComparator());
		Hashtable<Neuron,Boolean> seen = new Hashtable<Neuron,Boolean>();
		for (Neuron base : neuronBase)
			if (base.firing())
				levelQueue.add(base);
		
		while (!levelQueue.isEmpty()) {
			Neuron n = levelQueue.remove();
			n.update();
			if (n.firing()) {
				memoryManager.rememberFiringNeuron(n);
				for (Neuron parent : n.getParents())
					if (!seen.containsKey(parent)) {
						levelQueue.add(parent);
						seen.put(parent, true);
					}
			}
			if (monitoredChild != null && n.equals(monitoredChild))
				updateCycleTracking(n);
		}
		seen.clear();
	}
	
	private void updateCycleTracking (Neuron n) {
		if (n.getParents().isEmpty() || 
				n.getParents().get(0).getLastNonFiringTime() >= 0) {
			suspectedCap = null;
			monitoredChild = null;
			childFiringCount = 0;
			return;
		}
		
		if (n.firing()) {
			childFiring = true;
		} else {
			if (childFiring) {
				if (childFiringCount++ >= MIN_CYCLE_COUNT) {
					ArrayList<Neuron> parents = n.getParents();
					Neuron parent = parents.get(0);
					
					if (parent.hasNeverNotFired())
						reportCapNeuron(parent);
				}
			}
			childFiring = false;
		}
	}
	
	private void reportCapNeuron (Neuron cap) {
		suspectedCap = null;
		monitoredChild = null;
		foundNeuronCap = true;
	}
	
	/**
	 * Adds a new neuron to our hierarchy. When being added to
	 * the hierarchy, this neuron is given a unique id.
	 */
	public void addNeuron (Neuron n) {
		int height = n.getHeight();
		if (hierarchy.size() <= height)
			hierarchy.add(new LinkedList<Neuron>());
		hierarchy.get(height).add(n);
		
		n.setID(neuronCount++);
		if (n.getId() == 256)
			System.out.println();
		neuronRegistry.put(n.getId(), n);
		currentNeurons++;
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
	 * 
	 * This method is used only by the Sensory Relay.
	 */
	Neuron[] createInitialNeurons (int num) {
		int inputlen = num;
		neuronBase = new Neuron[num];
		
		createBaseNeurons(inputlen);
		
		// Create Zero Level Neurons
		for (int i = 0; i < inputlen; i++) {
			Neuron realNeuron = new Neuron(new Neuron[] {neuronBase[i]}, new Integer[] {0}, timeKeeper);
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
	private void createBaseNeurons (int num) {
		long initNC = neuronCount;
		long initCN = currentNeurons;
		
		// Create Base Leve Neurons
		for (int i = 0; i < num; i++) {
			Neuron baseNeuron = new Neuron(new Neuron[0], new Integer[0], timeKeeper);
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
