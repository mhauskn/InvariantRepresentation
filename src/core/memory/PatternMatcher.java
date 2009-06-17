package core.memory;

import haus.util.Lists;
import haus.util.WrappedList;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import core.Neuron;
import core.NeuronHierarchy;

/**
 * The Pattern Matcher is responsible for looking at patterns in the firings
 * of neurons with the intention of creating a new neuron when a given pattern
 * is observed frequently enough.
 */
public class PatternMatcher implements Serializable {
	private static final long serialVersionUID = -6649829759245460353L;
	
	public static final int MIN_SUPPORT = 2;

	Memory mem;
	
	NeuronHierarchy hier;
	
	/**
	 * The new Pattern matcher must have a reference to a memory upon which 
	 * to look for patterns and a neuron hierarchy to add the new neurons to
	 * once they are created.
	 */
	public PatternMatcher (Memory _mem, NeuronHierarchy hierarchy) {
		mem = _mem;
		hier = hierarchy;
	}
	
	/**
	 * Attempts to find combinatorial and sequential patterns in 
	 * the memory with the intention of creating a new combinatorial
	 * or sequential neuron.
	 */
	public void doPatternMatch () {
		int time = mem.getCurrentTimeStep();
		ArrayList<Neuron> currFreq = getSupportedSingles(mem.getFirings(time));
		Integer offset = getPreviousActiveFiringTime(time);
		
		if (offset == null) return;
		
		ArrayList<Neuron> prevFreq = getSupportedSingles(mem.getFirings(time - offset));
		
		findCombinatorialPattern(currFreq);
		findSequentialPattern(currFreq, prevFreq, offset);
	}
	
