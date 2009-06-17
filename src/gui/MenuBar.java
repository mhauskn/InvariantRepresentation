package gui;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * Creates the menu bar for the GUI.
 * Controls the buttons.
 */
public class MenuBar implements ActionListener {
	private static final long serialVersionUID = 5461363422683971543L;
	public static final String PATH_SER = "path.ser";
	
	File recentDir;
	
	private static final String playEvent = "play", pauseEvent = "pause",
		stepEvent = "step", loadEvent = "load", saveEvent = "save";
	
	JButton load, save, step, play;
	
	JFileChooser fc = new JFileChooser();
	
	JTextField textbox = new JTextField(5);
	
	boolean playing = false;
		
	ArrayList<StepUpdated> steppable = new ArrayList<StepUpdated>();
	
	BasicThread thread = new BasicThread();
	
	JFrame frame;
	
	JPanel panel;
	
	NeuronGui ng;
		
	public MenuBar (LocationManager locman, NeuronGui _ng) {
		ng = _ng;
		frame = new JFrame(LocationManager.MAIN_WINDOW);
		frame.setPreferredSize(new Dimension(200,200));
		locman.setupLocation(frame);
		
		panel = new JPanel();
		panel.setLayout(new FlowLayout());
				
		addButtons();
		
		frame.add(panel);
		frame.pack();
		frame.setVisible(true);
		
		fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
		recentDir = (File) haus.io.Serializer.deserialize(PATH_SER);
	}
	
	/**
	 * Adds another component which needs to be updated when
	 * a new step is taken.
	 */
	public void addSteppable (StepUpdated s) {
		steppable.add(s);
	}
	
	/**
	 * Creates and adds buttons to the menu
	 */
	void addButtons () {
		load = createButton("Load Hierarchy", loadEvent);
		save = createButton("Save Hierarchy", saveEvent);
		step = createButton(stepEvent, stepEvent);
		play = createButton(playEvent, playEvent);
		
		panel.add(load);
		panel.add(save);
		panel.add(step);
		panel.add(play);
		
		panel.add(textbox);
		textbox.addActionListener(this);
	}
	
	JButton createButton (String name, String event) {
		JButton out = new JButton(name);
		out.setActionCommand(event);
		out.addActionListener(this);
		return out;
	}

	public void actionPerformed(ActionEvent e) {
		String event = e.getActionCommand();
		if (textbox.equals(e.getSource())) {
			long neuronID = Integer.parseInt(event);
			ng.neuronInfo.setNeuron(Gui.INSTANCE.coreSys.getNeuronByID(neuronID), neuronID);
			ng.neuronInfo.update();
			return;
		}
		if (loadEvent.equals(event)) {
			fc.setCurrentDirectory(recentDir);
			int retVal = fc.showOpenDialog(frame);
			if (retVal == JFileChooser.APPROVE_OPTION) {
				recentDir = fc.getSelectedFile().getParentFile();
				haus.io.Serializer.serialize(recentDir, PATH_SER);
				Gui.INSTANCE.coreSys = new core.Core(fc.getSelectedFile().getAbsoluteFile().toString());
				updateSteppables();
			}
		} else if (saveEvent.equals(event)) {
			int retVal = fc.showSaveDialog(frame);
			if (retVal == JFileChooser.APPROVE_OPTION) {
				recentDir = fc.getSelectedFile().getParentFile();
				haus.io.Serializer.serialize(recentDir, PATH_SER);
				Gui.INSTANCE.coreSys.serializeCore(fc.getSelectedFile().getAbsolutePath().toString());
			}
		} else if (stepEvent.equals(event)) {
			takeStep();
		} else if (playEvent.equals(event)) {
			if (playing) {
				play.setText(playEvent);
				playing = false;
			} else {
				play.setText(pauseEvent);
				playing = true;
				thread = new BasicThread();
				thread.start();
			}
		}
	}
	
	void takeStep () {
		Gui.INSTANCE.coreSys.step();
		updateSteppables();
	}
	
	/**
	 * Updates all steppable components.
	 */
	void updateSteppables () {
		for (StepUpdated s : steppable)
			s.update();
	}
	
	class BasicThread extends Thread {
		public void run () {
			while (playing)
				takeStep();
			this.interrupt();
		}
	}
}
