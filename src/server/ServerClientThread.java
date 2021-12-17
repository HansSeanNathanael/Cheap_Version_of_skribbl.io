package server;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.List;

/**
 * Thread for every connection of player<br>
 * Handling IO of every player
 * @author Toshiba
 *
 */
public class ServerClientThread extends Thread {

	// socket for one connection
	private Socket socket; 
	
	// list of other thread, used to send data between player
	private List<ServerClientThread> clientThreads;

	// IO for receive data from or send data to player
	// out (DataOutputStream) must always synchronized when used
	// to send data, this is to prevent race that will
	// make data send to player become broken (not ordered properly)
	private DataInputStream in;
	private DataOutputStream out;
	
	// Name of the player
	private String playerName = null;
	
	// index of thread inside clientThreads
	private int currentIndex = 0;
	
	// utility variable to check some attribute faster by not sending
	// request data to main server thread, but by server main thread
	// sending important data to fasten the process
	private String word; // word to be guessed by player
	private int answered = 0; // count how many player already answered right answer in this turn
	private boolean alreadyAnswered = false; // indicate if the player has already answered the question or not
	private boolean currentlyDrawing = false; // indicate if the player is the one who currently draw the image
	
	// indicate if hint already sent to player, player could get maximum two hints but not
	// always, to get two hints the length of word must more than 5 characters and
	// the random must not same with the first hint, player will always get at least 1 hints
	private int alreadySendHint[] = {-1, -1}; 
	
	
	// used to count time (like timer)
	// lastClientRespon used to tell when last time client send data to server
	// lastSendMessage used to tell when last time server send test data to verify user
	// still connected
	// maximum time player didn't respond server was 10 seconds, more than this, they will be kicked
	// server will try to verify by sending one byte data every 0.5 seconds
	private long lastClientRespon = System.currentTimeMillis();
	private long lastSendMessage = System.currentTimeMillis();
	
	// property change listener to connect player thread with server main thread
	// this is used to send signal to server main thread to random new word
	// and change turn
	private PropertyChangeListener gameListener;
	
	// property change event to send signal to server main thread to update
	// time remaining of this player of this thread, this is created because
	// this property change event send many times (every 0.5 seconds),
	// so this event is created because of it's high frequency use
	private PropertyChangeEvent changeTime = new PropertyChangeEvent(this, "Time", null, null);
	
	/**
	 * Constructor for the class, thread used to handle IO of one player
	 * @param socket : socket of connection
	 * @param clientThreads : list contain all player thread
	 * @throws IOException if can't create new thread
	 */
	public ServerClientThread(Socket socket, List<ServerClientThread> clientThreads, boolean couldJoin, PropertyChangeListener gameListener) throws IOException
	{
		this.socket = socket;
		this.clientThreads = clientThreads;
		
		this.gameListener = gameListener;
		
		// create data stream
		in = new DataInputStream(this.socket.getInputStream());
		out = new DataOutputStream(this.socket.getOutputStream());
		
		
		synchronized (this.out) 
		{
			if (couldJoin)
			{
				synchronized (clientThreads) 
				{
					// synchronized clientThreads to prevent race (likely to happen)
					
					// adding this thread on clientThreads (list containing all player thread)
					// and set index of this thread in clientThreads
					this.currentIndex = this.clientThreads.size();
					this.clientThreads.add(this);
				}

				// must not forget to synchronized out
				// sending data to user the thread is ready and request for player data
				this.out.writeByte(-1);
			}
			else
			{
				// sending instruction to player they can't join the lobby
				this.out.writeByte(-2);
				this.in.close();
				this.out.flush();
				this.out.close();
				this.socket.close();
			}
		}
	}
	
