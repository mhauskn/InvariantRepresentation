package gui;

import java.awt.Point;
import java.awt.Dimension;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.Hashtable;

import javax.swing.JFrame;

/**
 * Remembers information regarding the locations of each
 * different window.
 */
public class LocationManager implements WindowListener {
	public static final String SER_WINDOW_LOCS = "locs.ser";
	public static final String SER_WINDOW_SIZES = "sizes.ser";
	
	public static final String MAIN_WINDOW = "Neuron Visualizer";
	
	Hashtable<String,Point> windowLocs = new Hashtable<String,Point>();
	Hashtable<String,Dimension> windowSizes = new Hashtable<String,Dimension>();
	
	ArrayList<JFrame> activeFrames = new ArrayList<JFrame>();

	/**
	 * Attempts to deserialize the hashtable of locations
	 */
	@SuppressWarnings("unchecked")
	public LocationManager () {
		if (haus.io.FileReader.exists(SER_WINDOW_LOCS))
			windowLocs = (Hashtable<String,Point>) haus.io.Serializer.deserialize(SER_WINDOW_LOCS);
		if (haus.io.FileReader.exists(SER_WINDOW_SIZES))
			windowSizes = (Hashtable<String,Dimension>) haus.io.Serializer.deserialize(SER_WINDOW_SIZES);
	}

	/**
	 * Remembers the location of the given window when it was
	 * closed.
	 */
	public void windowClosed (WindowEvent arg0) {
		JFrame src = (JFrame) arg0.getSource();
		String windowName = getFrameHash(src);
		windowLocs.put(windowName, src.getLocation());
		windowSizes.put(windowName, src.getSize());
		haus.io.Serializer.serialize(windowLocs, SER_WINDOW_LOCS);
		haus.io.Serializer.serialize(windowSizes, SER_WINDOW_SIZES);
		activeFrames.remove(src);
		if (windowName.equals(MAIN_WINDOW))
			for (JFrame active : activeFrames)
				active.dispose();
	}
	
	
	/**
	 * This method will register the new JFrame and set its
	 * location.
	 */
	public void setupLocation (JFrame frame) {
		frame.addWindowListener(this);
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		
		frame.setLocation(getWindowLocation(frame));
		setWindowSize(frame);
		
		
		activeFrames.add(frame);
	}
	
	/**
	 * Tells a window its correct location
	 */
	private Point getWindowLocation (JFrame frame) {
		String windowName = getFrameHash(frame);
		if (windowLocs.containsKey(windowName))
			return windowLocs.get(windowName);
		return new Point(0,0);
	}
	
	/**
	 * Sets the size of the window if we have serialized it. If not,
	 * the window size will not be set and will be allowed to default.
	 */
	private void setWindowSize (JFrame frame) {
		String windowName = getFrameHash(frame);
		if (!windowSizes.containsKey(windowName))
			return;
		frame.setPreferredSize(windowSizes.get(windowName));
	}

	/**
	 * Returns a unique hash for each JFrame window
	 */
	private String getFrameHash (JFrame frame) {
		return frame.getTitle();
	}
	
	public void windowActivated(WindowEvent arg0) {}
	public void windowClosing(WindowEvent arg0) {}
	public void windowDeactivated(WindowEvent arg0) {}
	public void windowDeiconified(WindowEvent arg0) {}
	public void windowIconified(WindowEvent arg0) {}
	public void windowOpened(WindowEvent arg0) {}
}
