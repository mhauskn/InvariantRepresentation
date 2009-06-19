package core.memory;

import haus.util.WrappedList;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.Hashtable;

import core.Neuron;
import core.NeuronHierarchy;

/**
 * Memory handles the storing and accessing the firing patterns
 * of individual neurons. 
 */
public class Memory implements Serializable {
	private static final long serialVersionUID = 1086066787870727016L;
	
	public static final int DELETE_INTERVAL = 5000;
	
	/**
	 * The length of the memory
	 */
	public static final int MEM_SIZE = 1000;
	
	/**
	 * The current time step
	 */
	private int time = 0;

	/**
	 * The main memory which remembers which neurons
	 * fired at each moment. It really serves as a 
	 * queue where new memory is added to the end and 
	 * old memory is removed from the front.
	 */
	private WrappedList<Hashtable<Neuron,Boolean>> memory = 
		new WrappedList<Hashtable<Neuron,Boolean>>(MEM_SIZE+1);
		
	/**
	 * Stores neurons which fire off in this time step
	 */
	private Hashtable<Neuron,Boolean> snapshot = new Hashtable<Neuron,Boolean>();
	
	/**
	 * Serves to index the time at which each 
	 * neuron fired.
	 */
	private Hashtable<Neuron,WrappedList<Integer>> mem_index =
		new Hashtable<Neuron,WrappedList<Integer>>();

	NeuronHierarchy hier;

	PatternMatcher pmatcher;
	
	//<><()><>//
	
	/**
	 * Gives the memory a neuron hierarchy to work with. 
	 * This is done in this manner because both Memory and Hierarchy
	 * need references to each other and they cannot be simultaneously
	 * constructed.
	 */
	public void setHierarchy (NeuronHierarchy hierarchy) {
		hier = hierarchy;
		pmatcher = new PatternMatcher(this, hier);
	}
	
	/**
	 * Gets the hash table full of neurons which fire at a certain
	 * time step, desiredTime.
	 */
	public Hashtable<Neuron,Boolean> getFirings (int desiredTime) {
		if (!inRange(desiredTime))
			return null;
		
		int memStart = getCurrentTimeStep() - memory.size() + 1;
		
		if (time < memory.size())
			return memory.get(desiredTime);
		
		return memory.get(desiredTime - memStart);
	}
	
	/**
	 * Notifies memory of the end of this time slice. 
	 * There are several tasks to be performed:
	 * 
	 * 1. Check if a new neuron needs to be created
	 * 2. Create new snapshot for next time slice
	 * 3. Remove old memories if memory length is surpassed
	 * 4. Increment time slice counter
	 */
	public void endStep () {
		indexNewFirings();
		memory.add(snapshot);
		snapshot = new Hashtable<Neuron,Boolean>();
		
		if (memory.size() > MEM_SIZE) {
			Hashtable<Neuron,Boolean> old = memory.remove();
			removeForgottenFiringIndexes(old);
		}
		
		
		
		//checkMemoryConsistency();
		
		pmatcher.doPatternMatch();
		
		//checkMemoryConsistency();
		
		if (time % DELETE_INTERVAL == 0)
			hier.deleteUnusedNeurons();
		
		time++;
	}
	
	/**
	 * This method is called by a neuron which has just fired.
	 * 
	 * 1. This new neuron must be added to the current
	 * time-slice's snapshot. 
	 * 2. Remove memory of all children of this neuron firing
	 */
	public void rememberFiringNeuron (Neuron n) {
		snapshot.put(n, true);
		removeCurrentSubNeurons(n);
	}
	
	/**
	 * Removes all references to a neuron
	 */
	public void remove (Neuron n) {
		if (!mem_index.containsKey(n))
			return;
		WrappedList<Integer> firings = mem_index.get(n);
		while (!firings.isEmpty()) {
			int firing = firings.remove();
			if (inRange(firing))
				getFirings(firing).remove(n);
		}
		mem_index.remove(n);
	}
	
