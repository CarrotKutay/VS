package de.htw.ds.sync;

import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

public class FutureDemo {
	static private final ExecutorService THREAD_POOL =
		Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
	static private final Random RANDOM = new Random();
	static private final Callable<Integer> WORKER = () -> {
		final int millies = RANDOM.nextInt(9000) + 1000;
		try {
			Thread.sleep(millies);
			return millies;
		} catch (InterruptedException e) {
			throw new AssertionError(e);
		}
	};

	static public void variant1 () throws InterruptedException, ExecutionException {
		final RunnableFuture<Integer> future = new FutureTask<>(WORKER);
		final Thread thread = new Thread(future);
		thread.start();

		// synchronize
		int result = future.get();
		System.out.println(result);
	}


	static public void variant2 () throws InterruptedException, ExecutionException {
		final Future<Integer> future = THREAD_POOL.submit(WORKER);

		// synchronize
		int result = future.get();
		System.out.println(result);
	}


	static public void main (String[] args) throws InterruptedException, ExecutionException {
		try {
			variant2();
		} finally {
			THREAD_POOL.shutdown();
		}
	}
}
