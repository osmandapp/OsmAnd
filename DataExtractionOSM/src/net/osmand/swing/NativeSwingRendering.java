package net.osmand.swing;

import java.io.IOException;

import org.xml.sax.SAXException;

import net.osmand.NativeLibrary;
import net.osmand.RenderingContext;
import net.osmand.osm.MapUtils;
import net.osmand.render.RenderingRuleSearchRequest;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.render.RenderingRulesStorage.RenderingRulesStorageResolver;

public class NativeSwingRendering extends NativeLibrary {

	static {
		System.load("/home/victor/projects/OsmAnd/git/Osmand-kernel/jni-prebuilt/linux-x86/osmand.lib");
	}
	
	private static RenderingRulesStorage getDefault() throws SAXException, IOException{
		RenderingRulesStorage storage = new RenderingRulesStorage();
		final RenderingRulesStorageResolver resolver = new RenderingRulesStorageResolver() {
			@Override
			public RenderingRulesStorage resolve(String name, RenderingRulesStorageResolver ref) throws SAXException {
				RenderingRulesStorage depends = new RenderingRulesStorage();
				try {
					depends.parseRulesFromXmlInputStream(RenderingRulesStorage.class.getResourceAsStream(name+".render.xml"),
							ref);
				} catch (IOException e) {
					throw new SAXException(e);
				}
				return depends;
			}
		};
		storage.parseRulesFromXmlInputStream(RenderingRulesStorage.class.getResourceAsStream("default.render.xml"), resolver);
		return storage;
	}
	
	public static void main(String[] args) throws SAXException, IOException {
//		ByteBuffer buff = java.nio.ByteBuffer.wrap(new byte[10]);
//		new ByteBufferI
//		ImageIO.read(new ByteInputStream());
		System.out.println("Native!");
		NativeSwingRendering lib = new NativeSwingRendering();
		lib.initMapFile("/home/victor/projects/OsmAnd/data/osm-gen/Cuba2.obf");
		lib.initMapFile("/home/victor/projects/OsmAnd/data/osm-gen/basemap_2.obf");
		RenderingContext ctx = new RenderingContext();
		RenderingRulesStorage df = getDefault();
		lib.initRenderingRulesStorage(df);
		
		RenderingRuleSearchRequest request = new RenderingRuleSearchRequest(df);
		double latTop = 23.5;
		double lonLeft = -80;
		double latBottom = 23;
		double lonRight = -79;
		int sleft = MapUtils.get31TileNumberX(lonLeft);
		int sright = MapUtils.get31TileNumberX(lonRight);
		int stop = MapUtils.get31TileNumberY(latTop);
		int sbottom = MapUtils.get31TileNumberY(latBottom);
		
		NativeSearchResult res = lib.searchObjectsForRendering(sleft, sright, stop, sbottom, 11, request, true, 
					ctx, "Nothing found");
	}
}
