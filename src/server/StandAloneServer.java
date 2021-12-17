package server;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Random;

/**
 * StandAloneServer class that could be run to make the server, so not the room master
 * that need to create the server. The algorithm almost same with ServerGameRunnable but with small
 * difference, when game finished if the server is embedded, the thread for the server will be closed
 * if the game is finished or the lobby master going back to the main menu. In this case, the server
 * will not deleted, but every variable is reset to initial value
 * 
 * this is still not tried many times and probably have many bugs, so it is recommended to just
 * use the embedded one
 * @author Toshiba
 *
 */
public class StandAloneServer {

	// socket connection variable, used to make server socket
	private static int port = 8080; // default port (actually value 8080 won't be used)
	private static ServerSocket serverSocket;  // used for server

	// utility variable for the game view
	private static Random random; // randomizer to shuffle the player turn and the hint given to player	
	private static int round = 4; // indicate how many game round left
	
	// save all thread list of connection, used to send data from one player to all player
	// clientThreads save the player thread, index of player thread comes from
	// index of them inside clientThreads (also used as their index in game)
	// 
	// playerTurnList save player thread too but was shuffled, playerTurnList will copy
	// clientThreads then shuffle it, then playerQueueTurn was queue to store the shuffled
	// player thread list that will be used when play the game. In a simple way,
	// playerTurnList was shuffled clientThreads used as template for player turn and
	// playerQueueTurn was queue of playerTurnList
	private static List<ServerClientThread> clientThreads = new ArrayList<ServerClientThread>();
	
	private static List<ServerClientThread> playerTurnList = new ArrayList<ServerClientThread>();
	
	private static Deque<ServerClientThread> playerQueueTurn = new ArrayDeque<ServerClientThread>();
	
	// player thread who is currently in drawing mode
	private static ServerClientThread currentlyDrawing;
	
	// indicate if the game not yet started and the player could join the server
	// if this is true, the player will get permission to enter the lobby view, but
	// if false, the player will get rejection code
	private static boolean couldJoin = false;
	
	// utilities variable for the game view, thingName was object name drawn
	// by player, startTime was used to save when was the turn started
	// and TIME_PER_ROUND was time for every turn, hint was used to
	// indicate if which character of object word will be shown to player as a hint,
	// if the value was -1, that means the hint is not yet randomized (
	// index 0 for the first hint and index 2 for the second hint)
	private static String thingName;
	private static long startTime;
	private static final long TIME_PER_ROUND = 40000;
	private static int hint[] = {-1, -1};
	
