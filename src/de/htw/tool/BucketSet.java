package de.htw.tool;

import java.io.Serializable;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;


/**
 * Instances of this class manage elements that are divided into n equally sized k-buckets using a bucket indexer, effectively
 * forming a matrix of elements with a fixed number of columns (n), and a maximum number of rows (k). Once a bucket reaches it's
 * capacity k, it will not accept more elements until space is freed up by removing elements from the same bucket. The idea
 * behind a k-bucket-set is that "important" elements have a much better chance not to be spilled (i.e. rejected from being
 * added) than "less important" elements that are indexed into crowded buckets. This provides a way of element filtering that
 * both efficiently selects for "important" elements, while still retaining some "less important" elements.
 * @param <E> the element type
 */
@Copyright(year = 2009, holders = "Sascha Baumeister")
public class BucketSet<E> extends AbstractSet<E>implements Cloneable, Serializable {
	static private final long serialVersionUID = -7040303224602680438L;

	static public interface Indexer {

		/**
		 * Returns the cardinality of the index calculation, i.e. the maximum index + 1.
		 * @return the cardinality, a strictly positive value
		 */
		int cardinality ();


		/**
		 * Returns the index calculated for the given object. The index returned must be guaranteed to be the same for equal values.
		 * @param object the object
		 * @return an index within range {@code [0, cardinality[}
		 * @throws NullPointerException if the given object cannot be indexed because it is {@code null}
		 * @throws IllegalArgumentException if the given object cannot be indexed for another reason
		 */
		int index (Object object) throws NullPointerException, IllegalArgumentException;
	}


	private final Indexer bucketIndexer;
	private final int bucketCapacity;
	private volatile Set<E>[] buckets;
	private volatile int size;


	/**
	 * Public constructor.
	 * @param indexer the indexer assigning elements to k-buckets
	 * @param bucketCapacity the maximum number of elements (k) within each k-bucket
	 * @throws NullPointerException if the given indexer is {@code null}
	 * @throws IllegalArgumentException if the given bucket capacity is negative
	 */
	public BucketSet (final Indexer bucketIndexer, final int bucketCapacity) {
		final int bucketCount = bucketIndexer.cardinality();
		if (bucketCount <= 0 | bucketCapacity <= 0) throw new IllegalArgumentException();

		@SuppressWarnings("unchecked")
		final Set<E>[] buckets = new Set[bucketCount];
		for (int index = 0; index < buckets.length; ++index) {
			buckets[index] = new HashSet<>(bucketCapacity);
		}

		this.bucketIndexer = bucketIndexer;
		this.bucketCapacity = bucketCapacity;
		this.buckets = buckets;
	}


	/**
	 * Returns a deep copy with an identical pivot indexer.
	 * @return the clone
	 */
	@Override
	@SuppressWarnings("unchecked")
	public BucketSet<E> clone () {
		final BucketSet<E> clone;
		try {
			clone = (BucketSet<E>) super.clone();
		} catch (final CloneNotSupportedException exception) {
			throw new AssertionError();
		}

		clone.buckets = this.buckets.clone();
		for (int index = 0; index < this.buckets.length; ++index) {
			clone.buckets[index] = new HashSet<>(this.buckets[index]);
		}
		return clone;
	}


	/**
	 * {@inheritDoc}
	 */
	public int size () {
		return this.size;
	}


	/**
	 * Returns the bucket capacity.
	 * @return the maximum number of elements (k) within each k-bucket
	 */
	public int bucketCapacity () {
		return this.bucketCapacity;
	}


	/**
	 * Returns the bucket count, which equals the pivot-indexer's cardinality.
	 * @return the number of available buckets
	 */
	public int bucketCount () {
		return this.buckets.length;
	}


	/**
	 * Returns the bucket indexer.
	 * @return the indexer assigning elements to k-buckets
	 */
	public Indexer bucketIndexer () {
		return this.bucketIndexer;
	}


