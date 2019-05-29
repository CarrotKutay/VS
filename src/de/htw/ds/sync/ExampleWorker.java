package de.htw.ds.sync;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import de.htw.tool.Copyright;


/**
 * Example worker facade.
 */
@Copyright(year=2013, holders="Sascha Baumeister")
public final class ExampleWorker {

	/**
	 * Performs a piece of example work (by sleeping) that may require up to the given maximum
	 * duration to complete. Returns the duration it actually took to complete.
	 * @param maximumDuration the maximum duration is seconds
	 * @return the "work" duration in seconds
	 * @throws ExampleCheckedException if there is a "work" related problem
	 */
	public static int work (final long maximumDuration) throws ExampleCheckedException {
		try {
			final int maximumDelay = (int) TimeUnit.SECONDS.toMillis(maximumDuration);
			final int actualDelay = ThreadLocalRandom.current().nextInt(maximumDelay);
			Thread.sleep(actualDelay);
			return actualDelay;
		} catch (final Exception exception) {
			throw new ExampleCheckedException(exception.getMessage(), exception.getCause());
		}
	}


	/**
	 * Prevents instantiation
	 */
	private ExampleWorker () {}
}