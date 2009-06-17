package gui;

import java.awt.Color;

import javax.swing.JTextPane;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import core.Neuron;

/**
 * Responsible for managing the coloring of neurons
 */
public class NeuronTextStylizer {
	public static final String black = "black", gray = "gray", italic = "italic", bold = "bold";
	
	JTextPane pane;
	
	StyledDocument doc;
	
	public void registerTextPane (JTextPane _pane) {
		pane = _pane;
		doc = pane.getStyledDocument();
		
		Style style = pane.addStyle(black, null);
		StyleConstants.setForeground(style, Color.black);
		
		style = pane.addStyle(gray, null);
		StyleConstants.setForeground(style, Color.gray);
		
		style = pane.addStyle(italic, null);
		StyleConstants.setItalic(style, true);
		
		style = pane.addStyle(bold, null);
		StyleConstants.setBold(style, true);
	}
	
	public void addStyle (Neuron n, int start, int len) {
		if (n.temporal())
			doc.setCharacterAttributes(start, len, pane.getStyle(gray), false);
		else
			doc.setCharacterAttributes(start, len, pane.getStyle(black), false);

	}
}
