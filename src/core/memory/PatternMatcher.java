package core.memory;

import haus.util.Lists;
import haus.util.WrappedList;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;

import core.Neuron;
import core.NeuronHierarchy;
import core.TimeKeeper;

/**
 * The Pattern Matcher is responsible for looking at patterns in the firings
 * of neurons with the intention of creating a new neuron when a given pattern
 * is observed frequently enough.
 */
public class PatternMatcher implements Serializable {
	private static final long serialVersionUID = -6649829759245460353L;
	
	/**
	 * The minimum number of times a pattern needs to occur before a new 
	 * neuron can be created for it
	 */
	private static final int MIN_SUPPORT = 3;

	private Memory mem;
	
	private MemoryManager memoryManager;
	
	private NeuronHierarchy hier;
	
	private TimeKeeper timeKeeper;
	
	/**
	 * The list of slices which should be examined next by the pattern matcher
	 */
	private LinkedList<Integer> slicesToExamine = new LinkedList<Integer>();
	
	private Hashtable<Integer,Integer> nonPermNeuronCounts = new Hashtable<Integer,Integer>();

	
	
	//<><(8)><>//
	
	
	
	/**
	 * The new Pattern matcher must have a reference to a memory upon which 
	 * to look for patterns and a neuron hierarchy to add the new neurons to
	 * once they are created.
	 */
	public PatternMatcher (TimeKeeper _tk) {
		timeKeeper = _tk;
	}
	
	public void setMemory (Memory _mem) {
		mem = _mem;
	}
	
	public void setMemoryManager (MemoryManager _manager) {
		memoryManager = _manager;
	}
	
	public void setHierarchy (NeuronHierarchy _hier) {
		hier = _hier;
	}
	
	public void checkNonPermConsistency () {
		int time = timeKeeper.getTime();
		while (mem.inRange(time)) {
			Hashtable<Neuron,Boolean> slice = mem.getFirings(time);
			boolean emptyCount = !nonPermNeuronCounts.containsKey(time) || 
				nonPermNeuronCounts.get(time) == 0;
			if (slice == null || slice.isEmpty())
				assert emptyCount;
			else
				assert nonPermNeuronCounts.get(time) == countNonPerms(slice);
			time--;
		}
	}
	
	private int countNonPerms (Hashtable<Neuron,Boolean> slice) {
		Enumeration<Neuron> e = slice.keys();
		int nonPerm = 0;
		while (e.hasMoreElements())
			if (!mem.permanent(e.nextElement(), slice))
				nonPerm++;
		
		return nonPerm;
	}
	
	public void setNonPermCount (int desiredTime, int count) {
		nonPermNeuronCounts.put(desiredTime, count);
		
		if (count == 0)
			slicesToExamine.add(desiredTime);
	}
	
	public void incrementNonPermCount (int desiredTime) {
		if (nonPermNeuronCounts.containsKey(desiredTime))
			nonPermNeuronCounts.put(desiredTime, nonPermNeuronCounts.get(desiredTime) + 1);
		else
			nonPermNeuronCounts.put(desiredTime, 1);
	}
	
	public void decrementNonPermCount (int desiredTime) {
		assert nonPermNeuronCounts.containsKey(desiredTime);
		
		int currCount = nonPermNeuronCounts.get(desiredTime);
		
		assert currCount >= 0;
		
		if (currCount == 1) {
			slicesToExamine.add(desiredTime);
		}
		
		nonPermNeuronCounts.put(desiredTime, nonPermNeuronCounts.get(desiredTime) - 1);
	}
	
	public void removeNonPermCount (int desiredTime) {
		assert nonPermNeuronCounts.containsKey(desiredTime);
		nonPermNeuronCounts.remove(desiredTime);
	}
	
	/**
	 * Check if the slice at time desired time is permanent.
	 */
	public boolean permanent (int desiredTime) {
		if (!mem.inRange(desiredTime))
			return false;
		return nonPermNeuronCounts.get(desiredTime) == 0;
	}
	
	/**
	 * The pattern matches must be notified when a slices becomes permanent so that
	 * it can add this permanent slice to be examined.
	 */
	public void addPermSlice (int absoluteTime) {
		slicesToExamine.add(absoluteTime);
	}
	
	public void doPatternMatch () {
		Hashtable<Neuron,Boolean> currentFirings = mem.getFirings(timeKeeper.getTime());
		createLevelCombinatorial(currentFirings);
		
		while (!slicesToExamine.isEmpty())
			examineNextSlice();
	}
	