	@Override
	public void run() 
	{
		// handling all input stream

		this.lastClientRespon = System.currentTimeMillis();
		try
		{
			// code used to get instruction from player (in form of integer code)
			// 0 : user responded to test byte (test byte send to player every 0.5 seconds
			// to check is the player responded, if not responded in 10 seconds, they will
			// be kicked from the game)
			// 1 : player send name information of them, this will be sent by player in responses
			// of byte -1 send by this thread in constructor, this will the player to
			// other's player screen
			// 2 : player is sending disconnect instruction, the player disconnected and must
			// removed from all player's list
			// 3 : player send a chat message, must be send to the player self and other player
			// (chat from lobby view)
			// 4 : player pressed the start button, starting the game
			// 10 : input stream coordinate player drawing including color and thickness of the
			// line
			// 11 : signal all player, the player who draw the image stop drawing (release mouse)
			// 12 : player send a chat message, similar to code 2, but this is from GameView
			// 13 : instruction code for the player to tell them the game is done
			byte code;
			
			while(true)
			{
				
				// waiting for instruction and read after the instruction is in input stream
				this.waitInput(1);
				code = in.readByte();
				
				if (code == 0)
				{
					// test byte, just update time lastClientRespon
					lastClientRespon = System.currentTimeMillis();
				}
				else if (code == 1)
				{
					// new player enter the lobby, getting it's name
					// format type send by the player was byte-integer-chars
					// byte was the code, integer for the length of chars (string)
					// and chars was the name of player (string)
					
					StringBuffer playerNameStringBuffer = new StringBuffer();
					
					// read integer (length of player name string) after present in input stream
					this.waitInput(4);
					int stringLength = in.readInt();
					
					// read the player name
					for (int i = 0; i < stringLength; i++)
					{
						// char was 2 byte in Java, so be careful
						this.waitInput(2);
						playerNameStringBuffer.append(in.readChar());
					}
					
					// build the player name string
					this.playerName = playerNameStringBuffer.toString();						
					
					// send new player data to all other player using their thread connection
					// to send the data
					synchronized (clientThreads) 
					{
						
						// name of other player (player who already in the lobby)
						String otherPlayerName = null;
						
						for (ServerClientThread serverClientThread : this.clientThreads)
						{
							
							// thread of other player send new player name to their player
							serverClientThread.sendNewPlayerToClient(this.currentIndex, this.playerName);
							
							// getting name of other player (if not this to prevent sending
							// two same player to the new player), because in the client side
							// when they enter the game, their didn't create their player
							// but must wait for server to send their own name back to
							// create their player, confusing?
							// in simple way, all the name of player showed in the game
							// was obtained from the server, including their own player name
							if (serverClientThread != this)
							{
								otherPlayerName = serverClientThread.getPlayerName();
								
								// send non null other player name to this thread player
								// this is safe because if the player who have null name
								// load their name from their client, they will send it too
								// to this thread and this thread player will process the name
								if (otherPlayerName != null)
								{
									this.sendNewPlayerToClient(
											serverClientThread.getIndex(),
											otherPlayerName
									);
								}
							}
						}
					}
					
					// update timer
					this.lastClientRespon = System.currentTimeMillis();
				}
				else if (code == 2)
				{
					
					// the player send disconnect instruction
					synchronized (this.clientThreads) 
					{
						
						// send the removal player instruction to all player
						// except for this thread player because the player didn't need
						// to remove himself, but just remove all other player from list
						// and going to main menu (instructed in client code)
						for (ServerClientThread serverClientThread : this.clientThreads) {
							if (serverClientThread != this)
							{
								try
								{
									serverClientThread.sendRemovalPlayer(this.currentIndex);
								}
								catch(IOException ignored) {}
								// it is likely to happen if two player exit by force close 
								// in the same time will make their thread still not removed 
								// but their socket is closed and if their socket is closed,
								// just ignore it because their thread doesn't need to remove 
								// this thread player, so this is just send removal player 
								// instruction to all player still in lobby
							}
						}
						
						// send signal to main server thread to remove the player
						// from player list, player turn queue, and shuffle queue template
						this.gameListener.propertyChange(
								new PropertyChangeEvent(
										this, "RemovePlayer", null, this
								)
						);
						
						// updating all player thread index
						for (int i = 0; i < this.clientThreads.size(); i++)
						{
							this.clientThreads.get(i).setIndex(i);
						}	
					}
					
					// close socket to prevent player trying to write into input stream
					// and give them IOException
					this.socket.close();
					this.in.close();
					this.out.close();
					break; // must break, if not will going inside catch because trying to read input stream which is already closed
				}
				else if (code == 3)
				{
					
					// Reading chat input from player (chat from lobby view)
					// first by read length of string, then read whole string
					// then send the chat to all player
					waitInput(4);
					int chatLength = this.in.readInt();
					
					// StringBuffer to build the chat
					StringBuffer chatStringBuffer = new StringBuffer();
					
					// reading whole chat
					for (int i = 0; i < chatLength; i++)
					{
						waitInput(2);
						chatStringBuffer.append(this.in.readChar());
					}
					
					String chat = chatStringBuffer.toString();
					
					synchronized (this.clientThreads) 
					{
						// synchronized clientThread to prevent exception
						// send the chat to all player
						for (ServerClientThread serverClientThread : this.clientThreads) 
						{
							serverClientThread.sendChatToPlayer(this.currentIndex, chat);
						}
					}
					
					// update timer
					this.lastClientRespon = System.currentTimeMillis();
				}
				else if (code == 4)
				{
					// the player send the start instruction, this will tell the ServerGameRunnable
					// (main thread for server) to tell all player thread to send start
					// instruction to player, so they will go to game view, shuffle player's turn
					// and start the game
					this.gameListener.propertyChange(
							new PropertyChangeEvent(
									this, "StartGame", null, null
							)
					);
					this.lastClientRespon = System.currentTimeMillis();
				}
				else if (code == 10)
				{
					// player send drawing instruction, 6 integer sent by player,
					// order: thickness, red, green, blue, x coordinate, y coordinate
					// then send the data to all player thread to send it to player
					waitInput(24);
					int data[] = {in.readInt(), in.readInt(), in.readInt(), in.readInt(), in.readInt(), in.readInt()};
				
					// sending drawing data to all player (including the one who draw the image)
					if (this.currentlyDrawing)
					{
						synchronized (clientThreads) 
						{
							for (ServerClientThread serverClientThread : clientThreads) 
							{
								serverClientThread.sendImageTexture(data);
							}
						}
					}
				}
				else if (code == 11)
				{
					// player stop drawing, this is not limited to just the player who
					// are taking the turns drawing, but every player could send this instruction
					// if they release their left mouse inside canvas
					
					// sending stop drawing instruction to all player 
					// including the one who draw the image
					synchronized (clientThreads) 
					{
						for (ServerClientThread serverClientThread : clientThreads) 
						{
							serverClientThread.sendStopDrawingInstruction();
						}
					}
				}
				else if (code == 12)
				{
					// player send chat but from game view, not lobby view, the algorithm
					// is similar to code 2 (chat from lobby view), but have small change
					// to check if the chat is the answer to the object word, so this will
					// check the word, if it same, the player will get score and the player
					// who draw the image will get score, the player can't get score more than
					// once for every turn
					waitInput(4);
					int chatLength = this.in.readInt();
					
					StringBuffer chatStringBuffer = new StringBuffer();
					
					for (int i = 0; i < chatLength; i++)
					{
						waitInput(2);
						chatStringBuffer.append(this.in.readChar());
					}
					
					String chat = chatStringBuffer.toString();
					
					// this is the difference, there's two different possibilities
					// if the chat is same with the object word, this mean
					// player answered right, so this chat will be consumed and
					// not send to all player
					if (this.word.compareToIgnoreCase(chat) == 0)
					{
						// player answered the right answer
					
						if (this.alreadyAnswered == false && this.currentlyDrawing == false)
						{
							// in this case, the player will get score
							// this if condition is important, the player must not yet answered
							// because if they already answered, they should not get more score
							// and the player who is drawing can't chat anything
							
							// sending additional score to the player because answered the
							// right answer and sending score to player who is drawing too
							// player will always get score every time someone guess their 
							// image right
							synchronized (this.clientThreads) 
							{
								for (ServerClientThread serverClientThread : this.clientThreads) 
								{
									serverClientThread.sendBroadcast(playerName + " guess the right word");
									serverClientThread.sendScore(currentIndex, false);
									serverClientThread.addWhoAnsweredOne();
									
									if (serverClientThread.currentlyDrawing)
									{
										for (ServerClientThread serverClientThread2 : this.clientThreads)
										{
											serverClientThread2.sendScore(serverClientThread.getIndex(), true);
										}
									}
								}
							}
							
							// set the player to already answered the question
							// to prevent them answered again and send the
							// whole object word to player
							this.alreadyAnswered = true;
							this.sendWord(word);
						}
					}
					else 
					{
						// the chat is just going to sent to all player but if they
						// are the one who is drawing, they can't send any chat
						if (this.currentlyDrawing == false)
						{
							// the player is not the one who is drawing, so
							// the chat will be sent to all player
							synchronized (this.clientThreads) 
							{
								for (ServerClientThread serverClientThread : this.clientThreads) 
								{
									serverClientThread.sendChatToPlayer(this.currentIndex, chat);
								}
							}							
						}
					}
					
					// update timer
					this.lastClientRespon = System.currentTimeMillis();
				}
			}
		}
		catch(IOException | InterruptedException e)
		{
			// there's problem in thread connection, it is likely because the player
			// close the program by not sending disconnect instruction
			// or the connection was bad and kicked after 10 seconds not responding
			
			// the algorithm is same with disconnect instruction
			// code == 2
			try 
			{
				synchronized (this.clientThreads) 
				{
					for (ServerClientThread serverClientThread : this.clientThreads) 
					{
						if (serverClientThread != this)
						{
							try
							{
								serverClientThread.sendRemovalPlayer(this.currentIndex);
							}
							catch(IOException ignored) {}
						}
					}
					
					this.gameListener.propertyChange(
							new PropertyChangeEvent(
									this, "RemovePlayer", null, this
							)
					);
					
					for (int i = 0; i < this.clientThreads.size(); i++)
					{
						this.clientThreads.get(i).setIndex(i);
					}
				}
				
				this.in.close();
				this.out.flush();
				this.out.close();
				this.socket.close();
			} 
			catch (IOException e1) 
			{
				
			}
		}
	}
	
