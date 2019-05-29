package de.htw.tool;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;


/**
 * Class loader extending it's parent class loader by reading class files from a given context directory.
 */
@Copyright(year = 2008, holders = "Sascha Baumeister")
public class ClassFileLoader extends ClassLoader {

	private final Path contextPath;


	/**
	 * Creates a new instance that will load classes from it's parent loader, and if not found from class files within the given
	 * context directory or one of it's children.
	 * @param parent the optional parent class loader, or {@code null}
	 * @param contextPath the context directory to load class files from
	 * @throws NullPointerException if the given context path is {@code null}
	 * @throws NotDirectoryException if the given context path is not a directory
	 */
	public ClassFileLoader (final ClassLoader parent, final Path contextPath) throws NotDirectoryException {
		super(parent);

		if (!Files.isDirectory(contextPath)) throw new NotDirectoryException(contextPath.toString());
		this.contextPath = contextPath;
	}


	/**
	 * {@inheritDoc}
	 * @throws ClassFormatError if the class file is malformed or too large
	 * @throws ClassNotFoundException if the class file could not be found
	 */
	@Override
	protected Class<?> findClass (final String name) throws ClassNotFoundException, ClassFormatError {
		final String separator = this.contextPath.getFileSystem().getSeparator();
		final Path classFilePath = this.contextPath.resolve(name.replace(".", separator) + ".class");

		try {
			final byte[] classBytes = Files.readAllBytes(classFilePath);
			return this.defineClass(name, classBytes, 0, classBytes.length);
		} catch (final NoSuchFileException | AccessDeniedException exception) {
			throw new ClassNotFoundException(name);
		} catch (final IOException exception) {
			throw new ClassFormatError(name + " is broken.");
		} catch (final OutOfMemoryError exception) {
			throw new ClassFormatError(name + " is too large.");
		}
	}
}