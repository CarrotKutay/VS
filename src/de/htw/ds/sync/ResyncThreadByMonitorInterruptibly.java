package de.htw.ds.sync;

import de.htw.tool.Copyright;


/**
 * Performs some work distributed over multiple child threads, resynchronizes INTERRUPTIBLY using
 * the blocking technique {@link Object#wait()}. Note that any remaining active child threads should
 * be stopped when ending resynchronization prematurely (by exception)!
 */
@Copyright(year=2013, holders="Sascha Baumeister")
public class ResyncThreadByMonitorInterruptibly {

	/**
	 * Performs some work INTERRUPTIBLY.
	 * @param args the runtime arguments
	 * @throws InterruptedException if the operation is interrupted
	 */
	static public void main (final String[] args) throws InterruptedException {
		workDistributed(args.length == 0 ? 4 : Integer.parseInt(args[0]));
	}


	/**
	 * Distributes some work INTERRUPTIBLY distributed over multiple child
	 * threads, and resynchronizes using {@link Object#wait()}.
	 * @param workerCount the number of worker threads
	 * @throws NegativeArraySizeException if the given workerCount is strictly negative
	 * @throws InterruptedException if the operation is interrupted
	 */
	static public void workDistributed (final int workerCount) throws NegativeArraySizeException, InterruptedException {
		final Object monitor = new Object();
		final Runnable worker = () -> {
			try {
				System.out.format("Thread %s: starting work.\n", Thread.currentThread().getName());
				try { ExampleWorker.work(10); } catch (ExampleCheckedException e) {}
				System.out.format("Thread %s: stoping work.\n", Thread.currentThread().getName());
			} finally {
				synchronized (monitor) {
					monitor.notify();
				}
			}
		};

		// Note that the sync block MUST encapsulate both thread spawning AND monitor.wait() calls!
		// This way it prevents early signals vanishing into nothingness that would cause deadlocks.
		final Thread[] workerThreads = new Thread[workerCount];
		synchronized(monitor) {
			System.out.format("Main-Thread: Executing workers in new threads!\n");
			for (int index = 0; index < workerCount; ++index) {
				(workerThreads[index] = new Thread(worker, "worker-thread-" + index)).start();
			}

			System.out.format("Main-Thread: Waiting for child threads to finish work!\n");
			try {
				for (int loop = 0; loop < workerCount; ++loop) {
					monitor.wait();
				}
			} catch (final Throwable exception) {
				for (final Thread thread : workerThreads) {
					thread.interrupt();
				}
				throw exception;
			}
		}
		System.out.format("Main-Thread: All child threads are terminated!\n");
	}
}