package de.htw.ds.sort;

import static java.nio.charset.StandardCharsets.UTF_8;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Queue;
import de.htw.tool.Copyright;


/**
 * String sorter implementation that forwards all requests to a sort server, using a custom
 * sort protocol. Note that a single connection is reused for all interactions with the server.
 * Also note that the simple CSP protocol only allows for string sorters, and it's syntax is
 * defined in EBNF as follows; note that two subsequent line separators demarcate the end of
 * CSP requests and responses - the same technique is used to demarcate the end of HTTP
 * request and response headers:
 * <pre>
 * cspRequest	:= { element, CR }, CR
 * cspResponse	:= { element, CR }, CR
 * CR			:= line separator
 * element		:= utf8-string - (null | "")
 * </pre>
 */
@SuppressWarnings("unused") // TODO: remove this line
@Copyright(year=2010, holders="Sascha Baumeister")
public class ProxySorter implements MergeSorter<String> {
	static private final int BUFFER_SIZE = 0xF000;

	private final Socket connection;
	private final BufferedReader charSource;
	private final BufferedWriter charSink;
	private State state;


	/**
	 * Creates a new instance in {@link State#WRITE} state that is able to communicate with
	 * a sort server using the given service address.
	 * @param serviceAddress the service address
	 * @throws NullPointerException if the given service address is {@code null}
	 * @throws IOException if there is an I/O related problem
	 */
	public ProxySorter (final InetSocketAddress serviceAddress) throws NullPointerException, IOException {
		this.connection = new Socket(serviceAddress.getAddress(), serviceAddress.getPort());
		this.charSource = new BufferedReader(new InputStreamReader(this.connection.getInputStream(), UTF_8), BUFFER_SIZE);
		this.charSink = new BufferedWriter(new OutputStreamWriter(this.connection.getOutputStream(), UTF_8), BUFFER_SIZE);
		this.state = State.WRITE;
	}


	/**
	 * {@inheritDoc}
	 */
	public void close () throws IOException {
		try {
			try {
				this.charSink.flush();
			} finally {
				this.connection.close();
			}
		} finally {
			this.state = State.CLOSED;
		}
	}


	/**
	 * {@inheritDoc}
	 */
	public void write (final String element) throws IllegalStateException, IOException {
		if (this.state != State.WRITE) throw new IllegalStateException(this.state.name());

		// TODO: If the given element is null, write newline characters to the char sink, flush
		// the latter, and set the state to SORT. Otherwise, write the given element to the
		// char sink, write newline characters to the latter, and do NOT flush it.
		
		if (element == null) {
			this.charSink.newLine();
			this.charSink.flush();
			this.state = State.SORT;
		} else {
			this.charSink.write(element);
			this.charSink.newLine();
		}
	}


	/**
	 * {@inheritDoc}
	 */
	public void sort () throws IllegalStateException {
		if (this.state != State.SORT) throw new IllegalStateException(this.state.name());

		this.state = State.READ;
	}


	/**
	 * {@inheritDoc}
	 */
	public String read () throws IllegalStateException, IOException {
		if (this.state != State.READ) throw new IllegalStateException(this.state.name());

		// TODO: Read the next line from the char source. Return said line if it is
		// neither null nor empty. Otherwise, set the state to WRITE and return null.
		
		String line = this.charSource.readLine();
		
		if (line == null || line.isEmpty()) {
			line = null;
			this.state = State.WRITE;
		}
		
		return line;
	}


	/**
	 * {@inheritDoc}
	 */
	public State getState () {
		return this.state;
	}


	/**
	 * Returns the root sorter instance of a balanced recursion tree of new sorters.
	 * Said tree will contain one proxy sorter instance for each of the given socket
	 * addresses. If there is exactly one given socket address, the result will be
	 * the sole proxy sorter instance created. Otherwise, the result will be a
	 * multi-thread sorter instance.
	 * @param serviceAddresses the sort server service addresses
	 * @return the root sorter created
	 * @throws NullPointerException if any of the given arguments is {@code null}
	 * @throws IllegalArgumentException if there is no argument given
	 * @throws IOException if there is an I/O related problem
	 */
	static public MergeSorter<String> newInstance (final InetSocketAddress... serviceAddresses) throws NullPointerException, IllegalArgumentException, IOException {
		if (serviceAddresses.length == 0) throw new IllegalArgumentException();

		// TODO Create a queue containing one proxy sorter instance for each of the given service
		// addresses - which will be at least one. While there is more than one sorter within said
		// queue, remove two of them, use these to create a new multi-thread sorter instance, and
		// add the latter to the queue - make sure this follows fist in first out semantics. This
		// way, the queue is guaranteed to contain exactly one element in the end, which shall be returned.
		
		final int numberOfAddresses = serviceAddresses.length;
		Queue<MergeSorter<String>> queue = new LinkedList<>();
		for (int i = 0; i < numberOfAddresses; i++) queue.add(new ProxySorter(serviceAddresses[i]));
		while(queue.size() > 1)	queue.add(new MultiThreadSorter<>(queue.remove(), queue.remove()));
		return queue.remove();
	}
}