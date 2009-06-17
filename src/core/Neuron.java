package core;

import haus.util.WrappedList;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;

import core.memory.Memory;


/**
 * The neuron represents our basic unit. It forms 
 * abstraction and transmits messages to other neurons.
 * It is really the basis for intelligence.
 *
 */
public class Neuron implements Serializable, Comparable<Neuron> {
	private static final long serialVersionUID = -3727807230607527105L;

	/**
	 * The unique id of this neuron
	 */
	long id;
	
	/**
	 * How distant is this neuron from the sensory input
	 */
	int height;
	
	/**
	 * Holds references to the neurons below a given neuron
	 */
	Neuron[] foundation;
	
	/**
	 * Delay in connections from lower level neurons
	 */
	Integer[] delays;
	
	/**
	 * The time that this neuron has last fired.
	 */
	int lastFiringTime;
	
	/**
	 * Holds references to all parents of this neuron
	 */
	transient ArrayList<Neuron> parents = new ArrayList<Neuron>();
	
	/**
	 * Is this neuron a temporal or non-temporal neuron
	 */
	boolean temporal;
		
	/**
	 * Is this neuron dead?
	 */
	boolean dead = false;
	
	/**
	 * Score to keep track of how often this neuron is used.
	 */
	public int score = 0;
	
	/**
	 * The pattern recognizer. Needs to know when this neuron fire.
	 */
	Memory mem;
	
	ArrayList<WrappedList<Integer>> firingQueue;
	
	/**
	 * Creates a new neuron.
	 * @param _id The unique id number of this neuron
	 * @param _height The height of this neuron in the neuron hierarchy
	 * @param _foundation The foundational neurons upon which this neuron rests
	 * @param _delays The delays between the foundational neurons to this neuron
	 * @param _mem The memory responsible for recording the firings of this neuron
	 */
	public Neuron (long _id, int _height, Neuron[] _foundation, Integer[] _delays, Memory _mem) {
		id = _id;
		height = _height;
		foundation = _foundation;
		delays = _delays;
		mem = _mem;
		
		temporal = false;
		firingQueue = new ArrayList<WrappedList<Integer>>(delays.length);
		for (int i : delays) {
			if (i > 0) {
				firingQueue.add(new WrappedList<Integer>(i));
				temporal = true;
			} else {
				firingQueue.add(null);
			}
		}
		
		for (Neuron child : foundation)
			child.addParent(this);
	}

	/**
	 * Updates this neuron by checking the last firing times of the 
	 * foundational neurons.
	 */
	public void update () {
		if (foundation == null) return; // Sub-base level neurons
		
		int time = mem.getCurrentTimeStep();
		
		boolean satisfied = true;
		for (int i = 0; i < foundation.length; i++) {
			Neuron child = foundation[i];
			boolean childFiring = child.firing();
			if (delays[i] == 0) {
				if (!childFiring)
					satisfied = false;
			} else {
				boolean foundts = false;
				WrappedList<Integer> w = firingQueue.get(i);
				while (!w.isEmpty()) {
					int next = w.get(0);
					if (next == time) { // Correct firing time
						foundts = true;
						w.remove();
					} else if (next < time) { // Past opportunities: eliminate
						w.remove();
					} else { // Future Possibilities don't eliminate
						break;
					}
				}
				if (!foundts)
					satisfied = false;
				
				if (childFiring)
					w.add(time + delays[i]);
			}
		}
		
		if (satisfied) {
			lastFiringTime = time;
			mem.rememberFiringNeuron(this);
			score++;
		}
	}
	
	public void setFiring () {
		lastFiringTime = mem.getCurrentTimeStep();
	}
	
	
	//<><()><>//
	
	
	public Integer[] getDelays () {
		return delays;
	}
	
	public Neuron[] getFoundation () {
		return foundation;
	}
	
	public ArrayList<Neuron> getParents () {
		return parents;
	}
	
	public void addParent (Neuron n) {
		if (!parents.contains(n))
			parents.add(n);
	}
	
	public int getHeight () {
		return height;
	}
	
	public long getId () {
		return id;
	}
	
	public int getScore () {
		return score;
	}
	
	public boolean firing () {
		return lastFiringTime == mem.getCurrentTimeStep();
	}
	
	public boolean adjustedFiring () {
		return lastFiringTime == mem.getCurrentTimeStep() - 1;
	}
	
	/**
	 * Determines if this temporal neuron has a possibility
	 * of firing next turn.
	 */
	public boolean preFiring () {
		if (!temporal())
			return false;
		int ready = 0;
		for (int i = 0; i < delays.length; i++) {
			if (delays[i] > 0 && foundation[i].lastFiringTime == mem.getCurrentTimeStep() - delays[i])
				ready++;
		}
		return ready == delays.length -1;
	}
	
	/*public boolean preFiring () {
		return false;
	}*/
	
	public String toString () {
		return "Neuron " + id +  " Level " + height;
	}
	
	public boolean temporal () {
		return temporal;
	}
	
	public void setScore (int newScore) {
		score = newScore;
	}

	public int compareTo(Neuron other) {
		return this.score < other.score ? 1 : -1;
	}
	
	public static class LevelComparator implements Comparator<Neuron>, Serializable {
		private static final long serialVersionUID = 4126700832051881876L;

		public int compare(Neuron o1, Neuron o2) {
			if (o1.getHeight() == o2.getHeight())
				return 0;
			return o1.getHeight() > o2.getHeight() ? 1 : -1;
		}
	}
	
	/**
	 * Default equality check compares neuron ids
	 */
	public boolean equals (Neuron other) {
		return this.id == other.id;
	}
	
	/**
	 * We wish to serialize everything except the array of parent
	 * neurons. When this is being serialized, we end up running out 
	 * of stack space because the large number of recursive serialization
	 * calls.
	 */
	private void writeObject (ObjectOutputStream out) throws IOException {
		out.defaultWriteObject();
	}
	
	/**
	 * Upon deserializing this neuron, we must reconstruct the parent 
	 * array. This is done in a top-down fashion, each higher level neuron
	 * reconstructing the parent array of its children.
	 */
	private void readObject (ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		parents = new ArrayList<Neuron>();
		for (Neuron child : foundation) {
			if (child.parents == null)
				child.parents = new ArrayList<Neuron>();
			child.addParent(this);
		}
	}
}
