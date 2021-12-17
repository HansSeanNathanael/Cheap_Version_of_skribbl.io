package game;

import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * UI for chat to be shown in chat panel
 * @author Toshiba
 *
 */
public class ChatCard extends JPanel{

	// generated serial number
	private static final long serialVersionUID = -3775899067346478406L;

	// font for every chat card, including broadcast from server
	private static Font fontForAllChatCard = new Font("Comic Sans MS", Font.BOLD, 14);

	/**
	 * Constructor of the chat chat
	 * @param text : text to be shown in the chat card
	 */
	public ChatCard(String text)
	{
		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
	
		// creating margin by creating empty border
		this.setBorder(BorderFactory.createEmptyBorder(8, 4, 8, 4));
		
		JLabel chatLabel = new JLabel(text);
		chatLabel.setFont(fontForAllChatCard );
		chatLabel.setBackground(null);
		this.add(chatLabel);
	}
}
