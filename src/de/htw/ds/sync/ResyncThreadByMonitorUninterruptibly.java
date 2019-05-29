package de.htw.ds.sync;

import de.htw.tool.Copyright;


/**
 * Performs some work distributed over multiple child threads, resynchronizes UNINTERRUPTIBLY using
 * the blocking technique {@link Object#wait()}.
 */
@Copyright(year=2013, holders="Sascha Baumeister")
public class ResyncThreadByMonitorUninterruptibly {

	/**
	 * Performs some work UNINTERRUPTIBLY.
	 * @param args the runtime arguments
	 */
	static public void main (final String[] args) {
		workDistributed(args.length == 0 ? 4 : Integer.parseInt(args[0]));
	}


	/**
	 * Distributes some work UNINTERRUPTIBLY distributed over multiple child
	 * threads, and resynchronizes using {@link Object#wait()}.
	 * @param workerCount the number of worker threads
	 * @throws NegativeArraySizeException if the given workerCount is strictly negative
	 */
	static public void workDistributed (final int workerCount) throws NegativeArraySizeException {
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
		synchronized(monitor) {
			System.out.format("Main-Thread: Executing workers in new threads!\n");
			for (int index = 0; index < workerCount; ++index) {
				new Thread(worker, "worker-thread-" + index).start();
			}

			System.out.format("Main-Thread: Waiting for child threads to finish!\n");
			for (int loop = 0; loop < workerCount; ++loop) {
				waitUninterruptibly(monitor);
			}
		}
		System.out.format("Main-Thread: All child threads are terminated!\n");
	}


	/**
	 * Repeat the (interruptible) blocking {@link Object#wait()} operation until it ends
	 * without being interrupted. Note that this implementation preserves the interrupt-status of
	 * it's thread, i.e. interruptions are only delayed, not ignored completely.
	 * @param monitor the object used as a monitor
	 * @throws NullPointerException if any of the given arguments is {@code null}
	 * @throws IllegalMonitorStateException if the call to this method is not embedded into a
	 *         synchronize block against the given monitor (which MUST encapsulate the forking
	 *         of the synchonization targets!)
	 */
	static private void waitUninterruptibly (final Object monitor) throws NullPointerException, IllegalMonitorStateException {
		boolean interrupted = false;
		try {
			while (true) {
				try {
					monitor.wait();
					break;
				} catch (final InterruptedException exception) {
					interrupted = true;
				}
			}
		} finally {
			if (interrupted) Thread.currentThread().interrupt();
		}
	}
}