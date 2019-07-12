package de.htw.ds.sort;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import de.htw.tool.Copyright;


/**
 * This class implements a single-threaded string sorter test case. It sorts all words
 * of a source file into a sink file. Note that this class is declared final because it
 * provides an application entry point, and therefore is not supposed to be extended.
 */
@Copyright(year=2010, holders="Sascha Baumeister")
public final class SortClient2 extends SortClient {

	/**
	 * Initializes a new instance based on the given arguments.
	 * @param sourcePath the source file path
	 * @param sinkPath the sink file path
	 * @param sorter the sorter
	 * @throws NullPointerException if any of the given arguments is {@code null}
	 * @throws IllegalArgumentException if either of the given file paths does not represent a regular file
	 */
	public SortClient2 (final Path sourcePath, final Path sinkPath, final MergeSorter<String> sorter) throws NullPointerException, IllegalArgumentException {
		super(sourcePath, sinkPath, sorter);
	}


	/**
	 * Sorts a source file's words into a sink file. Arguments must be the path to the source file,
	 * and the path of the sorted sink file.
	 * @param args the given runtime arguments
	 * @throws IllegalArgumentException if any of the given paths does not point to a regular file
	 * @throws IOException if there is an I/O related problem
	 */
	static public void main (final String[] args) throws IllegalArgumentException, IOException {
		final Path sourcePath = Paths.get(args[0]);
		final Path sinkPath = Paths.get(args[1]);

		final MergeSorter<String> sorter = SingleThreadSorter.newInstance();
		final SortClient2 client = new SortClient2(sourcePath, sinkPath, sorter);
		client.process();
	}
}