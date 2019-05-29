package de.htw.tool;

import java.io.IOException;
import java.math.BigInteger;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;


/**
 * This facade offers support for IP addresses and socket addresses. Specifically, it provides convenience methods to query the
 * local network interfaces, local IP addressses and host names, and concersion operations for IP addresses and socket addresses
 * to/from byte[], BigInteger and String types.<br />
 * Note that socket addresses only resolve a given hostname once, and are therefore considered harmful as long term information
 * storage; text or integer representations do not imply this restriction. Also note that IPv4-mapped address representations
 * are supported, while (deprecated) IPv4-compatible representations are not; the latter will simply be treated as normal IPv6
 * addresses.
 */
@Copyright(year = 2015, holders = "Sascha Baumeister")
public final class InetAddresses {

	/**
	 * Byte length of a raw IPv4 address.
	 */
	static public final int INET4_ADDRESS_SIZE = 4;

	/**
	 * Byte length of a raw IPv4 socket address.
	 */
	static public final int INET4_SOCKET_ADDRESS_SIZE = INET4_ADDRESS_SIZE + 2;

	/**
	 * Byte length of a raw IPv6 address.
	 */
	static public final int INET6_ADDRESS_SIZE = 16;

	/**
	 * Byte length of a raw IPv6 socket address.
	 */
	static public final int INET6_SOCKET_ADDRESS_SIZE = INET6_ADDRESS_SIZE + 2;

	/**
	 * The IPv4 wildcard address, i.e. "0.0.0.0".
	 */
	static public final Inet4Address WILDCARD_INET4_ADDRESS;

	/**
	 * The IPv6 wildcard address, i.e. "0:0:0:0:0:0:0:0".
	 */
	static public final Inet6Address WILDCARD_INET6_ADDRESS;

	/**
	 * The IPv4 loopback interface address, i.e. "127.0.0.1". Note that an operating systems may assign additional loopback
	 * address(s) to each network interface, usually "127.0.x.1"!
	 */
	static public final Inet4Address LOOPBACK_INET4_ADDRESS;

	/**
	 * The IPv6 loopback interface address, i.e. "0:0:0:0:0:0:0:1".
	 */
	static public final Inet6Address LOOPBACK_INET6_ADDRESS;

	/**
	 * Natural order comparator for network interfaces (by index, then by name)
	 */
	static public final Comparator<NetworkInterface> NATURAL_INTERFACE_ORDER = Comparator.comparing(NetworkInterface::getIndex).thenComparing(NetworkInterface::getName);

	/**
	 * Natural order comparator for network interfaces (by value, then by IP version)
	 */
	static public final Comparator<InetAddress> NATURAL_ADDRESS_ORDER = Comparator.comparing( (InetAddress address) -> toInteger(address)).thenComparing(address -> address instanceof Inet6Address);

	static private final BigInteger INET4_MAPPED_PREFIX = BigInteger.valueOf(0xffff).shiftLeft(Integer.SIZE);


	static {
		try {
			WILDCARD_INET4_ADDRESS = (Inet4Address) InetAddress.getByAddress(new byte[] { 0, 0, 0, 0 });
			WILDCARD_INET6_ADDRESS = (Inet6Address) InetAddress.getByAddress(new byte[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 });
			LOOPBACK_INET4_ADDRESS = (Inet4Address) InetAddress.getByAddress(new byte[] { 127, 0, 0, 1 });
			LOOPBACK_INET6_ADDRESS = (Inet6Address) InetAddress.getByAddress(new byte[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1 });
		} catch (final UnknownHostException exception) {
			throw new AssertionError(exception);
		}
	}


	/**
	 * Prevents external instantiation.
	 */
	private InetAddresses () {}


	/**
	 * Returns the physical network interfaces, excluding sub-interfaces.
	 * @return the physical network interfaces, ordered by index and name
	 */
	static public List<NetworkInterface> physicalNetworkInterfaces () {
		try {
			final List<NetworkInterface> networkInterfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
			networkInterfaces.sort(NATURAL_INTERFACE_ORDER);
			return networkInterfaces;
		} catch (final SocketException exception) {
			return Collections.emptyList();
		}
	}


