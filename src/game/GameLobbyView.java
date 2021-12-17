package game;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import javax.swing.JColorChooser;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import Utility.UtilityRandomName;

/**
 * GamelobbyView is a view connector between lobby view and game view
 * that's why the name was GameLobbyView
 * 
 * Extends JPanel and implements ResetableView
 * @author Toshiba
 *
 */
public class GameLobbyView extends JPanel implements ResetableView {

	// name of the player
	private String playerName;
	
	// connection to the server
	private Socket socket;
	private DataInputStream in;
	private DataOutputStream out;
	private Thread inputStreamThread = null; // thread for handling input
	
	// Data for the game
	private GameData gameData = new GameData();
	
	// lobby view (view for the player when is in lobby)
	private LobbyView lobbyView;
	
	// game view (view when player is playing the game, consisting the canvas)
	private GameView gameView;
	
	// currentView used to saved which JPanel that shown as the view
	private JPanel currentView;
	
	// saved when last time the player is drawing, interval for input of the position of
	// the player mouse cursor location
	private long mouseLastInput = System.currentTimeMillis();
	
	// listener for the condition of the player like 
	// disconnected from server or could join the server
	private PropertyChangeListener lobbyListener;
	
	// listener for button start game inside lobby view
	// and text field of chat
	private ActionListener listenerForLobbyView = new ActionListener() 
	{
		
		@Override
		public void actionPerformed(ActionEvent e) 
		{
			if (e.getActionCommand() == "StartGame")
			{
				// this one is for start button, will send start request to the server
				synchronized (out) 
				{
					try
					{
						out.writeByte(4);						
					}
					catch (IOException ex)
					{
						
					}
				}
			}
			else if (e.getSource() instanceof RTextField)
			{
				// this one is for chat text field, will send the chat to the server
				// if the chat is not empty
				String chat = lobbyView.getChatUserWrite();
				if (chat.length() > 0)
				{
					synchronized (out) 
					{
						try 
						{
							// send the code == 3 to indicate there's chat input
							// from client for the server
							// then the length of the chat, then the chat itself
							out.writeByte(3);
							out.writeInt(chat.length());
							out.writeChars(chat);
						} 
						catch (IOException e1) 
						{
							
						}
					}
				}				
			}
		}
	};
	
	// listener for all button and text field inside the game view
	// now consisting listener for chat text field (game view), pick color
	// button to change color, and then eraser button
	private ActionListener listenerForGameView = new ActionListener() 
	{
		
		@Override
		public void actionPerformed(ActionEvent e) 
		{
			if (e.getSource() instanceof RTextField)
			{
				// this one is for chat text field, will send the chat to the server
				// if the chat is not empty
				String chat = gameView.getChatUserWrite();
				if (chat.length() > 0)
				{
					synchronized (out) 
					{
						try 
						{
							// send the code == 12 to indicate there's chat input
							// from client for the server
							// then the length of the chat, then the chat itself
							// algorithm almost same with the one inside lobby view
							// but the code is 12 to differentiate the source of the chat
							out.writeByte(12);
							out.writeInt(chat.length());
							out.writeChars(chat);
						} 
						catch (IOException e1) 
						{
							
						}
					}
				}		
			}
			else if (e.getActionCommand().compareTo("PickColor") == 0)
			{
				// open dialog to choose color and change the color of the brush
				gameData.setBrushColor(JColorChooser.showDialog(gameView, "Choose", gameData.getBrushColor()));
				gameView.updateColor(gameData.getBrushColor());
			}
			else if (e.getActionCommand().compareTo("Eraser") == 0)
			{
				// this is basically change the color of the brush to white, just like that
				// because the background is white, then color white is like an eraser
				// IQ 200
				gameData.setBrushColor(Color.WHITE);
				gameView.updateColor(gameData.getBrushColor());
			}
		}
	};
	