	/**
	 * Used to send test byte, test byte just consist one byte with value 0
	 * that need to answered by the user to tell the server they are still active
	 * @throws IOException if the socket is already closed
	 */
	public void sendTestByte() throws IOException
	{
		synchronized (this.out) 
		{	
			this.out.writeByte(0);
		}
	}
	
	/**
	 * Used to get the player name
	 * @return name of the player
	 */
	public String getPlayerName()
	{
		return this.playerName;
	}
	
	/**
	 * Used to change index of the thread
	 * thread index is the same as player index which mean change the index of the player,
	 * but it is not likely (will be handled by client program, not the server)
	 * @param index : the new index
	 */
	public void setIndex(int index)
	{
		this.currentIndex = index;
	}
	
	/**
	 * Used to get index of the thread (thread index is the same as player index)
	 * @return index of the thread
	 */
	public int getIndex()
	{
		return this.currentIndex;
	}
	
	/**
	 * Send stream data of new player to all player
	 * @param index : index of the new player
	 * @param newPlayer : name of the new player
	 * @throws IOException if the socket is already closed
	 */
	public void sendNewPlayerToClient(int index, String newPlayer) throws IOException
	{
		synchronized (this.out) 
		{
			this.out.writeByte(1);
			this.out.writeInt(index);
			this.out.writeInt(newPlayer.length());
			this.out.writeChars(newPlayer);
		}
	}
	
