package game;

import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * JTextField with some modification to have limit of character that can be written by user
 * @author Toshiba
 *
 */
public class RTextField extends JTextField {

	// limit of the text field
	private int limit = 0;
	
	// auto generated serial version
	private static final long serialVersionUID = 2894947423866444952L;
	
	// Runnable to be run on Event Dispatcher Thread to update content of the
	// text field
	private Runnable updateThread = new Runnable() {
		
		@Override
		public void run() {
			setEditable(false);
			String text = getText();
			setText(text.substring(0, limit));
			setEditable(true);
		}
	};

	/**
	 * Constructor of the text field with its limit character
	 * @param limit : limit character could be written by user
	 */
	public RTextField(int limit)
	{
		this.limit = limit;
		
		// listener to check the length of string written by user
		// listener will check every time user do something to the
		// text input, like write or delete character
		this.getDocument().addDocumentListener(new DocumentListener() {
			
			@Override
			public void removeUpdate(DocumentEvent e) {
				checkLength();
			}
			
			@Override
			public void insertUpdate(DocumentEvent e) {
				checkLength();
			}
			
			@Override
			public void changedUpdate(DocumentEvent e) {
				checkLength();
			}
		});
		
	}
	
	/**
	 * Checking length of the string written by the user
	 */
	private void checkLength()
	{
		// if the length surpass limit, this trim the string and
		// will update the UI
		if (this.getText().length() > limit)
		{
			SwingUtilities.invokeLater(updateThread);
		}
	}
	
	/**
	 * Get limit length of the string that can be written
	 * @return limit length of the string
	 */
	public int getLimit()
	{
		return this.limit;
	}
	
	/**
	 * Change limit to some value
	 * @param limit : new limit for the text field
	 */
	public void setLimit(int limit)
	{
		this.limit = limit;
	}
	
	
}
