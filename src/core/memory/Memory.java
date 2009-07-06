package core.memory;

import haus.util.WrappedList;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedList;

import core.Neuron;
import core.TimeKeeper;

/**
 * Memory handles the storing and accessing the firing patterns
 * of individual neurons. 
 */
public class Memory implements Serializable {
	private static final long serialVersionUID = 1086066787870727016L;
		
	/**
	 * The length of the memory
	 */
	private static final int MAX_ACTIVE_MEM_SLOTS = 1000;

	/**
	 * The main memory which remembers which neurons
	 * fired at each moment. It really serves as a 
	 * queue where new memory is added to the end and 
	 * old memory is removed from the front.
	 */
	private WrappedList<Hashtable<Neuron,Boolean>> memory = 
		new WrappedList<Hashtable<Neuron,Boolean>>();
	
	/**
	 * Serves to index the time at which each 
	 * neuron fired.
	 */
	private Hashtable<Neuron,WrappedList<Integer>> mem_index =
		new Hashtable<Neuron,WrappedList<Integer>>();
	
	/**
	 * Allows access to time information
	 */
	private TimeKeeper timeKeeper;
	
	/**
	 * The total number of memory slots currently active
	 */
	private int activeMemSlots = 0;
	
	PatternMatcher patternMatcher;
	
	
	//<><()><>//
	
	
	public Memory (TimeKeeper _time, PatternMatcher _pm) {
		timeKeeper = _time;
		patternMatcher = _pm;
	}
	
	/**
	 * Gets the hash table full of neurons which fire at a certain
	 * time step, desiredTime.
	 */
	public Hashtable<Neuron,Boolean> getFirings (int desiredTime) {
		if (!inRange(desiredTime))
			return null;
		int adjustedTime = translateTime(desiredTime);
		return memory.get(adjustedTime);
	}
	
	/**
	 * Translates from absolute time (as specified by the TimeKeeper) 
	 * to a local memory index.
	 */
	private int translateTime (int absoluteTime) {
		if (!inRange(absoluteTime))
			return -1;
		
		if (timeKeeper.getTime() < memory.size())
			return absoluteTime;
		
		int memStart = timeKeeper.getTime() - (memory.size() - 1);
		return absoluteTime - memStart;
	}
	
	/**
	 * Returns the list of all times within memory the specified
	 * neuron has fired.
	 */
	public WrappedList<Integer> getNeuronFirings (Neuron n) {
		return mem_index.containsKey(n) ? mem_index.get(n) : null;
	}
	
	/**
	 * Adds another time-slice.
	 * @deprecated
	 */
	public void addFirings (Hashtable<Neuron,Boolean> firings) {
		if (firings == null || firings.isEmpty())
			memory.add(null);
		else {
			memory.add(firings);
			indexFirings(firings);
			reportFilledSlice();
		}
	}
	
	public void startStep () {
		memory.add(null); // Add in new firings
	}
	
	public void endStep () {
		Hashtable<Neuron,Boolean> latestSlice = getFirings(timeKeeper.getTime());
		
		if (latestSlice == null || latestSlice.isEmpty())
			return;
		
		indexFirings(latestSlice);
	}

	/**
	 * This method will add a single firing to memory. 
	 * 
	 * NOTE: it will not index this firing!
	 */
	public void addFiring (Neuron n, int desiredTime, boolean isPermanent) {
		Hashtable<Neuron,Boolean> slice = getFirings(desiredTime);
		
		if (slice == null) {
			slice = new Hashtable<Neuron,Boolean>();
			reportFilledSlice();
			if (!isPermanent)
				patternMatcher.setNonPermCount(desiredTime, 1);
		} else {
			boolean newNonPerm = !isPermanent && !slice.containsKey(n);
			boolean perm2NonPerm = slice.containsKey(n) && !isPermanent && permanent(n,slice);
			
			if (newNonPerm || perm2NonPerm)
				patternMatcher.incrementNonPermCount(desiredTime);
			
			boolean nonPerm2Perm = isPermanent && slice.containsKey(n) && 
				!permanent(n,slice);
			
			if (nonPerm2Perm)
				patternMatcher.decrementNonPermCount(desiredTime);
		}
		
		slice.put(n, isPermanent);
		setFirings(desiredTime, slice);	
	}
	
	/**
	 * Called at the end of a time-slice to update the indexes
	 * for each neuron, effectively remembering all neurons
	 * which fired this time-slice.
	 */
	public void indexFirings (Hashtable<Neuron,Boolean> snapshot) {
		int numNonPerm = 0;
		Enumeration<Neuron> keys = snapshot.keys();
		while (keys.hasMoreElements()) {
			Neuron n = keys.nextElement();
			if (permanent(n, snapshot))
				indexFirings(n);
			else
				numNonPerm++;
		}
		
		patternMatcher.setNonPermCount(timeKeeper.getTime(), numNonPerm);
	}
	
	/**
	 * Check if a neuron n in a given slice is permanent neuron or 
	 * not.
	 */
	public boolean permanent (Neuron n, Hashtable<Neuron,Boolean> slice) {
		assert slice.containsKey(n);
		return slice.get(n);
	}
	
	/**
	 * Updates the mem_index to account for neuron n firing this turn.
	 */
	private void indexFirings (Neuron n) {
		if (mem_index.containsKey(n)) {
			mem_index.get(n).add(timeKeeper.getTime());
		} else {
			WrappedList<Integer> list = new WrappedList<Integer>();
			list.add(timeKeeper.getTime());
			mem_index.put(n, list);
		}
	}
	
	private void setFirings (int absoluteTime, Hashtable<Neuron,Boolean> newFirings) {
		int adjustedTime = translateTime(absoluteTime);
		memory.set(adjustedTime, newFirings);
	}
	
