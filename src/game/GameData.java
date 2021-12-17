package game;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Class containing all data of the player
 * @author Toshiba
 *
 */
public class GameData {
	
	// list of player with their score
	private List<PlayerData> playerList;
	
	// brush color selected by the player used to draw the image
	private Color brushColor;
	
	// indicate the condition if the player is still drawing, or other player is still drawing
	// stillDrawing is for this player, otherPlayerStillDrawing for other player
	// this is important, still drawing used when drawing a line when player could draw
	// used in the listener in game view to track the line created by player
	// ohterPlayerStillDrawing important for the input of other player when they are drawing
	// to track if the other player stop drawing
	private boolean stillDrawing = false;
	private boolean otherPlayerStillDrawing = false;
	
	// thickness of the brush used by this player
	private int thickness = 1;
	
	// ArrayList of Integer to store image that will be shown in the canvas
	private List<Integer> image = new ArrayList<Integer>();
	
	// playerColor is the color for the player card one the player list, to differentiate
	// them from other player, so the player knew their name (especially if their name randomized)
	public static Color playerColor = new Color(53, 255, 88);
	
	/**
	 * Constructor for game data
	 */
	public GameData()
	{
		playerList = new ArrayList<PlayerData>();
		
		this.image.add(-1);
	}
	
	/**
	 * Add player to the player list at certain index
	 * @param index : index of the player inside player list
	 * @param playerName : name of the player
	 */
	public void addPlayer(int index, String playerName)
	{
		// synchronize player list to prevent race and destroy the index
		synchronized (this.playerList) 
		{
			// append until the size of player list is capable to insert the new player
			while (this.playerList.size() <= index)
			{
				this.playerList.add(null);
			}
			
			// set the new player at index
			this.playerList.set(index, new PlayerData(playerName));	
		}
	}
	
	/**
	 * Remove player inside player list at index
	 * @param index : index of player to be removed
	 */
	public void removePlayer(int index)
	{
		// synchronize player list to prevent race and destroy the index
		synchronized (this.playerList) 
		{
			if (index < this.playerList.size())
			{
				this.playerList.remove(index);							
			}
		}
	}
	
	/**
	 * Get player data from index
	 * @param index : index of the player
	 * @return data of the player at certain index
	 */
	public PlayerData getPlayerFromIndex(int index)
	{
		synchronized (this.playerList) 
		{
			return this.playerList.get(index);
		}
	}
	
	/**
	 * Get player list
	 * @return player list
	 */
	public List<PlayerData> getPlayerList()
	{
		return this.playerList;
	}
	
	/**
	 * Set the new color for the brush
	 * @param newColor : new color for the brush
	 */
	public void setBrushColor(Color newColor)
	{
		this.brushColor = newColor;
	}
	
	/**
	 * Get the brush color used by the player
	 * @return color of the brush used by the player
	 */
	public Color getBrushColor()
	{
		return this.brushColor;
	}
	
	/**
	 * Set thickness of the brush
	 * @param thickness : new thickness for the brush
	 */
	public void setThickness(int thickness)
	{
		this.thickness = thickness;
	}
	
	/**
	 * Get thickness of the brush used by the player
	 * @return thickness of the brush used by the player
	 */
	public int getThickness()
	{
		return this.thickness;
	}
	
	/**
	 * Add some code to the image ArrayList
	 * @param data : array of integer, consisting code of the image
	 */
	public void addTexture(int data[])
	{
		// data length will always 6 from start to end the code was for
		// thickness, red, green, blue, x coordinate, y coordinate
		
		synchronized (this.image) 
		{
			if (this.otherPlayerStillDrawing)
			{
				// because the player who is drawing is still drawing, no way the 
				// color of this point of line changed from the last point, 
				// so it just need to save the coordinate
				image.add(data[4]);
				image.add(data[5]);
			}
			else
			{
				// the player who is drawing start drawing, so it need to 
				// save all code to image AraryList
				for (int i : data) {
					image.add(i);				
				}
				this.otherPlayerStillDrawing = true;
			}			
		}
	}
	
	/**
	 * Get image ArrayList
	 * @return ArrayList of Integer representing the image
	 */
	public List<Integer> getImage()
	{
		return this.image;
	}
	
	/**
	 * Inserting -1 code to image ArrayList when the player who draw the image stop drawing
	 */
	public void stopDrawing()
	{
		synchronized (this.image) 
		{
			if (this.otherPlayerStillDrawing == true)
			{
				// won't insert -1 if the player who is drawing is not drawing
				// this is to prevent error
				
				this.image.add(-1);
				this.otherPlayerStillDrawing = false;			
			}			
		}
	}
	
	/**
	 * Get the condition of the player is it still drawing or not
	 * @return true if the player is still drawing, otherwise false
	 */
	public boolean isStillDrawing()
	{
		return this.stillDrawing;
	}
	
	/**
	 * Set the condition still drawing of the player
	 * @param stillDrawing : new condition of the player
	 */
	public void setStillDrawing(boolean stillDrawing)
	{
		this.stillDrawing = stillDrawing;
	}
	
	/**
	 * Add score to player with certain index inside player list
	 * @param index : index of the player
	 * @param score : additional score for the player
	 */
	public void addPlayerScore(int index, int score)
	{
		if (index < this.playerList.size())
		{
			this.playerList.get(index).addScore(score);
		}
	}
	
	/**
	 * Clear the player list and image; reset color of the brush, condition of the player,
	 * and the thickness of the brush
	 */
	public void reset()
	{
		synchronized (this.playerList) 
		{
			this.playerList.clear();
		}
		this.brushColor = Color.BLACK;
		this.stillDrawing = false;
		this.otherPlayerStillDrawing = false;
		
		synchronized (this.image) 
		{
			this.image.clear();
			this.image.add(-1);			
		}
		this.thickness = 1;
	}
	
	/**
	 * reset condition of the player, then reset the image
	 */
	public void resetPlayerCondition()
	{
		this.stillDrawing = false;
		this.otherPlayerStillDrawing = false;
		synchronized (this.image) 
		{
			this.image.clear();
			this.image.add(-1);			
		}
	}
}
