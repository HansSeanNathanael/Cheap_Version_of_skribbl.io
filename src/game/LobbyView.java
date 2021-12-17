package game;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

/**
 * View for the lobby
 * @author Toshiba
 *
 */
public class LobbyView extends JPanel implements ResetableView {

	// generated serial version
	private static final long serialVersionUID = -8678008608129049561L;
	
	// panel and and scroll pane for player list on left screen
	private JPanel playerListPanel = new JPanel();
	private JScrollPane playerList = new JScrollPane(this.playerListPanel);
	
	// panel for chat by player and text field to enter text by player
	private JPanel chatPanel = new JPanel();
	private JScrollPane chatScrollPanel = new JScrollPane(this.chatPanel);
	private RTextField chatColumn = new RTextField(512);
	
	// buttons
	private JButton startButton = new JButton("Start Game");
	private JButton backButton = new JButton("Back");
	
	// font for all text (button, label, everything)
	private Font fontForAllText = new Font("Arial", Font.BOLD, 16);
	
	// Runnable to be run on Event Dispatcher Thread to update the chat scroll panel
	// for auto scroll when adding new chat to the list if the scroll already at the bottom
	private Runnable updateScrollRunnable = new Runnable() {
		
		@Override
		public void run() {
			chatScrollPanel.getVerticalScrollBar().setValue(
					chatScrollPanel.getVerticalScrollBar().getMaximum()
			);
		}
	};

	/**
	 * Constructor for the class, creating the view
	 * @param listener : listener for the back button
	 */
	public LobbyView(ActionListener listener, ActionListener listenerForLobbyView)
	{
		this.setLayout(null);
		this.setBackground(new Color(147, 166, 255));
		
		this.playerListPanel.setSize(288, 480);
		this.playerListPanel.setLayout(new BoxLayout(this.playerListPanel, BoxLayout.Y_AXIS));
		
		this.playerList.setLocation(320, 120);
		this.playerList.setSize(288, 480);
		this.playerList.setHorizontalScrollBar(null);
		this.add(playerList);
		
		this.chatPanel.setSize(288, 288);
		this.chatPanel.setLayout(new BoxLayout(this.chatPanel, BoxLayout.Y_AXIS));
		
		this.chatScrollPanel.setLocation(640, 120);
		this.chatScrollPanel.setSize(288, 320);
		this.chatScrollPanel.getVerticalScrollBar().setUnitIncrement(16);
		this.add(chatScrollPanel);
		
		this.chatColumn.setLocation(640, 440);
		this.chatColumn.setSize(288, 32);
		this.chatColumn.setFont(fontForAllText);
		this.chatColumn.addActionListener(listenerForLobbyView);
		this.add(chatColumn);
		
		this.startButton.setFont(fontForAllText);
		this.startButton.setBorder(BorderFactory.createLineBorder(Color.black, 2));
		this.startButton.setFocusable(false);
		this.startButton.setFocusPainted(false);
		this.startButton.setLocation(800, 536);
		this.startButton.setSize(128, 64);
		this.startButton.setBackground(new Color(252, 255, 119));
		this.startButton.setActionCommand("StartGame");
		this.startButton.addActionListener(listenerForLobbyView);
		this.add(startButton);
		
		this.backButton.setFont(fontForAllText);
		this.backButton.setBorder(BorderFactory.createLineBorder(Color.black, 2));
		this.backButton.setFocusable(false);
		this.backButton.setFocusPainted(false);
		this.backButton.setLocation(640, 536);
		this.backButton.setSize(128, 64);
		this.backButton.setBackground(new Color(252, 255, 119));
		this.backButton.setActionCommand("BackToMainMenu");
		this.backButton.addActionListener(listener);
		this.add(backButton);
	}
	
	/**
	 * Adding new player name to the player list panel sorted by index ascending from top
	 * @param index : index of the player
	 * @param playerName : name of the player
	 */
	public void addPlayer(int index, String playerName, boolean thisPlayer)
	{
		// must synchronized playerListPanel to prevent race
		synchronized (this.playerListPanel) 
		{
			JLabel newPlayer;
			
			while (this.playerListPanel.getComponentCount() <= index)
			{
				// adding empty label if the player name still not loaded
				newPlayer = new JLabel("Loading...");
				newPlayer.setFont(fontForAllText);
				this.playerListPanel.add(newPlayer);
			}
			
			// set the name of the label
			newPlayer = (JLabel)this.playerListPanel.getComponent(index);
			newPlayer.setText(playerName);
			
			if (thisPlayer)
			{
				newPlayer.setOpaque(true);
				newPlayer.setBackground(new Color(53, 255, 88));
			}
			
			// repaint the list
			this.playerList.revalidate();
			this.playerList.repaint();
			this.playerListPanel.repaint();
			this.playerListPanel.revalidate();
		}
	}
	
	/**
	 * Remove player from it's index in player list
	 * @param index : index of the player
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
				this.playerList.revalidate();
				this.playerList.repaint();
				this.playerListPanel.repaint();
				this.playerListPanel.revalidate();
			}
			
		}
	}
	
	public String getChatUserWrite()
	{
		String chat = this.chatColumn.getText();
		this.chatColumn.setText("");
		return chat;
	}
	
	/**
	 * Adding chat from player in the chat list
	 * @param player
	 * @param text
	 */
	public void addChat(String player, String text)
	{
		synchronized (this.chatPanel) 
		{
			String textCombination = "<html>" + player + ":<br>" + text + "</html>";
			
			JScrollBar chatScrollBar = this.chatScrollPanel.getVerticalScrollBar();
			
			int scrollValue = chatScrollBar.getValue();
			int scrollMax = chatScrollBar.getMaximum() - 317;
			// - 317 because somehow if the scroll bar going to the bottom, the value and the maximum
			// value is different, the gap was 317, yet the scroll bar already at the bottom
			
			// add new chat
			this.chatPanel.add(new ChatCard(textCombination));
			this.chatPanel.revalidate();
			this.chatPanel.repaint();
			
			// auto scroll down if the scroll bar at the bottom
			if (scrollValue == scrollMax)
			{
				SwingUtilities.invokeLater(this.updateScrollRunnable);				
			}
		}
	}

	@Override
	public void reset() {
		
		// deleting all player list and all chat
		synchronized (this.playerListPanel) 
		{
			this.playerListPanel.removeAll();	
		}
		synchronized (this.chatPanel) 
		{
			this.chatPanel.removeAll();
		}
	}
}
