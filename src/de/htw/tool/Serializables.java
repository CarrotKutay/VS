package de.htw.tool;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.io.Serializable;
import java.io.StreamCorruptedException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import de.htw.tool.Copyright;


/**
 * This facade simplifies object serialization/deserialization, and cloning of serializable classes. Additionally, this it
 * modifies object stream I/O exception handling to adjust object serialization to modern exception handling standards -
 * therefore causing API use to be more streamlined, more consistent, and less error prone. This is achieved by eliminating all
 * checked I/O exceptions that cannot happen in this design, and additionally by retrowing all checked exceptions that usually
 * point to programming or algorithmic errors as unchecked exceptions.<br />
 * Note that modern design makes very selective and careful use of checked exceptions. An exception should be checked only if
 * BOTH of the following apply:
 * <ul>
 * <li>The problem is usually not caused by programming error or reasons unrelated to the code. Consider why
 * NullPointerException or OutOfMemoryError are unchecked.</li>
 * <li>There is a good chance the problem can be meaningfully remedied in a catch block, i.e. in other ways than simply
 * displaying the problem to an end user. Consider why the more modern JAX-WS API (WebServices) does NOT force any service
 * method to declare RemoteException, unlike the older Java-RMI standard.</li>
 * </ul>
 * Also note that this class is declared final because it is a facade, and therefore not supposed to be extended.
 */
@Copyright(year = 2010, holders = "Sascha Baumeister")
public final class Serializables {

	/**
	 * Prevents instantiation.
	 */
	private Serializables () {}


	/**
	 * Serializes the given objects and returns the resulting data. Note that the given objects may be null, but not the given
	 * variable arguments list.
	 * @param objects the objects
	 * @return the serialized data
	 * @throws NullPointerException if the given variable arguments list is {@code null}
	 * @throws NotSerializableException if one of the given objects, or one of it's direct or indirect fields is expected to be
	 *         serializable, but is not
	 */
	static public byte[] serializeObjects (final Serializable... objects) throws NotSerializableException {
		try (ByteArrayOutputStream byteSink = new ByteArrayOutputStream()) {
			try (ObjectOutputStream objectSink = new ObjectOutputStream(byteSink)) {
				for (final Serializable object : objects) {
					objectSink.writeObject(object);
				}
				return byteSink.toByteArray();
			}
		} catch (final IOException exception) {
			if (exception instanceof NotSerializableException) throw (NotSerializableException) exception;
			// others cannot happen because byte streams don't generate any IO exceptions, and
			// serializable classes are no longer expected to possess accessible no-arg constuctors.
			throw new AssertionError();
		}
	}


	/**
	 * Deserializes the given data and returns the resulting objects. Note that this method simplifies object stream exception
	 * handling by indicating incompatible serialVersionUIDs as ClassNotFoundException (similar logic to unreadable files
	 * causing FileNotFoundException), and both corrupted serialization headers and unexpected primitive data as
	 * IllegalArgumentException (because both point to messed up data).
	 * @param data the serialized data
	 * @param offset the offset
	 * @param length the length
	 * @return the deserialized objects
	 * @throws NullPointerException if the given byte array is {@code null}
	 * @throws IndexOutOfBoundsException if deserialization would cause access of data outside array bounds
	 * @throws IllegalArgumentException if the given data does not represent a valid serialized sequence of objects
	 * @throws ClassNotFoundException if a class is required that cannot be loaded, or has a serialVersionUID incompatible to
	 *         it's equivalent represented in the data
	 */
	static public Serializable[] deserializeObjects (final byte[] data, int offset, int length) throws ClassNotFoundException {
		if (offset < 0 || length < 0 || offset + length > data.length) throw new IndexOutOfBoundsException();

		try (ByteArrayInputStream byteSource = new ByteArrayInputStream(data, offset, length)) {
			final List<Serializable> result = new ArrayList<>();

			try (ObjectInputStream objectSource = new ObjectInputStream(byteSource)) {
				while (true) {
					result.add((Serializable) objectSource.readObject());
				}
			} catch (final EOFException exception) {
				// loop exit condition when the bytes array is truncated
			}

			return result.toArray(new Serializable[result.size()]);
		} catch (final InvalidClassException exception) {
			throw new ClassNotFoundException(exception.getMessage(), exception);
		} catch (final StreamCorruptedException exception) {
			throw new IllegalArgumentException(exception);
		} catch (final OptionalDataException exception) {
			throw new IllegalArgumentException(exception);
		} catch (final IOException exception) {
			throw new AssertionError();// cannot happen because byte sources don't generate any IO exceptions
		}
	}


	/**
	 * Deserializes the given data and returns the first object contained within it. This is a convenience method for use with
	 * serialized data containing single objects. Note that this method simplifies object stream exception handling by
	 * indicating incompatible serialVersionUIDs as ClassNotFoundException (similar logic to unreadable files causing
	 * FileNotFoundException), and both corrupted serialization headers and unexpected primitive data as
	 * IllegalArgumentException (because both point to messed up data).
	 * @param data the data
	 * @param offset the offset
	 * @param length the length
	 * @return the deserialized object
	 * @throws NullPointerException if the given byte array is {@code null}
	 * @throws IndexOutOfBoundsException if deserialization would cause access of data outside array bounds
	 * @throws NoSuchElementException if there is no serialized object in the given data
	 * @throws IllegalArgumentException if the given data does not represent a valid serialized sequence of objects
	 * @throws ClassNotFoundException if a class is required that cannot be loaded, or has a serialVersionUID incompatible to
	 *         it's equivalent represented in the data
	 */
	static public Serializable deserializeObject (final byte[] data, int offset, int length) throws ClassNotFoundException {
		final Serializable[] objects = Serializables.deserializeObjects(data, offset, length);
		if (objects.length == 0) throw new NoSuchElementException();
		return objects[0];
	}


	/**
	 * Clones the given object using serialization followed by immediate deserialization.
	 * @param <T> the object type
	 * @param object the object to be cloned, or {@code null}
	 * @return the clone, or {@code null}
	 * @throws NotSerializableException if the given object is not serializable
	 */
	@SuppressWarnings("unchecked")
	static public final <T extends Serializable> T clone (final T object) throws NotSerializableException {
		final byte[] bytes = Serializables.serializeObjects(object);
		try {
			return (T) Serializables.deserializeObject(bytes, 0, bytes.length);
		} catch (final ClassNotFoundException exception) {
			throw new AssertionError();// cannot happen because we serialized the data within the same process 
		}
	}
}