	// listener for the JSlider to choose thickness of the brush inside game view
	private ChangeListener listenerForThicknessSlider = new ChangeListener() {
		
		@Override
		public void stateChanged(ChangeEvent e) {
			if (e.getSource() instanceof JSlider)
			{
				// change the thickness to the new value
				gameData.setThickness(gameView.getSliderThicknessValue());
			}
		}
	};
	
	// listener for the mouse click inside the canvas inside game view
	// this is important to determine if the point created by player was part of the other line
	// or not
	private MouseListener mouseClickListenerForTheCanvas = new MouseListener() {
		
		@Override
		public void mouseReleased(MouseEvent e) {
			
			// mouse released, player stop drawing
			synchronized (gameData) 
			{
				gameData.setStillDrawing(false);
				synchronized (out) 
				{
					try 
					{
						 // send stop drawing signal to server
						out.writeByte(11);
					} 
					catch (IOException e1) 
					{
						e1.printStackTrace();
					}
				}
			}
		}
		
		@Override
		public void mousePressed(MouseEvent e) {
			
			// mouse pressed, player started drawing
			synchronized (gameData) 
			{
				gameData.setStillDrawing(true);
			}
		}
		
		@Override
		public void mouseExited(MouseEvent e) {
			// if mouse exit from the canvas, this is count as player stop drawing
			// algorithm is the same with mouseReleased
			synchronized (gameData) 
			{
				gameData.setStillDrawing(false);
				synchronized (out) 
				{
					try 
					{
						out.writeByte(11);
					} 
					catch (IOException e1) 
					{
						e1.printStackTrace();
					}
				}
			}
		}
		
		@Override
		public void mouseEntered(MouseEvent e) 
		{
			
		}
		
		@Override
		public void mouseClicked(MouseEvent e) {
			
		}
	};
	
	private MouseMotionListener mouseMotionListenerForTheCanvas = new MouseMotionListener() {
		
		@Override
		public void mouseMoved(MouseEvent e) 
		{
			
		}
		
		@Override
		public void mouseDragged(MouseEvent e) 
		{
			// must use mouseDragged, if mouse move when still pressed
			// this one is the one called, not the mouseMoved

			if (System.currentTimeMillis() - mouseLastInput >= 17)
			{
				// if condition is to give interval 
				// 17 from 1000 / 60  (60 frame rate per second)
				synchronized (gameData) 
				{
					if (gameData.isStillDrawing())
					{
						// send the data if the player is drawing, so this will not
						// send image data if player mouse is just moving inside the canvas
						try 
						{
							// send code 10 to tell the server there's image data coming
							// sent thickness, then red color value, then green color value,
							// then blue color value, then coordinate x and coordinate y 
							// of the mouse
							out.writeByte(10);
							out.writeInt(gameData.getThickness());
							out.writeInt(gameData.getBrushColor().getRed());
							out.writeInt(gameData.getBrushColor().getGreen());
							out.writeInt(gameData.getBrushColor().getBlue());
							out.writeInt(e.getX());
							out.writeInt(e.getY());
						} 
						catch (IOException e1) 
						{
							e1.printStackTrace();
						}
						
					}
				}
				
				// saved the new time of the input
				mouseLastInput = System.currentTimeMillis();
			}
			e.consume();
		}
	};
	
	private static final long serialVersionUID = -8678008608129049561L;
	
	/**
	 * Constructor for the class
	 * @param listener : listener for the back button
	 * @param lobbyListener : listener to check if player want to goint back to main menu
	 */
	public GameLobbyView(ActionListener listener, PropertyChangeListener lobbyListener)
	{
		// creating the view
		this.lobbyView = new LobbyView(listener, listenerForLobbyView);
		this.lobbyListener = lobbyListener;
		
		this.gameView = new GameView(
				listenerForGameView, listenerForThicknessSlider, gameData.getImage(),
				mouseClickListenerForTheCanvas, mouseMotionListenerForTheCanvas
		);
		
		this.setSize(new Dimension(1280, 720));
		this.setLayout(new BorderLayout());
		
		this.lobbyView.setBounds(0, 0, 1280, 720);
		
		// reset the view
		this.lobbyView.reset();
		this.setPanel(this.lobbyView);
		
		// create input thread
		this.inputStreamThread = new Thread(inputStreamRunnable);
		this.inputStreamThread.start();
	}
	
