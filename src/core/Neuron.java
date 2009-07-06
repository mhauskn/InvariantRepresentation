package core;

import haus.util.WrappedList;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;

/**
 * The neuron represents our basic unit. It forms 
 * abstraction and transmits messages to other neurons.
 * It is really the basis for intelligence.
 *
 */
public class Neuron implements Serializable, Comparable<Neuron> {
	private static final long serialVersionUID = -3727807230607527105L;

	/**
	 * The unique id of this neuron. This will be given
	 * when the neuron is added to a hierarchy.
	 */
	private long id = -1;
	
	/**
	 * How distant is this neuron from the sensory input.
	 */
	private int height = -1;
	
	/**
	 * Holds references to the neurons below a given neuron
	 */
	private ArrayList<Neuron> children = new ArrayList<Neuron>();
	
	/**
	 * Holds references to all parents of this neuron
	 */
	private transient ArrayList<Neuron> parents = new ArrayList<Neuron>();
	
	/**
	 * Delay in connections from lower level neurons
	 */
	private Integer[] delays;
	
	/**
	 * The time that this neuron has last fired.
	 */
	private int lastFiringTime = -1;
	
	/**
	 * The last time this neuron has failed to fire. This is important
	 * because cap neurons never fail to fire.
	 */
	private int lastNonFiringTime = -1;
	
	/**
	 * Is this neuron a temporal or non-temporal neuron
	 */
	private boolean temporal = false;
		
	/**
	 * Is this neuron dead?
	 */
	private boolean dead = false;
	
	/**
	 * Score to keep track of how often this neuron is used.
	 */
	private int score = 0;
	
	/**
	 * Keeps track of the max delay from this neuron to one of its parents.
	 */
	private int maxParentDelay = 0;
	
	/**
	 * TimeKeeper fields standard time inquiries.
	 */
	private TimeKeeper timeKeeper;
	
	/**
	 * The firingQueue
	 */
	private ArrayList<WrappedList<Integer>> firingQueue;
	
	
	//<><(Complex Methods)><>//
	
	
	/**
	 * Creates a new neuron.
	 * @param _id The unique id number of this neuron
	 * @param _height The height of this neuron in the neuron hierarchy
	 * @param _foundation The foundational neurons upon which this neuron rests
	 * @param _delays The delays between the foundational neurons to this neuron
	 * @param _mem The memory responsible for recording the firings of this neuron
	 */
	public Neuron (Neuron[] _foundation, Integer[] _delays, TimeKeeper _tk) {
		delays = _delays;
		timeKeeper = _tk;
		
		// Find the proper height for this neuron
		int maxChildHeight = -2; // -2 because lowest level neurons have no foundation and 
								 // should be given height = -1.
		for (Neuron child : _foundation)
			if (child.getHeight() > maxChildHeight)
				maxChildHeight = child.getHeight();
		height = maxChildHeight + 1;
		
		for (Neuron child : _foundation)
			children.add(child);
		
		firingQueue = new ArrayList<WrappedList<Integer>>(delays.length);
		for (int i : delays) {
			if (i > 0) {
				firingQueue.add(new WrappedList<Integer>(i));
				temporal = true;
			} else {
				firingQueue.add(null);
			}
		}
		
		for (Neuron child : children)
			child.addParent(this);
		
		for (int i = 0; i < delays.length; i++) {
			if (delays[i] == 0)
				continue;
			
			int delay = delays[i];
			Neuron child = children.get(i);
			WrappedList<Integer> pastFirings = firingQueue.get(i);
			
			addFutureFiringTime(pastFirings, child.getLastFiringTime() + delay);
		}
		
		lastFiringTime = timeKeeper.getTime();
	}
	
	private void addFutureFiringTime (WrappedList<Integer> pastFirings, int futureFiringTime) {
		pastFirings.add(futureFiringTime);
	}

	/**
	 * Updates this neuron by checking the last firing times of the 
	 * foundational neurons.
	 */
	public void update () {
		if (baseLevelNeuron())
			return;
		
		int currentTime = timeKeeper.getTime();
		
		if (id == 256)
			System.out.println();
		
		boolean shouldFire = true;
		
		for (int i = 0; i < children.size(); i++) {
			Neuron child = children.get(i);
			boolean childFiring = child.firing();
			int delay = delays[i];
			
			if (delay == 0) {
				if (!childFiring)
					shouldFire = false;
			} else {
				boolean foundPastFiring = false;
				WrappedList<Integer> pastFirings = firingQueue.get(i);
				
				while (!pastFirings.isEmpty()) {
					int nextFiringTime = pastFirings.get(0);
					if (nextFiringTime == currentTime) { // Correct firing time
						foundPastFiring = true;
						pastFirings.remove();
					} else if (nextFiringTime < currentTime) { // Past opportunities: eliminate
						pastFirings.remove();
					} else { // Future Possibilities don't eliminate
						break;
					}
				}
				
				if (!foundPastFiring)
					shouldFire = false;
				
				if (childFiring)
					addFutureFiringTime(pastFirings, currentTime + delay);
			}
		}
		
		if (shouldFire) {
			lastFiringTime = currentTime;
			score++;
		} else {
			lastNonFiringTime = currentTime;
		}
	}
	
