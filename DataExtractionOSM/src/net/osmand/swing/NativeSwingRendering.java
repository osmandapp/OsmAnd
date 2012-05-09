package net.osmand.swing;

import net.osmand.NativeLibrary;

public class NativeSwingRendering extends NativeLibrary {

	static {
		System.load("/home/victor/projects/OsmAnd/git/Osmand-kernel/jni-prebuilt/linux-x86/osmand.lib");
	}
	
	public static void main(String[] args) {
//		ByteBuffer buff = java.nio.ByteBuffer.wrap(new byte[10]);
//		new ByteBufferI
//		ImageIO.read(new ByteInputStream());
		System.out.println("Native!");
		NativeLibrary.initBinaryMapFile("/home/victor/projects/OsmAnd/data/osm-gen/Cuba2.obf");
		NativeLibrary.initBinaryMapFile("/home/victor/projects/OsmAnd/data/osm-gen/basemap_2.obf");
	}
}
