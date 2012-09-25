package net.osmand.plus.render;


import gnu.trove.list.TLongList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.osmand.Algoritms;
import net.osmand.IProgress;
import net.osmand.LogUtil;
import net.osmand.NativeLibrary.NativeSearchResult;
import net.osmand.access.AccessibleToast;
import net.osmand.binary.BinaryMapDataObject;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapIndexReader.MapIndex;
import net.osmand.binary.BinaryMapIndexReader.SearchRequest;
import net.osmand.binary.BinaryMapIndexReader.TagValuePair;
import net.osmand.data.MapAlgorithms;
import net.osmand.data.MapTileDownloader.IMapDownloaderCallback;
import net.osmand.osm.MapUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.OsmandSettings.CommonPreference;
import net.osmand.plus.R;
import net.osmand.plus.RotatedTileBox;
import net.osmand.plus.render.OsmandRenderer.RenderingContext;
import net.osmand.plus.views.OsmandMapLayer.DrawSettings;
import net.osmand.render.RenderingRuleProperty;
import net.osmand.render.RenderingRuleSearchRequest;
import net.osmand.render.RenderingRuleStorageProperties;
import net.osmand.render.RenderingRulesStorage;

import org.apache.commons.logging.Log;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

public class MapRenderRepositories {

	private final static Log log = LogUtil.getLog(MapRenderRepositories.class);
	private final OsmandApplication context;
	private final static int BASEMAP_ZOOM = 11;
	private Handler handler;
	private Map<String, BinaryMapIndexReader> files = new LinkedHashMap<String, BinaryMapIndexReader>();
	private Set<String> nativeFiles = new HashSet<String>();
	private OsmandRenderer renderer;


	// lat/lon box of requested vector data
	private RectF cObjectsBox = new RectF();
	// cached objects in order to render rotation without reloading data from db
	private List<BinaryMapDataObject> cObjects = new LinkedList<BinaryMapDataObject>();
	private NativeSearchResult cNativeObjects = null;

	// currently rendered box (not the same as already rendered)
	// this box is checked for interrupted process or
	private RotatedTileBox requestedBox = null;

	// location of rendered bitmap
	private RotatedTileBox prevBmpLocation = null;
	// already rendered bitmap
	private Bitmap prevBmp;

	// location of rendered bitmap
	private RotatedTileBox bmpLocation = null;
	// already rendered bitmap
	private Bitmap bmp;
	// Field used in C++
	private boolean interrupted = false;
	private RenderingContext currentRenderingContext;
	private SearchRequest<BinaryMapDataObject> searchRequest;
	private OsmandSettings prefs;

	public MapRenderRepositories(OsmandApplication context) {
		this.context = context;
		this.renderer = new OsmandRenderer(context);
		handler = new Handler(Looper.getMainLooper());
		prefs = context.getSettings();
	}

	public Context getContext() {
		return context;
	}

