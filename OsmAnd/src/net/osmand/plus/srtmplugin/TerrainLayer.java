package net.osmand.plus.srtmplugin;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.AsyncTask;

import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.data.QuadRect;
import net.osmand.data.QuadTree;
import net.osmand.data.RotatedTileBox;
import net.osmand.map.TileSourceManager.TileSourceTemplate;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.SQLiteTileSource;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.api.SQLiteAPI.SQLiteConnection;
import net.osmand.plus.views.MapTileLayer;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static net.osmand.plus.srtmplugin.TerrainMode.HILLSHADE;

public class TerrainLayer extends MapTileLayer {

	private final static Log log = PlatformUtil.getLog(TerrainLayer.class);
	private Map<String, SQLiteTileSource> resources = new LinkedHashMap<String, SQLiteTileSource>();
	private final static String HILLSHADE_CACHE = "hillshade.cache";
	private final static String SLOPE_CACHE = "slope.cache";
	private int ZOOM_BOUNDARY = 15;
	private final static int DEFAULT_ALPHA = 100;
	private SRTMPlugin srtmPlugin;
	private TerrainMode mode;

	private QuadTree<String> indexedResources = new QuadTree<String>(new QuadRect(0, 0, 1 << (ZOOM_BOUNDARY+1), 1 << (ZOOM_BOUNDARY+1)), 8, 0.55f);

	public TerrainLayer(MapActivity activity, SRTMPlugin srtmPlugin) {
		super(false);
		final OsmandApplication app = activity.getMyApplication();
		this.srtmPlugin = srtmPlugin;
		mode = srtmPlugin.getTerrainMode();
		indexTerrainFiles(app);
		setAlpha(DEFAULT_ALPHA);
		setMap(createTileSource(activity));
	}

	@Override
	public void onPrepareBufferImage(Canvas canvas, RotatedTileBox tileBox, DrawSettings drawSettings) {
		int zoom = tileBox.getZoom();
		if (zoom >= srtmPlugin.getTerrainMinZoom() && zoom <= srtmPlugin.getTerrainMaxZoom()) {
			setAlpha(srtmPlugin.getTerrainTransparency());
			super.onPrepareBufferImage(canvas, tileBox, drawSettings);
		} else if(zoom > srtmPlugin.getTerrainMaxZoom()) {
			// backward compatibility 100 -> 20 with overscale
			setAlpha(srtmPlugin.getTerrainTransparency() / 5);
			super.onPrepareBufferImage(canvas, tileBox, drawSettings);
		} else {
			// ignore
		}
	}