	private void examineNextSlice () {
		int currentSlice = slicesToExamine.remove();
		while (!permanent(currentSlice) && !slicesToExamine.isEmpty())
			currentSlice = slicesToExamine.remove();
		
		if (!permanent(currentSlice))
			return;
		
		int prevSlice = findActiveSlice(currentSlice, -1);
		int nextSlice = findActiveSlice(currentSlice, 1);
		
		if (permanent(prevSlice))
			doPatternMatch(prevSlice, currentSlice);
		if (permanent(nextSlice))
			doPatternMatch(currentSlice, nextSlice);
	}
	
	private int findActiveSlice (int currentSlice, int timeIncrement) {
		int prevTime = currentSlice + timeIncrement;
		Hashtable<Neuron,Boolean> slice;
		while (mem.inRange(prevTime)) {
			slice = mem.getFirings(prevTime);
			if (slice != null && slice.size() > 0)
				break;
			prevTime += timeIncrement;
		}
		return prevTime;
	}
	
	private void doPatternMatch (int timePrev, int timeCurr) {
		Hashtable<Neuron,Boolean> currHT = mem.getFirings(timeCurr);
		Hashtable<Neuron,Boolean> prevHT = mem.getFirings(timePrev);
		
		if (currHT == null || prevHT == null) 
			return;
		
		ArrayList<Neuron> currFreq = getSupportedSingles(currHT);
		ArrayList<Neuron> prevFreq = getSupportedSingles(prevHT);

		int offset = timeCurr - timePrev;
		
		findSequentialPattern(currFreq, prevFreq, offset);
	}
	
	/**
	 * Attempts to find combinatorial and sequential patterns in 
	 * the memory with the intention of creating a new combinatorial
	 * or sequential neuron.
	 */
	public void doPatternMatchOLD () {
		int time = timeKeeper.getTime();
		
		createLevelCombinatorial(mem.getFirings(time));
		
		if (time < 5)
			return;
		
		ArrayList<Neuron> currFreq = getSupportedSingles(mem.getFirings(time));
		Integer offset = getPreviousActiveFiringTime(time);
		
		if (offset == null) return;
		
		ArrayList<Neuron> prevFreq = getSupportedSingles(mem.getFirings(time - offset));
		
		findCombinatorialPattern(currFreq);
		findSequentialPattern(currFreq, prevFreq, offset);
	}
	
	/**
	 * Automatically creates combinatorial neurons for two or more neurons firings
	 * at the same time at the same level.
	 */
	private void createLevelCombinatorial (Hashtable<Neuron,Boolean> snapshot) {
		if (snapshot == null || snapshot.size() < 2)
			return;
		
		PriorityQueue<Neuron> q = new PriorityQueue<Neuron>(snapshot.size(), new Neuron.LevelComparator());
		
		Enumeration<Neuron> e = snapshot.keys();
		while (e.hasMoreElements()) 
			q.add(e.nextElement());
		
		int level = 0;
		ArrayList<Neuron> levelNeurons = new ArrayList<Neuron>();
		while (!q.isEmpty()) {
			Neuron n = q.remove();
			if (n.getHeight() == level)
				levelNeurons.add(n);
			else {
				if (levelNeurons.size() > 1)
					createNewLevelNeuron(levelNeurons);
				
				levelNeurons.clear();
				level = n.getHeight();
			}
		}
		
		if (levelNeurons.size() > 1)
			createNewLevelNeuron(levelNeurons);
	}
	
	private void createNewLevelNeuron (ArrayList<Neuron> levelNeurons) {
		Neuron[] foundation = new Neuron[levelNeurons.size()];
		foundation = levelNeurons.toArray(foundation);
		Integer[] delays = new Integer[foundation.length];
		Arrays.fill(delays, 0);
		Neuron newN = new Neuron(foundation, delays, timeKeeper);
		hier.addNeuron(newN);
		ArrayList<Integer> firingTimes = new ArrayList<Integer>();
		firingTimes.add(timeKeeper.getTime());
		
		reIndexCombinatorial(levelNeurons, firingTimes , newN);
	}
	
