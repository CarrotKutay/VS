package de.htw.tool;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.function.BooleanSupplier;


/**
 * This facade provides uninterruptible variants of blocking operations, i.e. the operations repeat blocking until the
 * underlying operation is completed without throwing an {@link InterruptedException}. Note that all operations preserve the
 * interrupted status of their executing thread, therefore interruptions can be handled later.
 */
@Copyright(year = 2013, holders = "Sascha Baumeister")
public class Uninterruptibles {

	/**
	 * Prevents external instantiation.
	 */
	private Uninterruptibles () {}


	/**
	 * Repeats the interruptible {@link Thread#sleep(long)} operation until the given amount of milliseconds has passed. Note
	 * that this operation cannot be interrupted by another thread, but preserves the interrupt status for later use.
	 * @param delay the minimum time to sleep
	 * @param unit the time unit of the timeout argument
	 */
	static public void sleep (long delay, final TimeUnit unit) throws IllegalArgumentException {
		boolean interrupted = false;
		try {
			for (long wait = unit.toMillis(delay), stop = wait + System.currentTimeMillis(); wait > 0; wait = stop - System.currentTimeMillis()) {
				try {
					Thread.sleep(wait);
				} catch (final InterruptedException exception) {
					interrupted = true;
				}
			}
		} finally {
			if (interrupted) Thread.currentThread().interrupt();
		}
	}


	/**
	 * Repeats the interruptible {@link Thread#sleep(long, int)} operation (passing 1ns, which in reality translates to about
	 * 1ms thread-scheduling latency) until the given supplier returns {@code true}. Note that this operation cannot be
	 * interrupted by another thread, but preserves the interrupt status for later use.
	 * @param supplier the boolean supplier
	 * @throws NullPointerException if any of the given arguments is {@code null}
	 */
	static public void poll (final BooleanSupplier supplier) throws NullPointerException {
		boolean interrupted = false;
		try {
			while (!supplier.getAsBoolean()) {
				try {
					Thread.sleep(0, 1);
				} catch (final InterruptedException exception) {
					interrupted = true;
				}
			}
		} finally {
			if (interrupted) Thread.currentThread().interrupt();
		}
	}


	/**
	 * Repeats the interruptible {@link Future#get()} operation until it ends without being interrupted. Note that this
	 * operation cannot be interrupted by another thread, but preserves the interrupt status for later use.
	 * @param future the future
	 * @throws NullPointerException if any of the given arguments is {@code null}
	 * @throws ExecutionException if there is a problem during the given future's execution
	 */
	static public <T> T get (final Future<T> future) throws NullPointerException, ExecutionException {
		boolean interrupted = false;
		try {
			while (true) {
				try {
					return future.get();
				} catch (final InterruptedException exception) {
					interrupted = true;
				}
			}
		} finally {
			if (interrupted) Thread.currentThread().interrupt();
		}
	}


	/**
	 * Repeats the interruptible {@link Future#get(long, TimeUnit)} operation until it ends without being interrupted. Note that
	 * this operation cannot be interrupted by another thread, but preserves the interrupt status for later use.
	 * @param future the future
	 * @param timeout the maximum time to wait
	 * @param unit the time unit of the timeout argument
	 * @throws NullPointerException if any of the given arguments is {@code null}
	 * @throws ExecutionException if there is a problem during the given future's execution
	 * @throws TimeoutException if the wait timed out
	 */
	static public <T> T get (final Future<T> future, final long timeout, TimeUnit unit) throws NullPointerException, ExecutionException, TimeoutException {
		boolean interrupted = false;
		try {
			while (true) {
				try {
					return future.get(timeout, unit);
				} catch (final InterruptedException exception) {
					interrupted = true;
				}
			}
		} finally {
			if (interrupted) Thread.currentThread().interrupt();
		}
	}


	/**
	 * Repeats the interruptible {@link Lock#lockInterruptibly()} operation until it ends without being interrupted. Note that
	 * this operation cannot be interrupted by another thread, but preserves the interrupt status for later use. Also note that
	 * this operation is included for completeness only, usually {@link Lock#lock()} should be preferrable.
	 * @param lock the lock
	 * @throws NullPointerException if any of the given arguments is {@code null}
	 */
	static public void lock (final Lock lock) throws NullPointerException {
		boolean interrupted = false;
		try {
			while (true) {
				try {
					lock.lockInterruptibly();
					return;
				} catch (final InterruptedException exception) {
					interrupted = true;
				}
			}
		} finally {
			if (interrupted) Thread.currentThread().interrupt();
		}
	}


