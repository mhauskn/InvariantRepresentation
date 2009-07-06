package gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.ListIterator;

import javax.swing.JFrame;
import javax.swing.JPanel;

import core.Neuron;

/**
 * Dynamically visualizes the neuron hierarchy.
 */
public class HierarchyViz extends JPanel implements StepUpdated {
	private static final long serialVersionUID = 1L;
	
	private static final int MAX_DRAW = 1000;
	
	int edge = 800;
	
	Gui inter = Gui.INSTANCE;
	
	ArrayList<LinkedList<Neuron>> hierarchy;
		
	Hashtable<Long,Dimension> mapping = new Hashtable<Long,Dimension>();
	
	public HierarchyViz (LocationManager locman) {
		this.setPreferredSize(new Dimension(edge,edge));
		JFrame frame = new JFrame("Neuron Hierarchy Viz");
		frame.setPreferredSize(new Dimension(edge,edge));
		locman.setupLocation(frame);
		
		frame.add(this);
		frame.pack();
		frame.setVisible(true);
	}
		 
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		hierarchy = inter.coreSys.getNeuronHierarchy();
		try {
			computeDynamicGrid(g);
		} catch (ConcurrentModificationException e) {}
	}
	
	/**
	 * Computes the dynamic grid upon which decides the spacing and placement
	 * of each different neuron in the hierarchy. This can sometimes throw 
	 * a concurrent modification exception while it iterates over the lists.
	 * This should really not be a problem as this method does not modify
	 * the hierarchy in any way.
	 */
	void computeDynamicGrid (Graphics g) throws ConcurrentModificationException {
		int maxDim = findMaxDimension();
		edge = this.getWidth();
		double space_per_neuron = edge / (double) (maxDim + 1);
		int neuronEdge = (int) Math.round(.75 * space_per_neuron);
		int padding = ((int) space_per_neuron) - neuronEdge;
		mapping.clear();
		
		int xDisp = 0, yDisp = 0;
		
		ListIterator<LinkedList<Neuron>> it = hierarchy.listIterator();
		while (it.hasNext()) {
			ListIterator<Neuron> level = it.next().listIterator();
			yDisp += padding;
			
			while (level.hasNext()) {
				Neuron n = level.next();
				setNeuronColor(g,n);
				
				xDisp += padding;
				
				g.fillRect(xDisp, yDisp, neuronEdge, neuronEdge);
				
				int xMid = xDisp + neuronEdge/2;
				int yMid = yDisp + neuronEdge/2;
				mapping.put(n.getId(), new Dimension(xMid,yMid));
				
				if (inter.coreSys.getNeuronCount() < MAX_DRAW) {
					for (Neuron other : n.getChildren()) {
						if (mapping.containsKey(other.getId())) {
							Dimension otherMid = mapping.get(other.getId());
							g.drawLine(otherMid.width, otherMid.height, xMid, yMid);
						}
					}
				}
				
				xDisp += neuronEdge;
			}
			yDisp += neuronEdge;
			xDisp = 0;
		}
	}
	
	void setNeuronColor (Graphics g, Neuron n) {
		if (n.temporal()) {
			if (n.firing())
				g.setColor(Color.PINK);
			else {
				if (n.primed())
					g.setColor(Color.cyan);
				else g.setColor(Color.GRAY);
			}
		} else {
			if (n.firing()) 
				g.setColor(Color.RED);
			else
				g.setColor(Color.BLACK);
		}
		
	}
	
	/**
	 * Finds the widest or tallest point in the neuron
	 * hierarchy
	 */
	int findMaxDimension () {
		int max = hierarchy.size();
		for (int i = 0; i < hierarchy.size(); i++) {
			int sz = hierarchy.get(i).size();
			if (sz > max) max = sz;
		}
		return max;
	}

	/**
	 * This component is repainted when a new step is taken
	 */
	public void update() {
		this.repaint();
	}
}
