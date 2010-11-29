package net.osmand.render;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntByteMap;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.osmand.IProgress;
import net.osmand.LogUtil;
import net.osmand.OsmandSettings;
import net.osmand.R;
import net.osmand.RotatedTileBox;
import net.osmand.binary.BinaryMapDataObject;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapIndexReader.SearchRequest;
import net.osmand.binary.BinaryMapIndexReader.TagValuePair;
import net.osmand.data.index.IndexConstants;
import net.osmand.data.preparation.MapTileDownloader.IMapDownloaderCallback;
import net.osmand.osm.MapRenderingTypes;
import net.osmand.osm.MapUtils;
import net.osmand.osm.MultyPolygon;
import net.osmand.render.OsmandRenderer.RenderingContext;

import org.apache.commons.logging.Log;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.graphics.Bitmap.Config;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

public class MapRenderRepositories {
	
	private final static Log log = LogUtil.getLog(MapRenderRepositories.class);
	private final Context context;
	private Handler handler;
	private Map<String, BinaryMapIndexReader> files = new LinkedHashMap<String, BinaryMapIndexReader>();
	private OsmandRenderer renderer;

	
	// lat/lon box of requested vector data 
	private RectF cObjectsBox = new RectF();
	// cached objects in order to render rotation without reloading data from db
	private List<BinaryMapDataObject> cObjects = new LinkedList<BinaryMapDataObject>();
	
	// currently rendered box (not the same as already rendered)
	//	this box is checked for interrupted process or 
	private RotatedTileBox requestedBox = null;

	// location of rendered bitmap
	private RotatedTileBox bmpLocation = null;
	// already rendered  bitmap
	private Bitmap bmp;
	
	private boolean interrupted = false;
	private RenderingContext currentRenderingContext;
	private SearchRequest<BinaryMapDataObject> searchRequest;
	private SharedPreferences prefs;
	
	public MapRenderRepositories(Context context){
		this.context = context;
		this.renderer = new OsmandRenderer(context);
		handler = new Handler(Looper.getMainLooper());
		prefs = OsmandSettings.getPrefs(context);
	}
	
