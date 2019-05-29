package de.htw.ds.sync;

import de.htw.tool.Copyright;

/**
 * Thread demo using explicit runnable class.
 */
@Copyright(year=2013, holders="Sascha Baumeister")
public class ThreadForkDemo {

	/**
	 * Application entry point. The runtime arguments must at least
	 * consist of a display text.
	 * @param args the runtime arguments
	 * @throws InterruptedException 
	 */
	static public void main (final String[] args) throws InterruptedException {
		System.out.println(Thread.currentThread().getName());

		final String text = args[0];
		final Runnable runnable1 = DemoRunnables.newLambdaRunnable(text);

		final Thread thread = new Thread(runnable1, "demo-thread");
		thread.setPriority(Thread.MIN_PRIORITY);	// default is parent's priority
		thread.setDaemon(false);					// default is false

		// execute thread's runnable asynchronously with minimal priority
		thread.start();

		// executes thread's runnable synchronously within the current (main) thread!
		thread.run();		

		// the stylish way to achieve a guaranteed deadlock: make a thread wait for itself to end.
		// the "advantage" over "while(true);" is that the former does not occupy a processor at 100%.
		Thread.currentThread().join();
	}
 }