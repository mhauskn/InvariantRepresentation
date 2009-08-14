package gui;

/**
 * Heads the GUI
 */
public class NeuronGui {
	MenuBar menu;
	HierarchyViz vizPanel;
	MemoryViz memviz;
	TextHierarchyViz tviz;
	IndividualNeuronInfo neuronInfo;
	
	public NeuronGui (LocationManager locMan) {		
		menu = new MenuBar(locMan, this);
		vizPanel = new HierarchyViz(locMan);
		memviz = new MemoryViz(locMan);
		tviz = new TextHierarchyViz(locMan);
		neuronInfo = new IndividualNeuronInfo(locMan);
		
		menu.addSteppable(memviz);
		menu.addSteppable(tviz);
		menu.addSteppable(vizPanel);
		menu.addSteppable(neuronInfo);
	}
}
