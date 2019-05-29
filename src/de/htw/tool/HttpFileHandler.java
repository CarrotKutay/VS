package de.htw.tool;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;


/**
 * HTTP handler class providing HTTP access to basic web resources contained within file systems or code repositories for the
 * Open/Oracle JDK HTTP server.
 */
@Copyright(year = 2010, holders = "Sascha Baumeister")
public abstract class HttpFileHandler implements HttpHandler {
	static private final short HTTP_OK = 200;
	static private final short NO_CONTENT = 204;
	static private final short HTTP_NOT_FOUND = 404;
	static private final short HTTP_METHOD_NOT_ALLOWED = 405;
	static private Map<String,String> DEFAULT_CONTENT_TYPES = new HashMap<>();


	static {
		DEFAULT_CONTENT_TYPES.put("xhtml", "application/xhtml+xml");
		DEFAULT_CONTENT_TYPES.put("html", "text/html");
		DEFAULT_CONTENT_TYPES.put("htm", "text/html");
		DEFAULT_CONTENT_TYPES.put("js", "text/javascript");
		DEFAULT_CONTENT_TYPES.put("css", "text/css");
		DEFAULT_CONTENT_TYPES.put("txt", "text/plain");
		DEFAULT_CONTENT_TYPES.put("tpl", "text/plain");
		DEFAULT_CONTENT_TYPES.put("rtf", "application/rtf");
		DEFAULT_CONTENT_TYPES.put("pdf", "application/pdf");
		DEFAULT_CONTENT_TYPES.put("ps", "application/postscript");
		DEFAULT_CONTENT_TYPES.put("eps", "application/postscript");
		DEFAULT_CONTENT_TYPES.put("bin", "application/octet-stream");
		DEFAULT_CONTENT_TYPES.put("jpeg", "image/jpeg");
		DEFAULT_CONTENT_TYPES.put("jpg", "image/jpeg");
		DEFAULT_CONTENT_TYPES.put("gif", "image/gif");
		DEFAULT_CONTENT_TYPES.put("png", "image/png");
		DEFAULT_CONTENT_TYPES.put("svg", "image/svg+xml");
		DEFAULT_CONTENT_TYPES.put("wav", "audio/wav");
		DEFAULT_CONTENT_TYPES.put("mp3", "audio/mp3");
		DEFAULT_CONTENT_TYPES.put("ogg", "audio/ogg");
		DEFAULT_CONTENT_TYPES.put("mp4", "video/mp4");
		DEFAULT_CONTENT_TYPES.put("mpeg", "video/mpeg");
		DEFAULT_CONTENT_TYPES.put("mpg", "video/mpeg");
		DEFAULT_CONTENT_TYPES.put("webm", "video/webm");
		DEFAULT_CONTENT_TYPES.put("flv", "video/x-flv");
		DEFAULT_CONTENT_TYPES.put("qt", "video/quicktime");
		DEFAULT_CONTENT_TYPES.put("mov", "video/quicktime");
	}


	private final String contextPath;
	private final Map<String,String> contentTypes;


	/**
	 * Creates a new instance.
	 * @param contextPath the context path
	 * @throws NullPointerException if any of the given arguments is {@code null}
	 */
	protected HttpFileHandler (String contextPath) throws NullPointerException {
		if (contextPath == null) throw new NullPointerException();
		if (!contextPath.startsWith("/")) contextPath = "/" + contextPath;
		if (!contextPath.endsWith("/")) contextPath = contextPath + "/";

		this.contextPath = contextPath;
		this.contentTypes = Collections.synchronizedMap(new HashMap<>(DEFAULT_CONTENT_TYPES));
	}


	/**
	 * Returns the context path without it's terminal slash (except for root paths).
	 * @return the context path
	 */
	public String getContextPath () {
		return this.contextPath.length() == 1 ? this.contextPath : this.contextPath.substring(0, this.contextPath.length() - 1);
	}