	/**
	 * Returns all available network interfaces, recursively including virtual interfaces.
	 * @return the local network interfaces, ordered depth-first by index and name
	 */
	static public List<NetworkInterface> allNetworkInterfaces () {
		final List<NetworkInterface> networkInterfaces = new ArrayList<>();
		final Consumer<NetworkInterface> recursiveCollector = new Consumer<NetworkInterface>() {
			public void accept (final NetworkInterface networkInterface) {
				if (networkInterfaces.add(networkInterface)) {
					final List<NetworkInterface> virtualInterfaces = Collections.list(networkInterface.getSubInterfaces());
					virtualInterfaces.sort(NATURAL_INTERFACE_ORDER);
					virtualInterfaces.forEach(this);
				}
			}
		};
		physicalNetworkInterfaces().forEach(recursiveCollector);
		return networkInterfaces;
	}


	/**
	 * Returns all IP addresses registered for the local network interfaces.
	 * @return all (unicast) local IP addresses
	 */
	static public Set<InetAddress> localAddresses () {
		final Set<InetAddress> networkAddresses = new HashSet<>();
		final List<NetworkInterface> networkInterfaces = allNetworkInterfaces();
		for (int index = 0, stop = networkInterfaces.size(); index < stop; ++index) {
			final NetworkInterface networkInterface = networkInterfaces.get(index);
			networkAddresses.addAll(Collections.list(networkInterface.getInetAddresses()));
			if (!networkInterface.isVirtual()) {
				try {
					final InetAddress loopbackAddress = InetAddress.getByAddress(new byte[] { 127, 0, (byte) index, 1 });
					if (loopbackAddress.isReachable(0)) networkAddresses.add(loopbackAddress);
				} catch (final IOException exception) {
					// do nothing
				}
			}
		}
		return networkAddresses;
	}


	/**
	 * Returns a resolved local IP address, or the IPv4 wildcard address if the former cannot be determined.
	 * @return the local address
	 */
	static public InetAddress localAddress () {
		final Set<InetAddress> networkAddresses = localAddresses();
		if (networkAddresses.isEmpty()) return WILDCARD_INET4_ADDRESS;

		final Comparator<InetAddress> preferenceComparator = Comparator.comparing( (InetAddress address) -> address.isLoopbackAddress()).thenComparing(address -> address.isSiteLocalAddress() | address.isLinkLocalAddress()).thenComparing(address -> address instanceof Inet6Address).thenComparing(address -> toInteger(address));
		return Collections.min(networkAddresses, preferenceComparator);
	}


	/**
	 * Returns the binary represenation of the given IP address.
	 * @param address the IPv4/6 address
	 * @return the corresponding big-endian 4/16 byte binary represenation
	 * @throws NullPointerException if the given argument is {@code null}
	 */
	static public byte[] toBinary (final InetAddress address) throws NullPointerException {
		return address.getAddress();
	}


	/**
	 * Returns the binary represenation of the given IP socket address.
	 * @param socketAddress the IPv4/6 socket address
	 * @return the corresponding big-endian 6/18 byte binary represenation
	 * @throws NullPointerException if the given argument is {@code null}
	 * @throws IllegalArgumentException if the given argument is unresolved
	 */
	static public byte[] toBinary (final InetSocketAddress socketAddress) throws NullPointerException, IllegalArgumentException {
		if (socketAddress.isUnresolved()) throw new IllegalArgumentException();
		final byte[] addressBytes = toBinary(socketAddress.getAddress());

		final byte[] socketAddressBytes = new byte[addressBytes.length + 2];
		System.arraycopy(addressBytes, 0, socketAddressBytes, 0, addressBytes.length);
		socketAddressBytes[socketAddressBytes.length - 2] = (byte) (socketAddress.getPort() >>> 8);
		socketAddressBytes[socketAddressBytes.length - 1] = (byte) socketAddress.getPort();
		return socketAddressBytes;
	}


	/**
	 * Returns the integer represenation of the given IP address, which is it's binary representation interpreted as a positive
	 * integer. Note that IPv4 addresses are automatically mapped into the IPv4-mapped value range ({@code 0xffffxxxxxxxx}) in
	 * order to keep the resulting values distinguishable from those of genuine IPv6 addresses.
	 * @param address the IP address
	 * @return the corresponding integer represenation
	 * @throws NullPointerException if the given argument is {@code null}
	 */
	static public BigInteger toInteger (final InetAddress address) throws NullPointerException {
		final byte[] bytes = toBinary(address);
		final BigInteger value = new BigInteger(1, bytes);
		return bytes.length == INET4_ADDRESS_SIZE ? value.or(INET4_MAPPED_PREFIX) : value;
	}