	/**
	 * Returns the bucket for the given value.
	 * @return the bucket
	 * @throws NullPointerException if the given element cannot be indexed because it is {@code null}
	 * @throws IllegalArgumentException if the given element cannot be indexed for another reason
	 * @throws IllegalStateException if the indexer returned an illegal bucket index
	 */
	protected Set<E> bucket (final Object object) throws NullPointerException, IllegalArgumentException, IllegalStateException {
		try {
			final int bucketIndex = this.bucketIndexer.index(object);
			return this.buckets[bucketIndex];
		} catch (final IndexOutOfBoundsException exception) {
			throw new IllegalStateException(exception);
		}
	}


	/**
	 * {@inheritDoc} Returns {@code true} if the given element was added to one of the receiver's k-buckets, {@code false}
	 * otherwise. Elements are added only if the element's bucket hasn't reached maximum capacity, the bucket-set doesn't
	 * already contain 2^31-1 elements, and the element isn't already included.
	 * @param element the element
	 * @return {@code true} if this collection changed as a result of the call
	 * @throws NullPointerException if the given element cannot be indexed because it is {@code null}
	 * @throws IllegalArgumentException if the given element cannot be indexed for another reason
	 * @throws IllegalStateException if the indexer returned an illegal bucket index
	 */
	@Override
	public boolean add (final E element) throws NullPointerException, IllegalArgumentException, IllegalStateException {
		if (this.size == Integer.MAX_VALUE) return false;

		final Set<E> bucket = this.bucket(element);
		if (bucket.size() == this.bucketCapacity) return false;
		final boolean changed = bucket.add(element);

		if (changed) this.size += 1;
		return changed;
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public void clear () {
		for (final Set<E> bucket : this.buckets) {
			bucket.clear();
		}
		this.size = 0;
	}


	/**
	 * {@inheritDoc}
	 * @throws IllegalStateException if the indexer returned an illegal bucket index
	 */
	@Override
	public boolean contains (final Object object) throws IllegalStateException {
		try {
			final Set<E> bucket = this.bucket(object);
			return bucket.contains(object);
		} catch (final NullPointerException | IllegalArgumentException exception) {
			return false;
		}
	}


	/**
	 * {@inheritDoc}
	 * @throws IllegalStateException if the indexer returned an illegal bucket index
	 */
	@Override
	public boolean remove (final Object object) throws IllegalStateException {
		try {
			final Set<E> bucket = this.bucket(object);
			final boolean changed = bucket.remove(object);
			if (changed) this.size -= 1;
			return changed;
		} catch (final NullPointerException | IllegalArgumentException exception) {
			return false;
		}
	}


	/**
	 * Returns an iterator over all the elements of this set. The elements are returned in a predefined order: The pivot element
	 * is first (if it is contained), then the elements of the first k-bucket in their respective order of addition, then that
	 * of the next bucket, and so on. Note that the resulting iterator supports the remove operation, except for the pivot
	 * element.
	 * @return an iterator
	 * @see java.util.Set#iterator()
	 * @see #remove(Object)
	 * @see #removeAll(Collection)
	 * @see #retainAll(Collection)
	 */
	public Iterator<E> iterator () {
		@SuppressWarnings("unchecked")
		final Iterator<E>[] bucketIterators = new Iterator[this.buckets.length];
		for (int bucketIndex = 0; bucketIndex < bucketIterators.length; ++bucketIndex) {
			bucketIterators[bucketIndex] = this.buckets[bucketIndex].iterator();
		}

		return new Iterator<E>() {
			private volatile int bucketIndex = 0;


			public boolean hasNext () {
				for (int bucketIndex = this.bucketIndex; bucketIndex < bucketIterators.length; ++bucketIndex) {
					if (bucketIterators[bucketIndex].hasNext()) return true;
				}
				return false;
			}


			public E next () {
				while (true) {
					try {
						return bucketIterators[this.bucketIndex].next();
					} catch (final NoSuchElementException exception) {
						if (this.bucketIndex == bucketIterators.length - 1) throw exception;
					}
					this.bucketIndex += 1;
				}
			}


			public void remove () {
				bucketIterators[this.bucketIndex].remove();
				BucketSet.this.size -= 1;
			}
		};
	}
}