	/**
	 * Repeats the interruptible {@link Lock#tryLock(long, TimeUnit)} operation until it ends without being interrupted. Note
	 * that this operation cannot be interrupted by another thread, but preserves the interrupt status for later use.
	 * @param lock the lock
	 * @param timeout the maximum time to wait
	 * @param unit the time unit of the timeout argument
	 * @throws NullPointerException if any of the given arguments is {@code null}
	 * @throws TimeoutException if the wait timed out
	 */
	static public void lock (final Lock lock, final long timeout, final TimeUnit unit) throws NullPointerException, TimeoutException {
		boolean interrupted = false;
		try {
			for (long wait = unit.toMillis(timeout), stop = wait + System.currentTimeMillis(); wait > 0; wait = stop - System.currentTimeMillis()) {
				try {
					if (lock.tryLock(wait, TimeUnit.MILLISECONDS)) return;
				} catch (final InterruptedException exception) {
					interrupted = true;
				}
			}
			throw new TimeoutException();
		} finally {
			if (interrupted) Thread.currentThread().interrupt();
		}
	}


	/**
	 * Repeats the interruptible {@link Object#wait()} operation until it ends without being interrupted. Note that this
	 * implementation preserves the interrupt-status of it's thread, i.e. interruptions are only delayed, not ignored
	 * completely.
	 * @param monitor the object used as a monitor
	 * @throws NullPointerException if any of the given arguments is {@code null}
	 * @throws IllegalMonitorStateException if the call to this method is not embedded into a synchronize block against the
	 *         given monitor (which MUST encapsulate the forking of the synchonization targets!)
	 */
	static public void wait (final Object monitor) throws NullPointerException, IllegalMonitorStateException {
		boolean interrupted = false;
		try {
			while (true) {
				try {
					monitor.wait();
					return;
				} catch (final InterruptedException exception) {
					interrupted = true;
				}
			}
		} finally {
			if (interrupted) Thread.currentThread().interrupt();
		}
	}


	/**
	 * Repeats the interruptible {@link Object#wait(long, int))} operation until it ends without being interrupted. Note that
	 * this implementation preserves the interrupt-status of it's thread, i.e. interruptions are only delayed, not ignored
	 * completely.
	 * @param monitor the object used as a monitor
	 * @param timeout the maximum time to wait
	 * @param unit the time unit of the timeout argument
	 * @throws NullPointerException if any of the given arguments is {@code null}
	 * @throws IllegalMonitorStateException if the call to this method is not embedded into a synchronize block against the
	 *         given monitor (which MUST encapsulate the forking of the synchonization targets!)
	 * @throws TimeoutException if the wait timed out
	 */
	static public void wait (final Object monitor, final long timeout, final TimeUnit unit) throws NullPointerException, IllegalMonitorStateException, TimeoutException {
		boolean interrupted = false;
		try {
			while (true) {
				try {
					unit.timedWait(monitor, timeout);
					return;
				} catch (final InterruptedException exception) {
					interrupted = true;
				}
			}
		} finally {
			if (interrupted) Thread.currentThread().interrupt();
		}
	}


	/**
	 * Repeats the interruptible {@link Semaphore#acquire()} operation until it ends without being interrupted. Note that this
	 * operation cannot be interrupted by another thread, but preserves the interrupt status for later use. Also note that this
	 * operation is included for completeness only, usually {@link Semaphore#acquireUninterruptibly()} should be preferrable.
	 * @param semaphore the semaphore
	 * @throws NullPointerException if any of the given arguments is {@code null}
	 */
	static public void acquire (final Semaphore semaphore) throws NullPointerException {
		boolean interrupted = false;
		try {
			while (true) {
				try {
					semaphore.acquire();
					return;
				} catch (final InterruptedException exception) {
					interrupted = true;
				}
			}
		} finally {
			if (interrupted) Thread.currentThread().interrupt();
		}
	}