	/**
	 * Removes the oldest time-slice.
	 */
	public void removeFirings () {
		Hashtable<Neuron,Boolean> removed = null;
		
		while (removed == null)
			removed = memory.remove();
		
		reportEmptiedSlice();
		removeForgottenFiringIndexes(removed);
		patternMatcher.removeNonPermCount(timeKeeper.getTime() - memory.size());
	}
	
	/**
	 * Called at the end of a time-slice to remove all
	 * neuron firings which have in effect fell off
	 * the end of our memory.
	 */
	private void removeForgottenFiringIndexes (Hashtable<Neuron,Boolean> old) {
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
	private void removeForgottenFiringIndexes (Neuron n) {
		WrappedList<Integer> firings = mem_index.get(n);
		while (!firings.isEmpty() && !inRange(firings.get(0)))
			firings.remove();
	}
	
	/**
	 * Removes all references to a neuron
	 */
	public void remove (Neuron n) {
		if (!mem_index.containsKey(n))
			return;
		WrappedList<Integer> firings = mem_index.get(n);
		while (!firings.isEmpty()) {
			int firingTime = firings.remove();
			if (inRange(firingTime))
				removeFiring(n, firingTime);
		}
		mem_index.remove(n);
	}
	
	/**
	 * Removes the firing of the given neuron at the specified time.
	 */
	public void removeFiring (Neuron n, int time) {		
		Hashtable<Neuron,Boolean> slice = getFirings(time);
		if (slice == null || !slice.containsKey(n))
			return;
		
		if (!permanent(n, slice))
			patternMatcher.decrementNonPermCount(time);
			
		slice.remove(n);
		if (slice.isEmpty()) {
			memory.set(translateTime(time), null);
			reportEmptiedSlice();
		}
	}
	
	public int getSize () {
		return memory.size();
	}
	
	/**
	 * Returns true if the memory has reached it capacity of 
	 * active slots.
	 */
	public boolean full () {
		return activeMemSlots >= MAX_ACTIVE_MEM_SLOTS;
	}
	
	/**
	 * This method should be called externally whenever a memory
	 * slice is emptied of all elements. The memory needs to be notified
	 * so that it knows the reduce the number of active slices.
	 */
	private void reportEmptiedSlice () {
		activeMemSlots--;
	}
	
	/**
	 * This method should be called externally whenever an empty
	 * memory slice is filled. Memory needs to know to increase the 
	 * number of active slices.
	 */
	private void reportFilledSlice () {
		activeMemSlots++;
	}
	
	/**
	 * This a method meant primarily for debugging purposes. It 
	 * checks the consistency of the memory with the mem_index
	 */
	public void checkMemoryConsistency () { 
		// Checks that each neuron in every timestep has 
		// a corresponding 
		for (int i = 0; i < memory.size(); i++) {
			Integer currTime = timeKeeper.getTime() - i;
			Hashtable<Neuron,Boolean> slice = getFirings(currTime);
			if (slice == null)
				continue;
			Enumeration<Neuron> e = slice.keys();
			while (e.hasMoreElements()) {
				Neuron n = e.nextElement();
				if (!permanent(n, slice))
					continue;
				WrappedList<Integer> firings = getNeuronFirings(n);
				assert (firings.indexOf(currTime) >= 0);
			}
		}
		
		// Go through mem_index checking that mem follows
		Enumeration<Neuron> indexedNeurons = mem_index.keys();
		while (indexedNeurons.hasMoreElements()) {
			Neuron n = indexedNeurons.nextElement();
			WrappedList<Integer> firings = getNeuronFirings(n);
			int lastFiringTime = -1;
			for (int i = 0; i < firings.size(); i++) {
				int firingTime = firings.get(i);
				assert firingTime != lastFiringTime;
				
				Hashtable<Neuron,Boolean> slice = getFirings(firingTime);
				assert slice.containsKey(n) && permanent(n,slice);
				lastFiringTime = firingTime;
			}
		}
	}
	
	public void checkActiveCounts () {
		int active = 0;
		
		for (int i = 0; i < memory.size(); i++) {
			Hashtable<Neuron,Boolean> slice = memory.get(i);
			if (slice == null || slice.isEmpty()) {
				// not active
			} else {
				active++;
			}
		}
		
		assert active == activeMemSlots;
	}
	
	/**
	 * Checks if a given time is within the range of active memory
	 */
	public boolean inRange (int desiredTime) {
		if (desiredTime < 0 || desiredTime > timeKeeper.getTime())
			return false;
		
		int memStart = timeKeeper.getTime() - memory.size() + 1;

		if (desiredTime < memStart)
			return false;
		
		return true;
	}
	
	/**
	 * Sets the index for a given neuron. This method is used 
	 * by the Pattern Matcher and thus was not scoped as public.
	 */
	void setNeuronIndex (Neuron n, WrappedList<Integer> index) {
		mem_index.put(n, index);
	}
	
	public String toString () {
		StringBuilder sb = new StringBuilder();
		
		for (int i = 0; i < memory.size(); i++) {
			sb.append("Timestep " + (timeKeeper.getTime() - i) + ": ");
			Hashtable<Neuron,Boolean> step = memory.get(memory.size() - i - 1);
			if (step != null) {
				Enumeration<Neuron> e = step.keys();
				while (e.hasMoreElements()) {
					Neuron n = e.nextElement();
					if (permanent(n, step))
						sb.append(n.getId() + " ");
					else
						sb.append("(" + n.getId() + ") ");
				}
			}
			sb.append("\n");
		}
		return sb.toString();
	}
}