	/**
	 * This a method meant primarily for debugging purposes. It 
	 * checks the consistency of the memory with the mem_index
	 */
	void checkMemoryConsistency () { 
		// Checks that each neuron in every timestep has 
		// a corresponding 
		for (int i = 0; i < memory.size(); i++) {
			Integer currTime = time - i;
			Hashtable<Neuron,Boolean> slice = getFirings(currTime);
			Enumeration<Neuron> e = slice.keys();
			while (e.hasMoreElements()) {
				Neuron n = e.nextElement();
				WrappedList<Integer> firings = getNeuronIndex(n);
				if (firings.indexOf(currTime) < 0) {
					System.out.println("Memory Inconsistent!");
					System.exit(1);
				}
			}
		}
		
		// Go through mem_index checking that mem follows
		Enumeration<Neuron> indexedNeurons = mem_index.keys();
		while (indexedNeurons.hasMoreElements()) {
			Neuron n = indexedNeurons.nextElement();
			WrappedList<Integer> firings = getNeuronIndex(n);
			int lastFiringTime = -1;
			for (int i = 0; i < firings.size(); i++) {
				int firingTime = firings.get(i);
				if (firingTime == lastFiringTime) {
					System.out.println("Duplicate firing time");
					System.exit(1);
				}
				Hashtable<Neuron,Boolean> slice = getFirings(firingTime);
				if (!slice.containsKey(n)) {
					System.out.println("Memory Inconsistent!!");
					System.exit(1);
				}
				lastFiringTime = firingTime;
			}
		}
	}
	
	/**
	 * Neuron n has fired this time-step. Therefore
	 * some of its children must have also fired. Since these
	 * firings were accounted for by n, we wish to ignore them
	 * in memory.
	 */
	void removeCurrentSubNeurons (Neuron n) {
		Integer[] delays = n.getDelays();
		Neuron[] foundation = n.getFoundation();
		
		for (int i = 0; i < delays.length; i++) {
			int delay = delays[i];
			Neuron child = foundation[i];
			if (delay == 0)
				snapshot.remove(child);
			else if (memory.size() >= delay) {
				memory.get(memory.size() - delay).remove(child);
				if (mem_index.containsKey(child)) {
					WrappedList<Integer> childFirings = mem_index.get(child);
					Integer toRemove = time - delay;
					childFirings.remove(toRemove);
				}
			}
		}
	}
	
	/**
	 * Called at the end of a time-slice to update the indexes
	 * for each neuron, effectively remembering all neurons
	 * which fired this time-slice.
	 */
	void indexNewFirings () {
		Enumeration<Neuron> keys = snapshot.keys();
		while (keys.hasMoreElements()) {
			Neuron n = keys.nextElement();
			if (mem_index.containsKey(n)) {
				mem_index.get(n).add(time);
			} else {
				WrappedList<Integer> list = new WrappedList<Integer>();
				list.add(time);
				mem_index.put(n, list);
			}
		}
	}
	
	/**
	 * Called at the end of a time-slice to remove all
	 * neuron firings which have in effect fell off
	 * the end of our memory.
	 */
	void removeForgottenFiringIndexes (Hashtable<Neuron,Boolean> old) {
		Enumeration<Neuron> keys = old.keys();
		while (keys.hasMoreElements()) {
			Neuron n = keys.nextElement();
			removeForgottenFiringIndexes(n);
		}
	}
	
	/**
	 * Looks through the indexes for a neuron and removes all 
	 * the ones that are older than our memory length
	 */
	void removeForgottenFiringIndexes (Neuron n) {
		WrappedList<Integer> firings = mem_index.get(n);
		while (!firings.isEmpty() && !inRange(firings.get(0)))
			firings.remove();
	}
	
	/**
	 * Checks if a given time is within the range of active memory
	 */
	boolean inRange (int desiredTime) {
		if (desiredTime < 0 || desiredTime > time)
			return false;
		
		int memStart = time - memory.size() + 1;

		if (desiredTime < memStart)
			return false;
		
		return true;
	}
	
	/**
	 * Returns the current time step
	 */
	public int getCurrentTimeStep () {
		return time;
	}
	
	/**
	 * Returns the firing index for a single neuron
	 */
	public WrappedList<Integer> getNeuronIndex (Neuron n) {
		return mem_index.containsKey(n) ? mem_index.get(n) : null;
	}
	
	/**
	 * Sets the index for a given neuron. This method is used 
	 * by the Pattern Matcher and thus was not scoped as public.
	 */
	void setNeuronIndex (Neuron n, WrappedList<Integer> index) {
		mem_index.put(n, index);
	}
	
	/**
	 * Returns a textual representation of the memory
	 */
	public String toString () {
		StringBuilder sb = new StringBuilder();
		if (snapshot.size() > 0) {
			sb.append("Current:\n");
			Enumeration<Neuron> e = snapshot.keys();
			while (e.hasMoreElements())
				sb.append("    " + e.nextElement().toString());
		}
		
		for (int i = 0; i < memory.size(); i++) {
			sb.append("Timestep " + (time - i) + ": ");
			Hashtable<Neuron,Boolean> step = memory.get(memory.size() - i - 1);
			if (step == null) continue;
			Enumeration<Neuron> e = step.keys();
			while (e.hasMoreElements()) {
				sb.append(e.nextElement().getId() + " ");
			}
			sb.append("\n");
		}
		return sb.toString();
	}
}