	/**
	 * Send stream byte code of player who left the game
	 * @param index : index of the player
	 * @throws IOException if the socket is already closed
	 */
	public void sendRemovalPlayer(int index) throws IOException
	{
		synchronized (this.out) 
		{
			this.out.writeByte(2);
			this.out.writeInt(index);
		}
	}
	
	/**
	 * Send stream byte code of chat from player with certain index
	 * @param playerIndex : index of the player who send the chat
	 * @param chat : chat sent by the player
	 * @throws IOException if the output stream or socket is already closed
	 */
	public void sendChatToPlayer(int playerIndex, String chat) throws IOException
	{
		synchronized (this.out) 
		{
			this.out.writeByte(3);
			this.out.writeInt(playerIndex);
			this.out.writeInt(chat.length());
			this.out.writeChars(chat);
		}
	}
	
	/**
	 * Send start instruction the the player (client)
	 * @throws IOException if the output stream or socket is already closed
	 */
	public void sendStartInstruction() throws IOException
	{
		synchronized (this.out) 
		{
			this.out.writeByte(4);			
		}
	}
	
	/**
	 * Send object word to the player, if the player is the one who drawing
	 * the whole object name will be sent, but if not, just question marks that
	 * will be sent
	 * @param word : object word to be sent
	 */
	public void sendWord(String word)
	{
		
		// save the word for faster processing and reset the hint data
		
		this.word = word;
		
		for (int i = 0; i < this.alreadySendHint.length ; i++)
		{
			this.alreadySendHint[i] = -1;
		}
		
		try
		{
			if (this.currentlyDrawing || this.alreadyAnswered)
			{
				// sending the whole word if the player is the one who is drawing
				// or if they already answered right
				synchronized (this.out) 
				{
					this.out.writeByte(5);
					this.out.writeInt(word.length());
					this.out.writeChars(word);
				}
			}
			else
			{
				
				// sending question mark if player is not drawing and still note
				// answered right
				synchronized (this.out) 
				{
					this.out.writeByte(5);
					this.out.writeInt(word.length());
					
					for(int i = 0; i < word.length(); i++)
					{
						this.out.writeChar('?');
					}
				}				
			}
		}
		catch (IOException e)
		{
			
		}
	}
	
