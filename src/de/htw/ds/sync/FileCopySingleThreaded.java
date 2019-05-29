package de.htw.ds.sync;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import de.htw.tool.Copyright;


/**
 * Demonstrates copying a file using a single thread. Note that this class is declared final because
 * it provides an application entry point, and therefore not supposed to be extended.
 */
@Copyright(year=2008, holders="Sascha Baumeister")
public final class FileCopySingleThreaded {

	/**
	 * Copies a file. The first argument is expected to be a qualified source file name, the second
	 * a qualified target file name.
	 * @param args the VM arguments
	 * @throws IOException if there's an I/O related problem
	 */
	static public void main (final String[] args) throws IOException {
		final Path sourcePath = Paths.get(args[0]);
		if (!Files.isReadable(sourcePath)) throw new IllegalArgumentException(sourcePath.toString());

		final Path sinkPath = Paths.get(args[1]);
		if (sinkPath.getParent() != null && !Files.isDirectory(sinkPath.getParent())) throw new IllegalArgumentException(sinkPath.toString());

	//	 Files.copy(sourcePath, sinkPath, StandardCopyOption.REPLACE_EXISTING);

		try (InputStream fis = Files.newInputStream(sourcePath)) {
			try (OutputStream fos = Files.newOutputStream(sinkPath)) {
				final byte[] buffer = new byte[0x10000];
				for (int bytesRead = fis.read(buffer); bytesRead != -1; bytesRead = fis.read(buffer)) {
					fos.write(buffer, 0, bytesRead);
				}
			}
		}
		
		System.out.println("done.");
	}
}