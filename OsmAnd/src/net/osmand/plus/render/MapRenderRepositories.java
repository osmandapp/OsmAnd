package net.osmand.plus.render;

import gnu.trove.list.TLongList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
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
import net.osmand.access.AccessibleToast;
import net.osmand.binary.BinaryMapDataObject;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapIndexReader.SearchRequest;
import net.osmand.binary.BinaryMapIndexReader.TagValuePair;
import net.osmand.data.IndexConstants;
import net.osmand.data.MapTileDownloader.IMapDownloaderCallback;
import net.osmand.osm.MapUtils;
import net.osmand.osm.MultyPolygon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.OsmandSettings.CommonPreference;
import net.osmand.plus.R;
import net.osmand.plus.RotatedTileBox;
import net.osmand.plus.render.NativeOsmandLibrary.NativeSearchResult;
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
	private final Context context;
	private Handler handler;
	private Map<String, BinaryMapIndexReader> files = new LinkedHashMap<String, BinaryMapIndexReader>();
	private Set<String> nativeFiles = new HashSet<String>();
	private OsmandRenderer renderer;

	private static String BASEMAP_NAME = "basemap";

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

	public MapRenderRepositories(Context context) {
		this.context = context;
		this.renderer = new OsmandRenderer(context);
		handler = new Handler(Looper.getMainLooper());
		prefs = OsmandApplication.getSettings();
	}

	public Context getContext() {
		return context;
	}

	public BinaryMapIndexReader initializeNewResource(final IProgress progress, File file) {
		long start = System.currentTimeMillis();
		if (files.containsKey(file.getAbsolutePath())) {
			closeConnection(files.get(file.getAbsolutePath()), file.getAbsolutePath());
			
		}
		RandomAccessFile raf = null;
		BinaryMapIndexReader reader = null;
		try {
			raf = new RandomAccessFile(file, "r"); //$NON-NLS-1$
			reader = new BinaryMapIndexReader(raf);
			if (reader.getVersion() != IndexConstants.BINARY_MAP_VERSION) {
				return null;
			}
			files.put(file.getAbsolutePath(), reader);

		} catch (IOException e) {
			log.error("No connection or unsupported version", e); //$NON-NLS-1$
			if (raf != null) {
				try {
					raf.close();
				} catch (IOException e1) {
				}
			}
			return null;
		} catch (OutOfMemoryError oome) {
			if (raf != null) {
				try {
					raf.close();
				} catch (IOException e1) {
				}
			}
			throw oome;
		}
		if (log.isDebugEnabled()) {
			log.debug("Initializing db " + file.getAbsolutePath() + " " + (System.currentTimeMillis() - start) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		return reader;
	}

	public RotatedTileBox getBitmapLocation() {
		return bmpLocation;
	}

	public RotatedTileBox getPrevBmpLocation() {
		return prevBmpLocation;
	}

	protected void closeConnection(BinaryMapIndexReader c, String file) {
		files.remove(file);
		nativeFiles.remove(file);
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
		if (files.isEmpty() || box == null) {
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
		for (String f : files.keySet()) {
			if (f.toLowerCase().contains(BASEMAP_NAME)) {
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
		// search lower level zooms only in basemap for now :) before it was intersection of maps on zooms 5-7
		boolean basemapSearch = false;
		if (zoom <= 7) {
			for (String f : files.keySet()) {
				if (f.toLowerCase().contains(BASEMAP_NAME)) {
					basemapSearch = true;
					break;
				}
			}
		}
		NativeSearchResult resultHandler = null;
		for (String mapName : files.keySet()) {
			if (basemapSearch && !mapName.toLowerCase().contains(BASEMAP_NAME)) {
				continue;
			}
			BinaryMapIndexReader reader = files.get(mapName);
			if(!reader.containsMapData(leftX, topY, rightX, bottomY, zoom)) {
				continue;
			}
			if (!nativeFiles.contains(mapName)) {
				nativeFiles.add(mapName);
				if (!library.initMapFile(mapName)) {
					continue;
				}
				log.debug("Native resource " + mapName + " initialized"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			resultHandler = library.searchObjectsForRendering(leftX, rightX, topY, bottomY, zoom, mapName, renderingReq,
					PerformanceFlags.checkForDuplicateObjectIds, resultHandler, this);
			if (checkWhetherInterrupted()) {
				library.deleteSearchResult(resultHandler);
				return false;
			}
		}
		cNativeObjects = resultHandler;
		cObjectsBox = dataBox;
		log.info(String.format("BLat=%s, TLat=%s, LLong=%s, RLong=%s, zoom=%s", //$NON-NLS-1$
				dataBox.bottom, dataBox.top, dataBox.left, dataBox.right, zoom));
		log.info(String.format("Native search: %s ms ", System.currentTimeMillis() - now)); //$NON-NLS-1$
		return true;
	}

	private boolean loadVectorData(RectF dataBox, final int zoom, final RenderingRuleSearchRequest renderingReq, final boolean nightMode) {
		double cBottomLatitude = dataBox.bottom;
		double cTopLatitude = dataBox.top;
		double cLeftLongitude = dataBox.left;
		double cRightLongitude = dataBox.right;

		long now = System.currentTimeMillis();

		if (files.isEmpty()) {
			cObjectsBox = dataBox;
			cObjects = new ArrayList<BinaryMapDataObject>();
			return true;
		}
		try {
			int count = 0;
			ArrayList<BinaryMapDataObject> tempList = new ArrayList<BinaryMapDataObject>();
			System.gc(); // to clear previous objects
			TLongSet ids = new TLongHashSet();
			Map<TagValuePair, List<BinaryMapDataObject>> multiPolygons = new LinkedHashMap<TagValuePair, List<BinaryMapDataObject>>();
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
			// search lower level zooms only in basemap for now :) before it was intersection of maps on zooms 5-7
			boolean basemapSearch = false;
			if (zoom <= 7) {
				for (String f : files.keySet()) {
					if (f.toLowerCase().contains(BASEMAP_NAME)) {
						basemapSearch = true;
						break;
					}
				}
			}

			for (String mapName : files.keySet()) {
				if (basemapSearch && !mapName.toLowerCase().contains(BASEMAP_NAME)) {
					continue;
				}
				
				BinaryMapIndexReader c = files.get(mapName);
				searchRequest = BinaryMapIndexReader.buildSearchRequest(leftX, rightX, topY, bottomY, zoom, searchFilter);
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

					// TODO refactor !
					if(r.containsType(r.getMapIndex().coastlineEncodingType)) {
						TagValuePair pair = r.getMapIndex().decodeType(r.getMapIndex().coastlineEncodingType);
						pair = new TagValuePair(pair.tag, pair.value, 0);
						if (!multiPolygons.containsKey(pair)) {
							multiPolygons.put(pair, new ArrayList<BinaryMapDataObject>());
						}
						multiPolygons.get(pair).add(r);
					}
					if (checkWhetherInterrupted()) {
						return false;
					}
					tempList.add(r);
				}
			}

			List<MultyPolygon> pMulti = proccessMultiPolygons(multiPolygons, leftX, rightX, bottomY, topY, zoom);
			tempList.addAll(pMulti);
			if (count > 0) {
				log.info(String.format("BLat=%s, TLat=%s, LLong=%s, RLong=%s, zoom=%s", //$NON-NLS-1$
						cBottomLatitude, cTopLatitude, cLeftLongitude, cRightLongitude, zoom));
				log.info(String.format("Searching: %s ms  (%s results found)", System.currentTimeMillis() - now, count)); //$NON-NLS-1$
			}
		

			cObjects = tempList;
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
				CommonPreference<String> settings = app.getSettings().getCustomRenderProperty(customProp.getAttrName());
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
					loaded = loadVectorDataNative(dataBox, requestedBox.getZoom(), renderingReq, nativeLib);
				} else {
					cNativeObjects = null;
					loaded = loadVectorData(dataBox, requestedBox.getZoom(), renderingReq, nightMode);
				}
				if (!loaded || checkWhetherInterrupted()) {
					return;
				}
			}
			final long searchTime = System.currentTimeMillis() - now;

			currentRenderingContext = new OsmandRenderer.RenderingContext(context);
			int fillColor = 0xf1eee8;
			renderingReq.clearState();
			renderingReq.setIntFilter(renderingReq.ALL.R_MINZOOM, requestedBox.getZoom());
			if(renderingReq.searchRenderingAttribute(RenderingRuleStorageProperties.A_DEFAULT_COLOR)) {
				fillColor = renderingReq.getIntPropertyValue(renderingReq.ALL.R_ATTR_COLOR_VALUE);
			}
			renderingReq.clearState();
			renderingReq.setIntFilter(renderingReq.ALL.R_MINZOOM, requestedBox.getZoom());
			if(renderingReq.searchRenderingAttribute(RenderingRuleStorageProperties.A_SHADOW_RENDERING)) {
				currentRenderingContext.shadowRenderingMode = renderingReq.getIntPropertyValue(renderingReq.ALL.R_ATTR_INT_VALUE);
				currentRenderingContext.shadowRenderingColor = renderingReq.getIntPropertyValue(renderingReq.ALL.R_SHADOW_COLOR);
			}
			currentRenderingContext.leftX = (float) requestedBox.getLeftTileX();
			currentRenderingContext.topY = (float) requestedBox.getTopTileY();
			currentRenderingContext.zoom = requestedBox.getZoom();
			currentRenderingContext.rotate = requestedBox.getRotate();
			currentRenderingContext.width = (int) (requestedBox.getTileWidth() * OsmandRenderer.TILE_SIZE);
			currentRenderingContext.height = (int) (requestedBox.getTileHeight() * OsmandRenderer.TILE_SIZE);
			currentRenderingContext.nightMode = nightMode;
			currentRenderingContext.highResMode = prefs.USE_HIGH_RES_MAPS.get();
			currentRenderingContext.mapTextSize = prefs.MAP_TEXT_SIZE.get();
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
			if(transparent) {
				bmp = Bitmap.createBitmap(currentRenderingContext.width, currentRenderingContext.height, Config.ARGB_8888);
			} else {
				bmp = Bitmap.createBitmap(currentRenderingContext.width, currentRenderingContext.height, Config.RGB_565);
			}

			// 1. generate image step by step
			this.prevBmp = this.bmp;
			this.prevBmpLocation = this.bmpLocation;
			this.bmp = bmp;
			this.bmpLocation = tileRect;
			
			
			if(nativeLib != null) {
				renderer.generateNewBitmapNative(currentRenderingContext, nativeLib, cNativeObjects, bmp, prefs.USE_ENGLISH_NAMES.get(), renderingReq,
						notifyList, fillColor);
			} else {
				renderer.generateNewBitmap(currentRenderingContext, cObjects, bmp, prefs.USE_ENGLISH_NAMES.get(), renderingReq,
						notifyList, fillColor);
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
					this.bmp = this.prevBmp;
					this.bmpLocation = this.prevBmpLocation;
				}
				currentRenderingContext = null;
				return;
			}
			currentRenderingContext = null;

			// 2. replace whole image
			this.prevBmp = null;
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
					AccessibleToast.makeText(context, R.string.rendering_out_of_memory, Toast.LENGTH_SHORT).show();
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
		return prevBmp;
	}

	public synchronized void clearCache() {
		cObjects = new ArrayList<BinaryMapDataObject>();
		cObjectsBox = new RectF();
		prevBmp = null;
		requestedBox = prevBmpLocation = null;
		// Do not clear main bitmap to not cause a screen refresh
//		bmp = null;
//		bmpLocation = null;
	}

	// / Manipulating with multipolygons

	public List<MultyPolygon> proccessMultiPolygons(Map<TagValuePair, List<BinaryMapDataObject>> multyPolygons, int leftX, int rightX,
			int bottomY, int topY, int zoom) {
		List<MultyPolygon> listPolygons = new ArrayList<MultyPolygon>(multyPolygons.size());
		List<TLongList> completedRings = new ArrayList<TLongList>();
		List<TLongList> incompletedRings = new ArrayList<TLongList>();
		List<String> completedRingNames = new ArrayList<String>();
		List<String> incompletedRingNames = new ArrayList<String>();
		for (TagValuePair type : multyPolygons.keySet()) {
			List<BinaryMapDataObject> directList;
			List<BinaryMapDataObject> inverselist;
			if (((type.additionalAttribute >> 15) & 1) == 1) {
				TagValuePair directType = new TagValuePair(type.tag, type.value, type.additionalAttribute & ((1 << 15) - 1));
				if (!multyPolygons.containsKey(directType)) {
					inverselist = multyPolygons.get(type);
					directList = Collections.emptyList();
				} else {
					// continue on inner boundaries
					continue;
				}
			} else {
				TagValuePair inverseType = new TagValuePair(type.tag, type.value, type.additionalAttribute | (1 << 15));
				directList = multyPolygons.get(type);
				inverselist = Collections.emptyList();
				if (multyPolygons.containsKey(inverseType)) {
					inverselist = multyPolygons.get(inverseType);
				}
			}
			completedRings.clear();
			incompletedRings.clear();
			completedRingNames.clear();
			incompletedRingNames.clear();
			log.debug("Process multypolygon " + type.tag + " " + type.value + //$NON-NLS-1$ //$NON-NLS-2$
					" direct list : " + directList + " rev : " + inverselist); //$NON-NLS-1$ //$NON-NLS-2$
			MultyPolygon pl = processMultiPolygon(leftX, rightX, bottomY, topY, listPolygons, completedRings, incompletedRings,
					completedRingNames, incompletedRingNames, type, directList, inverselist, zoom);
			if (pl != null) {
				listPolygons.add(pl);
			}
		}
		return listPolygons;
	}

	private MultyPolygon processMultiPolygon(int leftX, int rightX, int bottomY, int topY, List<MultyPolygon> listPolygons,
			List<TLongList> completedRings, List<TLongList> incompletedRings, List<String> completedRingNames,
			List<String> incompletedRingNames, TagValuePair type, List<BinaryMapDataObject> directList,
			List<BinaryMapDataObject> inverselist, int zoom) {
		MultyPolygon pl = new MultyPolygon();
		// delete direction last bit (to not show point)
		pl.setTag(type.tag);
		pl.setValue(type.value);
		long dbId = 0;
		for (int km = 0; km < 2; km++) {
			List<BinaryMapDataObject> list = km == 0 ? directList : inverselist;
			for (BinaryMapDataObject o : list) {
				int len = o.getPointsLength();
				if (len < 2) {
					continue;
				}
				dbId = o.getId() >> 1;
				TLongList coordinates = new TLongArrayList(o.getPointsLength() / 2);
				int px = o.getPoint31XTile(km == 0 ? 0 : len - 1);
				int py = o.getPoint31YTile(km == 0 ? 0 : len - 1);
				int x = px;
				int y = py;
				boolean pinside = leftX <= x && x <= rightX && y >= topY && y <= bottomY;
				if (pinside) {
					coordinates.add((((long) x) << 32) | ((long) y));
				}
				for (int i = 1; i < len; i++) {
					x = o.getPoint31XTile(km == 0 ? i : len - i - 1);
					y = o.getPoint31YTile(km == 0 ? i : len - i - 1);
					boolean inside = leftX <= x && x <= rightX && y >= topY && y <= bottomY;
					boolean lineEnded = calculateLineCoordinates(inside, x, y, pinside, px, py, leftX, rightX, bottomY, topY, coordinates);
					if (lineEnded) {
						processMultipolygonLine(completedRings, incompletedRings, completedRingNames, incompletedRingNames, coordinates,
								o.getName());
						// create new line if it goes outside
						coordinates = new TLongArrayList();
					}
					px = x;
					py = y;
					pinside = inside;
				}
				processMultipolygonLine(completedRings, incompletedRings, completedRingNames, incompletedRingNames, coordinates,
						o.getName());
			}
		}
		if (completedRings.size() == 0 && incompletedRings.size() == 0) {
			return null;
		}
		if (incompletedRings.size() > 0) {
			unifyIncompletedRings(incompletedRings, completedRings, completedRingNames, incompletedRingNames, leftX, rightX, bottomY, topY,
					dbId, zoom);
		} else {
			// due to self intersection small objects (for low zooms check only coastline)
			if (zoom >= 13 || ("natural".equals(type.tag) && "coastline".equals(type.value))) { //$NON-NLS-1$//$NON-NLS-2$
				boolean clockwiseFound = false;
				for (TLongList c : completedRings) {
					if (isClockwiseWay(c)) {
						clockwiseFound = true;
						break;
					}
				}
				if (!clockwiseFound) {
					// add whole bound
					TLongList whole = new TLongArrayList(4);
					whole.add((((long) leftX) << 32) | ((long) topY));
					whole.add((((long) rightX) << 32) | ((long) topY));
					whole.add((((long) rightX) << 32) | ((long) bottomY));
					whole.add((((long) leftX) << 32) | ((long) bottomY));
					completedRings.add(whole);
					log.info("!!! Isolated island !!!"); //$NON-NLS-1$
				}

			}
		}

		long[][] lns = new long[completedRings.size()][];
		for (int i = 0; i < completedRings.size(); i++) {
			TLongList ring = completedRings.get(i);
			lns[i] = new long[ring.size()];
			for (int j = 0; j < lns[i].length; j++) {
				lns[i][j] = ring.get(j);
			}
		}
		pl.setNames(completedRingNames.toArray(new String[completedRings.size()]));
		pl.setLines(lns);
		return pl;
	}

	// Copied from MapAlgorithms
	private boolean isClockwiseWay(TLongList c) {
		if (c.size() == 0) {
			return true;
		}

		// calculate middle Y
		int mask = 0xffffffff;
		long middleY = 0;
		for (int i = 0; i < c.size(); i++) {
			middleY += (c.get(i) & mask);
		}
		middleY /= (long) c.size();

		double clockwiseSum = 0;

		boolean firstDirectionUp = false;
		int previousX = Integer.MIN_VALUE;
		int firstX = Integer.MIN_VALUE;

		int prevX = (int) (c.get(0) >> 32);
		int prevY = (int) (c.get(0) & mask);

		for (int i = 1; i < c.size(); i++) {
			int x = (int) (c.get(i) >> 32);
			int y = (int) (c.get(i) & mask);
			int rX = ray_intersect_x(prevX, prevY, x, y, (int) middleY);
			if (rX != Integer.MIN_VALUE) {
				boolean skipSameSide = (y <= middleY) == (prevY <= middleY);
				if (skipSameSide) {
					continue;
				}
				boolean directionUp = prevY >= middleY;
				if (firstX == Integer.MIN_VALUE) {
					firstDirectionUp = directionUp;
					firstX = rX;
				} else {
					boolean clockwise = (!directionUp) == (previousX < rX);
					if (clockwise) {
						clockwiseSum += Math.abs(previousX - rX);
					} else {
						clockwiseSum -= Math.abs(previousX - rX);
					}
				}
				previousX = rX;
				prevX = x;
				prevY = y;
			}
		}
		if (firstX != Integer.MIN_VALUE) {
			boolean clockwise = (!firstDirectionUp) == (previousX < firstX);
			if (clockwise) {
				clockwiseSum += Math.abs(previousX - firstX);
			} else {
				clockwiseSum -= Math.abs(previousX - firstX);
			}
		}

		return clockwiseSum >= 0;
	}

	// Copied from MapAlgorithms
	private int ray_intersect_x(int prevX, int prevY, int x, int y, int middleY) {
		// prev node above line
		// x,y node below line
		if (prevY > y) {
			int tx = prevX;
			int ty = prevY;
			x = prevX;
			y = prevY;
			prevX = tx;
			prevY = ty;
		}
		if (y == middleY || prevY == middleY) {
			middleY -= 1;
		}
		if (prevY > middleY || y < middleY) {
			return Integer.MIN_VALUE;
		} else {
			if (y == prevY) {
				// the node on the boundary !!!
				return x;
			}
			// that tested on all cases (left/right)
			double rx = x + ((double) middleY - y) * ((double) x - prevX) / (((double) y - prevY));
			return (int) rx;
		}
	}

	private void processMultipolygonLine(List<TLongList> completedRings, List<TLongList> incompletedRings,
			List<String> completedRingsNames, List<String> incompletedRingsNames, TLongList coordinates, String name) {
		if (coordinates.size() > 0) {
			if (coordinates.get(0) == coordinates.get(coordinates.size() - 1)) {
				completedRings.add(coordinates);
				completedRingsNames.add(name);
			} else {
				boolean add = true;
				for (int k = 0; k < incompletedRings.size();) {
					boolean remove = false;
					TLongList i = incompletedRings.get(k);
					String oldName = incompletedRingsNames.get(k);
					if (coordinates.get(0) == i.get(i.size() - 1)) {
						i.addAll(coordinates.subList(1, coordinates.size()));
						remove = true;
						coordinates = i;
					} else if (coordinates.get(coordinates.size() - 1) == i.get(0)) {
						coordinates.addAll(i.subList(1, i.size()));
						remove = true;
					}
					if (remove) {
						incompletedRings.remove(k);
						incompletedRingsNames.remove(k);
					} else {
						k++;
					}
					if (coordinates.get(0) == coordinates.get(coordinates.size() - 1)) {
						completedRings.add(coordinates);
						if (oldName != null) {
							completedRingsNames.add(oldName);
						} else {
							completedRingsNames.add(name);
						}
						add = false;
						break;
					}
				}
				if (add) {
					incompletedRings.add(coordinates);
					incompletedRingsNames.add(name);
				}
			}
		}
	}

	private void unifyIncompletedRings(List<TLongList> incompletedRings, List<TLongList> completedRings, List<String> completedRingNames,
			List<String> incompletedRingNames, int leftX, int rightX, int bottomY, int topY, long dbId, int zoom) {
		int mask = 0xffffffff;
		Set<Integer> nonvisitedRings = new LinkedHashSet<Integer>();
		for (int j = 0; j < incompletedRings.size(); j++) {
			TLongList i = incompletedRings.get(j);
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
					str = " Start point (to close) not found : end_x = {0}, end_y = {1}, start_x = {2}, start_y = {3} : bounds {4} {5} - {6} {7}"; //$NON-NLS-1$
					System.err
							.println(MessageFormat.format(dbId + str, dx, dy, dsx, dsy, leftX + "", topY + "", rightX + "", bottomY + "")); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$//$NON-NLS-4$
				}
				if (!st) {
					str = " End not found : end_x = {0}, end_y = {1}, start_x = {2}, start_y = {3} : bounds {4} {5} - {6} {7}"; //$NON-NLS-1$
					System.err
							.println(MessageFormat.format(dbId + str, dx, dy, dsx, dsy, leftX + "", topY + "", rightX + "", bottomY + "")); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$//$NON-NLS-4$
				}
			} else {
				nonvisitedRings.add(j);
			}
		}
		for (int j = 0; j < incompletedRings.size(); j++) {
			TLongList i = incompletedRings.get(j);
			String name = incompletedRingNames.get(j);
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
						TLongList cni = incompletedRings.get(ni);
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
					i.addAll(incompletedRings.get(nextRingIndex));
					nonvisitedRings.remove(nextRingIndex);
					// get last point and start again going clockwise
					x = (int) (i.get(i.size() - 1) >> 32);
					y = (int) (i.get(i.size() - 1) & mask);
				}
			}

			completedRings.add(i);
			completedRingNames.add(name);
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

	/**
	 * @return -1 if there is no instersection or x<<32 | y
	 */
	private long calculateIntersection(int x, int y, int px, int py, int leftX, int rightX, int bottomY, int topY) {
		int by = -1;
		int bx = -1;
		// firstly try to search if the line goes in
		if (py < topY && y >= topY) {
			int tx = (int) (px + ((double) (x - px) * (topY - py)) / (y - py));
			if (leftX <= tx && tx <= rightX) {
				bx = tx;
				by = topY;
				return (((long) bx) << 32) | ((long) by);
			}
		}
		if (py > bottomY && y <= bottomY) {
			int tx = (int) (px + ((double) (x - px) * (py - bottomY)) / (py - y));
			if (leftX <= tx && tx <= rightX) {
				bx = tx;
				by = bottomY;
				return (((long) bx) << 32) | ((long) by);
			}
		}
		if (px < leftX && x >= leftX) {
			int ty = (int) (py + ((double) (y - py) * (leftX - px)) / (x - px));
			if (ty >= topY && ty <= bottomY) {
				by = ty;
				bx = leftX;
				return (((long) bx) << 32) | ((long) by);
			}

		}
		if (px > rightX && x <= rightX) {
			int ty = (int) (py + ((double) (y - py) * (px - rightX)) / (px - x));
			if (ty >= topY && ty <= bottomY) {
				by = ty;
				bx = rightX;
				return (((long) bx) << 32) | ((long) by);
			}

		}

		// try to search if point goes out
		if (py > topY && y <= topY) {
			int tx = (int) (px + ((double) (x - px) * (topY - py)) / (y - py));
			if (leftX <= tx && tx <= rightX) {
				bx = tx;
				by = topY;
				return (((long) bx) << 32) | ((long) by);
			}
		}
		if (py < bottomY && y >= bottomY) {
			int tx = (int) (px + ((double) (x - px) * (py - bottomY)) / (py - y));
			if (leftX <= tx && tx <= rightX) {
				bx = tx;
				by = bottomY;
				return (((long) bx) << 32) | ((long) by);
			}
		}
		if (px > leftX && x <= leftX) {
			int ty = (int) (py + ((double) (y - py) * (leftX - px)) / (x - px));
			if (ty >= topY && ty <= bottomY) {
				by = ty;
				bx = leftX;
				return (((long) bx) << 32) | ((long) by);
			}

		}
		if (px < rightX && x >= rightX) {
			int ty = (int) (py + ((double) (y - py) * (px - rightX)) / (px - x));
			if (ty >= topY && ty <= bottomY) {
				by = ty;
				bx = rightX;
				return (((long) bx) << 32) | ((long) by);
			}

		}

		if (px == rightX || px == leftX || py == topY || py == bottomY) {
			bx = px;
			by = py;
		}
		return -1l;
	}

	private boolean calculateLineCoordinates(boolean inside, int x, int y, boolean pinside, int px, int py, int leftX, int rightX,
			int bottomY, int topY, TLongList coordinates) {
		boolean lineEnded = false;
		if (pinside) {
			if (!inside) {
				long is = calculateIntersection(x, y, px, py, leftX, rightX, bottomY, topY);
				if (is == -1) {
					// it is an error (!)
					is = (((long) px) << 32) | ((long) py);
				}
				coordinates.add(is);
				lineEnded = true;
			} else {
				coordinates.add((((long) x) << 32) | ((long) y));
			}
		} else {
			long is = calculateIntersection(x, y, px, py, leftX, rightX, bottomY, topY);
			if (inside) {
				// assert is != -1;
				coordinates.add(is);
				coordinates.add((((long) x) << 32) | ((long) y));
			} else if (is != -1) {
				int bx = (int) (is >> 32);
				int by = (int) (is & 0xffffffff);
				coordinates.add(is);
				is = calculateIntersection(x, y, bx, by, leftX, rightX, bottomY, topY);
				coordinates.add(is);
				lineEnded = true;
			}
		}

		return lineEnded;
	}

	public Map<String, BinaryMapIndexReader> getMetaInfoFiles() {
		return files;
	}
}