	private void indexTerrainFiles(final OsmandApplication app) {
		@SuppressLint("StaticFieldLeak") AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
			private SQLiteDatabase sqliteDb;
			private String type = mode.name().toLowerCase();
			@Override
			protected Void doInBackground(Void... params) {
				File tilesDir = app.getAppPath(IndexConstants.TILES_INDEX_DIR);
				File cacheDir = app.getCacheDir();
				// fix http://stackoverflow.com/questions/26937152/workaround-for-nexus-9-sqlite-file-write-operations-on-external-dirs
				try {
					sqliteDb = SQLiteDatabase.openDatabase(
							new File(cacheDir, mode == HILLSHADE ? HILLSHADE_CACHE : SLOPE_CACHE).getPath(),
							null, SQLiteDatabase.ENABLE_WRITE_AHEAD_LOGGING
									| SQLiteDatabase.CREATE_IF_NECESSARY);
				} catch (RuntimeException e) {
					log.error(e.getMessage(), e);
					sqliteDb = null;
				}
				if (sqliteDb != null) {
					try {
						if (sqliteDb.getVersion() == 0) {
							sqliteDb.setVersion(1);
						}
						sqliteDb.execSQL("CREATE TABLE IF NOT EXISTS TILE_SOURCES(filename varchar2(256), date_modified int, left int, right int, top int, bottom int)");

						Map<String, Long> fileModified = new HashMap<String, Long>();
						Map<String, SQLiteTileSource> rs = readFiles(app, tilesDir, fileModified);
						indexCachedResources(fileModified, rs);
						indexNonCachedResources(fileModified, rs);
						sqliteDb.close();
						resources = rs;
					} catch (RuntimeException e) {
						log.error(e.getMessage(), e);
					}
				}
				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				app.getResourceManager().reloadTilesFromFS();
			}

			private void indexNonCachedResources(Map<String, Long> fileModified, Map<String, SQLiteTileSource> rs) {
				for(Map.Entry<String, Long> entry : fileModified.entrySet()) {
					String filename = entry.getKey();
					try {
						log.info("Indexing " + type + " file " + filename);
						ContentValues cv = new ContentValues();
						cv.put("filename", filename);
						cv.put("date_modified", entry.getValue());
						SQLiteTileSource ts = rs.get(filename);
						QuadRect rt = ts.getRectBoundary(ZOOM_BOUNDARY, 1);
						if (rt != null) {
							indexedResources.insert(filename, rt);
							cv.put("left", (int)rt.left);
							cv.put("right",(int) rt.right);
							cv.put("top", (int)rt.top);
							cv.put("bottom",(int) rt.bottom);
							sqliteDb.insert("TILE_SOURCES", null, cv);
						}
					} catch(RuntimeException e){
						log.error(e.getMessage(), e);
					}
				}
			}

			private void indexCachedResources(Map<String, Long> fileModified, Map<String, SQLiteTileSource> rs) {
				Cursor cursor = sqliteDb.rawQuery("SELECT filename, date_modified, left, right, top, bottom FROM TILE_SOURCES", 
						new String[0]);
				if(cursor.moveToFirst()) {
					do {
						String filename = cursor.getString(0);
						long lastModified = cursor.getLong(1);
						Long read = fileModified.get(filename);
						if(rs.containsKey(filename) && read != null && lastModified == read) {
							int left = cursor.getInt(2);
							int right = cursor.getInt(3);
							int top = cursor.getInt(4);
							float bottom = cursor.getInt(5);
							indexedResources.insert(filename, new QuadRect(left, top, right, bottom));
							fileModified.remove(filename);
						}
					} while(cursor.moveToNext());
				}
				cursor.close();
			}

			private Map<String, SQLiteTileSource> readFiles(final OsmandApplication app, File tilesDir, Map<String, Long> fileModified) {
				Map<String, SQLiteTileSource> rs = new LinkedHashMap<String, SQLiteTileSource>();
				File[] files = tilesDir.listFiles();
				if(files != null) {
					for(File f : files) {
						if (f != null
								&& f.getName().endsWith(IndexConstants.SQLITE_EXT)
								&& f.getName().toLowerCase().startsWith(type)) {
							SQLiteTileSource ts = new SQLiteTileSource(app, f, new ArrayList<TileSourceTemplate>());
							rs.put(f.getName(), ts);
							fileModified.put(f.getName(), f.lastModified());
						}
					}
				}
				return rs;
			}
		};
		executeTaskInBackground(task);
	}

	private SQLiteTileSource createTileSource(MapActivity activity) {
		return new SQLiteTileSource(activity.getMyApplication(), null, new ArrayList<TileSourceTemplate>()) {
			
			@Override
			protected SQLiteConnection getDatabase() {
				throw new UnsupportedOperationException();
			}
			
			public boolean isLocked() {
				return false;
			};
			
			List<String> getTileSource(int x, int y, int zoom) {
				ArrayList<String> ls = new ArrayList<String>();
				int z = (zoom - ZOOM_BOUNDARY);
				if (z > 0) {
					indexedResources.queryInBox(new QuadRect(x >> z, y >> z, (x >> z), (y >> z)), ls);
				} else {
					indexedResources.queryInBox(new QuadRect(x << -z, y << -z, (x + 1) << -z, (y + 1) << -z), ls);
				}
				return ls;
			}
			
			@Override
			public boolean exists(int x, int y, int zoom) {
				List<String> ts = getTileSource(x, y, zoom);
				for (String t : ts) {
					SQLiteTileSource sqLiteTileSource = resources.get(t);
					if(sqLiteTileSource != null && sqLiteTileSource.exists(x, y, zoom)) {
						return true;
					}
				}
				return false;
			}
			
			@Override
			public Bitmap getImage(int x, int y, int zoom, long[] timeHolder) {
				List<String> ts = getTileSource(x, y, zoom);
				for (String t : ts) {
					SQLiteTileSource sqLiteTileSource = resources.get(t);
					if (sqLiteTileSource != null) {
						Bitmap bmp = sqLiteTileSource.getImage(x, y, zoom, timeHolder);
						if (bmp != null) {
							return sqLiteTileSource.getImage(x, y, zoom, timeHolder);
						}
					}
				}
				return null;
			}
			
			@Override
			public int getBitDensity() {
				return 32;
			}
			
			@Override
			public int getMinimumZoomSupported() {
				return 5;
			}
			
			@Override
			public int getMaximumZoomSupported() {
				return 11;
			}
			
			@Override
			public int getTileSize() {
				return 256;
			}
			
			@Override
			public boolean couldBeDownloadedFromInternet() {
				return false;
			}
			
			@Override
			public String getName() {
				return Algorithms.capitalizeFirstLetter(mode.name().toLowerCase());
			}
			
			@Override
			public String getTileFormat() {
				return "jpg";
			}
		};
	}
}
