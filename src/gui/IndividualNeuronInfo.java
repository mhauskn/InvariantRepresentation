package gui;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Queue;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;

import core.Neuron;

/**
 * This window provides information on a single neuron
 */
public class IndividualNeuronInfo implements StepUpdated {
	JTextPane pane;
	JScrollPane scrollPane;
	
	Neuron n;
	
	long idealID;
	
	public IndividualNeuronInfo (LocationManager locMan) {
		JFrame frame = new JFrame("Individual Neuron Info");
		locMan.setupLocation(frame);
		
		pane = new JTextPane();
		pane.setEditable(false);
		scrollPane = new JScrollPane(pane);
		scrollPane.setPreferredSize(new Dimension(250,300));
				
		frame.add(scrollPane);
		frame.pack();
		frame.setVisible(true);
	}
	
	public void setNeuron (Neuron _n, long desiredID) {
		n = _n;
		idealID = desiredID;
	}
	
	/**
	 * Updates the memory visualizer with the latest version information
	 * from the memory
	 */
	public void update () {
		if (n == null) {
			pane.setText("Neuron " + idealID + " not found...");
			return;
		}
		
		StringBuilder out = new StringBuilder();
		out.append("Id: " + n.getId() + "\n");
		out.append("Level: " + n.getHeight() + "\n");
		out.append("Type: " + (n.temporal() ? "Temporal" : "Non-Temporal") + "\n");
		out.append("Firing: " + n.firing() + "\n");
		out.append("Score: " + n.getScore() + "\n");
		out.append("Firing Index: " + Gui.INSTANCE.coreSys.getMemoryIndex(n) + "\n");
		out.append("Children: " + getDirectChildren(n) + "\n");
		out.append("Firing Pattern: \n" + getLeaves(n) + "\n");
		//out.append(getChildren(n));
		
		pane.setText(out.toString());
		pane.select(0, 0);
		pane.requestFocus();
	}
	
	String getDirectChildren (Neuron n) {
		if (n.getHeight() <= 0)
			return "";
		String out = "";
		for (Neuron child : n.getFoundation())
			out += TextHierarchyViz.neuronToString(child);
		return out;
	}
	
	String getLeaves (Neuron n) {
		if (n.getHeight() <= 0)
			return "";
		String out = "";
		ArrayList<Hashtable<Integer,Boolean>> index = new ArrayList<Hashtable<Integer,Boolean>>();
		
		Queue<Neuron> nq = new LinkedList<Neuron>();
		Queue<Integer> iq = new LinkedList<Integer>();
		
		nq.add(n);
		iq.add(0);
		
		while (!nq.isEmpty()) {
			Neuron neu = nq.remove();
			int time = iq.remove();
			
			while (index.size() <= time)
				index.add(new Hashtable<Integer,Boolean>());
			
			int id = (int) neu.getId();
			if (neu.getHeight() == 0 && !index.get(time).containsKey(id))
				index.get(time).put(id, true);
			
			if (neu.getHeight() > 0) {
				Neuron[] foundation = neu.getFoundation();
				Integer[] delays = neu.getDelays();
				for (int i = 0; i < foundation.length; i++) {
					Neuron child = foundation[i];
					int delay = delays[i];
					nq.add(child);
					iq.add(time + delay);
				}
			}
		}
		
		ArrayList<Integer> interpretation = new ArrayList<Integer>();
		
		int cnt = 0;
		for (int i = index.size()-1; i >= 0; i--) {
			out += cnt++ + ": ";
			Enumeration<Integer> ids = index.get(i).keys();
			while (ids.hasMoreElements()) {
				int id = ids.nextElement();
				if (id != (int) n.getId()) {
					interpretation.add(id);
					out += id + " ";
				}
			}
			out += "\n";
		}
		
		out += getInterpretation(interpretation);
		
		return out;
	}
	
	String getInterpretation (ArrayList<Integer> arr) {
		String out = "";
		for (int i : arr) {
			out += (char) i;
			
		}
		out += "\n";
		return out;
	}
	
	String getChildren (Neuron n) {
		String out = "";
		Queue<Neuron> q = new LinkedList<Neuron>();
		q.add(n);
		int count = 1, nextNum = 2;
		
		while (!q.isEmpty()) {
			Neuron neu = q.remove();
			
			out += TextHierarchyViz.neuronToString(neu);
			
			if (neu.getHeight() > 0) {
				for (Neuron child : neu.getFoundation())
					q.add(child);
				/*Neuron[] foundation = neu.getFoundation();
				for (int i = foundation.length-1; i >= 0; i--)
					q.add(foundation[i]);*/
			}
			
			count++;
			if (count == nextNum) {
				out += "\n";
				nextNum *= 2;
			}
		}
		
		
		return out;
	}
}
