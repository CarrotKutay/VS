package de.htw.tool;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;


/**
 * This facade provides some basic operations for maps.
 */
@Copyright(year = 2016, holders = "Sascha Baumeister")
public final class Maps {

	/**
	 * Prevents external instantiation.
	 */
	private Maps () {}



	/**
	 * Parses all properties stored within the given byte source, and returns them as a String map.
	 * @param byteSource the property source
	 * @return the String map created
	 * @throws NullPointerException if any of the given arguments is {@code null}
	 * @throws IOException if there is an I/O related problem parsing the source
	 */
	static public Map<String,String> readProperties (InputStream byteSource) throws NullPointerException, IOException {
		final Properties properties = new Properties();
		properties.load(byteSource);

		@SuppressWarnings({ "unchecked", "rawtypes" })
		final Map<String,String> map = (Map) properties;
		return map;
	}
}