	// listener for all player thread to connect the player thread with this
	// server main thread, this is used for some instruction that needed to
	// be processed in the main server thread like shuffling the player
	// when time reached 0, removing player thread from clientThread, playerTurnList
	// and playerQueueTurn, and starting the game (server main thread is the one who shuffle
	// the player turn and save when a turn started)
	private static PropertyChangeListener gameConditionListener = new PropertyChangeListener() {
		
		@Override
		public void propertyChange(PropertyChangeEvent evt) 
		{
			if (evt.getPropertyName().compareTo("RemovePlayer") == 0)
			{
				// removing a player thread from clientThread, playerTurnList, and
				// playerQueueTurn
				removePlayerFromList((ServerClientThread) evt.getNewValue());
			}
			else if (evt.getPropertyName().compareTo("StartGame") == 0)
			{
				// StartGame instruction must be sent to this server main thread
				// because server main thread must know if the game is started
				// and will denied when try to connect
				if (clientThreads.size() > 1)
				{
					// the game will not started if there is just 1 player inside the lobby
					// at least 2 player must join the server (including lobby host)
					
					couldJoin = false;
					random = new Random(System.currentTimeMillis());
					
					// send start game instruction to all player
					synchronized (clientThreads) 
					{
						for (ServerClientThread serverClientThread : clientThreads) {
							playerTurnList.add(serverClientThread);
							try 
							{
								serverClientThread.sendStartInstruction();
							} 
							catch (IOException e) 
							{
								
							}
						}
					}
					
					
					// shuffle the player turn
					synchronized (playerTurnList) 
					{
						ServerClientThread temp;
						int firstIndex;
						int secondIndex;
						for (int i = 0; i < playerTurnList.size() * 3; i++)
						{
							firstIndex = random.nextInt(playerTurnList.size());
							secondIndex = random.nextInt(playerTurnList.size());
							temp = playerTurnList.get(firstIndex);
							playerTurnList.set(firstIndex, playerTurnList.get(secondIndex));
							playerTurnList.set(secondIndex, temp);
						}
						
						synchronized (playerQueueTurn) 
						{
							for (ServerClientThread serverClientThread : playerTurnList) {
								playerQueueTurn.add(serverClientThread);
							}
						}
					}
					
					// random the object name, reset the hint
					thingName = ServerUtilityData.getRandomThingName();
					for (int i = 0; i < hint.length; i++)
					{
						hint[i] = -1;
					}
					
					// send instruction to reset to all player and send the 
					// object name to player and set who get drawing for the first turn
					synchronized (clientThreads) 
					{
						currentlyDrawing = playerQueueTurn.poll();
						currentlyDrawing.setAlreadyAnswered(true);
						currentlyDrawing.setCurrentlyDrawing(true);
						currentlyDrawing.sendWord(thingName);
						currentlyDrawing.sendResetDrawing();
						currentlyDrawing.sendTurn(currentlyDrawing.getIndex());
						for (ServerClientThread serverClientThread : clientThreads) {
							if (serverClientThread != currentlyDrawing)
							{
								serverClientThread.sendResetDrawing();
								serverClientThread.sendTurn(currentlyDrawing.getIndex());
								serverClientThread.setAlreadyAnswered(false);
								serverClientThread.setCurrentlyDrawing(false);
								serverClientThread.sendWord(thingName);
							}
							serverClientThread.setHowManyAnswered(0);
						}
					}
					startTime = System.currentTimeMillis();
					
					// round used to indicate how many time the playerQueueTurn
					// could be refilled before the game ended
					// round = 3 means the game have 4 rounds for every player
					round = 3;
				}
			}
			else if (evt.getPropertyName().compareTo("Time") == 0)
			{
				// player thread requesting game round time remaining (in seconds)
				
				if (couldJoin == false)
				{
					// this request will be granted if the game is already started
					// if not, no need to update the time
					
					// remaining time is in seconds
					int remainingTime = (int)(TIME_PER_ROUND - (System.currentTimeMillis() - startTime)) / 1000;
					
					synchronized ((ServerClientThread) evt.getSource()) 
					{
						// send the time to the thread who is requesting the time remaining
						((ServerClientThread) evt.getSource()).sendTimeCondition(remainingTime);
					}
					
					// conditions when the time is below certain value
					
					if (remainingTime <= 20)
					{
						// below 20 seconds, the player will get the first hint
						
						if (hint[0] == -1)
						{
							// random the hint if not yet generated
							hint[0] = random.nextInt(thingName.length());
						}
						
						// send the hint to all player
						synchronized (clientThreads) 
						{
							for (ServerClientThread serverClientThread : clientThreads) {
								serverClientThread.sendHint(hint[0], 0);
							}
						}
					}
					
					if (remainingTime <= 10 && thingName.length() > 5)
					{
						// below 10 seconds and the object name length is more than 5 characters,
						// the player will get seconds hint
						
						if (hint[1] == -1)
						{
							// random the hint if not yet generated
							// because this is just a simple random,
							// the player could get same hint as the first hint
							// thus making the second hint useless and the player
							// just getting one hint
							hint[1] = random.nextInt(thingName.length());							
						}
						
						// sending hint to player
						synchronized (clientThreads) 
						{
							for (ServerClientThread serverClientThread : clientThreads) {
								serverClientThread.sendHint(hint[1], 1);
							}
						}
					}
					
					if (remainingTime <= 0)
					{
						// the time reached 0, the player drawing time is up
						// the turn will go to the next player
						
						if (round >= 0)
						{
							// the turn will go to the next player if the round value
							// is more than or same with 0, if not that means the game
							// is finished, the game will be ended, and server
							// will send game finished instruction to all player
							
							synchronized (clientThreads) 
							{
								synchronized (playerQueueTurn)
								{
									if (System.currentTimeMillis() - startTime >=  TIME_PER_ROUND)
									{
										// double check of time to prevent multiple process
										// from player thread and destroy the game by random
										// the new word multiple time and change the player turn
										// multiple time
										//
										// the algorithm is same with the one inside gameListener
										// StartGame request send by player thread
										
										thingName = ServerUtilityData.getRandomThingName();
										if (playerQueueTurn.size() == 0)
										{
											playerQueueTurn.addAll(playerTurnList);
											round -= 1;
										}
										for (int i = 0; i < hint.length; i++)
										{
											hint[i] = -1;
										}
										
										currentlyDrawing = playerQueueTurn.poll();
										currentlyDrawing.setAlreadyAnswered(true);
										currentlyDrawing.setCurrentlyDrawing(true);
										currentlyDrawing.sendResetDrawing();
										currentlyDrawing.sendWord(thingName);
										currentlyDrawing.sendTurn(currentlyDrawing.getIndex());
										for (ServerClientThread serverClientThread : clientThreads) {
											if (serverClientThread != currentlyDrawing)
											{
												serverClientThread.sendResetDrawing();
												serverClientThread.sendTurn(currentlyDrawing.getIndex());
												serverClientThread.setAlreadyAnswered(false);
												serverClientThread.setCurrentlyDrawing(false);
												serverClientThread.sendWord(thingName);
											}
											serverClientThread.setHowManyAnswered(0);
										}
										
										startTime = System.currentTimeMillis();										
									}
								}
							}
						}
						else
						{
							// the round reach < 0, the game is finished, the player
							// will get game finished instruction to tell them the game 
							// is already finished
							
							synchronized (clientThreads) 
							{
								for (ServerClientThread serverClientThread : clientThreads) {
									serverClientThread.sendGameFinishedInstruction();
								}
							}
						}
					}
				}

			}
		}
	};
	
