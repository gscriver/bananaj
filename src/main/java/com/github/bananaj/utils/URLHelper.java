package com.github.bananaj.utils;

import java.net.MalformedURLException;
import java.net.URL;

public class URLHelper {

	/**
	 * Helper method to construct {@code URL} by concatenating a list of strings.
	 * @param parts
	 * @return
	 * @throws MalformedURLException
	 */
	public static URL url(String... parts) throws MalformedURLException {
	    StringBuilder sb = new StringBuilder();
	    for (String arg : parts) {
	        sb.append(arg);
	    }
	    return new URL(join(parts));
	}
	
	/**
	 * Helper method to efficiently concatenate multiple strings
	 * @param parts
	 * @return
	 */
	public static String join(String... parts) {
	    StringBuilder sb = new StringBuilder(100);
	    for (String arg : parts) {
	        sb.append(arg);
	    }
	    return sb.toString();
	}
}
