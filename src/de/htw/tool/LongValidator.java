package de.htw.tool;

import java.util.function.Predicate;


/**
 * Simple predicate validator for texts representing integral numbers within a defined range.
 */
@Copyright(year = 2016, holders = "Sascha Baumeister")
public class LongValidator implements Predicate<String> {

	private final int radix;
	private final long min, max;


	/**
	 * Creates an unbounded validator for radix 10.
	 */
	public LongValidator () {
		this(10, Long.MIN_VALUE, Long.MAX_VALUE);
	}


	/**
	 * Creates a validator for radix 10 that accepts values between min and max.
	 * @param min the min value (included)
	 * @param max the max value (included)
	 * @throws IllegalArgumentException if min exceeds max
	 */
	public LongValidator (final long min, final long max) {
		this(10, min, max);
	}


	/**
	 * Creates a validator for the given radix that accepts values between min and max.
	 * @param radix the radix to be used when parsing texts
	 * @param min the min value (included)
	 * @param max the max value (included)
	 * @throws IllegalArgumentException if the given radix is outside range [2,36], or if min exceeds max
	 */
	public LongValidator (final int radix, final long min, final long max) {
		if (radix < Character.MIN_RADIX | radix > Character.MAX_RADIX | min > max) throw new IllegalArgumentException();

		this.radix = radix;
		this.min = min;
		this.max = max;
	}


	/**
	 * Returns the radix.
	 * @return the radix to be used when parsing texts
	 */
	public int getRadix () {
		return this.radix;
	}


	/**
	 * Returns the min value.
	 * @return the allowed minimum for parsed values
	 */
	public long getMin () {
		return this.min;
	}


	/**
	 * Returns the max value.
	 * @return the allowed maximum for parsed values
	 */
	public long getMax () {
		return this.max;
	}


	/**
	 * {@inheritDoc}
	 * @param text the (optional) text to be validated
	 * @return whether or not the given text validates to a valid long value within the given range
	 */
	@Override
	public boolean test (final String text) {
		try {
			final long value = Long.parseLong(text, this.radix);
			return value >= this.min & value <= this.max;
		} catch (final Exception exception) {
			return false;
		}
	}
}