	/**
	 * Remove player thread from all list of player thread. Removed from 
	 * clientThread, playerTurnList, and playerQueueTurn
	 * @param playerThread : player thread to be removed
	 */
	private static void removePlayerFromList(ServerClientThread playerThread)
	{
		// removing the player thread form all list of player thread
		
		synchronized (clientThreads) 
		{
			clientThreads.remove(playerThread);
			if (clientThreads.size() == 1 && couldJoin == false)
			{
				// stop the game if the there's just one player left inside the game
				// couldJoin to indicate if the player inside game view or lobby view
				// and this one is for game view
				sendGameFinishedInstruction();
			}
		}
		synchronized (playerTurnList) 
		{
			playerTurnList.remove(playerThread);
		}
		synchronized (playerQueueTurn) 
		{
			playerQueueTurn.remove(playerThread);
		}
	}
	
	/**
	 * Main method for the application, open the server connection
	 * @param args : arguments passed by the creator of the server
	 */
	public static void main(String args[])
	{
		couldJoin = true;
		if (args.length > 0)
		{
			
			try 
			{	
				port = Integer.parseInt(args[0]);
				serverSocket = new ServerSocket(port);
				System.out.println(port);
				System.out.println(serverSocket);
				try
				{
					
					// creating new thread for every connection
					while(true)
					{
						System.out.println("Waiting for player");
						// creating the thread, if the player could join (game still not started)
						// the thread will run, but if not, the thread will not run
						// and the thread will send denied join instruction
						ServerClientThread newClient = new ServerClientThread(
								serverSocket.accept(), clientThreads, couldJoin,
								gameConditionListener
						);
						if (couldJoin)
						{
							newClient.start();					
							System.out.println("Player join the server");
						}
					}				
				}
				catch (IOException e) 
				{
					
				}
			}
			catch (NumberFormatException e)
			{
				System.err.println("Error, port must number");
			}
			catch (IOException e) 
			{
				System.err.println("Failed to create the server");
			}
			
			couldJoin = true;		
			
		}
	}
	
	/**
	 * stop the game, send finished instruction to all player and reset the server
	 */
	public static void sendGameFinishedInstruction()
	{
		synchronized (clientThreads) 
		{
			for (ServerClientThread serverClientThread : clientThreads) {
				serverClientThread.sendGameFinishedInstruction();
			}				
			
			clientThreads.clear();
			playerTurnList.clear();
			playerQueueTurn.clear();
			
			currentlyDrawing = null;
			thingName = null;
		}
		couldJoin = true;
	}
}
