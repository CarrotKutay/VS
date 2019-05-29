package de.htw.tool;

import static javax.xml.bind.annotation.XmlAccessType.NONE;
import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * A string key-value pair. The {@link Map#entrySet()} method returns a collection-view of a map, consisting of
 * {@link Map#Entry} instances. However, these cannot be marshalled due to a lack of JAX-B annotations, and their generic types.
 * @see Map#entrySet()
 */
@XmlType
@XmlRootElement
@XmlAccessorType(NONE)
@Copyright(year = 2009, holders = "Sascha Baumeister")
public class StringEntry implements Entry<String,String>, Comparable<Entry<String,String>> {

	@XmlAttribute
	private String key;

	@XmlAttribute
	private String value;


	/**
	 * Associates null key and value (used by JAX-B unmarshaling).
	 */
	protected StringEntry () {
		this(null, null);
	}


	/**
	 * Associates the given key and value.
	 * @param key the key
	 * @param value the value
	 */
	public StringEntry (final String key, final String value) {
		this.key = key;
		this.value = value;
	}


	/**
	 * {@inheritDoc}
	 */
	public String getKey () {
		return this.key;
	}


	/**
	 * {@inheritDoc}
	 */
	public String getValue () {
		return this.value;
	}


	/**
	 * {@inheritDoc}
	 */
	public String setValue (final String value) {
		final String result = this.value;
		this.value = value;
		return result;
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString () {
		return this.key + " -> " + this.value;
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode () {
		return Objects.hashCode(this.key) ^ Objects.hashCode(this.value);
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals (final Object object) {
		if (!(object instanceof Entry)) return false;
		final Entry<?,?> entry = (Entry<?,?>) object;
		return Objects.equals(this.key, entry.getKey()) && Objects.equals(this.value, entry.getValue());
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public int compareTo (final Entry<String,String> entry) {
		final int compare = Objects.compare(this.key, entry.getKey(), Comparator.naturalOrder());
		return compare == 0 ? Objects.compare(this.value, entry.getValue(), Comparator.naturalOrder()) : compare;
	}
}