	/**
	 * Returns the handler's life (and synchronized) content type mappings. Note that the resulting map allows the registration
	 * of additional content types after file handler creation.
	 * @return the content type mappings
	 */
	public Map<String,String> getContentTypes () {
		return this.contentTypes;
	}


	/**
	 * Handles the given HTTP exchange by copying the content of it's request path to it's response. Only GET requests are
	 * supported. The request path is interpreted to be relative to the handler's context directory, all path's outside of this
	 * scope are inaccessible. Sets one of these HTTP response codes:
	 * <ul>
	 * <li>200 OK: if the operation is successful.</li>
	 * <li>204 OK: if the operation is successful, but the resource size is zero.</li>
	 * <li>404 NOT FOUND: if the requested resource could not be found.</li>
	 * <li>405 METHOD NOT ALLOWED: if the request method is not GET.</li>
	 * <li>500 INTERNAL SERVER ERROR: if this handler was not properly registered.</li>
	 * </ul>
	 * @param exchange the HTTP exchange
	 * @throws NullPointerException if any of the given arguments is {@code null}
	 * @throws IOException if there is an I/O related problem
	 */
	public void handle (final HttpExchange exchange) throws NullPointerException, IOException {
		try {
			final String requestPath = exchange.getRequestURI().getPath();
			if (!requestPath.startsWith(this.contextPath)) {
				exchange.sendResponseHeaders(HTTP_NOT_FOUND, -1);
				return;
			}

			if (!"GET".equals(exchange.getRequestMethod())) {
				exchange.getResponseHeaders().add("Allow", "GET");
				exchange.sendResponseHeaders(HTTP_METHOD_NOT_ALLOWED, -1);
				return;
			}

			final String resourcePath = requestPath.substring(this.contextPath.length());
			final String resourceExtension = requestPath.substring(requestPath.lastIndexOf('.') + 1);
			final String contentType = this.contentTypes.getOrDefault(resourceExtension.toLowerCase(), "application/octet-stream");
			exchange.getResponseHeaders().add("Content-Type", contentType);

			try {
				this.handle(exchange, resourcePath);
			} catch (final NoSuchFileException exception) {
				exchange.sendResponseHeaders(HTTP_NOT_FOUND, -1);
				return;
			}
		} finally {
			exchange.close();
		}
	}


	/**
	 * Handles the given HTTP exchange by copying the content of the given resource's content to the exchange's response. Note
	 * that the given exchange may be closed upon completion, but doesn't have to because it is closed by the caller anyways.
	 * Also note that the operation should always set a 2xx response code if it expects normal completion, or otherwise throw an
	 * exception.
	 * @param exchange the HTTP exchange
	 * @param resourcePath the (relative) resource path
	 * @throws NullPointerException if any of the given arguments is {@code null}
	 * @throws NoSuchFileException if the given file resource doesn't exist or cannot be read
	 * @throws IOException if there is an I/O related problem
	 */
	public abstract void handle (final HttpExchange exchange, final String resourcePath) throws NullPointerException, IOException;


	/**
	 * Reads all remaining bytes from the given byte source, and writes them to the given byte sink. Returns the number of bytes
	 * copied, and closes neither source nor sink. Note that {@link SocketException} is treated as a kind of EOF due to to other
	 * side terminating the stream.
	 * @param byteSource the byte source
	 * @param byteSink the byte sink
	 * @return the number of bytes copied
	 * @throws NullPointerException if any of the given arguments is {@code null}
	 * @throws IllegalArgumentException if the given buffer size is negative
	 * @throws IOException if there is an I/O related problem
	 */
	static private void copy (final InputStream byteSource, final OutputStream byteSink) throws IOException {
		try {
			final byte[] buffer = new byte[0x10000];
			for (int bytesRead = byteSource.read(buffer); bytesRead != -1; bytesRead = byteSource.read(buffer)) {
				byteSink.write(buffer, 0, bytesRead);
			}
		} catch (final EOFException | SocketException exception) {
			// treat as EOF because a TCP stream has been closed asynchronously
		}
	}


