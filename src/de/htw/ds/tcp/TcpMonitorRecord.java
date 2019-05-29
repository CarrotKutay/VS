package de.htw.ds.tcp;

import java.math.BigInteger;
import java.util.Random;
import de.htw.tool.Copyright;


/**
 * Instances of this class model records of TCP communications activity between a monitor's client
 * and a monitor's forward server.
 */
@Copyright(year=2012, holders="Sascha Baumeister")
public class TcpMonitorRecord {
	static private final Random RANDOMIZER = new Random();

	private final long identity;
	private final long openTimestamp;
	private final long closeTimestamp;
	private final byte[] requestData;
	private final byte[] responseData;


	/**
	 * Instances of this class model TCP monitor records of the data exchange between TCP clients
	 * and servers. The request data must be all the data sent from a client to the monitor and it's
	 * forward server, and the response data all the data sent back from the forward server to the
	 * monitor and it's client.
	 * @param openTimestamp the milliseconds since 1/1/1970 since both the client and forward server
	 *        connections were open
	 * @param closeTimestamp the milliseconds since 1/1/1970 since both the client and forward
	 *        server connections were closed
	 * @param requestData the data sent from a client to a monitor
	 * @param responseData the data sent from a server to a monitor
	 * @throws NullPointerException if the given request or response data is {@code null}
	 */
	public TcpMonitorRecord (final long openTimestamp, final long closeTimestamp, final byte[] requestData, final byte[] responseData) {
		if (requestData == null | responseData == null) throw new NullPointerException();

		this.identity = new BigInteger(63, RANDOMIZER).longValue();
		this.openTimestamp = openTimestamp;
		this.closeTimestamp = closeTimestamp;
		this.requestData = requestData;
		this.responseData = responseData;
	}


	/**
	 * Returns the time then both the client and forward server connections were closed.
	 * @return the close timestamp, in milliseconds since 1/1/1970
	 */
	public long getCloseTimestamp () {
		return this.closeTimestamp;
	}


	/**
	 * The generated identity of this record. Note that the huge value range of 2^63 possibilities,
	 * combined with the fact that random number generators in Java use scrambled system nano time
	 * as seed whenever they're generated, makes such identities unique "enough" to be used as
	 * worldwide IDs for many applications. The idea is that when it is much more likely to
	 * encounter a program or OS error than two generated IDs that are equal, the IDs are
	 * practically unique "enough".
	 * @return the generated identity (always positive)
	 */
	public long getIdentity () {
		return this.identity;
	}


	/**
	 * Returns the time then both the client and forward server connections were open.
	 * @return the open timestamp, in milliseconds since 1/1/1970
	 */
	public long getOpenTimestamp () {
		return this.openTimestamp;
	}


	/**
	 * Returns the data sent from a client to a monitor.
	 * @return the request data
	 */
	public byte[] getRequestData () {
		return this.requestData;
	}


	/**
	 * Returns the number of bytes in the request data. Note that this method allows property based
	 * applications to access the length via the bean introspection.
	 * @return the request length
	 */
	public int getRequestLength () {
		return this.requestData.length;
	}


	/**
	 * Returns the data sent from a forward server to a monitor.
	 * @return the response data
	 */
	public byte[] getResponseData () {
		return this.responseData;
	}


	/**
	 * Returns the number of bytes in the response data. Note that this method allows property based
	 * applications to access the length via the bean introspection.
	 * @return the response length
	 */
	public int getResponseLength () {
		return this.responseData.length;
	}
}