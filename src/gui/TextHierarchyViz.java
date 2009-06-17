package gui;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;

import core.Neuron;

/**
 * Visualizes the Hierarchy in a textual manner
 */
public class TextHierarchyViz implements StepUpdated {
	JTextPane pane;
	JScrollPane scrollPane;
	
	NeuronTextStylizer stylizer = new NeuronTextStylizer();
	
	public TextHierarchyViz (LocationManager locMan) {
		JFrame frame = new JFrame("Text Hierarchy Viz");
		locMan.setupLocation(frame);
		
		pane = new JTextPane();
		pane.setEditable(false);
				
		scrollPane = new JScrollPane(pane);
		//scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		scrollPane.setPreferredSize(new Dimension(500,500));
		
		stylizer.registerTextPane(pane);
				
		frame.add(scrollPane);
		frame.pack();
		frame.setVisible(true);
	}
	
	/**
	 * Updates the hierarchy
	 */
	public void update () {
		StringBuilder sb = new StringBuilder();
		sb.append("Current Timestep: " + Gui.INSTANCE.coreSys.getCurrentTimeStep() + "\n\n");
		ArrayList<LinkedList<Neuron>> hierarchy = Gui.INSTANCE.coreSys.getNeuronHierarchy();
		for (int i = 0; i < hierarchy.size(); i++) {
			sb.append("Level " + i + ": ");
			LinkedList<Neuron> level = hierarchy.get(i);
			Collections.sort(level);
			Iterator<Neuron> it = level.listIterator();
			while (it.hasNext()) {
				Neuron n = it.next();
				sb.append(neuronToString(n));
			}
			sb.append("\n\n");
		}
		pane.setText(sb.toString());
	}
	
	/**
	 * Returns a suitable string representation for this particular neuron.
	 * The information displayed differs from that displayed by the typical
	 * neuron.toString() method.
	 */
	public static String neuronToString (Neuron n) {
		String out = n.getId()+"";
		if (n.temporal()) 
			out = "<"+ out +">";
		
		out += getCaption(n) + " ";
		return out;
	}
	
	static String getCaption (Neuron n) {
		String out = "(";
		
		if (n.adjustedFiring())
			out += "F";
		if (n.preFiring())
			out += "P";
		
		if (out.length() == 1)
			return "";
		return out + ")";
	}
}