	/**
	 * Sending hint of object word if the player still not answered right and not the one
	 * who is drawing
	 * @param index : index generated by main thread to tell which character will be showed to player
	 * @param hint : hint data to save hint character and indicate if the hint already sent to player
	 */
	public void sendHint(int index, int hint)
	{
		if (this.alreadyAnswered == false && this.currentlyDrawing == false && this.alreadySendHint[hint] == -1)
		{
			// player must not yet answered right and not the one who is drawing and
			// the hint not yet send to the player
			
			// save the character hint
			this.alreadySendHint[hint] = index;

			try
			{
				synchronized (this.out) 
				{
					// creating the question marks string then change the hint character to
					// the right character, then sent the hint string to player
					
					StringBuffer hintStringBuffer = new StringBuffer();
					
					for(int i = 0; i < this.word.length(); i++)
					{
						hintStringBuffer.append('?');
					}
					
					for (int i = 0; i <= hint; i++)
					{
						hintStringBuffer.setCharAt(
								this.alreadySendHint[i], 
								this.word.charAt(this.alreadySendHint[i])
						);
					}
					
					String hintString = hintStringBuffer.toString();
					
					
					this.out.writeByte(5);
					this.out.writeInt(hintString.length());
					this.out.writeChars(hintString);
				}			
			}
			catch (IOException e)
			{
				
			}
		}
	}
	
	/**
	 * Send time remaining to the player, this will be sent every 0.5 seconds together with test byte
	 * @param time : remaining time in seconds
	 */
	public void sendTimeCondition(int time)
	{
		try
		{
			synchronized (this.out) 
			{
				// code 6 for remaining time code
				this.out.writeByte(6);
				this.out.writeInt(time);
			}			
		}
		catch (IOException e)
		{
			
		}
	}
	
	/**
	 * Give additional score to player who answered right or to the one who is drawing the image
	 * if there's someone answered right
	 * @param index : index of the player who will get additional score
	 * @param artist : true if the player who will get additional score is the one who is drawing the image
	 */
	public void sendScore(int index, boolean artist)
	{
		synchronized (clientThreads) 
		{			
			try
			{
				if (artist)
				{
					// sending additional 8 score to player who draw the image
					synchronized (this.out) 
					{
						this.out.writeByte(7);
						this.out.writeInt(index);
						this.out.writeInt(8);
					}
				}
				else
				{
					// sending additional score to player who answered right
					synchronized (this.out) 
					{
						this.out.writeByte(7);
						this.out.writeInt(index);
						this.out.writeInt((this.clientThreads.size() - this.answered) * 4 + 16);
						
						//score calculation f(x) = (amount of player - how many answered right) * 4 + 16
					}					
				}
			}
			catch (IOException e)
			{
				
			}
		}
	}
	
	/**
	 * Send instruction reset drawing to the player to reset the image data
	 */
	public void sendResetDrawing()
	{
		try
		{
			synchronized (this.out) 
			{
				this.out.writeByte(8);
			}
		}
		catch (IOException e)
		{
			
		}
	}
	
	/**
	 * Send index of player who in this turn is the one who draw the image
	 * @param index
	 */
	public void sendTurn(int index)
	{
		try
		{
			synchronized (this.out) 
			{
				this.out.writeByte(9);
				this.out.writeInt(index);
			}
		}
		catch (IOException e)
		{
			
		}
	}
	
	/**
	 * Send image data drawn by the player who is taking turn to draw
	 * @param data : array of integer consisting data of the image (thickness, color, and line coordinate)
	 */
	public void sendImageTexture(int data[])
	{
		try
		{
			synchronized (this.out) 
			{
				this.out.writeByte(10);
				for (int i : data) {
					this.out.writeInt(i);
				}
			}
		}
		catch (IOException e)
		{
			
		}
	}
	
	/**
	 * Send signal to all player to tell them the player who is drawing was stop drawing
	 * the line (relase left mouse click or going out of canvas)
	 */
	public void sendStopDrawingInstruction()
	{
		try
		{
			synchronized (this.out) 
			{
				this.out.writeByte(11);
			}
		}
		catch (IOException e)
		{
			
		}
	}
	
