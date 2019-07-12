package de.htw.ds.sort;

import static java.nio.charset.StandardCharsets.UTF_8;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import de.htw.tool.Copyright;


/**
 * This class defines a file sorter test case. It sorts all non-empty words of a source file
 * into a sink file. Note that this class is declared abstract because it provides common
 * operations for specific sub-types.
 */
@Copyright(year=2010, holders="Sascha Baumeister")
public abstract class SortClient {

	private final Path sourcePath;
	private final Path sinkPath;
	private final MergeSorter<String> sorter;


	/**
	 * Initializes a new instance based on the given arguments.
	 * @param sourcePath the source file path
	 * @param sinkPath the sink file path
	 * @param sorter the sorter
	 * @throws NullPointerException if any of the given arguments is {@code null}
	 * @throws IllegalArgumentException if the given source file paths does not represent a regular file, or
	 * 		   if the given sink file path represents a directory
	 */
	public SortClient (final Path sourcePath, final Path sinkPath, final MergeSorter<String> sorter) throws NullPointerException, IllegalArgumentException {
		if (!Files.isRegularFile(sourcePath) | Files.isDirectory(sinkPath)) throw new NullPointerException();

		this.sourcePath = sourcePath;
		this.sinkPath = sinkPath;
		this.sorter = Objects.requireNonNull(sorter);
	}


	/**
	 * Returns the sorter.
	 * @return the sorter
	 */
	public MergeSorter<String> getSorter () {
		return this.sorter;
	}


	/**
	 * Sorts the words within the given source file, and writes them into the given sink file.
	 * @throws IOException if an I/O related problem occurs
	 */
	public final void process () throws IOException {
		final long timestamp1, timestamp2, timestamp3, timestamp4;
		long wordCount = 0;

		try (BufferedReader charSource = Files.newBufferedReader(this.sourcePath, UTF_8)) {
			try (BufferedWriter charSink = Files.newBufferedWriter(this.sinkPath, UTF_8)) {
				timestamp1 = System.currentTimeMillis();
				for (String line = charSource.readLine(); line != null; line = charSource.readLine()) {
					for (final String word : line.split("[\\s,\\!,\\?,\\.,\\,,\\(,\\),\",:,;]")) {
						if (word.isEmpty()) continue;
						this.sorter.write(word);
						wordCount += 1;
					}
				}
				this.sorter.write(null);

				timestamp2 = System.currentTimeMillis();
				this.sorter.sort();

				timestamp3 = System.currentTimeMillis();
				for (String word = this.sorter.read(); word != null; word = this.sorter.read()) {
					charSink.write(word);
					charSink.newLine();
				}

				timestamp4 = System.currentTimeMillis();
			}
		} finally {
			this.sorter.close();
		}

		System.out.format("Sort ok, %d words sorted.\n", wordCount);
		System.out.format("Read time: %dms.\n", timestamp2 - timestamp1);
		System.out.format("Sort time: %dms.\n", timestamp3 - timestamp2);
		System.out.format("Write time: %dms.\n", timestamp4 - timestamp3);
	}
}