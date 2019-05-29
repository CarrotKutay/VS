package de.htw.ds.sync;

import de.htw.tool.Copyright;


/**
 * Performs some work distributed over multiple child threads, resynchronizes UNINTERRUPTIBLY using
 * the blocking technique {@link Thread#join()}.
 */
@Copyright(year=2013, holders="Sascha Baumeister")
public class ResyncThreadByJoiningUninterruptibly {

	/**
	 * Performs some work UNINTERRUPTIBLY.
	 * @param args the runtime arguments
	 */
	static public void main (final String[] args) {
		workDistributed(args.length == 0 ? 4 : Integer.parseInt(args[0]));
	}


	/**
	 * Distributes some work UNINTERRUPTIBLY distributed over multiple child
	 * threads, and resynchronizes using {@link Thread#join()}.
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
		for (final Thread thread : workerThreads) {
			joinUninterruptibly(thread);
		}

		System.out.format("Main-Thread: All child threads are terminated!\n");
	}


	/**
	 * Repeat the (interruptible) blocking {@link Thread#join()} operation until it ends without
	 * being interrupted. Note that this implementation preserves the interrupt-status of it's
	 * thread, i.e. interruptions are only delayed, not ignored completely.
	 * @param thread the thread to join the current thread
	 * @throws NullPointerException if any of the given arguments is {@code null}
	 */
	static public void joinUninterruptibly (final Thread thread) throws NullPointerException {
		boolean interrupted = false;
		try {
			while (true) {
				try {
					thread.join();
					break;
				} catch (final InterruptedException e) {
					interrupted = true;
				}
			}
		} finally {
			if (interrupted) Thread.currentThread().interrupt();
		}
	}
}