	/**
	 * Send broadcast message to player, almost similar to the chat but this
	 * will send the whole text without any format to be processed by the client
	 * @param broadcastText : broadcast text to be sent to the player
	 * @throws IOException if the socket or output stream is already closed
	 */
	public void sendBroadcast(String broadcastText) throws IOException
	{
		synchronized (this.out) 
		{
			this.out.writeByte(12);
			this.out.writeInt(broadcastText.length());
			this.out.writeChars(broadcastText);
		}
	}
	
	/**
	 * Send signal to player to indicate the game is finished and they must
	 * going back to the main menu view
	 */
	public void sendGameFinishedInstruction()
	{
		try
		{
			synchronized (this.out) 
			{
				this.out.writeByte(13);
			}			
		}
		catch (IOException e)
		{
			
		}
	}
	
	/**
	 * Increment amount of player who already answered right
	 */
	public void addWhoAnsweredOne()
	{
		this.answered  += 1;
	}
	
	/**
	 * Get how many player already answered right
	 * @return amount of player who already answered right
	 */
	public int getHowManyAnswered()
	{
		return this.answered;
	}
	
	/**
	 * Set or edit amount of player who already answered right
	 * @param answered : new amount of player who already answered right
	 */
	public void setHowManyAnswered(int answered)
	{
		this.answered = answered;
	}
	
	/**
	 * Set the player condition, is he/she/it already answered the question or not
	 * @param alreadyAnswered : condition of the player
	 */
	public void setAlreadyAnswered(boolean alreadyAnswered)
	{
		this.alreadyAnswered = alreadyAnswered;
	}
	
	/**
	 * Set the player condition, is he/she/it the one who drawing in this turn or not
	 * @param currentlyDrawing : condition of the player
	 */
	public void setCurrentlyDrawing(boolean currentlyDrawing)
	{
		this.currentlyDrawing = currentlyDrawing;
	}
	
	/**
	 * This method will wait for the input stream to have certain amount
	 * of byte because DataInputStream store data in for of bytes
	 * if user send data in form of integer and in read in byte type
	 * this will lead the byte have wrong value, otherwise if it sent in byte type
	 * and read by in form of integer, this will lead to exception (EOFException)
	 * so if the integer will be read, there must be 4 byte in input stream
	 * 
	 * This method will suspend the reading of input stream until certain amount of byte
	 * stored in the input stream
	 * 
	 * @param n : amount of byte needed
	 * @throws IOException if the socket is closed
	 * @throws InterruptedException if the thread is interrupted when sleeping
	 */
	public void waitInput(int n) throws IOException, InterruptedException
	{
		// This method also dealing with sending test byte to user because this method
		// will block the thread and called many times and also sending game time remaining
		// request to server main thread to forward it back to this thread to send
		// the time, this is important because the time of the game was saved in the
		// server main thread and not in every player thread, main server thread also check
		// if the game is already started to prevent player thread sending time when the game
		// not yet started (waste of time and memory)
		//
		// Why time remaining and test byte was sent together? Because the time game remaining wasn't
		// sent to player when they are still in lobby view, but the test byte must be sent to player
		// while still in lobby view and in game view too, but actually maybe this method could be
		// improved more likely in near future (no time because final exam college third semester
		// is near and have to learn other subject)
	
		while(this.in.available() < n)
		{
			
			if (System.currentTimeMillis() - lastSendMessage >= 500)
			{
				// send test byte and game time remaining every 0.5 seconds
				this.lastSendMessage = System.currentTimeMillis();
				this.sendTestByte();
				
				this.gameListener.propertyChange(changeTime);
			}
			if (System.currentTimeMillis() - lastClientRespon >= 10000)
			{
				// timeout condition, no respond from player for more than 10 seconds
				throw new IOException();
			}
			Thread.sleep(20);
			
		}
		
		// updating time again, maybe this is a waste and could removed, but
		// still don't have time to debug, so this will be left for now
		// except the update of lastClientRespon, must not removed!
		if (System.currentTimeMillis() - lastSendMessage >= 500)
		{
			this.lastSendMessage = System.currentTimeMillis();
			this.sendTestByte();
		}
		this.gameListener.propertyChange(changeTime);
		this.lastClientRespon = System.currentTimeMillis();
	}
	
	/**
	 * Force close this thread connection by closing the socket because
	 * the server was shut down
	 */
	public void forceCloseConnection()
	{
		try
		{
			this.socket.close();
			this.in.close();
			this.out.close();
		}
		catch(IOException e)
		{
			
		}
	}
}
