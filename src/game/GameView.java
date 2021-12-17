package game;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeListener;

public class GameView extends JPanel implements ResetableView {

	private static final long serialVersionUID = 449074491351022437L;

	// font for all text (button, label, everything)
	private Font fontForAllText = new Font("Arial", Font.BOLD, 16);
	
	// panel and and scroll pane for player list on left screen
	private JPanel playerListPanel = new JPanel();
	private JScrollPane playerListScrollPanel = new JScrollPane(this.playerListPanel);
	
	// panel for chat by player and text field to enter text by player
	private JPanel chatPanel = new JPanel();
	private JScrollPane chatScrollPanel = new JScrollPane(this.chatPanel);
	private RTextField chatColumn = new RTextField(512);
	
	// label information time, word, and the player who is currently drawing the image
	private JLabel timeLabel = new JLabel();
	private JLabel wordLabel = new JLabel();
	private JLabel playerTurn = new JLabel();
	
	// color section to choose color or change to eraser
	private JLabel colorLabel = new JLabel("Press to pick color");
	private JButton colorPickerButton = new JButton();
	private JButton eraserButton = new JButton();
	
	// slider to choose thickness for the brush
	private JSlider thicknessSlider = new JSlider();
	 
	// canvas where the player draw their image
	private Canvas canvas;
	
	// word to be shown on top of the frame (on wordLabel)
	private String word = null;
	
	// name of the player
	private String playerName;
	
	// time to be showed on the timeLabel on top left frame (turn remaining time)
	private int time = 0;
	
	// runnable to be run on the Event Dispatcher Thread for auto scroll when there's 
	// chat or broadcast from the server added to the chat list if the chat scroll
	// already at the bottom
	private Runnable autoScrollChatRunnable = new Runnable() {
		
		@Override
		public void run() {
			chatScrollPanel.getVerticalScrollBar().setValue(
					chatScrollPanel.getVerticalScrollBar().getMaximum()
			);
		}
	};
	
	// runnable to be run on the Event Dispatcher Thread to update the word that shown
	// on wordLabel at top frame (word of object to be guessed or drawn, could also the hint word)
	private Runnable wordUpdateRunnable = new Runnable() {
		
		@Override
		public void run() {
			wordLabel.setText(word);
			wordLabel.revalidate();
			wordLabel.repaint();
			word = null;
		}
	};
	
	// runnable to be run on the Event Dispatcher Thread to update the label of time remaining
	// of the current turn
	private Runnable timeUpdateRunnable = new Runnable() {
		
		@Override
		public void run() {
			timeLabel.setText("Time: " + String.valueOf(time));
			
		}
	};
	
	// reset the background of the panel of the player list to give
	// different color for our player name
	private Runnable resetPlayerListBackground = new Runnable() {
		
		@Override
		public void run() {
			Component playerCard[] = playerListPanel.getComponents();
			
			for (Component component : playerCard) {
				component.setBackground(null);
				
				if (((JLabel) component).getText().compareTo(playerName) == 0)
				{
					component.setBackground(GameData.playerColor);
				}
			}
			
			// repaint the list
			playerListScrollPanel.revalidate();
			playerListScrollPanel.repaint();
			playerListPanel.repaint();
			playerListPanel.revalidate();
			
		}
	};
	
