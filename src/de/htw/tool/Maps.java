package de.htw.tool;

import static javax.xml.bind.annotation.XmlAccessType.NONE;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * This facade provides some basic operations for maps.
 */
@Copyright(year = 2016, holders = "Sascha Baumeister")
public final class Maps {

	/**
	 * Prevents external instantiation.
	 */
	private Maps () {}



	/**
	 * Parses all properties stored within the given byte source, and returns them as a String map.
	 * @param byteSource the property source
	 * @return the String map created
	 * @throws NullPointerException if any of the given arguments is {@code null}
	 * @throws IOException if there is an I/O related problem parsing the source
	 */
	static public Map<String,String> readProperties (final InputStream byteSource) throws NullPointerException, IOException {
		final Properties properties = new Properties();
		properties.load(byteSource);

		@SuppressWarnings({ "unchecked", "rawtypes" })
		final Map<String,String> map = (Map) properties;
		return map;
	}


	/**
	 * Returns the given map as a set of associations that can be marshaled into JSON or XML.
	 * @param <K> the key type
	 * @param <V> the value type
	 * @param map the map
	 * @return the associations
	 */
	static public <K,V> Set<Association<K,V>> toAssociations (final Map<K,V> map) {
		return map
			.entrySet()
			.stream()
			.map(entry -> new Association<>(entry.getKey(), entry.getValue()))
			.collect(Collectors.toSet());
	}


	/**
	 * Returns the given set of associations that can be unmarshaled from JSON or XML as a map.
	 * @param <K> the key type
	 * @param <V> the value type
	 * @param associations the associations
	 * @return map the map
	 */
	static public <K,V> Map<K,V> fromAssociations (final Set<Association<K,V>> associations) {
		return associations
			.stream()
			.collect(Collectors.toMap(Association::getKey, Association::getValue));
	}



	/**
	 * Instances of this type model map entries that can be marshaled into JSON and XML.
	 * @param <K> the key type
	 * @param <V> the value type
	 */
	@XmlType @XmlRootElement @XmlAccessorType(NONE)
	static public class Association<K,V> implements Map.Entry<K,V> {

		private K key;
		private V value;


		/**
		 * Initializes a new instance with {@code null} key and value.
		 * Note that this operation is provided solely for marshaling purposes.
		 */
		protected Association () {
			this(null, null);
		}


		/**
		 * Initializes a new instance with the given key and value.
		 * @param key the key, or {@code null} for none
		 * @param value the value, or {@code null} for none
		 */
		public Association (final K key, final V value) {
			this.key = key;
			this.value = value;
		}


		/**
		 * {@inheritDoc}
		 * @return the key, or {@code null} for none
		 */
		@Override
		public K getKey () {
			return this.key;
		}


		/**
		 * Replaces the key corresponding to this entry with the specified key.
		 * @param key the new key to be stored in this entry, or {@code null} for none
		 * @return the old key corresponding to this entry, or {@code null} for none
		 */
		public K setKey (final K key) {
			final K oldKey = this.key;
			this.key = key;
			return oldKey;
		}


		/**
		 * {@inheritDoc}
		 * @return the value, or {@code null} for none
		 */
		@Override
		public V getValue () {
			return this.value;
		}


		/**
		 * Replaces the value corresponding to this entry with the specified value.
		 * @param value the new value to be stored in this entry, or {@code null} for none
		 * @return the old value corresponding to this entry, or {@code null} for none
		 */
		@Override
		public V setValue (final V value) {
			final V oldValue = this.value;
			this.value = value;
			return oldValue;
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
		public boolean equals (final Object other) {
			if (!(other instanceof Association)) return false;
			final Association<?,?> association = (Association<?,?>) other;
			return Objects.equals(this.key, association.key) & Objects.equals(this.value, association.value);
		}
	}
}