	/**
	 * Returns a new file handler that provides HTTP access to basic web resources contained within the current class-path,
	 * including the current project and JAR-files.
	 * @param contextPath the context path
	 * @throws NullPointerException if any of the given arguments is {@code null}
	 */
	static public HttpFileHandler newInstance (final String contextPath) {
		return new HttpFileModuleHandler(contextPath);
	}


	/**
	 * Returns a new file handler that provides HTTP access to basic web resources contained within a file system.
	 * @param contextPath the context path
	 * @param resourceDirectory the resource directory
	 * @throws NullPointerException if any of the given arguments is {@code null}
	 * @throws IllegalArgumentException if the given resource directory is not a directory
	 */
	static public HttpFileHandler newInstance (final String contextPath, final Path resourceDirectory) throws NullPointerException, IllegalArgumentException {
		return new HttpFileSystemHandler(contextPath, resourceDirectory);
	}



	/**
	 * HTTP file handlers that provide HTTP access to basic web resources contained within any JAR-file and project within the
	 * current class-path.
	 */
	static private class HttpFileModuleHandler extends HttpFileHandler {

		/**
		 * Creates a new instance.
		 * @param contextPath the context path
		 * @throws NullPointerException if any of the given arguments is {@code null}
		 */
		public HttpFileModuleHandler (final String contextPath) throws NullPointerException {
			super(contextPath);
		}


		/**
		 * {@inheritDoc}
		 * @throws NullPointerException {@inheritDoc}
		 * @throws NoSuchFileException {@inheritDoc}
		 * @throws IOException if there {@inheritDoc}
		 */
		public void handle (final HttpExchange exchange, final String resourcePath) throws NullPointerException, IllegalArgumentException, IOException {
			try (InputStream fileSource = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath)) {
				if (fileSource == null) throw new NoSuchFileException(resourcePath);
				exchange.sendResponseHeaders(HTTP_OK, 0);
				copy(fileSource, exchange.getResponseBody());
			}
		}


		/**
		 * {@inheritDoc}
		 */
		@Override
		public String toString () {
			return String.format("%s(contextPath=%s)", this.getClass().getName(), this.getContextPath());
		}
	}



	/**
	 * Instances of this class provide HTTP access to basic web resources contained within a file system.
	 */
	static private class HttpFileSystemHandler extends HttpFileHandler {
		private final Path resourceDirectory;


		/**
		 * Creates a new instance.
		 * @param contextPath the context path
		 * @param resourceDirectory the resource directory
		 * @throws NullPointerException if any of the given arguments is {@code null}
		 * @throws IllegalArgumentException if the given resource directory is not a directory
		 */
		public HttpFileSystemHandler (final String contextPath, final Path resourceDirectory) throws NullPointerException, IllegalArgumentException {
			super(contextPath);
			this.resourceDirectory = resourceDirectory.toAbsolutePath();
			if (!Files.isDirectory(this.resourceDirectory)) throw new IllegalArgumentException();
		}


		/**
		 * {@inheritDoc}
		 * @throws NullPointerException {@inheritDoc}
		 * @throws NoSuchFileException {@inheritDoc}
		 * @throws IOException if there {@inheritDoc}
		 */
		public void handle (final HttpExchange exchange, final String resourcePath) throws NullPointerException, IOException {
			final Path filePath = this.resourceDirectory.resolve(resourcePath);
			final long fileSize = Files.size(filePath);

			try (InputStream fileSource = Files.newInputStream(filePath)) {
				if (fileSource == null) throw new NoSuchFileException(filePath.toString());
				exchange.sendResponseHeaders(fileSize > 0 ? HTTP_OK : NO_CONTENT, fileSize);
				copy(fileSource, exchange.getResponseBody());
			}
		}


		/**
		 * {@inheritDoc}
		 */
		@Override
		public String toString () {
			return String.format("%s(contextPath=%s, resourceDirectory=%s)", this.getClass().getName(), this.getContextPath(), this.resourceDirectory);
		}
	}
}