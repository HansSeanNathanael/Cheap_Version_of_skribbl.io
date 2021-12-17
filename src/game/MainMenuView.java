package game;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * View for the main menu, extends JPanel, consisting text field for text input
 * and button for enter lobby or create lobby
 * @author Toshiba
 *
 */
public class MainMenuView extends JPanel implements ResetableView {

	private static final long serialVersionUID = 3569683025103853383L;
	
	// label for text field
	private JLabel nameLabel = new JLabel("Name");
	private JLabel hostLabel = new JLabel("Hostname");
	private JLabel portLabel = new JLabel("Port");
	
	// Text field for player input
	private RTextField nameTextField = new RTextField(12); // JTextField with maximum character 12
	private JTextField hostTextField = new JTextField();
	private JTextField portTextFIeld = new JTextField();
	
	// Button to enter lobby or create lobby
	private JButton enterLobbyButton = new JButton("Enter Lobby");
	private JButton createLobbyButton = new JButton("Create Lobby");

	/**
	 * Constructor for main menu view
	 * @param listener : listener for the buttons
	 */
	public MainMenuView(ActionListener listener) 
	{
		
		// no layout (no dynamic location), everything is static
		// background color is light blue
		this.setBackground(new Color(147, 166, 255));
		this.setLayout(null);
		
		// make all label have transparent background
		this.nameLabel.setBackground(null);
		this.hostLabel.setBackground(null);
		this.portLabel.setBackground(null);
		
		// font for label, button, and text field
		// the name is very obvious
		Font fontForAllLabelAndButton = new Font("Comic Sans MS", Font.BOLD, 24);
		Font fontForAllTextField = new Font("Arial", Font.BOLD, 16);
		
		// adding every label, text field, and button
		this.nameLabel.setLocation(512, 128);
		this.nameLabel.setSize(256, 32);
		this.nameLabel.setFont(fontForAllLabelAndButton);
		this.add(nameLabel);
		
		this.nameTextField.setLocation(512, 160);
		this.nameTextField.setSize(new Dimension(256, 32));
		this.nameTextField.setFont(fontForAllTextField);
		this.add(nameTextField);
		
		this.hostLabel.setLocation(512, 224);
		this.hostLabel.setSize(256, 32);
		this.hostLabel.setFont(fontForAllLabelAndButton);
		this.add(hostLabel);
		
		this.hostTextField.setLocation(512, 256);
		this.hostTextField.setSize(new Dimension(256, 32));
		this.hostTextField.setFont(fontForAllTextField);
		this.add(hostTextField);
		
		this.portLabel.setLocation(512, 320);
		this.portLabel.setSize(256, 32);
		this.portLabel.setFont(fontForAllLabelAndButton);
		this.add(portLabel);
		
		this.portTextFIeld.setLocation(512, 352);
		this.portTextFIeld.setSize(new Dimension(256, 32));
		this.portTextFIeld.setFont(fontForAllTextField);
		this.add(portTextFIeld);
		
		this.enterLobbyButton.setSize(192, 64);
		this.enterLobbyButton.setLocation(672, 480);
		this.enterLobbyButton.setFont(fontForAllLabelAndButton);
		this.enterLobbyButton.setBorder(BorderFactory.createLineBorder(Color.black, 2));
		this.enterLobbyButton.setFocusable(false);
		this.enterLobbyButton.setFocusPainted(false);
		this.enterLobbyButton.setBackground(new Color(252, 255, 119));
		this.enterLobbyButton.setActionCommand("EnterLobby");
		this.enterLobbyButton.addActionListener(listener);
		this.add(enterLobbyButton);
		
		this.createLobbyButton.setSize(192, 64);
		this.createLobbyButton.setLocation(416, 480);
		this.createLobbyButton.setFont(fontForAllLabelAndButton);
		this.createLobbyButton.setBorder(BorderFactory.createLineBorder(Color.black, 2));
		this.createLobbyButton.setFocusable(false);
		this.createLobbyButton.setFocusPainted(false);
		this.createLobbyButton.setBackground(new Color(252, 255, 119));
		this.createLobbyButton.setActionCommand("CreateLobby");
		this.createLobbyButton.addActionListener(listener);
		this.add(createLobbyButton);
		
	}
	
	/**
	 * Get all data inside text field written by player
	 * @return array of string with length 3 consisting data from all text field
	 */
	public String[] getAllData()
	{
		String data[] = new String[3];
		
		data[0] = this.nameTextField.getText();
		data[1] = this.hostTextField.getText();
		data[2] = this.portTextFIeld.getText();
		
		return data;
	}
	
	
	@Override
	public void reset()
	{
		this.nameTextField.setText("");
		this.hostTextField.setText("");
		this.portTextFIeld.setText("");
	}
}