	/**
	 * Returns the integer represenation of the given IP socket address, which is it's address binary representation, extened by
	 * a two byte big-endian port representation, interpreted as a positive integer. Note that IPv4 addresses are automatically
	 * mapped into the IPv4-mapped value range ({@code 0xffffxxxxxxxxpppp}) in order to keep the resulting values
	 * distinguishable from those of genuine IPv6 addresses.
	 * @param socketAddress the IP socket address
	 * @return the corresponding integer represenation
	 * @throws NullPointerException if the given argument is {@code null}
	 * @throws IllegalArgumentException if the given argument is unresolved
	 */
	static public BigInteger toInteger (final InetSocketAddress socketAddress) throws NullPointerException, IllegalArgumentException {
		if (socketAddress.isUnresolved()) throw new IllegalArgumentException();
		return toInteger(socketAddress.getAddress()).shiftLeft(Short.SIZE).or(BigInteger.valueOf(socketAddress.getPort()));
	}


	/**
	 * Returns the text representation of the given IP address.
	 * @param address the IP address
	 * @param reverseLookup wether or not a reverse DNS lookup may be performed to possibly return a host name instead of a
	 *        textual address
	 * @return the text representation in decimal (IPv4) or hexadecimal (IPv6) notation
	 * @throws NullPointerException if the given argument is {@code null}
	 */
	static public String toString (final InetAddress address, final boolean reverseLookup) throws NullPointerException {
		return reverseLookup ? address.getCanonicalHostName() : address.getHostAddress();
	}


	/**
	 * Returns the text representation of the given IP socket address. Note that the port of the given socket address is
	 * represented in decimal notation for IPv4, or hexadecimal notation for IPv6 addresses.
	 * @param address the IP socket address
	 * @param reverseLookup wether or not a reverse DNS lookup may be performed to possibly return a host name instead of a
	 *        textual address
	 * @return the text representation in decimal (IPv4) or hexadecimal (IPv6) notation
	 * @throws NullPointerException if the given argument is {@code null}
	 */
	static public String toString (final InetSocketAddress socketAddress, final boolean reverseLookup) throws NullPointerException {
		final String port = Integer.toUnsignedString(socketAddress.getPort());
		final String hostname = socketAddress.isUnresolved() ? (reverseLookup ? socketAddress.getHostName() : socketAddress.getHostString()) : (reverseLookup ? socketAddress.getAddress().getCanonicalHostName() : socketAddress.getAddress().getHostAddress());
		return "0.0.0.0".equals(hostname) | "0:0:0:0:0:0:0:0".equals(hostname) ? port : hostname + ":" + port;
	}


	/**
	 * Returns an IPv4/6 address for the given binary represenation. Note that 4-byte arrays always result in IPv4 addresses;
	 * 16-byte arrays result in IPv6 addresses, except IPv4-mapped arrays {@code [0,0,0,0,0,0,0,0,0,0,-1,-1,x,x,x,x]} which
	 * result as IPv4 addresses as well.
	 * @param addressBytes the 4/16 byte big-endian binary represenation
	 * @return the corresponding IPv4/6 address
	 * @throws NullPointerException if the given argument is {@code null}
	 * @throws IllegalArgumentException if the given argument's length is neither 4 nor 16
	 */
	static public InetAddress toAddress (final byte[] addressBytes) throws NullPointerException, IllegalArgumentException {
		Objects.requireNonNull(addressBytes);
		try {
			return InetAddress.getByAddress(addressBytes);
		} catch (final UnknownHostException exception) {
			throw new IllegalArgumentException(exception);
		}
	}


	/**
	 * Returns the IP address for the given integer value. Note that all values result in IPv6 addresses, except IPv4-mapped
	 * values ({@code 0xffffxxxxxxxx}) which result in IPv4 addresses instead.
	 * @param value the integer value
	 * @return the IP address
	 * @throws NullPointerException if the given argument is {@code null}
	 * @throws IllegalArgumentException if the given value is outside range [0, 2^128 - 1]
	 */
	static public InetAddress toAddress (final BigInteger value) throws NullPointerException, IllegalArgumentException {
		if (value.bitLength() > INET6_ADDRESS_SIZE * Byte.SIZE | value.signum() == -1) throw new IllegalArgumentException();

		final byte[] valueBytes = value.toByteArray(), addressBytes;
		if (valueBytes.length == INET6_ADDRESS_SIZE) {
			addressBytes = valueBytes;
		} else {
			addressBytes = new byte[INET6_ADDRESS_SIZE];
			System.arraycopy(valueBytes, Math.max(valueBytes.length - addressBytes.length, 0), addressBytes, Math.max(addressBytes.length - valueBytes.length, 0), Math.min(valueBytes.length, addressBytes.length));
		}

		try {
			return InetAddress.getByAddress(addressBytes);
		} catch (final UnknownHostException exception) {
			throw new AssertionError(exception);
		}
	}