	/**
	 * Repeats the interruptible {@link Semaphore#acquire()} operation until it ends without being interrupted. Note that this
	 * implementation intentionally CLEARS the interrupt-status of it's thread because it uses thread interruption for timeout
	 * implementation.
	 * @param semaphore the semaphore
	 * @param timeout the maximum time to wait
	 * @param unit the time unit of the timeout argument
	 * @throws NullPointerException if any of the given arguments is {@code null}
	 * @throws TimeoutException if the wait timed out
	 */
	static public void acquire (final Semaphore semaphore, final long timeout, final TimeUnit unit) throws NullPointerException, TimeoutException {
		final Thread thread = Thread.currentThread();
		final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

		try {
			scheduler.schedule( () -> thread.interrupt(), timeout, unit);
			semaphore.acquire();
		} catch (final InterruptedException exception) {
			throw new TimeoutException();
		} finally {
			// must clear the interrupt flag because the scheduled task might just set it during shutdown/await
			scheduler.shutdownNow();
			awaitTermination(scheduler);
			Thread.interrupted();
		}
	}


	/**
	 * Repeats the interruptible {@link Thread#join()} operation until it ends without being interrupted. Note that this
	 * operation cannot be interrupted by another thread, but preserves the interrupt status for later use.
	 * @param thread the thread to join the current thread
	 * @throws NullPointerException if any of the given arguments is {@code null}
	 */
	static public void join (final Thread thread) throws NullPointerException {
		boolean interrupted = false;
		try {
			while (true) {
				try {
					thread.join();
					return;
				} catch (final InterruptedException exception) {
					interrupted = true;
				}
			}
		} finally {
			if (interrupted) Thread.currentThread().interrupt();
		}
	}


	/**
	 * Repeats the interruptible {@link Thread#join()} operation until it ends without being interrupted. Note that this
	 * operation cannot be interrupted by another thread, but preserves the interrupt status for later use.
	 * @param thread the thread to join the current thread
	 * @param timeout the maximum time to wait
	 * @param unit the time unit of the timeout argument
	 * @throws NullPointerException if any of the given arguments is {@code null}
	 * @throws TimeoutException if the wait timed out
	 */
	static public void join (final Thread thread, final long timeout, final TimeUnit unit) throws NullPointerException {
		boolean interrupted = false;
		try {
			while (true) {
				try {
					unit.timedJoin(thread, timeout);
					return;
				} catch (final InterruptedException exception) {
					interrupted = true;
				}
			}
		} finally {
			if (interrupted) Thread.currentThread().interrupt();
		}
	}


	/**
	 * Repeats the interruptible {@link ExecutorService#awaitTermination(long, TimeUnit)} operation until it ends successfully
	 * without being interrupted. Note that this operation cannot be interrupted by another thread, but preserves the interrupt
	 * status for later use.
	 * @param executorService the executor service
	 * @throws NullPointerException if any of the given arguments is {@code null}
	 */
	static public void awaitTermination (final ExecutorService executorService) throws NullPointerException {
		boolean interrupted = false;
		try {
			while (true) {
				try {
					if (executorService.awaitTermination(1, TimeUnit.SECONDS)) return;
				} catch (final InterruptedException exception) {
					interrupted = true;
				}
			}
		} finally {
			if (interrupted) Thread.currentThread().interrupt();
		}
	}


	/**
	 * Repeats the interruptible {@link ExecutorService#awaitTermination(long, TimeUnit)} operation until it ends successfully
	 * without being interrupted. Note that this operation cannot be interrupted by another thread, but preserves the interrupt
	 * status for later use.
	 * @param executorService the executor service
	 * @param timeout the maximum time to wait
	 * @param unit the time unit of the timeout argument
	 * @throws NullPointerException if any of the given arguments is {@code null}
	 * @throws TimeoutException if the wait timed out
	 */
	static public void awaitTermination (final ExecutorService executorService, final long timeout, final TimeUnit unit) throws NullPointerException, TimeoutException {
		boolean interrupted = false;
		try {
			for (long wait = unit.toMillis(timeout), stop = wait + System.currentTimeMillis(); wait > 0; wait = stop - System.currentTimeMillis()) {
				try {
					if (executorService.awaitTermination(wait, TimeUnit.MILLISECONDS)) return;
				} catch (final InterruptedException exception) {
					interrupted = true;
				}
			}
			throw new TimeoutException();
		} finally {
			if (interrupted) Thread.currentThread().interrupt();
		}
	}


