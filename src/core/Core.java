package core;

import haus.util.WrappedList;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;

import senses.basic.WordSense;
import senses.graphical.PongSense;

public class Core implements Serializable {
	private static final long serialVersionUID = 4306196679766563885L;
		
	SensoryRelay relay;
	
	public Core (Sense s) {
		relay = new SensoryRelay(s);
	}
	
	/**
	 * Creates a new Core System by deserializing a specified
	 * core system.
	 */
	public Core (String ser) {
		relay = (SensoryRelay) haus.io.Serializer.deserialize(ser);
	}
	
	/**
	 * Returns the hierarchy of neurons used
	 */
	public ArrayList<LinkedList<Neuron>> getNeuronHierarchy() {
		return relay.hier.getHierarchy();
	}
	
	public long getNeuronCount () {
		return relay.hier.getNeuronCount();
	}
	
	public void step() {
		relay.step();
	}
	
	/**
	 * Serializes the core of the network. Specifically, the 
	 * NeuronHierarchy, Memory, Pattern Matcher, and Scorer
	 * are serialized.
	 * 
	 * The sensory relay is not serialized as it is not possible
	 * to serialize certain senses.
	 */
	public void serializeCore (String filename) {
		haus.io.Serializer.serialize(relay, filename);
	}
	
	public String getMemoryRepresentation () {
		return relay.hier.mem.toString();
	}
	
	public Neuron getNeuronByID (long id) {
		return relay.hier.getNeuronByID(id);
	}
	
	public WrappedList<Integer> getMemoryIndex (Neuron n) {
		return relay.hier.mem.getNeuronIndex(n);
	}
	
	public long getCurrentTimeStep () {
		return relay.hier.mem.getCurrentTimeStep();
	}

	public static void main (String[] args) {
		Core i = new Core(new WordSense());
		
		long start = System.currentTimeMillis();
		int count = 0;
		while(count < 10000) {
			i.step();
			System.out.println(count++ + " " + i.getNeuronCount());
		}
		System.out.println("Done: " + (System.currentTimeMillis() - start) + "ms");
		i.serializeCore("largeRan.ser");
	}
}