	/**
	 * Returns the IP address for the given text, or the IPv4 wildcard address if the text is empty.
	 * @param text the text
	 * @return the IP address
	 * @throws NullPointerException if the given argument is {@code null}
	 * @throws IllegalArgumentException if the given value is a malformed IP address representation, or a hostname that cannot
	 *         be resolved
	 * @throws SecurityException if a security manager prohibits this operation
	 */
	static public InetAddress toAddress (final String text) throws NullPointerException, IllegalArgumentException {
		if (text.isEmpty()) return WILDCARD_INET4_ADDRESS;
		try {
			return InetAddress.getByName(text);
		} catch (UnknownHostException exception) {
			throw new IllegalArgumentException(exception);
		}
	}


	/**
	 * Returns an IPv4/6 socket address for the given binary represenation. Note that 6-byte arrays always result in IPv4 socket
	 * addresses; 18-byte arrays result in IPv6 socket addresses, except IPv4-mapped arrays
	 * {@code [0,0,0,0,0,0,0,0,0,0,-1,-1,x,x,x,x,p,p]} which result as IPv4 socket addresses as well.
	 * @param socketAddressBytes the 6/18 byte big-endian binary represenation
	 * @return the corresponding IPv4/6 address
	 * @throws NullPointerException if the given argument is {@code null}
	 * @throws IllegalArgumentException if the given argument's length is neither 6 nor 18
	 */
	static public InetSocketAddress toSocketAddress (final byte[] socketAddressBytes) throws NullPointerException, IllegalArgumentException {
		if (socketAddressBytes.length != INET4_SOCKET_ADDRESS_SIZE & socketAddressBytes.length != INET6_SOCKET_ADDRESS_SIZE) throw new IllegalArgumentException();
		final int port = ((socketAddressBytes[socketAddressBytes.length - 2] & 0xff) << Byte.SIZE) | (socketAddressBytes[socketAddressBytes.length - 1] & 0xff);
		final byte[] addressBytes = new byte[socketAddressBytes.length - 2];
		System.arraycopy(socketAddressBytes, 0, addressBytes, 0, addressBytes.length);
		return new InetSocketAddress(toAddress(addressBytes), port);
	}


	/**
	 * Returns the IP socket address for the given integer value. Note that all values result in IPv6 socket addresses, except
	 * IPv4-mapped values ({@code 0xffffxxxxxxxxpppp}) which result in IPv4 socket addresses instead.
	 * @param value the integer value
	 * @return the IP socket address
	 * @throws NullPointerException if the given argument is {@code null}
	 * @throws IllegalArgumentException if the given value is outside range [0, 2^144 - 1]
	 */
	static public InetSocketAddress toSocketAddress (final BigInteger value) throws NullPointerException, IllegalArgumentException {
		if (value.signum() == -1) throw new IllegalArgumentException();
		return new InetSocketAddress(toAddress(value.shiftRight(Short.SIZE)), value.shortValue() & 0xffff);
	}


	/**
	 * Returns the IP socket address for the given text, expected in the following EBNF format:<pre>
	 * socketAddress := [[[hostname], ":"], port]
	 * port          := {(0-9)} within range [0,65535]</pre> Valid examples are "", "80", ":80", "127.0.0.1:80", "::1:80",
	 * "en.wikipedia.org:80".
	 * @param text the textual representation of a socket-address
	 * @throws NullPointerException if the given argument is {@code null}
	 * @throws IllegalArgumentException if the argument's port is not a number, or outside range [0, 65535]
	 * @throws SecurityException if a security manager prohibits this operation
	 */
	static public InetSocketAddress toSocketAddress (final String text) throws NullPointerException, IllegalArgumentException, SecurityException {
		final int delimiterPosition = text.lastIndexOf(':');
		final String portText = text.substring(delimiterPosition + 1);
		final int port = portText.isEmpty() ? 0 : Integer.parseUnsignedInt(portText);
		final String hostname = delimiterPosition <= 0 ? "" : text.substring(0, delimiterPosition);
		return hostname.isEmpty() ? new InetSocketAddress((InetAddress) null, port) : new InetSocketAddress(hostname, port);
	}
}