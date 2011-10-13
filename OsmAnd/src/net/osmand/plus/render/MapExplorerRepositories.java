package net.osmand.plus.render;

import gnu.trove.list.array.TIntArrayList;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;

import android.content.Context;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;

import net.osmand.IProgress;
import net.osmand.LogUtil;
import net.osmand.binary.BinaryMapDataObject;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapIndexReader.SearchRequest;
import net.osmand.binary.BinaryMapIndexReader.TagValuePair;
import net.osmand.data.IndexConstants;
import net.osmand.osm.MapUtils;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.activities.OsmandApplication;
import net.osmand.plus.render.OsmandRenderer.RenderingContext;
import net.osmand.render.OsmandRenderingRulesParser;

public class MapExplorerRepositories {
	private final Context context;
	
	private final static Log log = LogUtil.getLog(MapExplorerRepositories.class);

	private Map<String, BinaryMapIndexReader> files = new LinkedHashMap<String, BinaryMapIndexReader>();

	// lat/lon box of requested vector data 
	private RectF objectsBox = new RectF();

	// cached objects in order to explore without reloading data from db
	private List<BinaryMapDataObject> objects = new LinkedList<BinaryMapDataObject>();

	public MapExplorerRepositories(Context context){
		this.context = context;
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
		} catch (OutOfMemoryError oome) {
			if(raf != null){
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

	protected void closeConnection(BinaryMapIndexReader c, String file){
		files.remove(file);
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

	public List<BinaryMapDataObject> loadMapObjects(RectF rect, final int zoom) {
		if (objectsBox != null && objects != null && 
			objectsBox.top == rect.top && objectsBox.left == rect.left &&
			objectsBox.right == rect.right && objectsBox.bottom == rect.bottom) 
			return objects;

		double cBottomLatitude = rect.bottom;
		double cTopLatitude = rect.top;
		double cLeftLongitude = rect.left;
		double cRightLongitude = rect.right;

		if (objects == null)
			objects = new ArrayList<BinaryMapDataObject>(); 
		else
			objects.clear();

		log.info(String.format("BLat=%s, TLat=%s, LLong=%s, RLong=%s, zoom=%s", //$NON-NLS-1$
				cBottomLatitude, cTopLatitude, cLeftLongitude, cRightLongitude, zoom)); 

		if (files.isEmpty())
			return objects;

		try {
			int leftX = MapUtils.get31TileNumberX(cLeftLongitude);
			int rightX = MapUtils.get31TileNumberX(cRightLongitude);
			int bottomY = MapUtils.get31TileNumberY(cBottomLatitude);
			int topY = MapUtils.get31TileNumberY(cTopLatitude);

			OsmandApplication app = ((OsmandApplication)context.getApplicationContext());
			Boolean renderDay = app.getDaynightHelper().getDayNightRenderer();
			final boolean nightMode = renderDay != null && !renderDay.booleanValue();
			final BaseOsmandRender renderingType = app.getRendererRegistry().getCurrentSelectedRenderer();

			SearchRequest<BinaryMapDataObject> request = BinaryMapIndexReader.buildSearchRequest(leftX, rightX, topY, bottomY, zoom);
			if (zoom < 17) {
				request.setSearchFilter(new BinaryMapIndexReader.SearchFilter() {

					@Override
					public boolean accept(TIntArrayList types, BinaryMapIndexReader.MapIndex root) {
						for (int j = 0; j < types.size(); j++) {
							int type = types.get(j);
							int mask = type & 3;
							TagValuePair pair = root.decodeType(type);
							if (pair != null &&  renderingType.isObjectVisible(pair.tag, pair.value, zoom, mask, nightMode)) {
								return true;
							}
							if(pair != null && mask == OsmandRenderingRulesParser.POINT_STATE && 
									renderingType.isObjectVisible(pair.tag, pair.value, zoom, OsmandRenderingRulesParser.TEXT_STATE, nightMode)){
								return true;
							}
						}
						return false;
					}
				});
			}
			for (String mapName : files.keySet()) {
				BinaryMapIndexReader c  = files.get(mapName);
				List<BinaryMapDataObject> res = c.searchMapIndex(request);

				objects.addAll(res);
			}
		} catch (IOException e) {
			log.debug("Search failed", e); //$NON-NLS-1$
			return objects;
		}			
		return objects;
	}

	public void clearAllResources(){
		clearCache();
		for(String f : new ArrayList<String>(files.keySet())){
			closeConnection(files.get(f), f);
		}
	}

	public synchronized void clearCache() {
		objects = new ArrayList<BinaryMapDataObject>();
		objectsBox = new RectF();
	}
}