	/**
	 * Looks through all the Neurons which fired in the last
	 * and sees which of them have fired frequently enough to 
	 * have support.
	 */
	private ArrayList<Neuron> getSupportedSingles (Hashtable<Neuron,Boolean> timestep) {
		ArrayList<Neuron> supported_neurons = new ArrayList<Neuron>();
		Enumeration<Neuron> e = timestep.keys();
		while (e.hasMoreElements()) {
			Neuron n = e.nextElement();
			if (supported(n, timestep))
				supported_neurons.add(n);
		}
		return supported_neurons;
	}
	
	/**
	 * Looks for a time step prior to the last time step at which some neuron has fired.
	 * This is needed to create new sequential patterns. It doesn't suffice to simply use
	 * the last time step as often there will be no firings in the last time step -- instead
	 * we wish to go back through memory to find the most recent time step which has active
	 * firings.
	 */
	private Integer getPreviousActiveFiringTime (int desiredStartTime) {
		for (int i = 1; i < mem.getSize(); i++)
			if (mem.getFirings(desiredStartTime - i).size() > 0)
				return i;
		return null;
	}
	
	/**
	 * Checks for combinatorial (non-temporal) patterns of two
	 * neurons firing. Does this by looking at all pairs of 
	 * frequent neurons in the current turn's frequently firing list.
	 */
	private void findCombinatorialPattern (ArrayList<Neuron> freqSingles) {		
		if (freqSingles.size() < 2) return;
		
		List<Integer> combined = getCombinatorialFiringList(freqSingles);
		
		if (supported(combined)) {
			Neuron[] out = new Neuron[freqSingles.size()];
			out = freqSingles.toArray(out);
			Integer[] delays = new Integer[out.length];
			Arrays.fill(delays, 0);
			Neuron newN = new Neuron(out, delays, timeKeeper);
			hier.addNeuron(newN);
			
			reIndexCombinatorial(freqSingles, combined, newN);
		}
	}
	
	/**
	 * Returns a list of all time steps at which all the
	 * neurons in the provided list fired together.
	 */
	@SuppressWarnings("unchecked")
	private List<Integer> getCombinatorialFiringList (List<Neuron> l) {
		ArrayList combo = new ArrayList<ArrayList<Neuron>>();
		for (Neuron n : l)
			combo.add(mem.getNeuronFirings(n));
		return (List) Lists.mergeSortedLists(combo);
	}
	
	/**
	 * Checks for sequentially (temporal) patterns of two
	 * neurons firing. Does this by looking at all frequent neurons
	 * firing in this turn and last turn and creating a new 
	 * neuron if the pair has occurred often.
	 */
	private void findSequentialPattern (ArrayList<Neuron> freqSingles, 
			ArrayList<Neuron> oldFreqSingles, int offset) {
		int numCreated = 0;
		for (int i = 0; i < freqSingles.size(); i++) {
			for (int j = 0; j < oldFreqSingles.size(); j++) {
				Neuron second = freqSingles.get(i);
				Neuron first = oldFreqSingles.get(j);
				
				List<Integer> combined = getSequentialFiringList(second, first, offset);
				
				if (supported(combined)) {
					int acceptedOffset = offset;
					Neuron firstChild = first;
					Neuron secondChild = second;
					List<Integer> acceptedFirings = combined;
					
					// Check for a reversal
					if (!first.equals(second)) {
						for (int newOffset = 1; newOffset < offset; newOffset++) {
							List<Integer> reversal = getSequentialFiringList(first, second, newOffset);
							if (reversal.size() >= combined.size() / 2) {
								acceptedFirings = reversal;
								acceptedOffset = newOffset;
								firstChild = second;
								secondChild = first;
								break;
							}
						}
					}
					
					Neuron newN = new Neuron(new Neuron[] {firstChild, secondChild},
							new Integer[] {acceptedOffset,0}, timeKeeper);
					hier.addNeuron(newN);
					reIndexSequential(secondChild, firstChild, acceptedFirings, newN, acceptedOffset);
					numCreated++;
				}
			}
		}
	}
	
	/**
	 * Returns a list of all time steps at which neuron 'first'
	 * fired and then neuron 'second' fired.
	 */
	@SuppressWarnings("unchecked")
	private List<Integer> getSequentialFiringList (Neuron second, Neuron first, int offset) {
		List<Integer> l1 = mem.getNeuronFirings(second);
		List<Integer> l2 = mem.getNeuronFirings(first);
		WrappedList<Integer> l3 = new WrappedList<Integer>(l2.size());
		for (int i = 0; i < l2.size(); i++)
			l3.add(l2.get(i) + offset);
		
		return (List<Integer>) Lists.mergeSortedLists(l1,l3);
	}
	
