package gui;

import java.awt.Dimension;

import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JScrollPane;

/**
 * Assists in visualizing the activity of the memory 
 * at each step.
 */
public class MemoryViz implements StepUpdated {
	JEditorPane pane;
	JScrollPane scrollPane;
	
	public MemoryViz (LocationManager locMan) {
		JFrame frame = new JFrame("Memory Visualizer");
		locMan.setupLocation(frame);
		
		pane = new JEditorPane();
		pane.setEditable(false);
		scrollPane = new JScrollPane(pane);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		scrollPane.setPreferredSize(new Dimension(250,300));
				
		frame.add(scrollPane);
		frame.pack();
		frame.setVisible(true);
	}
	
	/**
	 * Updates the memory visualizer with the latest version information
	 * from the memory
	 */
	public void update () {
		String memRep = Gui.INSTANCE.coreSys.getMemoryRepresentation();
		pane.setText(memRep);
		pane.select(0, 0);
		pane.requestFocus();
	}
}
