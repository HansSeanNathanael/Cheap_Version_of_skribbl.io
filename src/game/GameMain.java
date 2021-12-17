package game;

import javax.swing.SwingUtilities;

/**
 * Main class for the game application
 * @author Toshiba
 *
 */
public class GameMain {

	private static GameMainFrame mainFrame;
	
	// Runnable for UI to be run on Event Dispatcher Thread
	// by called in SwingUtilities.invokeLater
	private static Runnable UIThread = new Runnable() {
		
		@Override
		public void run() {
			mainFrame = new GameMainFrame();
			mainFrame.setVisible(true);
		}
	};
	
	/**
	 * Main function
	 * @param args : arguments given by user when run the program
	 */
	public static void main(String[] args) {
		
		SwingUtilities.invokeLater(UIThread);
		
	}
}