	/**
	 * Connect the program to the server
	 * @param socket : socket connection
	 * @param playerName : name of player (random if doesn't have name)
	 * @throws IOException if failed to connect to server
	 */
	public void connectToLobby(Socket socket, String playerName) throws IOException
	{
		this.socket = socket;
		
		if (playerName.length() > 0)
		{
			this.playerName = playerName;			
		}
		else
		{
			// random player name if player doesn't write their name
			this.playerName = UtilityRandomName.getRandomName();
		}
		
		// create the data stream
		this.in = new DataInputStream(this.socket.getInputStream());
		this.out = new DataOutputStream(this.socket.getOutputStream());
		
		// continue the thread
		this.inputStreamRunnable.setMustRun(true);
		
		// reset view
		this.reset();
	}
	
	private InputStreamRunnable inputStreamRunnable = new InputStreamRunnable();
	
	// inner class for runnable for input stream thread, this runnable
	// could paused and resumed to prevent creating new Thread which is
	// costly
	class InputStreamRunnable implements Runnable 
	{
		// save when last time this thread read instruction from server
		// if more than 10 seconds no respond from server, player
		// will be kicked from server
		long lastInput = System.currentTimeMillis();
		
		// used to pause the thread
		boolean mustRun = false;
		
		@Override
		public void run() {
			
			// runnable will always run, to make it reusable and doesn't need
			// to make new thread that will be costly for the system
			while(true)
			{
				
				// update time of last input received by the client
				lastInput = System.currentTimeMillis();
				if (mustRun)
				{
					try
					{
						byte code = 0;
						while(true)
						{
							// waiting for byte code
							waitInput(1);
							
							code = in.readByte();
							
							if (code == 0)
							{
								// send back the byte to server
								respondTestByte();
								continue;
							}
							else if (code == -1)
							{
								// receive permission from server to join the server,
								// send instruction to GameMainFrame to change view to lobby view
								lobbyListener.propertyChange(
										new PropertyChangeEvent(this, "JoinServer", false, true)
										);
								synchronized (out) 
								{
									// send data of the player to the server
									out.writeByte(1);
									out.writeInt(playerName.length());
									out.writeChars(playerName);
								}
							}
							else if (code == -2)
							{
								// receive denied instruction, the player can't join the server
								socket.close();
								in.close();
								out.flush();
								out.close();
								break;
							}
							else if (code == 1)
							{
								// receive other player information because
								// some player join the server or getting information of
								// all player after joining the lobby
								
								// algorithm is first read integer index of the player,
								// then integer length of the name of the player,
								// then the string name of the player
								StringBuffer playerNameStringBuffer = new StringBuffer();
								
								waitInput(4);
								int index = in.readInt();
								waitInput(4);
								int stringLength = in.readInt();
								
								for (int i = 0; i < stringLength; i++)
								{
									waitInput(2);
									playerNameStringBuffer.append(in.readChar());
								}
								
								String playerName = playerNameStringBuffer.toString();
								
								// add the player to gameData and to lobby view
								synchronized (gameData) 
								{
									gameData.addPlayer(index, playerName);
								}
								synchronized (lobbyView) 
								{
									if (playerName.compareTo(GameLobbyView.this.playerName) == 0)
									{
										lobbyView.addPlayer(index, playerName, true);										
									}
									else
									{
										lobbyView.addPlayer(index, playerName, false);
									}
								}
								GameLobbyView.this.repaint();
							}
							else if (code == 2)
							{
								// someone disconnected from the server,
								// must removed them from the list of player
								
								// read index of the player who disconnected from server
								waitInput(4);
								int index = in.readInt();
								
								// remove the player from the player list
								synchronized (gameData) 
								{
									gameData.removePlayer(index);
								}
								if (currentView == lobbyView)
								{
									synchronized (lobbyView)
									{
										lobbyView.removePlayer(index);
									}									
								}
								else if (currentView == gameView)
								{
									synchronized (gameView) 
									{
										gameView.removePlayer(index);
									}									
								}
							}
							else if (code == 3)
							{
								// getting chat from player in lobby view
								
								// get player index who write the chat
								waitInput(4);
								int playerIndex = in.readInt();
								
								// get length of chat
								waitInput(4);
								int chatLength = in.readInt();
								
								StringBuffer chatStringBuffer = new StringBuffer();
								
								// read the whole chat
								for (int i = 0; i < chatLength; i++)
								{
									waitInput(2);
									chatStringBuffer.append(in.readChar());
								}
								
								// add the chat to the current view
								if (currentView == lobbyView)
								{
									lobbyView.addChat(
											gameData.getPlayerFromIndex(playerIndex).getName(),
											chatStringBuffer.toString()
									);
								}
								else if (currentView == gameView)
								{
									gameView.addChat(
											gameData.getPlayerFromIndex(playerIndex).getName(),
											chatStringBuffer.toString(), false
									);
								}
								
							}
							else if (code == 4)
							{
								// receive start instruction, going to the game view
								// and the game will be started
								synchronized (gameView) 
								{
									gameView.reset();
									synchronized (gameData.getPlayerList()) {
										gameView.addPlayer(gameData.getPlayerList(), playerName);									
									}
									setPanel(gameView);
									gameView.repaint();
									repaint();									
								}
							}
							else if (code == 5)
							{
								// receive the object name (or nouns) from the server
								// to be shown on top of the game view
								
								// read the length of the word then read the whole string
								// then showed it inside game view
								waitInput(4);
								
								int wordLength = in.readInt();
								
								StringBuffer wordStringBuffer = new StringBuffer();
								for (int i = 0; i < wordLength; i++)
								{
									waitInput(2);
									wordStringBuffer.append(in.readChar());
								}
								
								synchronized (gameView) 
								{
									gameView.setWord(wordStringBuffer.toString());
								}
							}
							else if (code == 6)
							{
								// received game turn time remaining from the server (in seconds)
								// then showed it inside game view
								waitInput(4);
								int time = in.readInt();
								
								synchronized (gameView) {
									gameView.setTime(time);
								}
							}
							else if (code == 7)
							{
								// received instruction to give additional score to certain player
								// first read the index of the player then the additional score
								// the update the score on the player list
								waitInput(8);
								int playerIndex = in.readInt();
								int additionalScore = in.readInt();
								
								synchronized (gameData) 
								{
									gameData.addPlayerScore(playerIndex, additionalScore);
									synchronized (gameView) 
									{
										gameView.updatePlayer(gameData.getPlayerList());
									}
								}
							}
							else if (code == 8)
							{
								// received instruction to clear the image
								// because turn is over and the next turn
								// someone going to draw new image
								synchronized (gameData) 
								{
									gameData.resetPlayerCondition();
									gameData.setStillDrawing(false);
								}
							}
							else if (code == 9)
							{
								// someone start drawing, going to change the name of the player
								// who is drawing by getting their index from server
								waitInput(4);
								int playerTurn = in.readInt();
								synchronized (gameData) 
								{
									gameView.setPlayerTurn(gameData.getPlayerFromIndex(playerTurn).getName());
								}
							}
							else if (code == 10)
							{
								// receive image data send by server from the player who in this turn is
								// drawing the image
								// there's six integer sent from server
								// thickness, color red value, color green value, color blue value, x coordinate, y coordinate
								waitInput(24);
								int data[] = {in.readInt(), in.readInt(), in.readInt(), in.readInt(), in.readInt(), in.readInt()};
							
								// update the new image and show it on the canvas
								gameData.addTexture(data);
								synchronized (gameView) 
								{
									gameView.repaint();
								}
							}
							else if (code == 11)
							{
								// receive code from server indicating the player who is drawing the image
								// stop drawing the line (release left mouse)
								gameData.stopDrawing();
							}
							else if (code == 12)
							{
								// reading broadcast from the server, the broadcast from server will be
								// shown fully without any edit
								// first read the length of the broadcast, then read the whole broadcast,
								// and then show it inside game view chat list
								waitInput(4);
								int stringLength = in.readInt();
								
								StringBuffer broadcastStringBuffer = new StringBuffer();
								
								for (int i = 0; i < stringLength; i++)
								{
									waitInput(2);
									broadcastStringBuffer.append(in.readChar());
								}
								
								gameView.addChat(null, broadcastStringBuffer.toString(), true);
							}
							else if (code == 13)
							{
								// receive instruction to disconnect from the server because the
								// game is already finished
								disconnectFromServer();
							}
						}
						
						// suspend the thread from reading the input stream because the game is already finished
						setMustRun(false);
					}
					catch (IOException e)
					{
						// exception happen because there's some error to connect to the server
						// or the player timeout (no respond from server more than 10 seconds) or 
						// the player going back to the MainMenuView and closing the input stream
						// which lead to exception because waitInput trying to read from closed
						// input stream
						
						try 
						{
							// try to close again because it could happen
							in.close();
							out.flush();
							out.close();
						} 
						catch (IOException e1) 
						{
							
						}
						
						// send signal to main frame to change the view back to the main menu
						lobbyListener.propertyChange(new PropertyChangeEvent(this, "Disconnected", false, true));
						
						// suspend the thread
						setMustRun(false);
					}
					catch (InterruptedException e)
					{
						e.printStackTrace();
					}
				}
				else
				{
					try
					{
						// sleep thread if still not used
						Thread.sleep(50);						
					}
					catch (InterruptedException e)
					{
						
					}
				}
			}
		}
		