	private boolean baseLevelNeuron () {
		return children == null || children.isEmpty();
	}
	
	/**
	 * This is used by the Neuron Hierarchy to link lowest 
	 * level neruons to sensory array
	 */
	public void setFiring () {
		lastFiringTime = timeKeeper.getTime();
	}
	
	/**
	 * Returns true if the neuron is a top-level neuron.
	 */
	public boolean topLevel () {
		return parents.isEmpty();
	}

	/**
	 * A temporal neuron is primed when one of its two child neurons
	 * has gone off.
	 */
	public boolean primed () {
		if (!temporal())
			return false;
				
		for (int i = 0; i < delays.length; i++) {
			int delay = delays[i];
			if (delay == 0) continue;
			
			int lastChildFiring = children.get(i).lastFiringTime;
			
			boolean recentChildFire = lastChildFiring > 
				timeKeeper.getTime() - delays[i];
			
			if (!recentChildFire)
				return false;
		}
		
		return true;
	}
	
	
	
	/**
	 * This method is called when the neruon is meant to
	 * be removed from the network. It deletes all references
	 * to other neurons and all references from other neurons
	 * to itself.
	 * 
	 * This method doesn't remove all external references to the 
	 * neuron such as the neuron hierarchy, so more work
	 * is necessary to have it in a state ready for garbage collection.
	 */
	void kill () {		
		dead = true;
		
		for (Neuron child : children)
			if (!child.dead)
				child.parents.remove(this);
		children = null;
		
		// Kill all parents
		for (Neuron parent : parents)
			parent.dead = true;
		parents = null;
		
		firingQueue = null;
	}
	
	/**
	 * Looks at all the parents of this neuron and finds the one
	 * with the greatest delay.
	 */
	private int findMaxParentDelay () {
		int longestDelay = 0;
		
		for (Neuron parent : parents)
			for (int i = 0; i < parent.delays.length; i++)
				if (parent.children.get(i).equals(this) && parent.delays[i] > longestDelay)
					longestDelay = parent.delays[i];
		
		return longestDelay;
	}
	
	
	//<><(Getters and Setters)><>//
	
	
	public int getLongestParentDelay () {
		return maxParentDelay;
	}
	
	public Integer[] getDelays () {
		return delays;
	}
	
	public ArrayList<Neuron> getChildren () {
		return children;
	}
	
	public ArrayList<Neuron> getParents () {
		return parents;
	}
	
	public int getLastFiringTime () {
		return lastFiringTime;
	}
	
	public int getLastNonFiringTime () {
		return lastNonFiringTime;
	}
	
	public boolean hasNeverNotFired () {
		return lastNonFiringTime == -1;
	}
	
	public void addParent (Neuron n) {
		if (!parents.contains(n))
			parents.add(n);
		
		// TODO: This sorta fucks up the limbo neuron ttl
		maxParentDelay = findMaxParentDelay();
	}
	
	public void removeParent (Neuron n) {
		parents.remove(n);
		maxParentDelay = findMaxParentDelay();
	}
	
	public int getHeight () {
		return height;
	}
	
	public long getId () {
		return id;
	}
	
	public void setID (long _id) {
		id = _id;
	}
	
	public int getScore () {
		return score;
	}
	
	public boolean firing () {
		return lastFiringTime == timeKeeper.getTime();
	}
	
	public boolean dead () {
		return dead;
	}
	
	public String toString () {
		return "Neuron " + id +  " Level " + height;
	}
	
	public String toAdvancedString () {
		StringBuilder sb = new StringBuilder();
		
		sb.append("Id: " + getId() + "\n");
		sb.append("Height: " + getHeight() + "\n");
		
		sb.append("Children: ");
		for (Neuron child : children)
			sb.append(child.getId() + ", ");
		sb.append("\n");
		
		sb.append("Delays: ");
		for (int delay : delays)
			sb.append(delay + ", ");
		sb.append("\n");
		
		sb.append("Parents: ");
		for (Neuron parent : parents)
			sb.append(parent.getId() + ", ");
		sb.append("\n");
		
		sb.append("Max Parent Delay: " + getLongestParentDelay() + "\n");
		
		sb.append("Last Firing Time: " + getLastFiringTime() + "\n");
		
		sb.append("Last NonFiring Time: " + getLastNonFiringTime() + "\n");
		
		sb.append("Score: " + getScore() + "\n");
		
		return sb.toString();
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
	
	/**
	 * This is a comparator to compare neurons by level
	 */
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
		if (dead) return;
		for (Neuron child : children) {
			if (child.parents == null)
				child.parents = new ArrayList<Neuron>();
			child.addParent(this);
		}
	}
}
