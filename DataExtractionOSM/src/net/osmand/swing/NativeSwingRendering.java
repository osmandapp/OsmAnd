package net.osmand.swing;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;

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
	
	RenderingRulesStorage storage;
	
	private RenderingRulesStorage getDefault() throws SAXException, IOException{
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
	
	public NativeSwingRendering(){
		try {
			storage = getDefault();
		} catch (SAXException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	
	
	public BufferedImage renderImage(int sleft, int sright, int stop, int sbottom, int zoom) throws IOException {
		RenderingContext rctx = new RenderingContext();
		RenderingRuleSearchRequest request = new RenderingRuleSearchRequest(storage);
		NativeSearchResult res = searchObjectsForRendering(sleft, sright, stop, sbottom, zoom, request, true, 
					rctx, "Nothing found");
		
		rctx.leftX = (float) (((double)sleft)/ MapUtils.getPowZoom(31-zoom));
		rctx.topY = (float) (((double)stop)/ MapUtils.getPowZoom(31-zoom));
		rctx.width = (int) ((sright - sleft) / MapUtils.getPowZoom(31 - zoom - 8));
		rctx.height = (int) ((sbottom - stop) / MapUtils.getPowZoom(31 - zoom - 8));
		rctx.shadowRenderingMode = 2;
		rctx.zoom = zoom;
		
		final RenderingGenerationResult rres = NativeSwingRendering.generateRenderingIndirect(rctx, res.nativeHandler,  
				false, request, true);
		System.out.println(rres.bitmapBuffer.capacity());
		InputStream inputStream = new InputStream() {
			int nextInd = 0;
			@Override
			public int read() throws IOException {
				if(nextInd >= rres.bitmapBuffer.capacity()) {
					return -1;
				}
				byte b = rres.bitmapBuffer.get(nextInd++) ;
				if(b < 0) {
					return b + 256;
				} else {
					return b;
				}
			}
		};
		Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("png");
		ImageReader reader = readers.next();
		reader.setInput(new MemoryCacheImageInputStream(inputStream), true);
		BufferedImage img = reader.read(0);
		return img;
	}
	
	
	public static void main(String[] args) throws SAXException, IOException {
		NativeSwingRendering lib = new NativeSwingRendering();
		lib.initMapFile("/home/victor/projects/OsmAnd/data/osm-gen/Cuba2.obf");
		lib.initMapFile("/home/victor/projects/OsmAnd/data/osm-gen/basemap_2.obf");
		
		double latTop = 22.5;
		double lonLeft = -80;
		int zoom = 11;
		MapPanel.showMainWindow(512, 512, latTop, lonLeft, zoom, lib);
	}
}