		/**
		 * Set the condition of the runnable
		 * @param run : true if runnable need to run, false if need to suspended
		 */
		public void setMustRun(boolean run)
		{
			mustRun = run;
		}
		
		/**
		 * Respond to test byte send by server by sending the test byte back to server
		 * @throws IOException if can't send test byte to server
		 */
		private void respondTestByte() throws IOException
		{
			synchronized (out) 
			{
				out.writeByte(0);
			}
		}
		
		/**
		 * Method to wait for certain amount of byte exist in input stream to prevent
		 * EOFException when reading input stream
		 * @param n : amount of byte needed
		 * @throws IOException if input stream is closed
		 * @throws InterruptedException if thread is interrupted
		 */
		public void waitInput(int n) throws IOException, InterruptedException
		{
			while(in.available() < n)
			{
				if (System.currentTimeMillis() - lastInput >= 10000)
				{
					// no input more than 10 seconds, throw an exception
					// to disconnect the player
					throw new IOException();
				}
				Thread.sleep(10);
			}
			lastInput = System.currentTimeMillis();
		}
	};

	@Override
	public void reset() {
		
		// clear view and data, change view to lobby view
		
		this.lobbyView.reset();
		this.setPanel(this.lobbyView);
		this.gameView.reset();
		
		this.gameData.reset();
	}
	
	/**
	 * change panel that will be shown
	 * @param panel : JPanel that will be shown
	 */
	public void setPanel(JPanel panel)
	{
		// remove current panel and change to the new one
		this.removeAll();
		this.add(panel);
		this.currentView = panel;
	}
	
	/**
	 * Disconnect the player from the server
	 */
	public void disconnectFromServer()
	{
		try
		{
			// sending disconnect instruction to server and close
			// all data stream
			
			synchronized (this.out) 
			{
				this.out.writeByte(2);
			}
			this.in.close();
			this.out.flush();
			this.out.close();
			
			this.reset();
		}
		catch (IOException e) 
		{
			
		}
	}
}
