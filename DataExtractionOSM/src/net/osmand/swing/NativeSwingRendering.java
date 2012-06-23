package net.osmand.swing;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;

import org.xml.sax.SAXException;

import resources._R;

import net.osmand.NativeLibrary;
import net.osmand.RenderingContext;
import net.osmand.osm.MapUtils;
import net.osmand.render.RenderingRuleSearchRequest;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.render.RenderingRulesStorage.RenderingRulesStorageResolver;

public class NativeSwingRendering extends NativeLibrary {

	RenderingRulesStorage storage;
	private static NativeSwingRendering defaultLoadedLibrary; 
	
	public void loadRuleStorage(String path) throws SAXException, IOException{
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
		if(path == null || path.equals("default.render.xml")) {
			storage.parseRulesFromXmlInputStream(RenderingRulesStorage.class.getResourceAsStream("default.render.xml"), resolver);
		} else {
			storage.parseRulesFromXmlInputStream(new FileInputStream(path), resolver);
		}
		this.storage = storage;
	}
	
	public NativeSwingRendering(){
		try {
			loadRuleStorage(null);
		} catch (SAXException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	
	
	
	public BufferedImage renderImage(int sleft, int sright, int stop, int sbottom, int zoom) throws IOException {
		long time = -System.currentTimeMillis();
		RenderingContext rctx = new RenderingContext() {
			@Override
			protected byte[] getIconRawData(String data) {
				return _R.getIconData(data);
			}
		};
		rctx.nightMode = true;
		
		RenderingRuleSearchRequest request = new RenderingRuleSearchRequest(storage);
		NativeSearchResult res = searchObjectsForRendering(sleft, sright, stop, sbottom, zoom, request, true, 
					rctx, "Nothing found");
		
		rctx.leftX = (float) (((double)sleft)/ MapUtils.getPowZoom(31-zoom));
		rctx.topY = (float) (((double)stop)/ MapUtils.getPowZoom(31-zoom));
		rctx.width = (int) ((sright - sleft) / MapUtils.getPowZoom(31 - zoom - 8));
		rctx.height = (int) ((sbottom - stop) / MapUtils.getPowZoom(31 - zoom - 8));
		rctx.shadowRenderingMode = 2;
		rctx.shadowRenderingColor = 0xff969696;
		rctx.defaultColor = 0xfff1eee8;
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
	
	private static NativeSwingRendering loaded = null;
	public static NativeSwingRendering loadLibrary(String path){
		if(loaded == null) {
			System.load(path);
			loaded = new NativeSwingRendering();
		}
		return loaded;
	}
	
	public static NativeSwingRendering getDefaultFromSettings() {
		if(defaultLoadedLibrary != null) {
			return defaultLoadedLibrary;
		}
		String filename = DataExtractionSettings.getSettings().getNativeLibFile();
		if(filename.length() == 0 || !(new File(filename).exists())) {
			return null;
		}
		NativeSwingRendering lib = NativeSwingRendering.loadLibrary(filename);
		if(lib != null){
			lib.initFilesInDir(new File(DataExtractionSettings.getSettings().getBinaryFilesDir()));
			defaultLoadedLibrary = lib;
		}
		return lib;
	}
}
