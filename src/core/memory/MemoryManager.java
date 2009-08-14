package core.memory;

import haus.util.WrappedList;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Hashtable;

import core.Neuron;
import core.TimeKeeper;

/**
 * The MemoryManager is responsible for manipulating the Memory.
 */
public class MemoryManager implements Serializable {
	private static final long serialVersionUID = -3803249481451780923L;

	/**
	 * The memory unit being managed
	 */
	private Memory memory;
	
	/**
	 * Allows access to time information
	 */
	private TimeKeeper timeKeeper;
	
	
	TTLManager limboNeuronTTL = new TTLManager();
	
	
	//<><(8)><>//
	
	
	private class TTLManager implements Serializable {
		private static final long serialVersionUID = -7919228394563438033L;
		
		private WrappedList<ArrayList<Neuron>> limboNeuronTTL = 
			new WrappedList<ArrayList<Neuron>>();
		
		/**
		 * Adds the firing of neuron n at the future time delay
		 */
		public void add (Neuron n, int delay) {
			while (limboNeuronTTL.size() <= delay + 1) // Fill up with nulls
				limboNeuronTTL.add(null);
			ArrayList<Neuron> slice = limboNeuronTTL.get(delay);
			if (slice == null) {
				slice = new ArrayList<Neuron>();
				slice.add(n);
				limboNeuronTTL.set(delay, slice);
			} else {
				slice.add(n);
			}
		}
		
		public void remove (Neuron n, int delay) {
			if (delay >= limboNeuronTTL.size())
				return;
			
			ArrayList<Neuron> ttlSlice = limboNeuronTTL.get(delay);
			
			if (ttlSlice == null) 
				return;
			
			ttlSlice.remove(n);
		}
		
		/**
		 * Removes the next slice of TTLs coming due
		 */
		public ArrayList<Neuron> remove () {
			return limboNeuronTTL.remove();
		}
	}
	
	
	/**
	 * Creates a new Memory Manager
	 */
	public MemoryManager (TimeKeeper _tk, Memory _mem) {
		timeKeeper = _tk;
		memory = _mem;
	}
	
	public void startStep () {
		memory.startStep();
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
		//memory.addFirings(snapshot);
		//snapshot = new Hashtable<Neuron,Boolean>();
		memory.endStep();
		
		processLimboNeurons();		
		
		if (memory.full())
			memory.removeFirings();
	}
	
	/**
	 * Removes the next slice of limbo neurons from the TTL queue
	 * and upgrades their status to fully unexplained neurons.
	 */
	private void processLimboNeurons () {
		ArrayList<Neuron> ttlSlice = limboNeuronTTL.remove();
		
		if (ttlSlice == null)
			return;
		
		for (Neuron unexplained : ttlSlice) {
			int firingTime = timeKeeper.getTime() - unexplained.getLongestParentDelay();
			// upgrade to full status
			memory.addFiring(unexplained, firingTime, true);
			memory.getNeuronFirings(unexplained).add(firingTime);
		}
	}
	
	/**
	 * This method is called by a neuron which has just fired.
	 * 
	 * 1. This new neuron must be added to the current
	 * time-slice's snapshot. 
	 * 2. Remove memory of all children of this neuron firing
	 */
	public void rememberFiringNeuron (Neuron n) {
		if (n.getHeight() < 0)
			return;
		
		if (n.topLevel())
			memory.addFiring(n, timeKeeper.getTime(), true);
		else {
			memory.addFiring(n, timeKeeper.getTime(), false);
			limboNeuronTTL.add(n, n.getLongestParentDelay());
		}
		
		if (n.getHeight() > 0)
			removeSubNeurons(n);
	}
	
	/**
	 * Neuron n has fired this time-step. Therefore
	 * some of its children must have also fired. Since these
	 * firings were accounted for by n, we wish to ignore them
	 * in memory.
	 */
	private void removeSubNeurons (Neuron n) {
		ArrayList<Neuron> children = n.getChildren();
		Integer[] delays = n.getDelays();
		for (int i = 0; i < children.size(); i++) {
			Neuron child = children.get(i);
			int delay = delays[i];
			
			// Remove from TTL queue the limbo child at timestep LongestPar - delay to child
			limboNeuronTTL.remove(child, child.getLongestParentDelay() - delay);
			
			// Remove limbo child from memory
			memory.removeFiring(child, timeKeeper.getTime() - delay);
		}
	}
}