	public void initializeNewResource(final IProgress progress, File file, BinaryMapIndexReader reader) {
		if (files.containsKey(file.getAbsolutePath())) {
			closeConnection(files.get(file.getAbsolutePath()), file.getAbsolutePath());
		
		}
		files.put(file.getAbsolutePath(), reader);
		NativeOsmandLibrary nativeLib = prefs.NATIVE_RENDERING.get() ? NativeOsmandLibrary.getLoadedLibrary() : null;
		if (nativeLib != null) {
			if (!nativeLib.initMapFile(file.getAbsolutePath())) {
				log.error("Initializing native db " + file.getAbsolutePath() + " failed!"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			} else {
				nativeFiles.add(file.getAbsolutePath());
			}
		}
	}

	public RotatedTileBox getBitmapLocation() {
		return bmpLocation;
	}

	public RotatedTileBox getPrevBmpLocation() {
		return prevBmpLocation;
	}

	protected void closeConnection(BinaryMapIndexReader c, String file) {
		files.remove(file);
		if(nativeFiles.contains(file)){
			NativeOsmandLibrary lib = NativeOsmandLibrary.getLoadedLibrary();
			if(lib != null) {
				lib.closeMapFile(file);
				nativeFiles.remove(file);
			}
		}
		
		try {
			c.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public boolean containsLatLonMapData(double lat, double lon, int zoom) {
		int x = MapUtils.get31TileNumberX(lon);
		int y = MapUtils.get31TileNumberY(lat);
		for (BinaryMapIndexReader reader : files.values()) {
			if (reader.containsMapData(x, y, zoom)) {
				return true;
			}
		}
		return false;
	}

	public void clearAllResources() {
		clearCache();
		bmp = null;
		bmpLocation = null;
		for (String f : new ArrayList<String>(files.keySet())) {
			closeConnection(files.get(f), f);
		}
	}

	public boolean updateMapIsNeeded(RotatedTileBox box, DrawSettings drawSettings) {
		if (box == null) {
			return false;
		}
		if (requestedBox == null) {
			return true;
		}
		if (drawSettings.isForce()) {
			return true;
		}
		if (requestedBox.getZoom() != box.getZoom()) {
			return true;
		}

		float deltaRotate = requestedBox.getRotate() - box.getRotate();
		if (deltaRotate > 180) {
			deltaRotate -= 360;
		} else if (deltaRotate < -180) {
			deltaRotate += 360;
		}
		if (Math.abs(deltaRotate) > 25) {
			return true;
		}
		return !requestedBox.containsTileBox(box);
	}

	public boolean isEmpty() {
		return files.isEmpty();
	}

	public void interruptLoadingMap() {
		interrupted = true;
		if (currentRenderingContext != null) {
			currentRenderingContext.interrupted = true;
		}
		if (searchRequest != null) {
			searchRequest.setInterrupted(true);
		}
	}

	private boolean checkWhetherInterrupted() {
		if (interrupted || (currentRenderingContext != null && currentRenderingContext.interrupted)) {
			requestedBox = bmpLocation;
			return true;
		}
		return false;
	}

	public boolean basemapExists() {
		for (BinaryMapIndexReader f : files.values()) {
			if (f.isBasemap()) {
				return true;
			}
		}
		return false;
	}
	
	
	private boolean loadVectorDataNative(RectF dataBox, final int zoom, final RenderingRuleSearchRequest renderingReq, 
			NativeOsmandLibrary library) {
		int leftX = MapUtils.get31TileNumberX(dataBox.left);
		int rightX = MapUtils.get31TileNumberX(dataBox.right);
		int bottomY = MapUtils.get31TileNumberY(dataBox.bottom);
		int topY = MapUtils.get31TileNumberY(dataBox.top);
		long now = System.currentTimeMillis();

		// check that everything is initialized
		for (String mapName : files.keySet()) {
			if (!nativeFiles.contains(mapName)) {
				nativeFiles.add(mapName);
				if (!library.initMapFile(mapName)) {
					continue;
				}
				log.debug("Native resource " + mapName + " initialized"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		
		NativeSearchResult resultHandler = library.searchObjectsForRendering(leftX, rightX, topY, bottomY, zoom, renderingReq,
				PerformanceFlags.checkForDuplicateObjectIds, this, context.getString(R.string.switch_to_raster_map_to_see));
		if (checkWhetherInterrupted()) {
			resultHandler.deleteNativeResult();
			return false;
		}
		if(cNativeObjects != null) {
			cNativeObjects.deleteNativeResult();
		}
		cNativeObjects = resultHandler;
		cObjectsBox = dataBox;
		log.info(String.format("BLat=%s, TLat=%s, LLong=%s, RLong=%s, zoom=%s", //$NON-NLS-1$
				dataBox.bottom, dataBox.top, dataBox.left, dataBox.right, zoom));
		log.info(String.format("Native search: %s ms ", System.currentTimeMillis() - now)); //$NON-NLS-1$
		return true;
	}

	private boolean loadVectorData(RectF dataBox, final int zoom, final RenderingRuleSearchRequest renderingReq) {
		double cBottomLatitude = dataBox.bottom;
		double cTopLatitude = dataBox.top;
		double cLeftLongitude = dataBox.left;
		double cRightLongitude = dataBox.right;

		long now = System.currentTimeMillis();

		try {
			System.gc(); // to clear previous objects
			int count = 0;
			ArrayList<BinaryMapDataObject> tempResult = new ArrayList<BinaryMapDataObject>();
			ArrayList<BinaryMapDataObject> basemapResult = new ArrayList<BinaryMapDataObject>();
			TLongSet ids = new TLongHashSet();
			List<BinaryMapDataObject> coastLines = new ArrayList<BinaryMapDataObject>();
			List<BinaryMapDataObject> basemapCoastLines = new ArrayList<BinaryMapDataObject>();
			int leftX = MapUtils.get31TileNumberX(cLeftLongitude);
			int rightX = MapUtils.get31TileNumberX(cRightLongitude);
			int bottomY = MapUtils.get31TileNumberY(cBottomLatitude);
			int topY = MapUtils.get31TileNumberY(cTopLatitude);
			BinaryMapIndexReader.SearchFilter searchFilter = new BinaryMapIndexReader.SearchFilter() {

				@Override
				public boolean accept(TIntArrayList types, BinaryMapIndexReader.MapIndex root) {
					for (int j = 0; j < types.size(); j++) {
						int type = types.get(j);
						TagValuePair pair = root.decodeType(type);
						if (pair != null) {
							// TODO is it fast enough ?
							for (int i = 1; i <= 3; i++) {
								renderingReq.setIntFilter(renderingReq.ALL.R_MINZOOM, zoom);
								renderingReq.setStringFilter(renderingReq.ALL.R_TAG, pair.tag);
								renderingReq.setStringFilter(renderingReq.ALL.R_VALUE, pair.value);
								if (renderingReq.search(i, false)) {
									return true;
								}
							}
							renderingReq.setStringFilter(renderingReq.ALL.R_TAG, pair.tag);
							renderingReq.setStringFilter(renderingReq.ALL.R_VALUE, pair.value);
							if (renderingReq.search(RenderingRulesStorage.TEXT_RULES, false)) {
								return true;
							}
						}
					}
					return false;
				}

			};
			if (zoom > 16) {
				searchFilter = null;
			}
			boolean ocean = false;
			boolean land = false;
			MapIndex mi = null;
			searchRequest = BinaryMapIndexReader.buildSearchRequest(leftX, rightX, topY, bottomY, zoom, searchFilter);
			for (BinaryMapIndexReader c : files.values()) {
				searchRequest.clearSearchResults();
				List<BinaryMapDataObject> res = c.searchMapIndex(searchRequest);
				for (BinaryMapDataObject r : res) {
					if (PerformanceFlags.checkForDuplicateObjectIds) {
						if (ids.contains(r.getId()) && r.getId() > 0) {
							// do not add object twice
							continue;
						}
						ids.add(r.getId());
					}
					count++;

					if (r.containsType(r.getMapIndex().coastlineEncodingType)) {
						if (c.isBasemap()) {
							basemapCoastLines.add(r);
						} else {
							coastLines.add(r);
						}
					} else {
						// do not mess coastline and other types
						if (c.isBasemap()) {
							basemapResult.add(r);
						} else {
							tempResult.add(r);
						}
					}
					if (checkWhetherInterrupted()) {
						return false;
					}
				}

				if (searchRequest.isOcean()) {
					mi = c.getMapIndexes().get(0);
					ocean = true;
				}  
				if (searchRequest.isLand()) {
					mi = c.getMapIndexes().get(0);
					land = true;
				}
			}

			String coastlineTime = "";
			boolean addBasemapCoastlines = true;
			boolean emptyData = zoom > BASEMAP_ZOOM && tempResult.isEmpty() && coastLines.isEmpty();
			boolean basemapMissing = zoom <= BASEMAP_ZOOM && basemapCoastLines.isEmpty() && mi == null; 
			boolean detailedLandData = zoom >= 14 && tempResult.size() > 0;
			if(!coastLines.isEmpty()) {
				long ms = System.currentTimeMillis();
				boolean coastlinesWereAdded = processCoastlines(coastLines, leftX, rightX, bottomY, topY, zoom, 
						basemapCoastLines.isEmpty(), true, tempResult);
				addBasemapCoastlines = (!coastlinesWereAdded && !detailedLandData) || zoom <= BASEMAP_ZOOM;
				coastlineTime = "(coastline " + (System.currentTimeMillis() -  ms) + " ms )";
			} else {
				addBasemapCoastlines = !detailedLandData;
			}
			if(addBasemapCoastlines){
				long ms = System.currentTimeMillis();
				boolean coastlinesWereAdded = processCoastlines(basemapCoastLines, leftX, rightX, bottomY, topY, zoom, 
						true, true, tempResult);
				addBasemapCoastlines = !coastlinesWereAdded;
				coastlineTime = "(coastline " + (System.currentTimeMillis() -  ms) + " ms )";
			}
			if(addBasemapCoastlines && mi != null){
				BinaryMapDataObject o = new BinaryMapDataObject(new int[] { leftX, topY, rightX, topY, rightX, bottomY, leftX, bottomY, leftX,
						topY }, new int[] { ocean && !land ? mi.coastlineEncodingType : (mi.landEncodingType) }, null, -1);
				o.setMapIndex(mi);
				tempResult.add(o);
			}
			if(emptyData || basemapMissing){
				// message
				MapIndex mapIndex;
				if(!tempResult.isEmpty()) {
					mapIndex = tempResult.get(0).getMapIndex();
				} else {
					mapIndex = new MapIndex();
					mapIndex.initMapEncodingRule(0, 1, "natural", "coastline");
					mapIndex.initMapEncodingRule(0, 2, "name", "");
				}
				// avoid overflow int errors
				BinaryMapDataObject o = new BinaryMapDataObject(new int[] { leftX + (rightX - leftX) / 2, topY + (bottomY - topY) / 2 },
						new int[] { mapIndex.coastlineEncodingType }, null, -1);
				o.setMapIndex(mapIndex);
				o.putObjectName(mapIndex.nameEncodingType, context.getString(R.string.switch_to_raster_map_to_see));
				tempResult.add(o);
			}
			if(zoom <= BASEMAP_ZOOM || emptyData) {
				tempResult.addAll(basemapResult);
			}
			
			
			if (count > 0) {
				log.info(String.format("BLat=%s, TLat=%s, LLong=%s, RLong=%s, zoom=%s", //$NON-NLS-1$
						cBottomLatitude, cTopLatitude, cLeftLongitude, cRightLongitude, zoom));
				log.info(String.format("Searching: %s ms  %s (%s results found)", System.currentTimeMillis() - now, coastlineTime, count)); //$NON-NLS-1$
			}
		

			cObjects = tempResult;
			cObjectsBox = dataBox;
		} catch (IOException e) {
			log.debug("Search failed", e); //$NON-NLS-1$
			return false;
		}

		return true;
	}

	private void validateLatLonBox(RectF box) {
		if (box.top > 90) {
			box.top = 85.5f;
		}
		if (box.bottom < -90) {
			box.bottom = -85.5f;
		}
		if (box.left <= -180) {
			box.left = -179.5f;
		}
		if (box.right > 180) {
			box.right = 180.0f;
		}
	}

	public synchronized void loadMap(RotatedTileBox tileRect, List<IMapDownloaderCallback> notifyList) {
		interrupted = false;
		if (currentRenderingContext != null) {
			currentRenderingContext = null;
		}
		try {
			// find selected rendering type
			OsmandApplication app = ((OsmandApplication) context.getApplicationContext());
			Boolean renderDay = app.getDaynightHelper().getDayNightRenderer();
			boolean nightMode = renderDay != null && !renderDay.booleanValue();
			
			// boolean moreDetail = prefs.SHOW_MORE_MAP_DETAIL.get();
			RenderingRulesStorage storage = app.getRendererRegistry().getCurrentSelectedRenderer();
			RenderingRuleSearchRequest renderingReq = new RenderingRuleSearchRequest(storage);
			renderingReq.setBooleanFilter(renderingReq.ALL.R_NIGHT_MODE, nightMode);
			for (RenderingRuleProperty customProp : storage.PROPS.getCustomRules()) {
				if (customProp.isBoolean()) {
					CommonPreference<Boolean> pref = prefs.getCustomRenderBooleanProperty(customProp.getAttrName());
					renderingReq.setBooleanFilter(customProp, pref.get());
				} else {
					CommonPreference<String> settings = prefs.getCustomRenderProperty(customProp.getAttrName());
					String res = settings.get();
					if (!Algoritms.isEmpty(res)) {
						if (customProp.isString()) {
							renderingReq.setStringFilter(customProp, res);
						} else if (customProp.isBoolean()) {
							renderingReq.setBooleanFilter(customProp, "true".equalsIgnoreCase(res));
						} else {
							try {
								renderingReq.setIntFilter(customProp, Integer.parseInt(res));
							} catch (NumberFormatException e) {
								e.printStackTrace();
							}
						}
					}
				}
			}
			renderingReq.saveState();
			NativeOsmandLibrary nativeLib = prefs.NATIVE_RENDERING.get() ? NativeOsmandLibrary.getLibrary(storage) : null;

			// prevent editing
			requestedBox = new RotatedTileBox(tileRect);

			// calculate data box
			RectF dataBox = requestedBox.calculateLatLonBox(new RectF());
			long now = System.currentTimeMillis();

			if (cObjectsBox.left > dataBox.left || cObjectsBox.top > dataBox.top || cObjectsBox.right < dataBox.right
					|| cObjectsBox.bottom < dataBox.bottom || (nativeLib != null) == (cNativeObjects == null)) {
				// increase data box in order for rotate
				if ((dataBox.right - dataBox.left) > (dataBox.top - dataBox.bottom)) {
					double wi = (dataBox.right - dataBox.left) * .2;
					dataBox.left -= wi;
					dataBox.right += wi;
				} else {
					double hi = (dataBox.top - dataBox.bottom) * .2;
					dataBox.top += hi;
					dataBox.bottom -= hi;
				}
				validateLatLonBox(dataBox);
				boolean loaded;
				if(nativeLib != null) {
					cObjects = new LinkedList<BinaryMapDataObject>();
					loaded = loadVectorDataNative(dataBox, requestedBox.getIntZoom(), renderingReq, nativeLib);
				} else {
					cNativeObjects = null;
					loaded = loadVectorData(dataBox, requestedBox.getIntZoom(), renderingReq);
					
				}
				if (!loaded || checkWhetherInterrupted()) {
					return;
				}
			}
			final long searchTime = System.currentTimeMillis() - now;

			currentRenderingContext = new OsmandRenderer.RenderingContext(context);
			renderingReq.clearState();
			renderingReq.setIntFilter(renderingReq.ALL.R_MINZOOM, requestedBox.getIntZoom());
			if(renderingReq.searchRenderingAttribute(RenderingRuleStorageProperties.A_DEFAULT_COLOR)) {
				currentRenderingContext.defaultColor = renderingReq.getIntPropertyValue(renderingReq.ALL.R_ATTR_COLOR_VALUE);
			}
			renderingReq.clearState();
			renderingReq.setIntFilter(renderingReq.ALL.R_MINZOOM, requestedBox.getIntZoom());
			if(renderingReq.searchRenderingAttribute(RenderingRuleStorageProperties.A_SHADOW_RENDERING)) {
				currentRenderingContext.shadowRenderingMode = renderingReq.getIntPropertyValue(renderingReq.ALL.R_ATTR_INT_VALUE);
				currentRenderingContext.shadowRenderingColor = renderingReq.getIntPropertyValue(renderingReq.ALL.R_SHADOW_COLOR);
			}
			currentRenderingContext.leftX = requestedBox.getLeftTileX();
			currentRenderingContext.topY = requestedBox.getTopTileY() ;
			currentRenderingContext.zoom = requestedBox.getIntZoom();
			currentRenderingContext.rotate = requestedBox.getRotate();
			currentRenderingContext.width = (int) (requestedBox.getTileWidth() * OsmandRenderer.TILE_SIZE);
			currentRenderingContext.height = (int) (requestedBox.getTileHeight() * OsmandRenderer.TILE_SIZE);
			currentRenderingContext.nightMode = nightMode;
			currentRenderingContext.useEnglishNames = prefs.USE_ENGLISH_NAMES.get();
			currentRenderingContext.setDensityValue(prefs.USE_HIGH_RES_MAPS.get(), 
					prefs.MAP_TEXT_SIZE.get(), renderer.getDensity());
			// init rendering context
			currentRenderingContext.tileDivisor = (float) MapUtils.getPowZoom(31 - requestedBox.getZoom());
			if (checkWhetherInterrupted()) {
				return;
			}

			now = System.currentTimeMillis();
			Bitmap bmp;
			boolean transparent = false;
			RenderingRuleProperty rr = storage.PROPS.get("noPolygons");
			if (rr != null) {
				transparent = renderingReq.getIntPropertyValue(rr) > 0;
			}

			// 1. generate image step by step
			Bitmap reuse = prevBmp;
			this.prevBmp = this.bmp;
			this.prevBmpLocation = this.bmpLocation;
			if (reuse != null && reuse.getWidth() == currentRenderingContext.width && reuse.getHeight() == currentRenderingContext.height) {
				bmp = reuse;
				bmp.eraseColor(currentRenderingContext.defaultColor);
			} else {
				if(reuse != null){
					log.error(String.format("Create new image ? %d != %d (w) %d != %d (h) ", currentRenderingContext.width, reuse.getWidth(), currentRenderingContext.height, reuse.getHeight()));
				}
				bmp = Bitmap.createBitmap(currentRenderingContext.width, currentRenderingContext.height, Config.RGB_565);
			}
			this.bmp = bmp;
			this.bmpLocation = tileRect;
			
			
			
			if(nativeLib != null) {
				renderer.generateNewBitmapNative(currentRenderingContext, nativeLib, cNativeObjects, bmp, renderingReq, notifyList);
			} else {
				renderer.generateNewBitmap(currentRenderingContext, cObjects, bmp, renderingReq, notifyList);
			}
			// Force to use rendering request in order to prevent Garbage Collector when it is used in C++
			if(renderingReq != null){
				log.info("Debug :" + renderingReq != null);				
			}
			String renderingDebugInfo = currentRenderingContext.renderingDebugInfo;
			currentRenderingContext.ended = true;
			if (checkWhetherInterrupted()) {
				// revert if it was interrupted 
				// (be smart a bit do not revert if road already drawn) 
				if(currentRenderingContext.lastRenderedKey < 35) {
					reuse = this.bmp;
					this.bmp = this.prevBmp;
					this.bmpLocation = this.prevBmpLocation;
					this.prevBmp = reuse;
					this.prevBmpLocation = null;
				}
				currentRenderingContext = null;
				return;
			}
			currentRenderingContext = null;

			// 2. replace whole image
			// keep cache
			// this.prevBmp = null;
			this.prevBmpLocation = null;
			if (prefs.DEBUG_RENDERING_INFO.get()) {
				String timeInfo = "Searching: " + searchTime + " ms"; //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
				if (renderingDebugInfo != null) {
					timeInfo += "\n" + renderingDebugInfo;
				}
				final String msg = timeInfo;
				log.info(msg);
				handler.post(new Runnable() {
					@Override
					public void run() {
						AccessibleToast.makeText(context, msg, Toast.LENGTH_LONG).show();
					}
				});
			}
		} catch (RuntimeException e) {
			log.error("Runtime memory exception", e); //$NON-NLS-1$
			handler.post(new Runnable() {
				@Override
				public void run() {
					AccessibleToast.makeText(context, R.string.rendering_exception, Toast.LENGTH_SHORT).show();
				}
			});
		} catch (OutOfMemoryError e) {
			log.error("Out of memory error", e); //$NON-NLS-1$
			cObjects = new ArrayList<BinaryMapDataObject>();
			cObjectsBox = new RectF();
			handler.post(new Runnable() {
				@Override
				public void run() {
//					ActivityManager activityManager = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
//					ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
//					activityManager.getMemoryInfo(memoryInfo);
//					int avl = (int) (memoryInfo.availMem / (1 << 20));
					int max = (int) (Runtime.getRuntime().maxMemory() / (1 << 20)); 
					int avl = (int) (Runtime.getRuntime().freeMemory() / (1 << 20));
					String s = " (" + avl + " MB available of " + max  + ") ";
					AccessibleToast.makeText(context, context.getString(R.string.rendering_out_of_memory) + s , Toast.LENGTH_SHORT).show();
				}
			});
		} finally {
			if(currentRenderingContext != null) {
				currentRenderingContext.ended = true;
			}
		}

	}

	public Bitmap getBitmap() {
		return bmp;
	}

	public Bitmap getPrevBitmap() {
		return prevBmpLocation == null ? null : prevBmp ;
	}

	public synchronized void clearCache() {
		cObjects = new ArrayList<BinaryMapDataObject>();
		cObjectsBox = new RectF();

		requestedBox = prevBmpLocation = null;
		// Do not clear main bitmap to not cause a screen refresh
//		prevBmp = null;
//		bmp = null;
//		bmpLocation = null;
	}
	
	public Map<String, BinaryMapIndexReader> getMetaInfoFiles() {
		return files;
	}

	/// MULTI POLYGONS (coastline)
	// returns true if coastlines were added!
	private boolean processCoastlines(List<BinaryMapDataObject> coastLines, int leftX, int rightX, 
			int bottomY, int topY, int zoom, boolean doNotAddIfIncompleted, boolean addDebugIncompleted, List<BinaryMapDataObject> result) {
		List<TLongList> completedRings = new ArrayList<TLongList>();
		List<TLongList> uncompletedRings = new ArrayList<TLongList>();
		MapIndex mapIndex = null;
		long dbId = 0;
		for (BinaryMapDataObject o : coastLines) {
			int len = o.getPointsLength();
			if (len < 2) {
				continue;
			}
			mapIndex = o.getMapIndex();
			dbId = o.getId() >> 1;
			TLongList coordinates = new TLongArrayList(o.getPointsLength() / 2);
			int px = o.getPoint31XTile(0);
			int py = o.getPoint31YTile(0);
			int x = px;
			int y = py;
			boolean pinside = leftX <= x && x <= rightX && y >= topY && y <= bottomY;
			if (pinside) {
				coordinates.add(combine2Points(x, y));
			}
			for (int i = 1; i < len; i++) {
				x = o.getPoint31XTile(i);
				y = o.getPoint31YTile(i);
				boolean inside = leftX <= x && x <= rightX && y >= topY && y <= bottomY;
				boolean lineEnded = calculateLineCoordinates(inside, x, y, pinside, px, py, leftX, rightX, bottomY, topY, coordinates);
				if (lineEnded) {
					combineMultipolygonLine(completedRings, uncompletedRings, coordinates);
					// create new line if it goes outside
					coordinates = new TLongArrayList();
				}
				px = x;
				py = y;
				pinside = inside;
			}
			combineMultipolygonLine(completedRings, uncompletedRings, coordinates);
		}
		if (completedRings.size() == 0 && uncompletedRings.size() == 0) {
			return false;
		}
		if (uncompletedRings.size() > 0) {
			unifyIncompletedRings(uncompletedRings, completedRings, leftX, rightX, bottomY, topY, dbId, zoom);
		}
		long mask = 0xffffffffl;
		// draw uncompleted for debug purpose
		for (int i = 0; i < uncompletedRings.size(); i++) {
			TLongList ring = uncompletedRings.get(i);
			int[] coordinates = new int[ring.size() * 2];
			for (int j = 0; j < ring.size(); j++) {
				coordinates[j * 2] = (int) (ring.get(j) >> 32);
				coordinates[j * 2 + 1] = (int) (ring.get(j) & mask);
			}
			BinaryMapDataObject o = new BinaryMapDataObject(coordinates, new int[] { mapIndex.coastlineBrokenEncodingType }, null, dbId);
			o.setMapIndex(mapIndex);
			result.add(o);
		}
		if(!doNotAddIfIncompleted && uncompletedRings.size() > 0){
			return false;
		}
		boolean clockwiseFound = false;
		
		for (int i = 0; i < completedRings.size(); i++) {
			TLongList ring = completedRings.get(i);
			int[] coordinates = new int[ring.size() * 2];
			for (int j = 0; j < ring.size(); j++) {
				coordinates[j * 2] = (int) (ring.get(j) >> 32);
				coordinates[j * 2 + 1] = (int) (ring.get(j) & mask);
			}
			boolean clockwise = MapAlgorithms.isClockwiseWay(ring);
			clockwiseFound = clockwiseFound || clockwise;
			BinaryMapDataObject o = new BinaryMapDataObject(coordinates, new int[] { clockwise ? mapIndex.coastlineEncodingType
					: mapIndex.landEncodingType }, null, dbId);
			o.setMapIndex(mapIndex);
			o.setArea(true);
			result.add(o);
		}
		
		if (!clockwiseFound && uncompletedRings.size() == 0) {
			// add complete water tile
			BinaryMapDataObject o = new BinaryMapDataObject(new int[] { leftX, topY, rightX, topY, rightX, bottomY, leftX, bottomY, leftX,
					topY }, new int[] { mapIndex.coastlineEncodingType }, null, dbId);
			o.setMapIndex(mapIndex);
			log.info("!!! Isolated islands !!!"); //$NON-NLS-1$
			result.add(o);

		}
		return true;
	}
	
	private boolean eq(long i1, long i2){
		return i1 == i2;
	}
	
	private void combineMultipolygonLine(List<TLongList> completedRings, List<TLongList> incompletedRings,	TLongList coordinates) {
		if (coordinates.size() > 0) {
			if (eq(coordinates.get(0), coordinates.get(coordinates.size() - 1))) {
				completedRings.add(coordinates);
			} else {
				boolean add = true;
				for (int k = 0; k < incompletedRings.size();) {
					boolean remove = false;
					TLongList i = incompletedRings.get(k);
					if (eq(coordinates.get(0), i.get(i.size() - 1))) {
						i.addAll(coordinates.subList(1, coordinates.size()));
						remove = true;
						coordinates = i;
					} else if (eq(coordinates.get(coordinates.size() - 1), i.get(0))) {
						coordinates.addAll(i.subList(1, i.size()));
						remove = true;
					}
					if (remove) {
						incompletedRings.remove(k);
					} else {
						k++;
					}
					if (eq(coordinates.get(0), coordinates.get(coordinates.size() - 1))) {
						completedRings.add(coordinates);
						add = false;
						break;
					}
				}
				if (add) {
					incompletedRings.add(coordinates);
				}
			}
		}
	}

	private void unifyIncompletedRings(List<TLongList> toProcces, List<TLongList> completedRings, int leftX, int rightX, int bottomY, int topY, long dbId, int zoom) {
		int mask = 0xffffffff;
		List<TLongList> uncompletedRings = new ArrayList<TLongList>(toProcces);
		toProcces.clear();
		Set<Integer> nonvisitedRings = new LinkedHashSet<Integer>();
		for (int j = 0; j < uncompletedRings.size(); j++) {
			TLongList i = uncompletedRings.get(j);
			int x = (int) (i.get(i.size() - 1) >> 32);
			int y = (int) (i.get(i.size() - 1) & mask);
			int sx = (int) (i.get(0) >> 32);
			int sy = (int) (i.get(0) & mask);
			boolean st = y == topY || x == rightX || y == bottomY || x == leftX;
			boolean end = sy == topY || sx == rightX || sy == bottomY || sx == leftX;
			// something goes wrong
			// These exceptions are used to check logic about processing multipolygons
			// However this situation could happen because of broken multipolygons (so it should data causes app error)
			// that's why these exceptions could be replaced with return; statement.
			if (!end || !st) {
				float dx = (float) MapUtils.get31LongitudeX(x);
				float dsx = (float) MapUtils.get31LongitudeX(sx);
				float dy = (float) MapUtils.get31LatitudeY(y);
				float dsy = (float) MapUtils.get31LatitudeY(sy);
				String str;
				if (!end) {
					str = " Starting point (to close) not found : end_x = {0}, end_y = {1}, start_x = {2}, start_y = {3} : bounds {4} {5} - {6} {7}"; //$NON-NLS-1$
					System.err
							.println(MessageFormat.format(dbId + str, dx, dy, dsx, dsy, leftX + "", topY + "", rightX + "", bottomY + "")); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$//$NON-NLS-4$
				}
				if (!st) {
					str = " End not found : end_x = {0}, end_y = {1}, start_x = {2}, start_y = {3} : bounds {4} {5} - {6} {7}"; //$NON-NLS-1$
					System.err
							.println(MessageFormat.format(dbId + str, dx, dy, dsx, dsy, leftX + "", topY + "", rightX + "", bottomY + "")); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$//$NON-NLS-4$
				}
				toProcces.add(i);
			} else {
				nonvisitedRings.add(j);
			}
		}
		for (int j = 0; j < uncompletedRings.size(); j++) {
			TLongList i = uncompletedRings.get(j);
			if (!nonvisitedRings.contains(j)) {
				continue;
			}

			int x = (int) (i.get(i.size() - 1) >> 32);
			int y = (int) (i.get(i.size() - 1) & mask);
			// 31 - (zoom + 8)
			int EVAL_DELTA = 6 << (23 - zoom);
			int UNDEFINED_MIN_DIFF = -1 - EVAL_DELTA;
			while (true) {
				int st = 0; // st already checked to be one of the four
				if (y == topY) {
					st = 0;
				} else if (x == rightX) {
					st = 1;
				} else if (y == bottomY) {
					st = 2;
				} else if (x == leftX) {
					st = 3;
				}
				int nextRingIndex = -1;
				// BEGIN go clockwise around rectangle
				for (int h = st; h < st + 4; h++) {

					// BEGIN find closest nonvisited start (including current)
					int mindiff = UNDEFINED_MIN_DIFF;
					for (Integer ni : nonvisitedRings) {
						TLongList cni = uncompletedRings.get(ni);
						int csx = (int) (cni.get(0) >> 32);
						int csy = (int) (cni.get(0) & mask);
						if (h % 4 == 0) {
							// top
							if (csy == topY && csx >= safelyAddDelta(x, -EVAL_DELTA)) {
								if (mindiff == UNDEFINED_MIN_DIFF || (csx - x) <= mindiff) {
									mindiff = (csx - x);
									nextRingIndex = ni;
								}
							}
						} else if (h % 4 == 1) {
							// right
							if (csx == rightX && csy >= safelyAddDelta(y, -EVAL_DELTA)) {
								if (mindiff == UNDEFINED_MIN_DIFF || (csy - y) <= mindiff) {
									mindiff = (csy - y);
									nextRingIndex = ni;
								}
							}
						} else if (h % 4 == 2) {
							// bottom
							if (csy == bottomY && csx <= safelyAddDelta(x, EVAL_DELTA)) {
								if (mindiff == UNDEFINED_MIN_DIFF || (x - csx) <= mindiff) {
									mindiff = (x - csx);
									nextRingIndex = ni;
								}
							}
						} else if (h % 4 == 3) {
							// left
							if (csx == leftX && csy <= safelyAddDelta(y, EVAL_DELTA)) {
								if (mindiff == UNDEFINED_MIN_DIFF || (y - csy) <= mindiff) {
									mindiff = (y - csy);
									nextRingIndex = ni;
								}
							}
						}
					} // END find closest start (including current)

					// we found start point
					if (mindiff != UNDEFINED_MIN_DIFF) {
						break;
					} else {
						if (h % 4 == 0) {
							// top
							y = topY;
							x = rightX;
						} else if (h % 4 == 1) {
							// right
							y = bottomY;
							x = rightX;
						} else if (h % 4 == 2) {
							// bottom
							y = bottomY;
							x = leftX;
						} else if (h % 4 == 3) {
							y = topY;
							x = leftX;
						}
						i.add((((long) x) << 32) | ((long) y));
					}

				} // END go clockwise around rectangle
				if (nextRingIndex == -1) {
					// it is impossible (current start should always be found)
				} else if (nextRingIndex == j) {
					i.add(i.get(0));
					nonvisitedRings.remove(j);
					break;
				} else {
					i.addAll(uncompletedRings.get(nextRingIndex));
					nonvisitedRings.remove(nextRingIndex);
					// get last point and start again going clockwise
					x = (int) (i.get(i.size() - 1) >> 32);
					y = (int) (i.get(i.size() - 1) & mask);
				}
			}

			completedRings.add(i);
		}
	}

	private int safelyAddDelta(int number, int delta) {
		int res = number + delta;
		if (delta > 0 && res < number) {
			return Integer.MAX_VALUE;
		} else if (delta < 0 && res > number) {
			return Integer.MIN_VALUE;
		}
		return res;
	}
	
	private static long combine2Points(int x, int y) {
		return (((long) x ) <<32) | ((long)y );
	}
	
	private boolean calculateLineCoordinates(boolean inside, int x, int y, boolean pinside, int px, int py, int leftX, int rightX,
			int bottomY, int topY, TLongList coordinates) {
		boolean lineEnded = false;
		if (pinside) {
			if (!inside) {
				long is = MapAlgorithms.calculateIntersection(x, y, px, py, leftX, rightX, bottomY, topY);
				if (is == -1) {
					// it is an error (!)
					is = combine2Points(px, py);
				}
				coordinates.add(is);
				lineEnded = true;
			} else {
				coordinates.add(combine2Points(x, y));
			}
		} else {
			long is = MapAlgorithms.calculateIntersection(x, y, px, py, leftX, rightX, bottomY, topY);
			if (inside) {
				// assert is != -1;
				coordinates.add(is);
				coordinates.add(combine2Points(x, y));
			} else if (is != -1) {
				int bx = (int) (is >> 32);
				int by = (int) (is & 0xffffffff);
				coordinates.add(is);
				is = MapAlgorithms.calculateIntersection(x, y, bx, by, leftX, rightX, bottomY, topY);
				coordinates.add(is);
				lineEnded = true;
			}
		}

		return lineEnded;
	}

	


}
