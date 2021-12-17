package game;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.net.Socket;

import javax.swing.JFrame;

import server.ServerGameRunnable;

/**
 * Main frame containing all UI
 * @author Toshiba
 *
 */
public class GameMainFrame extends JFrame {

	// generated serial code (random)
	private static final long serialVersionUID = -4901296736593655071L;
	
	// thread for server if this player is the one who make the lobby
	private Thread serverThread = null;
	
	// tell if the player is the one who make the lobby
	private boolean createdLobby = false;
	
	// runnable for serverThread
	private ServerGameRunnable serverRunnable;
	
	// listener for all button inside MainMenuView and back button inside LobbyView
	private ActionListener mainMenuButtonStartListener = new ActionListener() {
		
		@Override
		public void actionPerformed(ActionEvent e) 
		{
			try
			{
				if (e.getActionCommand() == "EnterLobby")
				{
					// for button to enter lobby in MainMenuView

					// get input data from all text field in MainMenuView
					String data[] = mainMenuView.getAllData();
					
					// build the connection
					Socket socket = new Socket(data[1], Integer.parseInt(data[2]));
					gameLobbyView.connectToLobby(socket, data[0]);

					GameMainFrame.this.createdLobby = false;
				}
				else if (e.getActionCommand() == "CreateLobby")
				{
					// for button to create lobby in MainMenuView

					// get input data from all text field in MainMenuView
					String data[] = mainMenuView.getAllData();
					int port = Integer.parseInt(data[2]);
					
					// build the server
					GameMainFrame.this.serverRunnable = new ServerGameRunnable(port);
					serverThread = new Thread(GameMainFrame.this.serverRunnable);
					serverThread.start();

					// build the connection
					Socket socket = new Socket("localhost", port);
					gameLobbyView.connectToLobby(socket, data[0]);
					
					GameMainFrame.this.createdLobby = true;
				}
				else if (e.getActionCommand() == "BackToMainMenu")
				{
					// for button back in the LobbyView
					
					// sending disconnect instruction to server
					GameMainFrame.this.gameLobbyView.disconnectFromServer();
				}
			}
			catch (NumberFormatException exception) 
			{
				
			}
			catch (IOException exception)
			{
				
			}
		}
	};
	
	// Listener to check the condition of the player inside the server
	private PropertyChangeListener conditionListener = new PropertyChangeListener() {
		
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			if (evt.getPropertyName() == "Disconnected")
			{
				// Disconnected happen when socket is closed or timeout (server not giving instruction
				// more than 10 seconds)
				
				synchronized (GameMainFrame.this.mainMenuView) 
				{
					// change back page to main menu
					GameMainFrame.this.setContentPane(mainMenuView);
					GameMainFrame.this.repaint();
				}
				if (GameMainFrame.this.createdLobby)
				{
					// destroy lobby if the player is the one who created the server
					// likely will not happen, but maybe just in case
					GameMainFrame.this.serverRunnable.deleteServer();
					GameMainFrame.this.createdLobby = false;						
				}					
			}
			else if (evt.getPropertyName() == "JoinServer")
			{
				// Server give instruction player could join to the server
				// change the page to lobby
				GameMainFrame.this.setContentPane(gameLobbyView);
				GameMainFrame.this.repaint();
			}
		}
	};
	
	// View for the UI
	private MainMenuView mainMenuView = new MainMenuView(mainMenuButtonStartListener);
	private GameLobbyView gameLobbyView = new GameLobbyView(mainMenuButtonStartListener, conditionListener);

	/**
	 * Constructor for main frame
	 * The frame have static view, not resizing the frame, and created in the middle of screen
	 */
	public GameMainFrame()
	{
		// creating the main frame UI
		super("Skribbl.io Cheap Version (Please don't sue me)");
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setMinimumSize(new Dimension(1280, 720));
		this.setMaximumSize(this.getMinimumSize());
		this.setResizable(false);
		
		this.setLocationRelativeTo(null);
		
		this.setVisible(true);
		
		// set view to main menu when started the application
		this.setContentPane(mainMenuView);
		mainMenuView.reset();
		
		this.createdLobby = false;
	}
}