	/**
	 * Re-indexing is performed after a new neuron is created. Essentially we 
	 * need to go through memory making it as if the new neuron has been the one
	 * firing rather than the subsidiary neurons.
	 * 
	 * To re-index a combinatorial neuron we need to do several things:
	 * 
	 * 1. Replace all neurons in foundation with the new neuron for all firings
	 *  in memory.
	 *  
	 * 2. Re-Index each foundation neuron in mem_index to reflect these changes.
	 */
	private void reIndexCombinatorial (List<Neuron> foundation, List<Integer> firings, 
			Neuron newNeuron) {
		WrappedList<Integer> newNeuronIndex = new WrappedList<Integer>();
		for (Integer firingTime : firings) {
			for (Neuron n : foundation)
				mem.removeFiring(n, firingTime);
			mem.addFiring(newNeuron, firingTime, true);
			newNeuronIndex.add(firingTime);
		}
		mem.setNeuronIndex(newNeuron, newNeuronIndex);
		
		// Fix up the memory indexing
		Hashtable<Integer,Boolean> firing_index = new Hashtable<Integer,Boolean>();
		for (Integer i : firings)
			firing_index.put(i, true);
		
		for (Neuron n : foundation) {
			WrappedList<Integer> old = mem.getNeuronFirings(n);
			WrappedList<Integer> wrap = new WrappedList<Integer>();
			while (old.size() > 0) {
				int i = old.remove();
				if (!firing_index.containsKey(i))
					wrap.add(i);
			}
			mem.setNeuronIndex(n, wrap);
		}
	}
	
	/**
	 * This method re-indexes sequential neurons. When a new sequential neuron is created
	 * we need to make sure each past occurrence of the sequence is replaced by the new
	 * neuron.
	 * 
	 * 1. Remove all indexes in memory of the older firing
	 * 2. Adjust the memory_index to remember only the newer firing.
	 * 
	 * Note that the list of firings indexes the newer firing neuron
	 */
	private void reIndexSequential (Neuron second, Neuron first, List<Integer> firings, Neuron newNeuron, int offset) {
		WrappedList<Integer> newNeuronIndex = new WrappedList<Integer>();
		for (Integer firingTime : firings) {
			assert mem.getFirings(firingTime).containsKey(second);

			mem.removeFiring(second, firingTime);
			mem.addFiring(newNeuron, firingTime, true);
			
			newNeuronIndex.add(firingTime);
			
			boolean sameNeuron = first.equals(second);
			assert sameNeuron || mem.getFirings(firingTime - offset).containsKey(first);
			
			mem.removeFiring(first, firingTime - offset);
		}
		mem.setNeuronIndex(newNeuron, newNeuronIndex);
				
		// Fix up the foundational neurons' memory indexing
		
		// Create an index for all the sequential firings
		Hashtable<Integer,Boolean> firing_index = new Hashtable<Integer,Boolean>();
		for (Integer i : firings)
			firing_index.put(i, true);
		
		// Replace all occurrences of first neuron
		WrappedList<Integer> old = mem.getNeuronFirings(first);
		WrappedList<Integer> wrap = new WrappedList<Integer>();
		while (old.size() > 0) {
			int i = old.remove();
			if (!firing_index.containsKey(i + offset))
				wrap.add(i);
		}
		mem.setNeuronIndex(first, wrap);
		
		// Replace all occurrences of second neuron
		old = mem.getNeuronFirings(second);
		wrap = new WrappedList<Integer>();
		while (old.size() > 0) {
			int i = old.remove();
			if (!firing_index.containsKey(i))
				wrap.add(i);
		}
		mem.setNeuronIndex(second, wrap);
	}
	
	/**
	 * Checks if a given neuron has occurred frequently enough
	 * to be a candidate for new neuron creation.
	 */
	private boolean supported (Neuron n, Hashtable<Neuron,Boolean> slice) {
		if (!mem.permanent(n, slice))
			return false;
		return supported(mem.getNeuronFirings(n));
	}
	
	/**
	 * Checks if a neuron is supported based on a list of time 
	 * steps at which it has fired.
	 */
	private boolean supported (List<Integer> firings) {
		if (firings == null)
			return false;
		return firings.size() >= MIN_SUPPORT;
	}
}
