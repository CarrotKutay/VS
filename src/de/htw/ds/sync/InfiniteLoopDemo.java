package de.htw.ds.sync;

/**
 * Burns through a lot of processing power trying to figure out if
 * {@code true} really means {@code true}.
 */
public class InfiniteLoopDemo {

	/**
	 * Loops while {@code true} really is {@code true}.
	 * @param args the runtime arguments
	 */
	public static void main (final String[] args) {
		for (int index = Runtime.getRuntime().availableProcessors() - 1; index >= 0; --index) {
			new Thread( () -> { while (true); }, "seek truth - " + index).start();
		}
	}
}