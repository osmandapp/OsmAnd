package net.osmand;

import gnu.trove.list.array.TIntArrayList;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;

import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteRegion;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteSubregion;
import net.osmand.binary.RouteDataObject;
import net.osmand.render.RenderingRuleSearchRequest;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.router.GeneralRouter;
import net.osmand.router.RouteCalculationProgress;
import net.osmand.router.RouteSegmentResult;
import net.osmand.router.RoutingConfiguration;

public class NativeLibrary {

	public static class RenderingGenerationResult {
		public RenderingGenerationResult(ByteBuffer bitmap) {
			bitmapBuffer = bitmap;
		}

		public final ByteBuffer bitmapBuffer;
	}

	public static class NativeSearchResult {

		public long nativeHandler;

		private NativeSearchResult(long nativeHandler) {
			this.nativeHandler = nativeHandler;
		}

		@Override
		protected void finalize() throws Throwable {
			deleteNativeResult();
			super.finalize();
		}

		public void deleteNativeResult() {
			if (nativeHandler != 0) {
				deleteSearchResult(nativeHandler);
				nativeHandler = 0;
			}
		}
	}

	public static class NativeRouteSearchResult {

		public long nativeHandler;
		public RouteDataObject[] objects;
		public RouteSubregion region;

		public NativeRouteSearchResult(long nativeHandler, RouteDataObject[] objects) {
			this.nativeHandler = nativeHandler;
			this.objects = objects;
		}

		@Override
		protected void finalize() throws Throwable {
			deleteNativeResult();
			super.finalize();
		}

		public void deleteNativeResult() {
			if (nativeHandler != 0) {
				deleteRouteSearchResult(nativeHandler);
				nativeHandler = 0;
			}
		}
	}

	/**
	 * @param searchResultHandle
	 *            - must be null if there is no need to append to previous results returns native handle to results
	 */
	public NativeSearchResult searchObjectsForRendering(int sleft, int sright, int stop, int sbottom, int zoom,
			RenderingRuleSearchRequest request, boolean skipDuplicates, Object objectWithInterruptedField, String msgIfNothingFound) {
		int renderRouteDataFile = 0;
		if (request.searchRenderingAttribute("showRoadMapsAttribute")) {
			renderRouteDataFile = request.getIntPropertyValue(request.ALL.R_ATTR_INT_VALUE);
		}
		return new NativeSearchResult(searchNativeObjectsForRendering(sleft, sright, stop, sbottom, zoom, request, skipDuplicates,
				renderRouteDataFile, objectWithInterruptedField, msgIfNothingFound));
	}

	public RouteDataObject[] getDataObjects(NativeRouteSearchResult rs, int x31, int y31) {
		if (rs.nativeHandler == 0) {
			// do not throw exception because it is expected situation
			return new RouteDataObject[0];
		}
		return getRouteDataObjects(rs.region.routeReg, rs.nativeHandler, x31, y31);
	}

	public boolean initMapFile(String filePath) {
		return initBinaryMapFile(filePath);
	}

	public boolean initCacheMapFile(String filePath) {
		return initCacheMapFiles(filePath);
	}

	public boolean closeMapFile(String filePath) {
		return closeBinaryMapFile(filePath);
	}

	public RouteSegmentResult[] runNativeRouting(int sx31, int sy31, int ex31, int ey31, RoutingConfiguration config,
			RouteRegion[] regions, RouteCalculationProgress progress) {
		TIntArrayList state = new TIntArrayList();
		List<String> keys = new ArrayList<String>();
		List<String> values = new ArrayList<String>();
		GeneralRouter r = (GeneralRouter) config.router;
		fillObjects(state, keys, values, 0, r.highwaySpeed);
		fillObjects(state, keys, values, 1, r.highwayPriorities);
		fillObjects(state, keys, values, 2, r.avoid);
		fillObjects(state, keys, values, 3, r.obstacles);
		fillObjects(state, keys, values, 4, r.routingObstacles);
		LinkedHashMap<String, String> attrs = new LinkedHashMap<String, String>(config.attributes);
		attrs.putAll(r.attributes);
		fillObjects(state, keys, values, 5, attrs);

		return nativeRouting(new int[] { sx31, sy31, ex31, ey31 }, state.toArray(), keys.toArray(new String[keys.size()]),
				values.toArray(new String[values.size()]), config.initialDirection == null ? -360 : config.initialDirection.floatValue(),
				regions, progress);
	}

	public <T> void fillObjects(TIntArrayList state, List<String> keys, List<String> values, int s, Map<String, T> map) {
		Iterator<Entry<String, T>> it = map.entrySet().iterator();
		while (it.hasNext()) {
			Entry<String, T> n = it.next();
			state.add(s);
			keys.add(n.getKey());
			values.add(n.getValue() + "");
		}
	}

	public NativeRouteSearchResult loadRouteRegion(RouteSubregion sub, boolean loadObjects) {
		NativeRouteSearchResult lr = loadRoutingData(sub.routeReg, sub.routeReg.getName(), sub.routeReg.getFilePointer(), sub, loadObjects);
		if (lr != null && lr.nativeHandler != 0) {
			lr.region = sub;
		}
		return lr;
	}

	/**/
	protected static native NativeRouteSearchResult loadRoutingData(RouteRegion reg, String regName, int regfp, RouteSubregion subreg,
			boolean loadObjects);