	/**
	 * Looks through all the Neurons which fired in the last
	 * and sees which of them have fired frequently enough to 
	 * have support.
	 */
	ArrayList<Neuron> getSupportedSingles (Hashtable<Neuron,Boolean> timestep) {
		ArrayList<Neuron> supported_neurons = new ArrayList<Neuron>();
		Enumeration<Neuron> e = timestep.keys();
		while (e.hasMoreElements()) {
			Neuron n = e.nextElement();
			if (supported(n))
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
	Integer getPreviousActiveFiringTime (int desiredStartTime) {
		for (int i = 1; i < Memory.MEM_SIZE; i++)
			if (mem.getFirings(desiredStartTime - i).size() > 0)
				return i;
		return null;
	}
	
	/**
	 * Checks for combinatorial (non-temporal) patterns of two
	 * neurons firing. Does this by looking at all pairs of 
	 * frequent neurons in the current turn's frequently firing list.
	 */
	void findCombinatorialPattern (ArrayList<Neuron> freqSingles) {		
		if (freqSingles.size() < 2) return;
		
		List<Integer> combined = getCombinatorialFiringList(freqSingles);
		
		if (combSupported(combined)) {
			Neuron[] out = new Neuron[freqSingles.size()];
			out = freqSingles.toArray(out);
			Integer[] delays = new Integer[out.length];
			Arrays.fill(delays, 0);
			Neuron newN = hier.createNeuron(out, delays);
			hier.addNeuron(newN);
			
			reIndexCombinatorial(freqSingles, combined, newN);
		}
	}
	
	/**
	 * Returns a list of all time steps at which all the
	 * neurons in the provided list fired together.
	 */
	@SuppressWarnings("unchecked")
	List<Integer> getCombinatorialFiringList (List<Neuron> l) {
		ArrayList combo = new ArrayList<ArrayList<Neuron>>();
		for (Neuron n : l)
			combo.add(mem.getNeuronIndex(n));
		return (List) Lists.mergeSortedLists(combo);
	}
	
	/**
	 * Checks for sequentially (temporal) patterns of two
	 * neurons firing. Does this by looking at all frequent neurons
	 * firing in this turn and last turn and creating a new 
	 * neuron if the pair has occurred often.
	 */
	void findSequentialPattern (ArrayList<Neuron> freqSingles, ArrayList<Neuron> oldFreqSingles, int offset) {
		int numCreated = 0;
		ArrayList<Neuron> test = new ArrayList<Neuron>();
		for (int i = 0; i < freqSingles.size(); i++) {
			for (int j = 0; j < oldFreqSingles.size(); j++) {
				Neuron second = freqSingles.get(i);
				Neuron first = oldFreqSingles.get(j);
				
				List<Integer> combined = getSequentialFiringList(second, first, offset);
				
				if (seqSupported(combined)) {
					Neuron newN = hier.createNeuron(new Neuron[] {first,second}, new Integer[] {offset,0});
					hier.addNeuron(newN);
					test.add(newN);
					
					reIndexSequential(second, first, combined, newN, offset);
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
	List<Integer> getSequentialFiringList (Neuron second, Neuron first, int offset) {
		List<Integer> l1 = mem.getNeuronIndex(second);
		List<Integer> l2 = mem.getNeuronIndex(first);
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
	void reIndexCombinatorial (List<Neuron> foundation, List<Integer> firings, Neuron newNeuron) {
		WrappedList<Integer> newNeuronIndex = new WrappedList<Integer>();
		for (Integer i : firings) {
			Hashtable<Neuron,Boolean> ht = mem.getFirings(i);
			for (Neuron n: foundation)
				ht.remove(n);
			ht.put(newNeuron, true);
			newNeuronIndex.add(i);
		}
		mem.setNeuronIndex(newNeuron, newNeuronIndex);
		
		// Fix up the memory indexing
		Hashtable<Integer,Boolean> firing_index = new Hashtable<Integer,Boolean>();
		for (Integer i : firings)
			firing_index.put(i, true);
		
		for (Neuron n : foundation) {
			WrappedList<Integer> old = mem.getNeuronIndex(n);
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
	void reIndexSequential (Neuron second, Neuron first, List<Integer> firings, Neuron newNeuron, int offset) {
		WrappedList<Integer> newNeuronIndex = new WrappedList<Integer>();
		for (Integer i : firings) {
			Hashtable<Neuron,Boolean> ht = mem.getFirings(i);
			if (!ht.containsKey(second)) {
				System.out.println("Attempted to remove non existant neuron");
				System.exit(1);
			}
			ht.remove(second);
			ht.put(newNeuron, true);
			newNeuronIndex.add(i);
			
			ht = mem.getFirings(i - offset);
			if (!ht.containsKey(first) && !first.equals(second)) {
				System.out.println("Attempted to remove non existant neuron");
				System.exit(1);
			}
			ht.remove(first);
		}
		mem.setNeuronIndex(newNeuron, newNeuronIndex);
				
		// Fix up the foundational neurons' memory indexing
		
		// Create an index for all the sequential firings
		Hashtable<Integer,Boolean> firing_index = new Hashtable<Integer,Boolean>();
		for (Integer i : firings)
			firing_index.put(i, true);
		
		// Replace all occurrences of first neuron
		WrappedList<Integer> old = mem.getNeuronIndex(first);
		WrappedList<Integer> wrap = new WrappedList<Integer>();
		while (old.size() > 0) {
			int i = old.remove();
			if (!firing_index.containsKey(i + offset))
				wrap.add(i);
		}
		mem.setNeuronIndex(first, wrap);
		
		// Replace all occurrences of second neuron
		old = mem.getNeuronIndex(second);
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
	boolean supported (Neuron n) {
		return supported(mem.getNeuronIndex(n));
	}
	
	/**
	 * Checks if a neuron is supported based on a list of time 
	 * steps at which it has fired.
	 */
	boolean supported (List<Integer> firings) {
		if (firings == null)
			return false;
		return firings.size() >= MIN_SUPPORT;
	}
	
	boolean combSupported (List<Integer> firings) {
		return firings.size() >= 2;
	}
	
	boolean seqSupported (List<Integer> firings) {
		return firings.size() >= 2;
	}
}
