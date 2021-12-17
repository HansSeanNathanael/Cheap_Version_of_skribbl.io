package game;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.List;

import javax.swing.JPanel;

/**
 * Custom JPanel act as canvas where the player could draw their image
 * inside here by translating array of integer
 * @author Toshiba
 *
 */
public class Canvas extends JPanel {

	// generate serial number
	private static final long serialVersionUID = 7632832569233507551L;
	
	// array of integer that used to store code (in form of integer)
	// that will be translated to image and drawn 
	private List<Integer> image;
	
	// list of basic stroke for the Graphics2D used to draw line
	// and image with length of 10, consisting brush with width 1 to 10
	// ordered ascending
	private static BasicStroke stroke[] = {
			new BasicStroke(1),
			new BasicStroke(2),
			new BasicStroke(3),
			new BasicStroke(4),
			new BasicStroke(5),
			new BasicStroke(6),
			new BasicStroke(7),
			new BasicStroke(8),
			new BasicStroke(9),
			new BasicStroke(10)
	};
	
	/**
	 * Constructor for the canvas with parameter list of integer
	 * that will translated to become an image, remember the list
	 * will not copied but will store memory location of the passed
	 * list of integer, so changing it outside this class will change
	 * the image too, by this definition, the changing of image
	 * will happen outside of this class just by adding or removing
	 * some value from the list
	 * @param image
	 */
	public Canvas(List<Integer> image)
	{
		this.image = image;
	}
	
	@Override
	protected void paintComponent(Graphics g)
	{
		super.paintComponent(g);
		
		// override the paintComponent so the class
		// could draw the image from translating the
		// list of integer pass in constructor
		
		// change to Graphics2D to make it possible using BasicStroke
		// to change the width of the line
		Graphics2D brush = (Graphics2D) g;
		
		// coordinate of the previous point
		// if value is negative, that means there is no previous point
		int prevX = -1;
		int prevY = -1;
		
		// coordinate of the current point
		// if value is negative, that means there is no previous point
		int x = -1;
		int y = -1;
		
		int thickness = 0; // thickness of the line
		
		
		// So a brief explanation how the list of integer translated to image
		// this algorithm isn't efficient, because this is the first algorithm 
		// that come to my brain, which i find the faster way by using BufferedImage
		// to draw the image faster, but no time to change it (exam is near)
		//
		// SO, the algorithm is actually the list of integer is storing thickness,
		// color RGB, and coordinate of a point of line. The image was actually just
		// bunch of point of a line, the list start with -1 at start, -1 is used to
		// tell that a line is already end, so when finding -1, that means the
		// canvas must make new line, first by taking 4 integer right next the
		// -1 index, the next one is the thickness, and the three next again is
		// the color of the line, and then there's bunch of pair coordinate of the
		// point of line (x, y)
		// Confusing? So here's example
		// [-1, 4, 255, 88, 221, 0, 1, 1, 2, 4, 5, 8, 4, -1, 3, 0, 0, 0, 10, 4, 5, 8]
		// read from left to the right from the start
		// 
		// translation:
		// [ {-1}, 4, 255, 88, 221, 0, 1, 1, 2, 4, 5, 8, 4, -1, 3, 0, 0, 0, 10, 4, 5, 8]
		// reading the first one 
		//
		// [-1, {4, 255, 88, 221}, 0, 1, 1, 2, 4, 5, 8, 4, -1, 3, 0, 0, 0, 10, 4, 5, 8]
		// found -1, means creating new line, read four integer right next to it 
		//
		// [-1, {4}, 255, 88, 221, 0, 1, 1, 2, 4, 5, 8, 4, -1, 3, 0, 0, 0, 10, 4, 5, 8]
		// the right next one is for thickness which is 4, 
		//
		// [-1, 4, {255, 88, 221}, 0, 1, 1, 2, 4, 5, 8, 4, -1, 3, 0, 0, 0, 10, 4, 5, 8]
		// then the three next one is color, Red = 255, Green = 88, Blue = 221
		//
		// [-1, 4, 255, 88, 221, {0, 1, 1, 2, 4, 5, 8, 4}, -1, 3, 0, 0, 0, 10, 4, 5, 8]
		// then there's bunch of pair integer for coordinate x and y, this translated to
		// points (0, 1), (1, 2), (4, 5), (8, 4), then creating straight line between point
		//
		// [-1, 4, 255, 88, 221, 0, 1, 1, 2, 4, 5, 8, 4, {-1}, 3, 0, 0, 0, 10, 4, 5, 8]
		// then reading the next -1 one, means creating new line, and repeat for the whole list
		// algorithm is not efficient, maybe will improved if there's time and moti
		synchronized (this.image) 
		{
			
			for (int i = 0; i < this.image.size(); i++)
			{
				if (this.image.get(i) == -1)
				{
					if (i + 4 < this.image.size())
					{
						brush.setColor(
								new Color(
										this.image.get(i+2), this.image.get(i+3), this.image.get(i+4)
								)
						);
						thickness = this.image.get(i+1);
						brush.setStroke(stroke[thickness - 1]);
					}
					x = -1;
					y = -1;
					prevX = -1;
					prevY = -1;
					i = i + 4;
				}
				else
				{
					if (i + 1 < this.image.size())
					{
						x = this.image.get(i) - 304;
						y = this.image.get(i+1) - 144;
						
						if (prevX >= 0 && prevY >= 0 && x >= 0 && y >= 0)
						{
							brush.drawLine(prevX, prevY, x, y);
						}
						if (x >= 0 && y >= 0)
						{
							brush.fillOval(x - thickness/2, y - thickness/2, thickness, thickness);
						}
						prevX = x;
						prevY = y;
						
					}
					i = i + 1;
				}
			}
		}
	}
	
}
