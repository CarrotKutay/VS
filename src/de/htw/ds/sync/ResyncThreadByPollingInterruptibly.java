package de.htw.ds.sync;

import de.htw.tool.Copyright;


/**
 * Performs some work distributed over multiple child threads, resynchronizes UNINTERRUPTIBLY using
 * the POLLING technique. Note that any remaining active child threads should be stopped when ending
 * resynchronization prematurely (by exception)!
 */
@Copyright(year=2013, holders="Sascha Baumeister")
public class ResyncThreadByPollingInterruptibly {

	/**
	 * Performs some work INTERRUPTIBLY.
	 * @param args the runtime arguments
	 * @throws InterruptedException if the operation is interrupted
	 */
	static public void main (final String[] args) throws InterruptedException {
		workDistributed(args.length == 0 ? 4 : Integer.parseInt(args[0]));
	}

	
	/**
	 * Distributes some work UNINTERRUPTIBLY distributed over multiple child
	 * threads, and resynchronizes using POLLING.
	 * @param workerCount the number of worker threads
	 * @throws NegativeArraySizeException if the given workerCount is strictly negative
	 * @throws InterruptedException if the operation is interrupted
	 */
	static public void workDistributed (final int workerCount) throws NegativeArraySizeException, InterruptedException {
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
		try {
			for (final Thread thread : workerThreads) {
				while (thread.isAlive()) {
					Thread.sleep(1);
				}
			}
		} catch (final Throwable exception) {
			for (final Thread thread : workerThreads) {
				thread.interrupt();
			}
			throw exception;
		}

		System.out.format("Main-Thread: All child threads are terminated!\n");
	}
}