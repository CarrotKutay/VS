package de.htw.tool;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


/**
 * This is a wrapper class enriching underlying string maps with event callback functionality. Instances notify their registered
 * event handlers of every change event within the underlying map (via {@link Map#put(Object, Object)},
 * {@link Map#remove(Object)}, or {@link Map#clear()} messages). This includes modifications caused by map views (
 * {@link Map#keySet()}, {@link Map#values()}, or {@link Map#entrySet()}) and their associated iterators. Note that event
 * notification doesn't happen if the underlying map (the one provided with the constructor) is modified directly. Also note
 * that this implementation supports only string keys because property change events only support string property names.
 * @param <E> the value type
 */
@Copyright(year = 2010, holders = "Sascha Baumeister")
public class CallbackMap<E> extends AbstractMap<String,E>implements Map<String,E> {

	private final Map<String,E> delegateMap;
	private final CallbackEntrySet entrySet;
	private final Set<VetoableChangeListener> listeners;


	/**
	 * Creates a new instance based on an empty hash map.
	 */
	public CallbackMap () {
		this(new HashMap<String,E>());
	}


	/**
	 * Creates a new instance based on the delegate map. Note that no put events are spawned for the elements already existing
	 * within the delegate map.
	 * @param delegateMap the underlying map
	 * @see Map#putAll(Map)
	 */
	public CallbackMap (final Map<String,E> delegateMap) {
		this.delegateMap = delegateMap;
		this.entrySet = new CallbackEntrySet();
		this.listeners = Collections.synchronizedSet(new HashSet<VetoableChangeListener>());
	}


	/**
	 * Returns the registered event listeners.
	 * @return the event listeners
	 */
	public Set<VetoableChangeListener> getListeners () {
		return this.listeners;
	}


	/**
	 * Returns the underlying delegate map. Note that event notifications don't take place when modifying the delegate map
	 * directly, which is the intended use-case for this method.
	 * @return the delegate map
	 */
	public Map<String,E> getDelegateMap () {
		return this.delegateMap;
	}


	/**
	 * {@inheritDoc}
	 * @throws ClassCastException {@inheritDoc}
	 * @throws NullPointerException {@inheritDoc}
	 */
	@Override
	public boolean containsKey (final Object key) {
		return this.delegateMap.containsKey(key);
	}


	/**
	 * {@inheritDoc}
	 * @throws ClassCastException {@inheritDoc}
	 * @throws NullPointerException {@inheritDoc}
	 */
	@Override
	public E get (final Object key) {
		return this.delegateMap.get(key);
	}


	/**
	 * {@inheritDoc}
	 * @throws UnsupportedOperationException {@inheritDoc}
	 * @throws NullPointerException {@inheritDoc}
	 * @throws ClassCastException {@inheritDoc}
	 * @throws IllegalArgumentException {@inheritDoc}
	 * @throws IllegalStateException if a listener vetoes the change
	 */
	@Override
	public E put (final String key, final E value) {
		final E oldValue = this.delegateMap.get(key);
		this.fireChangeEvent(key, oldValue, value);
		return this.delegateMap.put(key, value);
	}


	/**
	 * {@inheritDoc}
	 * @throws UnsupportedOperationException {@inheritDoc}
	 * @throws NullPointerException {@inheritDoc}
	 * @throws ClassCastException {@inheritDoc}
	 * @throws IllegalStateException if a listener vetoes the change
	 */
	@Override
	public E remove (final Object key) {
		if (!this.containsKey(key)) return null;

		final Map.Entry<String,E> entry = new AbstractMap.SimpleEntry<>((String) key, this.delegateMap.get(key));
		this.entrySet.remove(entry);
		return entry.getValue();
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public Set<Entry<String,E>> entrySet () {
		return this.entrySet;
	}


	/**
	 * Notifies all listeners of an impeding change. If one of the listeners throws a {@link PropertyVetoException} during
	 * notification, this map will not perform the change.
	 * @param key the key
	 * @param oldValue the old value
	 * @param newValue the new value
	 * @see VetoableChangeListener#vetoableChange(PropertyChangeEvent)
	 * @throws IllegalStateException if one of the listeners vetoes the change
	 */
	private void fireChangeEvent (final String key, final E oldValue, final E newValue) {
		final PropertyChangeEvent event = new PropertyChangeEvent(this, key == null ? null : key.toString(), oldValue, newValue);
		final VetoableChangeListener[] listeners = CallbackMap.this.listeners.toArray(new VetoableChangeListener[0]);
		for (final VetoableChangeListener listener : listeners) {
			try {
				listener.vetoableChange(event);
			} catch (final PropertyVetoException exception) {
				throw new IllegalStateException(exception);
			}
		}
	}



	/**
	 * Inner class defining an entry set view on a callback map.
	 */
	private final class CallbackEntrySet extends AbstractSet<Entry<String,E>> {

		@Override
		public int size () {
			return CallbackMap.this.delegateMap.entrySet().size();
		}


		@Override
		public boolean contains (final Object object) {
			return CallbackMap.this.delegateMap.entrySet().contains(object);
		}


		@Override
		public boolean remove (final Object object) {
			if (!this.contains(object)) return false;

			@SuppressWarnings("unchecked")
			final Map.Entry<String,E> entry = (Map.Entry<String,E>) object;
			if (this.contains(object)) CallbackMap.this.fireChangeEvent(entry.getKey(), entry.getValue(), null);
			return CallbackMap.this.delegateMap.entrySet().remove(object);
		}


		@Override
		public Iterator<Map.Entry<String,E>> iterator () {
			return new CallbackEntrySetIterator();
		}



		/**
		 * Inner class defining an entry set view's callback iterator.
		 */
		private final class CallbackEntrySetIterator implements Iterator<Map.Entry<String,E>> {
			private final Iterator<Map.Entry<String,E>> iterator = CallbackMap.this.delegateMap.entrySet().iterator();
			private Map.Entry<String,E> currentEntry = null;


			@Override
			public boolean hasNext () {
				return this.iterator.hasNext();
			}


			@Override
			public Map.Entry<String,E> next () {
				return this.currentEntry = this.iterator.next();
			}


			@Override
			public void remove () {
				CallbackMap.this.fireChangeEvent(this.currentEntry.getKey(), this.currentEntry.getValue(), null);
				this.iterator.remove();
			}
		}
	}
}