	/**
	 * Repeats the interruptible {@link CyclicBarrier#await()} operation until it ends successfully without being interrupted.
	 * Note that this operation cannot be interrupted by another thread, but preserves the interrupt status for later use.
	 * @param barrier the barrier
	 * @return the arrival index of the current thread, where index {@code getParties() - 1} indicates the first to arrive and
	 *         zero indicates the last to arrive
	 * @throws NullPointerException if any of the given arguments is {@code null}
	 * @throws BrokenBarrierException if <em>another</em> thread was interrupted or timed out while the current thread was
	 *         waiting, or the barrier was reset, or the barrier was broken when {@code await} was called, or the barrier action
	 *         (if present) failed due to an exception
	 */
	static public int await (final CyclicBarrier barrier) throws NullPointerException, BrokenBarrierException {
		boolean interrupted = false;
		try {
			while (true) {
				try {
					return barrier.await();
				} catch (final InterruptedException exception) {
					interrupted = true;
				}
			}
		} finally {
			if (interrupted) Thread.currentThread().interrupt();
		}
	}


	/**
	 * Repeats the interruptible {@link CyclicBarrier#await(long, TimeUnit)} operation until it ends successfully without being
	 * interrupted. Note that this operation cannot be interrupted by another thread, but preserves the interrupt status for
	 * later use.
	 * @param barrier the barrier
	 * @param timeout the maximum time to wait
	 * @param unit the time unit of the timeout argument
	 * @return the arrival index of the current thread, where index {@code getParties() - 1} indicates the first to arrive and
	 *         zero indicates the last to arrive
	 * @throws NullPointerException if any of the given arguments is {@code null}
	 * @throws BrokenBarrierException if <em>another</em> thread was interrupted or timed out while the current thread was
	 *         waiting, or the barrier was reset, or the barrier was broken when {@code await} was called, or the barrier action
	 *         (if present) failed due to an exception
	 * @throws TimeoutException if the wait timed out
	 */
	static public int await (final CyclicBarrier barrier, final long timeout, final TimeUnit unit) throws NullPointerException, BrokenBarrierException, TimeoutException {
		boolean interrupted = false;
		try {
			while (true) {
				try {
					return barrier.await(timeout, unit);
				} catch (final InterruptedException exception) {
					interrupted = true;
				}
			}
		} finally {
			if (interrupted) Thread.currentThread().interrupt();
		}
	}


	/**
	 * Repeats the interruptible {@link BlockingQueue#put(T)} operation until it ends without being interrupted. Note that this
	 * operation cannot be interrupted by another thread, but preserves the interrupt status for later use.
	 * @param queue the blocking queue
	 * @param element the element to be added
	 * @throws NullPointerException if any of the given arguments is {@code null}
	 */
	static public <T> void put (final BlockingQueue<? super T> queue, final T element) throws NullPointerException {
		boolean interrupted = false;
		try {
			while (true) {
				try {
					queue.put(element);
					return;
				} catch (final InterruptedException exception) {
					interrupted = true;
				}
			}
		} finally {
			if (interrupted) Thread.currentThread().interrupt();
		}
	}


	/**
	 * Repeats the interruptible {@link BlockingQueue#take()} operation until it ends without being interrupted. Note that this
	 * operation cannot be interrupted by another thread, but preserves the interrupt status for later use.
	 * @param queue the blocking queue
	 * @return the element removed
	 * @throws NullPointerException if any of the given arguments is {@code null}
	 */
	static public <T> T take (final BlockingQueue<? extends T> queue) throws NullPointerException {
		boolean interrupted = false;
		try {
			while (true) {
				try {
					return queue.take();
				} catch (final InterruptedException exception) {
					interrupted = true;
				}
			}
		} finally {
			if (interrupted) Thread.currentThread().interrupt();
		}
	}
}