	/**
	 * Constructor for the game view, initializing the component and build the UI
	 * @param listenerForGameView : listener for chat column and the button color picker and eraser
	 * @param listenerForThicknessSlider : listener for JSlider thickness
	 * @param image : list of integer that will be translated to image and shown at canvas
	 * @param mouseClickListenerForTheCanvas : listener for the mouse click inside the canvas
	 * @param mouseMotionListenerForTheCanvas : listener for the mouse movement inside the canvas
	 */
	public GameView(ActionListener listenerForGameView, ChangeListener listenerForThicknessSlider, 
			List<Integer> image, MouseListener mouseClickListenerForTheCanvas, 
			MouseMotionListener mouseMotionListenerForTheCanvas)
	{
		// Creating the UI
		
		this.time = 0;
		
		this.setSize(1280, 720);
		this.setLayout(null);
		this.setBackground(new Color(147, 166, 255));

		this.playerListPanel.setSize(288, 512);
		this.playerListPanel.setLayout(new BoxLayout(this.playerListPanel, BoxLayout.Y_AXIS));
		
		this.playerListScrollPanel.setLocation(0, 176);
		this.playerListScrollPanel.setSize(288, 512);
		this.playerListScrollPanel.getVerticalScrollBar().setUnitIncrement(16);
		this.add(this.playerListScrollPanel);
		
		this.chatPanel.setSize(320, 480);
		this.chatPanel.setLayout(new BoxLayout(this.chatPanel, BoxLayout.Y_AXIS));
		
		this.chatScrollPanel.setLocation(960, 176);
		this.chatScrollPanel.setSize(320, 480);
		this.chatScrollPanel.getVerticalScrollBar().setUnitIncrement(16);
		this.add(this.chatScrollPanel);
		
		this.chatColumn.setLocation(960, 656);
		this.chatColumn.setSize(320, 32);
		this.chatColumn.addActionListener(listenerForGameView);
		this.add(this.chatColumn);
		
		this.timeLabel.setLocation(0, 72);
		this.timeLabel.setSize(288, 32);
		this.timeLabel.setHorizontalAlignment(SwingConstants.CENTER);
		this.timeLabel.setFont(this.fontForAllText);
		this.add(this.timeLabel);
		
		this.colorLabel.setLocation(352, 592);
		this.colorLabel.setSize(256, 32);
		this.colorLabel.setFont(this.fontForAllText);
		this.add(this.colorLabel);
		
		this.colorPickerButton.setLocation(352, 624);
		this.colorPickerButton.setSize(64, 32);
		this.colorPickerButton.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2, true));
		this.colorPickerButton.setActionCommand("PickColor");
		this.colorPickerButton.addActionListener(listenerForGameView);
		this.add(this.colorPickerButton);
		
		this.eraserButton.setText("ERASER");
		this.eraserButton.setLocation(432, 624);
		this.eraserButton.setSize(128, 32);
		this.eraserButton.setActionCommand("Eraser");
		this.eraserButton.addActionListener(listenerForGameView);
		this.add(this.eraserButton);
		
		this.wordLabel.setLocation(256, 64);
		this.wordLabel.setSize(768, 64);
		this.wordLabel.setFont(new Font("ARIAL", Font.BOLD, 24));
		this.wordLabel.setHorizontalAlignment(SwingConstants.CENTER);
		this.add(this.wordLabel);
		
		this.playerTurn.setLocation(1024, 64);
		this.playerTurn.setSize(256, 64);
		this.playerTurn.setFont(new Font("Comic Sans MS", Font.BOLD, 16));
		this.add(this.playerTurn);
		
		this.thicknessSlider.setLocation(736, 608);
		this.thicknessSlider.setSize(160, 64);
		this.thicknessSlider.setMaximum(10);
		this.thicknessSlider.setMinimum(1);
		this.thicknessSlider.setMajorTickSpacing(3);
		this.thicknessSlider.setMinorTickSpacing(1);
		this.thicknessSlider.setPaintTicks(true);
		this.thicknessSlider.setPaintLabels(true);
		this.thicknessSlider.setBackground(null);
		this.thicknessSlider.addChangeListener(listenerForThicknessSlider);
		this.add(this.thicknessSlider);
		
		this.canvas = new Canvas(image);
		this.canvas.setLocation(304, 144);
		this.canvas.setSize(640, 432);
		this.canvas.setBackground(Color.WHITE);
		this.addMouseListener(mouseClickListenerForTheCanvas);
		this.addMouseMotionListener(mouseMotionListenerForTheCanvas);
		this.add(this.canvas);
	}
	
	/**
	 * Get what user write inside the chat text field
	 * @return text written by the player
	 */
	public String getChatUserWrite()
	{
		String chat = this.chatColumn.getText();
		this.chatColumn.setText("");
		return chat;
	}
	
	/**
	 * Add chat or broadcast from server to the chat list
	 * @param player : name of the player who send the chat
	 * @param text : text of the chat or broadcast that will be added to chat list
	 * @param broadcast : indicate if the text is broadcast from the server or not
	 */
	public void addChat(String player, String text, boolean broadcast)
	{
		JScrollBar chatScroll = this.chatScrollPanel.getVerticalScrollBar();
		int scrollValue = chatScroll.getValue();
		int scrollMax = chatScroll.getMaximum() - 477;
		
		synchronized (chatPanel) 
		{
			if (broadcast)
			{
				// broadcast, add the whole text without any change
				this.chatPanel.add(
						new ChatCard(
								"<html>" + text + "</html>"
								)
						);
			}
			else
			{
				// show chat with their player name
				this.chatPanel.add(
						new ChatCard(
								"<html>" + player + ":<br>" + text + "</html>"
								)
						);
			}			
		}
		
		this.chatPanel.revalidate();
		this.chatPanel.repaint();
		this.chatScrollPanel.revalidate();
		this.chatScrollPanel.repaint();
		
		// auto scroll
		if (scrollValue == scrollMax)
		{
			SwingUtilities.invokeLater(this.autoScrollChatRunnable);
		}
	}
	
	/**
	 * Add player name on the player list (called when going inside the game view) after
	 * receiving start game instruction from server
	 * @param playerList : list of PlayerData consisting their name 
	 * @param playerName : name of our player
	 */
	public void addPlayer(List<PlayerData> playerList, String playerName)
	{
		this.playerName = playerName;
		synchronized (playerList) 
		{
			synchronized (playerListPanel) 
			{
				JLabel newPlayer;
				for (PlayerData playerData : playerList) {
					//show name and the score of the player
					newPlayer = new JLabel(playerData.getName() + " - score: " + playerData.getScore());
					newPlayer.setFont(this.fontForAllText);
					newPlayer.setOpaque(true);
					this.playerListPanel.add(newPlayer);
					
					if (playerData.getName().compareTo(playerName) == 0)
					{	
						newPlayer.setBackground(GameData.playerColor);
					}
				}
				
				// repaint the list
				this.playerListScrollPanel.revalidate();
				this.playerListScrollPanel.repaint();
				this.playerListPanel.repaint();
				this.playerListPanel.revalidate();
			}
		}
	}
	
	/**
	 * Update the remaining time of the current turn and show it on the screen
	 * @param time : new remaining time
	 */
	public void setTime(int time)
	{
		this.time = time;
		SwingUtilities.invokeLater(this.timeUpdateRunnable);
	}
	
	/**
	 * Update the word that will be showed at wordLabel at top of screen
	 * @param word : word to be showed
	 */
	public void setWord(String word)
	{
		this.word = word;
		SwingUtilities.invokeLater(this.wordUpdateRunnable);
	}
	
	/**
	 * Get thickness of the brush selected by the player
	 * @return thickness of the brush selected by the player
	 */
	public int getSliderThicknessValue()
	{
		return this.thicknessSlider.getValue();
	}
	
	/**
	 * Update the color of the buttonPicker color to become same with 
	 * the color chosen by the player
	 * @param buttonColor
	 */
	public void updateColor(Color buttonColor)
	{
		this.colorPickerButton.setBackground(buttonColor);
	}
	
	/**
	 * Remove player from the player list
	 * @param index : index of the player to be removed
	 */
	public void removePlayer(int index)
	{
		// must synchronized playerListPanel to prevent race
		synchronized (this.playerListPanel) 
		{
			if (index < this.playerListPanel.getComponentCount())
			{
				// remove the player name
				this.playerListPanel.remove(index);

				// repaint the list
				this.playerListScrollPanel.revalidate();
				this.playerListScrollPanel.repaint();
				this.playerListPanel.repaint();
				this.playerListPanel.revalidate();
			}			
		}
	}
	
	/**
	 * Update the playerListPanel to show their new score
	 * @param playerList : list of PlayerData consisting their name and their score
	 */
	public void updatePlayer(List<PlayerData> playerList)
	{
		synchronized (playerList) 
		{
			Component playerCard[] = this.playerListPanel.getComponents();
			for (int i = 0; i < playerList.size(); i++)
			{
				((JLabel)playerCard[i]).setText(playerList.get(i).getName() + " - score: " + playerList.get(i).getScore());
			}
			
			// repaint the list
			this.playerListScrollPanel.revalidate();
			this.playerListScrollPanel.repaint();
			this.playerListPanel.repaint();
			this.playerListPanel.revalidate();
		}
	}
	
	/**
	 * Reset the color of the our player panel color
	 * 
	 * Still not used, abandoned feature, doesn't have time to implement
	 */
	public void resetPlayerlistBackground()
	{
		SwingUtilities.invokeLater(this.resetPlayerListBackground);
	}
	
	/**
	 * Change the label name showing the name of the player who is drawing in the current turn
	 * @param name : name of the player who is currently drawing in this turn
	 */
	public void setPlayerTurn(String name)
	{
		synchronized (this.playerTurn) 
		{
			this.playerTurn.setText("Turn: " + name);
		}
	}
	
	@Override
	protected void paintComponent(Graphics g)
	{
		super.paintComponent(g);
		
		// also calling repaint for the canvas
		this.canvas.repaint();
	}

	@Override
	public void reset() {
		
		// reset the view, reset all label and text field, player list panel, chat panel
		this.chatColumn.setText("");
		this.chatPanel.removeAll();
		this.playerListPanel.removeAll();
		this.playerTurn.setText("");
		this.wordLabel.setText("");
		this.thicknessSlider.setValue(1);
		
		this.time = 0;
		this.timeLabel.setText("TIME");
		this.updateColor(Color.BLACK);
	}
}
