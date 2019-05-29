package de.htw.ds.sync;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import de.htw.tool.Copyright;
import de.htw.tool.InetAddresses;


/**
 * Demonstrates copying a file using two separate threads for file-read and file-write. Note that
 * this is only expected to be more efficient that a single-threaded implementation when using
 * multi-core systems with multiple hard drives! Also note that this class is declared final because
 * it provides an application entry point, and therefore not supposed to be extended.
 */
@Copyright(year=2008, holders="Sascha Baumeister")
public final class FileCopyReceive {

	/**
	 * Copies a file. The first argument is expected to be a qualified source file name, the second
	 * a qualified target file name.
	 * @param args the VM arguments
	 * @throws IOException if there's an I/O related problem
	 */
	static public void main (final String[] args) throws IOException {
		final Path sinkPath = Paths.get(args[0]);
		final InetSocketAddress address = InetAddresses.toSocketAddress(args[1]);
		if (sinkPath.getParent() != null && !Files.isDirectory(sinkPath.getParent())) throw new IllegalArgumentException(sinkPath.toString());

		try (Socket connection = new Socket(address.getAddress(), address.getPort())) {
			Files.copy(connection.getInputStream(), sinkPath);
		}
		System.out.println("done.");
	}
}