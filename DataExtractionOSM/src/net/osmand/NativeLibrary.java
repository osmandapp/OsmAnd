package net.osmand;

import java.nio.ByteBuffer;

import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteRegion;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteSubregion;
import net.osmand.binary.RouteDataObject;
import net.osmand.render.RenderingRuleSearchRequest;
import net.osmand.render.RenderingRulesStorage;

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
		return new NativeSearchResult(searchNativeObjectsForRendering(sleft, sright, stop, sbottom, zoom, request, skipDuplicates,
				objectWithInterruptedField, msgIfNothingFound));
	}
	
	public RouteDataObject[] getDataObjects(NativeRouteSearchResult rs, int x31, int y31) {
		if(rs.nativeHandler == 0) {
			throw new IllegalStateException("Native route handler is 0");
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
	
	
	
	public NativeRouteSearchResult loadRouteRegion(RouteSubregion sub, boolean loadObjects) {
		NativeRouteSearchResult lr = loadRoutingData(sub.routeReg, sub.routeReg.getName(), sub.routeReg.getFilePointer(),
				sub, loadObjects);
		if(lr != null && lr.nativeHandler != 0){
			lr.region = sub;
		}
		return lr;
	}

	
	protected static native NativeRouteSearchResult loadRoutingData(RouteRegion reg, String regName, int regfp,RouteSubregion subreg,
			boolean loadObjects); 
	
	protected static native void deleteRouteSearchResult(long searchResultHandle);
	
	protected static native RouteDataObject[] getRouteDataObjects(RouteRegion reg, long rs, int x31, int y31);
	
	protected static native void deleteSearchResult(long searchResultHandle);

	protected static native boolean initBinaryMapFile(String filePath);
	
	protected static native boolean initCacheMapFiles(String filePath);
	
	protected static native boolean closeBinaryMapFile(String filePath);
	
	protected static native void initRenderingRulesStorage(RenderingRulesStorage storage);


	protected static native RenderingGenerationResult generateRenderingIndirect(RenderingContext rc, long searchResultHandler,
			boolean isTransparent, RenderingRuleSearchRequest render, boolean encodePng);
	
	protected static native long searchNativeObjectsForRendering(int sleft, int sright, int stop, int sbottom, int zoom, 
			RenderingRuleSearchRequest request, boolean skipDuplicates, Object objectWithInterruptedField, String msgIfNothingFound);

}
