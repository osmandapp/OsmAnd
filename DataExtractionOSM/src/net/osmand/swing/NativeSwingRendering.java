package net.osmand.swing;

import java.awt.image.BufferedImage;
import java.io.File;
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
//		System.load("/home/victor/projects/OsmAnd/git/Osmand-kernel/jni-prebuilt/linux-x86/osmand.lib");
	}
	
	RenderingRulesStorage storage;
	private final File baseDirRC;
	
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
	
	public NativeSwingRendering(File baseDirRC){
		this.baseDirRC = baseDirRC;
		try {
			storage = getDefault();
		} catch (SAXException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	
	
	public BufferedImage renderImage(int sleft, int sright, int stop, int sbottom, int zoom) throws IOException {
		long time = -System.currentTimeMillis();
		RenderingContext rctx = new RenderingContext(baseDirRC);
		
		RenderingRuleSearchRequest request = new RenderingRuleSearchRequest(storage);
		NativeSearchResult res = searchObjectsForRendering(sleft, sright, stop, sbottom, zoom, request, true, 
					rctx, "Nothing found");
		
		rctx.leftX = (float) (((double)sleft)/ MapUtils.getPowZoom(31-zoom));
		rctx.topY = (float) (((double)stop)/ MapUtils.getPowZoom(31-zoom));
		rctx.width = (int) ((sright - sleft) / MapUtils.getPowZoom(31 - zoom - 8));
		rctx.height = (int) ((sbottom - stop) / MapUtils.getPowZoom(31 - zoom - 8));
		rctx.shadowRenderingMode = 2;
		rctx.zoom = zoom;
		long search = time + System.currentTimeMillis();
		final RenderingGenerationResult rres = NativeSwingRendering.generateRenderingIndirect(rctx, res.nativeHandler,  
				false, request, true);
		long rendering = time + System.currentTimeMillis() - search;
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
		long last = time + System.currentTimeMillis() - rendering;
		System.out.println(" TIMES search - " + search + " rendering - " + rendering + " unpack - " + last);
		return img;
	}
	
	public void initFilesInDir(File filesDir){
		File[] lf = filesDir.listFiles();
		for(File f : lf){
			if(f.getName().endsWith(".obf")) {
				initMapFile(f.getAbsolutePath());
			}
		}
	}
	
	
	public static void main(String[] args) throws SAXException, IOException {
		System.load("/home/victor/projects/OsmAnd/git/Osmand-kernel/jni-prebuilt/linux-x86/osmand.lib");
		NativeSwingRendering lib = new NativeSwingRendering(
				new File("/home/victor/projects/OsmAnd/git/OsmAnd/res/drawable-mdpi/"));
		lib.initFilesInDir(new File("/home/victor/projects/OsmAnd/data/version2"));		
		double latTop = 22.5;
		double lonLeft = -80;
		int zoom = 11;
		
		float tileX = 2;
		float tileY = 2;
		double latBottom = MapUtils.getLatitudeFromTile(zoom, MapUtils.getTileNumberY(zoom, latTop) + tileY);
		double lonRight = MapUtils.getLongitudeFromTile(zoom, MapUtils.getTileNumberX(zoom, lonLeft) + tileX);
		int sleft = MapUtils.get31TileNumberX(lonLeft);
		int sright = MapUtils.get31TileNumberX(lonRight);
		int stop = MapUtils.get31TileNumberY(latTop);
		int sbottom = MapUtils.get31TileNumberY(latBottom);
		lib.renderImage(sleft, sright, stop, sbottom, zoom);
		
		MapPanel.showMainWindow(512, 512, lib);
	}
}
