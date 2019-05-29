package de.htw.ds.sync;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import de.htw.tool.Copyright;


/**
 * Demonstrates copying a file using two separate processes for file-read and file-write. 
 */
@Copyright(year=2008, holders="Sascha Baumeister")
public final class FileCopySend {

	/**
	 * Copies a file. The first argument is expected to be a qualified source file name, the second
	 * a TCP port.
	 * @param args the VM arguments
	 * @throws IOException if there's an I/O related problem
	 */
	static public void main (final String[] args) throws IOException {
		final Path sourcePath = Paths.get(args[0]);
		final int port = Integer.parseInt(args[1]);
		if (!Files.isReadable(sourcePath)) throw new IllegalArgumentException(sourcePath.toString());

		try (ServerSocket service = new ServerSocket(port)) {
			try (Socket connection = service.accept()) {
				Files.copy(sourcePath, connection.getOutputStream());
			}
		}
		System.out.println("done.");
	}
}