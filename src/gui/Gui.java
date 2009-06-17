package gui;

import senses.graphical.PongSense;

public class Gui {
	public final static Gui INSTANCE = new Gui();
	private Gui() {};
	
	// -------- Connected Subsystems ----------------//
	core.Core coreSys = new core.Core(new PongSense());
	
	public static void main(String[] args) {		
		LocationManager locMan = new LocationManager();
		new NeuronGui(locMan);
	}
}
