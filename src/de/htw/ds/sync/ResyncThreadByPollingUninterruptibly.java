package de.htw.ds.sync;

import de.htw.tool.Copyright;


/**
 * Performs some work distributed over multiple child threads, resynchronizes UNINTERRUPTIBLY using
 * the POLLING technique.
 */
@Copyright(year=2013, holders="Sascha Baumeister")
public class ResyncThreadByPollingUninterruptibly {

	/**
	 * Performs some work UNINTERRUPTIBLY.
	 * @param args the runtime arguments
	 */
	static public void main (final String[] args) {
		workDistributed(args.length == 0 ? 4 : Integer.parseInt(args[0]));
	}

	
	/**
	 * Distributes some work UNINTERRUPTIBLY distributed over multiple child
	 * threads, and resynchronizes using POLLING.
	 * @param workerCount the number of worker threads
	 * @throws NegativeArraySizeException if the given workerCount is strictly negative
	 */
	static public void workDistributed (final int workerCount) throws NegativeArraySizeException {
		final Runnable worker = () -> {
			System.out.format("Thread %s: starting work.\n", Thread.currentThread().getName());
			try { ExampleWorker.work(10); } catch (ExampleCheckedException e) {}
			System.out.format("Thread %s: stoping work.\n", Thread.currentThread().getName());
		};

		System.out.format("Main-Thread: Executing workers in new threads!\n");
		final Thread[] workerThreads = new Thread[workerCount];
		for (int index = 0; index < workerCount; ++index) {
			(workerThreads[index] = new Thread(worker, "worker-thread-" + index)).start();
		} 

		System.out.format("Main-Thread: Waiting for child threads to die!\n");
		boolean interrupted = false;
		try {
			for (final Thread thread : workerThreads) {
				while (thread.isAlive()) {
					try {
						Thread.sleep(1);
					} catch (final InterruptedException exception) {
						interrupted = true;
					}
				}
			}
		} finally {
			if (interrupted) Thread.currentThread().interrupt();
		}

		System.out.format("Main-Thread: All child threads are terminated!\n");
	}
}