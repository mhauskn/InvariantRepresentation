package senses.graphical;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;

import javax.swing.JFrame;
import javax.swing.JPanel;

import core.Sense;

public class PongSense implements Sense {
	private static final long serialVersionUID = 1899901346453098198L;
		
	private int fieldWidth = 51, fieldHeight = 50;	
	private int visualWidth = 11, visualHeight = 11;
	
	Rectangle visualField = new Rectangle(visualWidth, visualHeight);
	
	boolean[][] field;
	
	/**
	 * Position of the pong ball
	 */
	int xPos = 25, yPos = 0;
	
	/**
	 * Velocity of the pong ball
	 */
	int xVelocity = 0, yVelocity = 1;
	
	private PongViz pongWindow = new PongViz();
	
	public PongSense () {
		field = new boolean[fieldWidth][fieldHeight];
		initBoundaries();		
	}

	public boolean[] getInput() {
		boolean[] visData = new boolean[getInputLength()];
		step();
		
		int left = visualField.x, top = visualField.y, count = 0;
		for (int y = top; y < top + visualHeight; y++) {
			for (int x = left; x < left + visualWidth; x++) {
				if (x < 0 || x >= fieldWidth || y < 0 || y >= fieldHeight)
					visData[count++] = false;
				else if (x == 0 || x == fieldWidth -1 || y == 0 || y == fieldHeight -1)
					visData[count++] = true;
				else if (x == xPos && y == yPos)
					visData[count++] = true;
				else
					visData[count++] = false;
			}
		}
		return visData;
	}

	public int getInputLength() {
		return visualWidth * visualHeight;
	}
	
	/**
	 * Initializes our field to be fenced around the perimeter
	 * and to have the ball active.
	 */
	private void initBoundaries () {
		for (int y = 0; y < fieldHeight; y++) {
			for (int x = 0; x < fieldWidth; x++) {
				if (x == 0 || x == fieldHeight -1 ||
						y == 0 || y == fieldWidth -1)
					field[x][y] = true;
				else if (x == xPos && y == yPos)
					field[x][y] = true;
				else
					field[x][y] = false;
			}
		}
	}
	
	/**
	 * Takes another step in the pong game.. Calculation 
	 * invovles wall bounces.
	 */
	private void step () {
		pongWindow.repaint();
		
		xPos += xVelocity;
		yPos += yVelocity;
		
		if (xPos < 0) {
			xPos = -xPos;
			xVelocity = -xVelocity;
		}
		if (xPos >= fieldWidth) {
			int offset = xPos - fieldWidth;
			xPos = fieldWidth - offset;
			xVelocity = -xVelocity;
		}
		if (yPos < 0) {
			yPos = -yPos;
			yVelocity = -yVelocity;
		}
		if (yPos >= fieldHeight) {
			int offset = yPos - fieldHeight;
			yPos = fieldHeight - offset;
			yVelocity = -yVelocity;
		}
		
		// This code will be removed
		visualField.x = xPos - visualWidth/2;
		visualField.y = yPos - visualHeight/2;
	}
	
	private class PongViz extends JPanel {
		private static final long serialVersionUID = 651113137231597693L;

		public PongViz () {
			JFrame frame = new JFrame("Pong Window");
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setPreferredSize(new Dimension(fieldWidth*2,fieldHeight*2));
			
			frame.add(this);
			frame.pack();
			frame.setVisible(true);
		}
		
		/**
		 * Paints the pong window
		 */
		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			
			g.setColor(Color.pink);
			g.fillRect(visualField.x, visualField.y, visualWidth, visualHeight);
			
			g.setColor(Color.black);
			g.fillRect(0, 0, fieldWidth, 1);
			g.fillRect(0, fieldHeight, fieldWidth, 1);
			g.fillRect(0, 0, 1, fieldHeight);
			g.fillRect(fieldWidth, 0, 1, fieldHeight);
			g.fillRect(xPos, yPos, 1, 1);
		}
	}
	
	public static void main (String[] args) {
		PongSense p = new PongSense();
		while (true) {
			p.step();
			try {
				Thread.sleep(20);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
