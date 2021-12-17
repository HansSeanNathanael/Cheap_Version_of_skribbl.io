package game;

/**
 * Class for player containing data of the player
 * implements comparable with itself, comparing by the name
 * @author Toshiba
 *
 */
public class PlayerData implements Comparable<PlayerData> {
	
	// name of the player
	private String name;
	
	// score of the player
	private int score = 0;
	
	/**
	 * Constructor for the player data
	 * @param name : name of the player
	 */
	public PlayerData(String name)
	{
		this.name = name;
		this.score = 0; // score set to 0 by default
	}
	
	/**
	 * Get name of the player
	 * @return player name string
	 */
	public String getName()
	{
		return this.name;
	}
	
	/**
	 * Get score of the player
	 * @return score of the player
	 */
	public int getScore()
	{
		return this.score;
	}
	
	/**
	 * Adding some value to the player score
	 * @param addition : score to be added to player score
	 */
	public void addScore(int addition)
	{
		this.score += addition;
	}

	@Override
	public int compareTo(PlayerData o) 
	{
		// comparing just by name
		return this.name.compareTo(o.getName());
	}	
}