	public Context getContext() {
		return context;
	}
	
	
	public BinaryMapIndexReader initializeNewResource(final IProgress progress, File file) {
		long start = System.currentTimeMillis();
		if(files.containsKey(file.getAbsolutePath())){
			closeConnection(files.get(file.getAbsolutePath()), file.getAbsolutePath());
		}
		RandomAccessFile raf = null;
		BinaryMapIndexReader reader = null;
		try {
			raf = new RandomAccessFile(file, "r"); //$NON-NLS-1$
			reader = new BinaryMapIndexReader(raf);
			if(reader.getVersion() != IndexConstants.BINARY_MAP_VERSION){
				return null;
			}
			files.put(file.getAbsolutePath(), reader);
			
		} catch (IOException e) {
			log.error("No connection or unsupported version", e); //$NON-NLS-1$
			if(raf != null){
				try {
					raf.close();
				} catch (IOException e1) {
				}
			}
			return null;
		}
		if (log.isDebugEnabled()) {
			log.debug("Initializing db " + file.getAbsolutePath() + " " + (System.currentTimeMillis() - start) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		return reader;
	}

	
	public RotatedTileBox getBitmapLocation() {
		return bmpLocation;
	}
	
	protected void closeConnection(BinaryMapIndexReader c, String file){
		files.remove(file);
		try {
			c.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void clearAllResources(){
		clearCache();
		for(String f : new ArrayList<String>(files.keySet())){
			closeConnection(files.get(f), f);
		}
	}
	

	public boolean updateMapIsNeeded(RotatedTileBox box){
		if (files.isEmpty() || box == null) {
			return false;
		}
		if(requestedBox == null){
			return true;
		}
		if(requestedBox.getZoom() != box.getZoom()){
			return true;
		}
		
		float deltaRotate = requestedBox.getRotate() - box.getRotate();
		if(deltaRotate > 180){
			deltaRotate -= 360;
		} else if(deltaRotate < -180){
			deltaRotate += 360;
		}
		if(Math.abs(deltaRotate) > 25){
			return true;
		}
		return !requestedBox.containsTileBox(box);
	}

	public boolean isEmpty(){
		return files.isEmpty();
	}
	
	public void interruptLoadingMap(){
		interrupted = true;
		if(currentRenderingContext != null){
			currentRenderingContext.interrupted = true;
		}
		if(searchRequest != null){
			searchRequest.setInterrupted(true);
		}
	}
	
	private boolean checkWhetherInterrupted(){
		if(interrupted || (currentRenderingContext != null && currentRenderingContext.interrupted)){
			requestedBox = bmpLocation;
			return true;
		}
		return false;
	}
	
	private boolean loadVectorData(RectF dataBox, final int zoom){
		double cBottomLatitude = dataBox.bottom;
		double cTopLatitude = dataBox.top;
		double cLeftLongitude = dataBox.left;
		double cRightLongitude = dataBox.right;

		log.info(String.format("BLat=%s, TLat=%s, LLong=%s, RLong=%s, zoom=%s", //$NON-NLS-1$
				cBottomLatitude, cTopLatitude, cLeftLongitude, cRightLongitude, zoom)); 

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
			searchRequest = BinaryMapIndexReader.buildSearchRequest(leftX, rightX, topY, bottomY, zoom);
			if (zoom < 15) {
				searchRequest.setSearchFilter(new BinaryMapIndexReader.SearchFilter() {
					TIntByteMap map = MapRenderingTypes.getObjectTypeMinZoom();

					@Override
					public boolean accept(TIntArrayList types) {
						for (int j = 0; j < types.size(); j++) {
							int type = types.get(j);
							if ((type & 3) == MapRenderingTypes.POINT_TYPE) {
								// keep 13 bit after last 2 bits and
								// 0111 1111 1111 1100
								type &= 0x7ffc;
							} else {
								// 0000 1111 1111 1100
								type &= 0xffc;
							}
							byte b = map.get(type);
							if (b != 0 && b <= zoom) {
								return true;
							}
						}
						return false;
					}

				});
			}
			
			for (BinaryMapIndexReader c : files.values()) {
				List<BinaryMapDataObject> res = c.searchMapIndex(searchRequest);
				if (checkWhetherInterrupted()) {
					return false;
				}
				for (BinaryMapDataObject r : res) {
					if (PerformanceFlags.checkForDuplicateObjectIds) {
						if (ids.contains(r.getId())) {
							// do not add object twice
							continue;
						}
						ids.add(r.getId());
					}
					count++;
					
					for(int i=0; i < r.getTypes().length; i++){
						if ((r.getTypes()[i] & 0x3) == MapRenderingTypes.MULTY_POLYGON_TYPE) {
							// multy polygon
							int type = MapRenderingTypes.getMainObjectType(r.getTypes()[i]);
							int subtype = MapRenderingTypes.getObjectSubType(r.getTypes()[i]);
							TagValuePair pair = r.getMapIndex().decodeType(type, subtype);
							if(pair != null){
								pair = new TagValuePair(pair.tag, pair.value, r.getTypes()[i]);
								if (!multiPolygons.containsKey(pair)) {
									multiPolygons.put(pair, new ArrayList<BinaryMapDataObject>());
								}
								multiPolygons.get(pair).add(r);
							}
						}
					}
					
					
					if (checkWhetherInterrupted()) {
						return false;
					}
					tempList.add(r);
				}
				searchRequest.clearSearchResults();
			}
			
			List<MultyPolygon> pMulti = proccessMultiPolygons(multiPolygons, leftX, rightX, bottomY, topY, zoom);
			tempList.addAll(pMulti);
			log.info(String.format("Search has been done in %s ms. %s results were found.", System.currentTimeMillis() - now, count)); //$NON-NLS-1$
			
			cObjects = tempList;
			cObjectsBox = dataBox;
		} catch (IOException e) {
			log.debug("Search failed", e); //$NON-NLS-1$
			return false;
		}
		
		return true;
	}
		
	
	public synchronized void loadMap(RotatedTileBox tileRect, List<IMapDownloaderCallback> notifyList) {
		interrupted = false;
		if(currentRenderingContext != null){
			currentRenderingContext = null;
		}
		try {
			// prevent editing
			requestedBox = new RotatedTileBox(tileRect);

			// calculate data box
			RectF dataBox = requestedBox.calculateLatLonBox(new RectF());
			long now = System.currentTimeMillis();
			if (cObjectsBox.left > dataBox.left || cObjectsBox.top > dataBox.top || cObjectsBox.right < dataBox.right
					|| cObjectsBox.bottom < dataBox.bottom) {
				// increase data box in order for rotate
				if ((dataBox.right - dataBox.left) > (dataBox.top - dataBox.bottom)) {
					double wi = (dataBox.right - dataBox.left) * .2;
					dataBox.left -= wi;
					dataBox.right += wi;
				} else {
					double hi = (dataBox.bottom - dataBox.top) * .2;
					dataBox.top -= hi;
					dataBox.bottom += hi;
				}
				boolean loaded = loadVectorData(dataBox, requestedBox.getZoom());
				if (!loaded || checkWhetherInterrupted()) {
					return;
				}
			}
			final long searchTime = System.currentTimeMillis() - now;

			currentRenderingContext = new OsmandRenderer.RenderingContext();
			currentRenderingContext.leftX = (float) requestedBox.getLeftTileX();
			currentRenderingContext.topY = (float) requestedBox.getTopTileY();
			currentRenderingContext.zoom = requestedBox.getZoom();
			currentRenderingContext.rotate = requestedBox.getRotate();
			currentRenderingContext.width = (int) (requestedBox.getTileWidth() * OsmandRenderer.TILE_SIZE);
			currentRenderingContext.height = (int) (requestedBox.getTileHeight() * OsmandRenderer.TILE_SIZE);
			if (checkWhetherInterrupted()) {
				return;
			}

			now = System.currentTimeMillis();
			
			
			Bitmap bmp = Bitmap.createBitmap(currentRenderingContext.width, currentRenderingContext.height, Config.RGB_565);
			
			boolean stepByStep = OsmandSettings.isUsingStepByStepRendering(prefs);
			// 1. generate image step by step
			if (stepByStep) {
				Bitmap oldBmp = this.bmp;
				this.bmp = bmp;
				this.bmpLocation = tileRect;
				if (oldBmp != null) {
					oldBmp.recycle();
				}
			}
			
			renderer.generateNewBitmap(currentRenderingContext, cObjects, bmp, 
					OsmandSettings.usingEnglishNames(prefs), stepByStep ? notifyList : null);
			if (checkWhetherInterrupted()) {
				currentRenderingContext = null;
				return;
			}
			final long renderingTime = System.currentTimeMillis() - now;
			currentRenderingContext = null;
			
			// 2. replace whole image
			if (!stepByStep) {
				Bitmap oldBmp = this.bmp;
				this.bmp = bmp;
				this.bmpLocation = tileRect;
				if (oldBmp != null) {
					oldBmp.recycle();
				}
			}
			if(OsmandSettings.isDebugRendering(context)){
				final String msg = "Search done in "+ searchTime+" ms\nRendering done in "+ renderingTime+ " ms";    //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
				handler.post(new Runnable(){
					@Override
					public void run() {
						Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
					}
				});
			}
		} catch (RuntimeException e) {
			log.error("Runtime memory exception", e); //$NON-NLS-1$
			handler.post(new Runnable(){
				@Override
				public void run() {
					Toast.makeText(context, R.string.rendering_exception, Toast.LENGTH_SHORT).show();
				}
			});
		} catch (OutOfMemoryError e) {
			log.error("Out of memory error", e); //$NON-NLS-1$
			cObjects = new ArrayList<BinaryMapDataObject>();
			cObjectsBox = new RectF();
			handler.post(new Runnable(){
				@Override
				public void run() {
					Toast.makeText(context, R.string.rendering_out_of_memory, Toast.LENGTH_SHORT).show();
				}
			});
			
		}
		
	}
	
	public Bitmap getBitmap() {
		return bmp;
	}
	
	
	public synchronized void clearCache() {
		cObjects = new ArrayList<BinaryMapDataObject>();
		cObjectsBox = new RectF();
		if(bmp != null){
			bmp.recycle();
			bmp = null;
		}
		requestedBox = bmpLocation = null;
	}

	
	/// Manipulating with multipolygons
	
	public List<MultyPolygon> proccessMultiPolygons(Map<TagValuePair, List<BinaryMapDataObject>> multyPolygons, int leftX, int rightX, int bottomY, int topY, int zoom){
		List<MultyPolygon> listPolygons = new ArrayList<MultyPolygon>(multyPolygons.size());
		List<List<Long>> completedRings = new ArrayList<List<Long>>();
		List<List<Long>> incompletedRings = new ArrayList<List<Long>>();
		List<String> completedRingNames = new ArrayList<String>();
		List<String> incompletedRingNames = new ArrayList<String>();
		for (TagValuePair type : multyPolygons.keySet()) {
			List<BinaryMapDataObject> directList;
			List<BinaryMapDataObject> inverselist;
			if(((type.additionalAttribute >> 15) & 1) == 1){
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
			
			MultyPolygon pl = processMultiPolygon(leftX, rightX, bottomY, topY, listPolygons, completedRings, incompletedRings, 
					completedRingNames, incompletedRingNames, type,	directList, inverselist, zoom);
			if(pl != null){
				listPolygons.add(pl);
			}
		}
		return listPolygons;
	}

	private MultyPolygon processMultiPolygon(int leftX, int rightX, int bottomY, int topY, List<MultyPolygon> listPolygons,
			List<List<Long>> completedRings, List<List<Long>> incompletedRings, List<String> completedRingNames, List<String> incompletedRingNames, 
			TagValuePair type, List<BinaryMapDataObject> directList, List<BinaryMapDataObject> inverselist, int zoom) {
		MultyPolygon pl = new MultyPolygon();
		// TODO delete setType at all!!!
		// delete direction last bit (to not show point)
		pl.setType(type.additionalAttribute);
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
				dbId = o.getId() >> 3;
				List<Long> coordinates = new ArrayList<Long>(o.getPointsLength() / 2);
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
					calculateLineCoordinates(inside, x, y, pinside, px, py, leftX, rightX, bottomY, topY, coordinates);
					if(pinside && !inside){
						processMultipolygonLine(completedRings, incompletedRings, completedRingNames, incompletedRingNames, 
								coordinates, o.getName());
						// create new line if it goes outside
						coordinates = new ArrayList<Long>();
					}
					px = x;
					py = y;
					pinside = inside;
				}
				processMultipolygonLine(completedRings, incompletedRings, completedRingNames, incompletedRingNames, 
						coordinates, o.getName());
			}
		}
		if(completedRings.size() == 0 && incompletedRings.size() == 0){
			return null;
		}
		if (incompletedRings.size() > 0) {
			unifyIncompletedRings(incompletedRings, completedRings, completedRingNames, incompletedRingNames, leftX, rightX, bottomY, topY, dbId, zoom);
		} else {
			// due to self intersection small objects (for low zooms check only coastline)
			if (zoom >= 13
					|| ("natural".equals(type.tag) && "coastline".equals(type.value))) {  //$NON-NLS-1$//$NON-NLS-2$
				boolean clockwiseFound = false;
				for (List<Long> c : completedRings) {
					if (isClockwiseWay(c)) {
						clockwiseFound = true;
						break;
					}
				}
				if (!clockwiseFound) {
					// add whole bound
					List<Long> whole = new ArrayList<Long>(4);
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
			List<Long> ring = completedRings.get(i);
			lns[i] = new long[ring.size()];
			for (int j = 0; j < lns[i].length; j++) {
				lns[i][j] = ring.get(j);
			}
		}
		pl.setNames(completedRingNames.toArray(new String[completedRings.size()]));
		pl.setLines(lns);
		return pl;
	}
	
	private boolean isClockwiseWay(List<Long> c){
		double angle = 0;
		double prevAng = 0;
		int px = 0;
		int py = 0;
		int mask = 0xffffffff;
		for (int i = 0; i < c.size(); i++) {
			int x = (int) (c.get(i) >> 32);
			int y = (int) (c.get(i) & mask);
			if (i >= 1) {
				double ang = Math.atan2(py - y, x - px);
				if (i > 1) {
					double delta = (ang - prevAng);
					if (delta < -Math.PI) {
						delta += 2 * Math.PI;
					} else if (delta > Math.PI) {
						delta -= 2 * Math.PI;
					}
					angle += delta;
					prevAng = ang;
				} else {
					prevAng = ang;
				}
			}
			px = x;
			py = y;

		}
		return angle < 0;
	}


	private void processMultipolygonLine(List<List<Long>> completedRings, List<List<Long>> incompletedRings, 
			List<String> completedRingsNames, List<String> incompletedRingsNames, List<Long> coordinates, String name) {
		if (coordinates.size() > 0) {
			if (coordinates.get(0).longValue() == coordinates.get(coordinates.size() - 1).longValue()) {
				completedRings.add(coordinates);
				completedRingsNames.add(name);
			} else {
				boolean add = true;
				for (int k = 0; k < incompletedRings.size();) {
					boolean remove = false;
					List<Long> i = incompletedRings.get(k);
					String oldName = incompletedRingsNames.get(k);
					if (coordinates.get(0).longValue() == i.get(i.size() - 1).longValue()) {
						i.addAll(coordinates.subList(1, coordinates.size()));
						remove = true;
						coordinates = i;
					} else if (coordinates.get(coordinates.size() - 1).longValue() == i.get(0).longValue()) {
						coordinates.addAll(i.subList(1, i.size()));
						remove = true;
					}
					if (remove) {
						incompletedRings.remove(k);
						incompletedRingsNames.remove(k);
					} else {
						k++;
					}
					if (coordinates.get(0).longValue() == coordinates.get(coordinates.size() - 1).longValue()) {
						completedRings.add(coordinates);
						if(oldName != null){
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

	private void unifyIncompletedRings(List<List<Long>> incompletedRings, List<List<Long>> completedRings, 
			List<String> completedRingNames, List<String> incompletedRingNames, 
			int leftX, int rightX,	int bottomY, int topY, long dbId, int zoom) {
		int mask = 0xffffffff;
		Set<Integer> nonvisitedRings = new LinkedHashSet<Integer>();
		for(int j = 0; j< incompletedRings.size(); j++){
			List<Long> i = incompletedRings.get(j);
			int x = (int) (i.get(i.size() - 1) >> 32);
			int y = (int) (i.get(i.size() - 1) & mask);
			int sx = (int) (i.get(0) >> 32);
			int sy = (int) (i.get(0) & mask);
			boolean st = y == topY || x == rightX || y == bottomY || x == leftX;
			boolean end = sy == topY || sx == rightX || sy == bottomY || sx == leftX;
			// something wrong here
			// These exceptions are used to check logic about processing multipolygons
			// However in map data this situation could happen with broken multipolygons (so it would data causes app error)
			// that's why these exceptions could be replaced with return; statement.
			if (!end || !st) {
				float dx = (float) MapUtils.get31LongitudeX(x);
				float dsx = (float) MapUtils.get31LongitudeX(sx);
				float dy = (float) MapUtils.get31LatitudeY(y);
				float dsy = (float) MapUtils.get31LatitudeY(sy);
				String str;
				if(!end){
					str = " Start point (to close) not found : end_x = {0}, end_y = {1}, start_x = {2}, start_y = {3} : bounds {4} {5} - {6} {7}"; //$NON-NLS-1$
					System.err.println(
						MessageFormat.format(dbId + str,  
								dx, dy, dsx, dsy, leftX+"", topY+"", rightX+"", bottomY+""));        //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$//$NON-NLS-4$
				}
				if(!st){
					str = " End not found : end_x = {0}, end_y = {1}, start_x = {2}, start_y = {3} : bounds {4} {5} - {6} {7}"; //$NON-NLS-1$
					System.err.println(
						MessageFormat.format(dbId + str,  
								dx, dy, dsx, dsy, leftX+"", topY+"", rightX+"", bottomY+""));        //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$//$NON-NLS-4$
				}
			} else {
				nonvisitedRings.add(j);
			}
		}
		for(int j = 0; j< incompletedRings.size(); j++){
			List<Long> i = incompletedRings.get(j);
			String name = incompletedRingNames.get(j);
			if(!nonvisitedRings.contains(j)){
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
						List<Long> cni = incompletedRings.get(ni);
						int csx = (int) (cni.get(0) >> 32);
						int csy = (int) (cni.get(0) & mask);
						if (h % 4 == 0) {
							// top
							if (csy == topY && csx >= x - EVAL_DELTA) {
								if (mindiff == UNDEFINED_MIN_DIFF || (csx - x) <= mindiff) {
									mindiff = (csx - x);
									nextRingIndex = ni;
								}
							}
						} else if (h % 4 == 1) {
							// right
							if (csx == rightX && csy >= y - EVAL_DELTA) {
								if (mindiff == UNDEFINED_MIN_DIFF || (csy - y) <= mindiff) {
									mindiff = (csy - y);
									nextRingIndex = ni;
								}
							}
						} else if (h % 4 == 2) {
							// bottom
							if (csy == bottomY && csx <= x + EVAL_DELTA) {
								if (mindiff == UNDEFINED_MIN_DIFF || (x - csx) <= mindiff) {
									mindiff = (x - csx);
									nextRingIndex = ni;
								}
							}
						} else if (h % 4 == 3) {
							// left
							if (csx == leftX && csy <= y + EVAL_DELTA) {
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

	private void calculateLineCoordinates(boolean inside, int x, int y, boolean pinside, int px, int py, int leftX, int rightX,
			int bottomY, int topY, List<Long> coordinates) {
		if (pinside) {
			 if(!inside) {
				int by = -1;
				int bx = -1;
				if (by == -1 && y < topY && py >= topY) {
					int tx = (int) (px + ((double) (x - px) * (topY - py)) / (y - py));
					if (leftX <= tx && tx <= rightX) {
						bx = tx;
						by = topY;
					}
				}
				if (by == -1 && y > bottomY && py <= bottomY) {
					int tx = (int) (px + ((double) (x - px) * (py - bottomY)) / (py - y));
					if (leftX <= tx && tx <= rightX) {
						bx = tx;
						by = bottomY;
					}
				}
				if (by == -1 && x < leftX && px >= leftX) {
					by = (int) (py + ((double) (y - py) * (leftX - px)) / (x - px));
					bx = leftX;
				}
				if (by == -1 && x > rightX && px <= rightX) {
					by = (int) (py + ((double) (y - py) * (px - rightX)) / (px - x));
					bx = rightX;
				}
				coordinates.add((((long) bx) << 32) | ((long) by));
			}
		} else if (inside) {
			int by = -1;
			int bx = -1;
			if (by == -1 && py < topY && y >= topY) {
				int tx = (int) (px + ((double) (x - px) * (topY - py)) / (y - py));
				if (leftX <= tx && tx <= rightX) {
					bx = tx;
					by = topY;
				}
			}
			if (by == -1 && py > bottomY && y <= bottomY) {
				int tx = (int) (px + ((double) (x - px) * (py - bottomY)) / (py - y));
				if (leftX <= tx && tx <= rightX) {
					bx = tx;
					by = bottomY;
				}
			}
			if (by == -1 && px < leftX && x >= leftX) {
				by = (int) (py + ((double) (y - py) * (leftX - px)) / (x - px));
				bx = leftX;
			}
			if (by == -1 && px > rightX && x <= rightX) {
				by = (int) (py + ((double) (y - py) * (px - rightX)) / (px - x));
				bx = rightX;
			}
			coordinates.add((((long) bx) << 32) | ((long) by));
		}
		
		if (inside) {
			coordinates.add((((long) x) << 32) | ((long) y));
		}
	}


}
