package game;

/**
 * Interface for View to override reset method to reset their view
 * the view will be reusable
 * @author Toshiba
 *
 */
public interface ResetableView {
	
	/**
	 * Reset the view to make them reusable
	 */
	void reset();
}