	protected static native void deleteRouteSearchResult(long searchResultHandle);

	protected static native RouteDataObject[] getRouteDataObjects(RouteRegion reg, long rs, int x31, int y31);

	protected static native RouteSegmentResult[] nativeRouting(int[] coordinates, int[] state, String[] keyConfig, String[] valueConfig,
			float initDirection, RouteRegion[] regions, RouteCalculationProgress progress);

	protected static native void deleteSearchResult(long searchResultHandle);

	protected static native boolean initBinaryMapFile(String filePath);

	protected static native boolean initCacheMapFiles(String filePath);

	protected static native boolean closeBinaryMapFile(String filePath);

	protected static native void initRenderingRulesStorage(RenderingRulesStorage storage);

	protected static native RenderingGenerationResult generateRenderingIndirect(RenderingContext rc, long searchResultHandler,
			boolean isTransparent, RenderingRuleSearchRequest render, boolean encodePng);

	protected static native long searchNativeObjectsForRendering(int sleft, int sright, int stop, int sbottom, int zoom,
			RenderingRuleSearchRequest request, boolean skipDuplicates, int renderRouteDataFile, Object objectWithInterruptedField,
			String msgIfNothingFound);

	public static native void testRoutingPing();

	public static native int testNativeRouting(String obfPath, double sLat, double sLon, double eLat, double eLon);

	/**/
	// Empty native impl
	/*
	 * protected static NativeRouteSearchResult loadRoutingData(RouteRegion reg, String regName, int regfp,RouteSubregion subreg, boolean
	 * loadObjects) { return null;}
	 * 
	 * protected static void deleteRouteSearchResult(long searchResultHandle) {}
	 * 
	 * protected static RouteDataObject[] getRouteDataObjects(RouteRegion reg, long rs, int x31, int y31){return null;}
	 * 
	 * protected static RouteSegmentResult[] nativeRouting(int[] coordinates, int[] state, String[] keyConfig, String[] valueConfig, float
	 * initDirection, RouteRegion[] regions, RouteCalculationProgress progress) {return null;}
	 * 
	 * protected static void deleteSearchResult(long searchResultHandle) {}
	 * 
	 * protected static boolean initBinaryMapFile(String filePath) {return false;}
	 * 
	 * protected static boolean initCacheMapFiles(String filePath) {return false;}
	 * 
	 * protected static boolean closeBinaryMapFile(String filePath) {return false;}
	 * 
	 * protected static void initRenderingRulesStorage(RenderingRulesStorage storage) {}
	 * 
	 * 
	 * protected static RenderingGenerationResult generateRenderingIndirect(RenderingContext rc, long searchResultHandler, boolean
	 * isTransparent, RenderingRuleSearchRequest render, boolean encodePng) { return null; }
	 * 
	 * protected static long searchNativeObjectsForRendering(int sleft, int sright, int stop, int sbottom, int zoom,
	 * RenderingRuleSearchRequest request, boolean skipDuplicates, int renderRouteDataFile, Object objectWithInterruptedField, String
	 * msgIfNothingFound) { return 0; }
	 * 
	 * public static void testRoutingPing() {}
	 * 
	 * public static int testNativeRouting(String obfPath, double sLat, double sLon, double eLat, double eLon) {return 0;} /*
	 */

	private static final Log log = PlatformUtil.getLog(NativeLibrary.class);
	public static boolean loadAllLibs(String path) {
		boolean b = true;
		b &= load("Qt5Core", path);
		b &= load("OsmAndCore", path);
		b &= load("OsmAndCoreUtils", path);
		b &= load("OsmAndJNI", path);
		return b;
	}

	public static boolean load(String libBaseName, String path) {
		// look for a pre-installed library
		if (path != null) {
			try {
				System.load(path + libBaseName);
				return true;
			} catch (UnsatisfiedLinkError e) {
				log.error(e);
			} // fall through
		}

		// guess what a bundled library would be called
		String osname = System.getProperty("os.name").toLowerCase();
		String osarch = System.getProperty("os.arch");
		if (osname.startsWith("mac os")) {
			osname = "mac";
			osarch = "universal";
		}
		if (osname.startsWith("windows"))
			osname = "win";
		if (osname.startsWith("sunos"))
			osname = "solaris";
		if (osarch.startsWith("i") && osarch.endsWith("86"))
			osarch = "x86";
		String libname = libBaseName + "-" + osname + '-' + osarch + ".lib";

		// try a bundled library
		try {
			ClassLoader cl = NativeLibrary.class.getClassLoader();
			InputStream in = cl.getResourceAsStream( libname);
			if (in == null) {
				log.error("libname: " + libname + " not found");
				return false;
			}
			File tmplib = File.createTempFile(libBaseName + "-", ".lib");
			tmplib.deleteOnExit();
			OutputStream out = new FileOutputStream(tmplib);
			byte[] buf = new byte[1024];
			for (int len; (len = in.read(buf)) != -1;)
				out.write(buf, 0, len);
			in.close();
			out.close();

			System.load(tmplib.getAbsolutePath());
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			log.error(e.getMessage(), e);
		} catch (UnsatisfiedLinkError e) {
			log.error(e.getMessage(), e);
		} // fall through
		return false;
	}
}
