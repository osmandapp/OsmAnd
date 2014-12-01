package net.osmand.plus.render;

import net.osmand.NativeLibrary;

/**
 * Created by Denis on 02.10.2014.
 */
public class NativeCppLibrary extends NativeLibrary {

	public NativeCppLibrary(boolean newLibrary) {
		super(newLibrary);
	}

	public static void loadLibrary(String name) {
		try {
			System.out.println("Loading " + name);
			System.loadLibrary(name);
		} catch( UnsatisfiedLinkError e ) {
			System.err.println("Failed to load '"+name + "':" + e);
			throw e;
		}
	}
}
