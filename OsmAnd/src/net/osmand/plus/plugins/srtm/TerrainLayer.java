package net.osmand.plus.plugins.srtm;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Canvas;
import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.core.android.MapRendererContext;
import net.osmand.core.android.MapRendererView;
import net.osmand.core.android.TileSourceProxyProvider;
import net.osmand.core.jni.GeoTiffCollection;
import net.osmand.core.jni.HeightRasterMapLayerProvider;
import net.osmand.core.jni.HillshadeRasterMapLayerProvider;
import net.osmand.core.jni.IRasterMapLayerProvider;
import net.osmand.core.jni.SlopeRasterMapLayerProvider;
import net.osmand.core.jni.ZoomLevel;
import net.osmand.data.QuadRect;
import net.osmand.data.QuadTree;
import net.osmand.data.RotatedTileBox;
import net.osmand.map.ITileSource;
import net.osmand.plus.OsmAndTaskManager;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.api.SQLiteAPI.SQLiteConnection;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.development.OsmandDevelopmentPlugin;
import net.osmand.plus.resources.SQLiteTileSource;
import net.osmand.plus.views.corenative.NativeCoreContext;
import net.osmand.plus.views.layers.MapTileLayer;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class TerrainLayer extends MapTileLayer {

	private static final Log log = PlatformUtil.getLog(TerrainLayer.class);
	private Map<String, SQLiteTileSource> resources = new LinkedHashMap<>();

	private static final int ZOOM_BOUNDARY = 15;
	private static final int DEFAULT_ALPHA = 100;
	private final SRTMPlugin srtmPlugin;
	private final TerrainMode mode;

	private final QuadTree<String> indexedResources = new QuadTree<>(new QuadRect(0, 0, 1 << (ZOOM_BOUNDARY+1), 1 << (ZOOM_BOUNDARY+1)), 8, 0.55f);

	private IRasterMapLayerProvider layerProvider;
	private int cachedMinVisibleZoom = -1;
	private int cachedMaxVisibleZoom = -1;

	public TerrainLayer(@NonNull Context context, @NonNull SRTMPlugin srtmPlugin) {
		super(context, false);
		OsmandApplication app = (OsmandApplication) context.getApplicationContext();
		this.srtmPlugin = srtmPlugin;
		mode = srtmPlugin.getTerrainMode();
		indexTerrainFiles(app);
		setAlpha(DEFAULT_ALPHA);
		setMap(createTileSource(app));
	}

	@Override
	protected boolean setLayerProvider(@Nullable ITileSource map) {
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer == null) {
			return false;
		}

		int layerIndex = view.getLayerIndex(this);
		if (map == null) {
			mapRenderer.resetMapLayerProvider(layerIndex);
			layerProvider = null;
			return true;
		}

		OsmandDevelopmentPlugin developmentPlugin = PluginsHelper.getPlugin(OsmandDevelopmentPlugin.class);
		boolean terrainFromHeightmap = developmentPlugin != null && developmentPlugin.generateTerrainFrom3DMaps();

		MapRendererContext mapRendererContext = NativeCoreContext.getMapRendererContext();
		GeoTiffCollection geoTiffCollection = mapRendererContext != null
				? mapRendererContext.getGeoTiffCollection()
				: null;

		if (terrainFromHeightmap && geoTiffCollection != null) {
			layerProvider = createGeoTiffLayerProvider(mode, geoTiffCollection);
			if (layerProvider != null) {
				mapRenderer.setMapLayerProvider(layerIndex, layerProvider);
			}
		} else {
			TileSourceProxyProvider prov = new TerrainTilesProvider(getApplication(), map, srtmPlugin);
			mapRenderer.setMapLayerProvider(layerIndex, prov.instantiateProxy(true));
			prov.swigReleaseOwnership();
			layerProvider = null;
		}

		return true;
	}

	@NonNull
	private IRasterMapLayerProvider createGeoTiffLayerProvider(TerrainMode mode, @NonNull GeoTiffCollection geoTiffCollection) {
		OsmandApplication app = getApplication();
		File heightmapDir = app.getAppPath(IndexConstants.CLR_PALETTE_DIR);
		String mainColorFilename = new File(heightmapDir, mode.getMainFile()).getAbsolutePath();
		IRasterMapLayerProvider provider = null;
		if (mode.getType() == TerrainMode.TerrainType.HILLSHADE) {
			String slopeSecondaryColorFilename = new File(heightmapDir, mode.getSecondFile()).getAbsolutePath();
			provider =
					new HillshadeRasterMapLayerProvider(geoTiffCollection, mainColorFilename, slopeSecondaryColorFilename);
			((HillshadeRasterMapLayerProvider) provider).setMinVisibleZoom(ZoomLevel.swigToEnum(srtmPlugin.getTerrainMinZoom()));
			((HillshadeRasterMapLayerProvider) provider).setMaxVisibleZoom(ZoomLevel.swigToEnum(srtmPlugin.getTerrainMaxZoom()));
		} else if (mode.getType() == TerrainMode.TerrainType.SLOPE) {
			provider = new SlopeRasterMapLayerProvider(geoTiffCollection, mainColorFilename);
			((SlopeRasterMapLayerProvider) provider).setMinVisibleZoom(ZoomLevel.swigToEnum(srtmPlugin.getTerrainMinZoom()));
			((SlopeRasterMapLayerProvider) provider).setMaxVisibleZoom(ZoomLevel.swigToEnum(srtmPlugin.getTerrainMaxZoom()));
		} else if (mode.getType() == TerrainMode.TerrainType.HEIGHT) {
			provider = new HeightRasterMapLayerProvider(geoTiffCollection, mainColorFilename);
			((HeightRasterMapLayerProvider) provider).setMinVisibleZoom(ZoomLevel.swigToEnum(srtmPlugin.getTerrainMinZoom()));
			((HeightRasterMapLayerProvider) provider).setMaxVisibleZoom(ZoomLevel.swigToEnum(srtmPlugin.getTerrainMaxZoom()));
		}
		// provider.setKey(mode.getKey()); // opengl binding (cache should be key +'.cache')

		return provider;
	}


	@Override
	public void onPrepareBufferImage(Canvas canvas, RotatedTileBox tileBox, DrawSettings drawSettings) {
		int zoom = tileBox.getZoom();
		int newMinVisibleZoom = srtmPlugin.getTerrainMinZoom();
		int newMaxVisibleZoom = srtmPlugin.getTerrainMaxZoom();
		boolean fitsZoomBounds = zoom >= newMinVisibleZoom && zoom <= newMaxVisibleZoom;

		if (cachedMinVisibleZoom != newMinVisibleZoom || cachedMaxVisibleZoom != newMaxVisibleZoom) {
			boolean clearTilesForCurrentZoom = (cachedMinVisibleZoom != -1 && cachedMaxVisibleZoom != -1)
					&& (zoom >= cachedMinVisibleZoom && zoom <= cachedMaxVisibleZoom)
					&& !fitsZoomBounds;
			cachedMinVisibleZoom = newMinVisibleZoom;
			cachedMaxVisibleZoom = newMaxVisibleZoom;
			MapRendererView mapRenderer = getMapRenderer();
			if (mapRenderer != null) {
				ZoomLevel minVisibleZoomLevel = ZoomLevel.swigToEnum(newMinVisibleZoom);
				ZoomLevel maxVisibleZoomLevel = ZoomLevel.swigToEnum(newMaxVisibleZoom);
				if (layerProvider instanceof  HillshadeRasterMapLayerProvider) {
					((HillshadeRasterMapLayerProvider) layerProvider).setMinVisibleZoom(minVisibleZoomLevel);
					((HillshadeRasterMapLayerProvider) layerProvider).setMaxVisibleZoom(maxVisibleZoomLevel);
				} else if (layerProvider instanceof  SlopeRasterMapLayerProvider) {
					((SlopeRasterMapLayerProvider) layerProvider).setMinVisibleZoom(minVisibleZoomLevel);
					((SlopeRasterMapLayerProvider) layerProvider).setMaxVisibleZoom(maxVisibleZoomLevel);
				}
				if (clearTilesForCurrentZoom) {
					mapRenderer.reloadEverything();
				}
			}
		}

		setAlpha(srtmPlugin.getTerrainTransparency());
		if (hasMapRenderer() || fitsZoomBounds) {
			super.onPrepareBufferImage(canvas, tileBox, drawSettings);
		}
	}

	private void indexTerrainFiles(OsmandApplication app) {
		@SuppressLint("StaticFieldLeak") AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
			private SQLiteDatabase sqliteDb;
			@Override
			protected Void doInBackground(Void... params) {
				File tilesDir = app.getAppPath(IndexConstants.TILES_INDEX_DIR);
				File cacheDir = app.getCacheDir();
				// fix http://stackoverflow.com/questions/26937152/workaround-for-nexus-9-sqlite-file-write-operations-on-external-dirs
				try {
					sqliteDb = SQLiteDatabase.openDatabase(
							new File(cacheDir, mode.getCacheFileName()).getPath(),
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

						Map<String, Long> fileModified = new HashMap<>();
						Map<String, SQLiteTileSource> rs = readFiles(app, tilesDir, fileModified);
						indexCachedResources(fileModified, rs);
						indexNonCachedResources(fileModified, rs);
						resources = rs;
					} catch (RuntimeException e) {
						log.error(e.getMessage(), e);
					} finally {
						sqliteDb.close();
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
						log.info("Indexing " + mode.getMainFile() + " file " + filename);
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

			private Map<String, SQLiteTileSource> readFiles(OsmandApplication app, File tilesDir, Map<String, Long> fileModified) {
				Map<String, SQLiteTileSource> rs = new LinkedHashMap<>();
				File[] files = tilesDir.listFiles();
				if(files != null) {
					for(File f : files) {
						if (f != null
								&& f.getName().endsWith(IndexConstants.SQLITE_EXT)
								&& f.getName().toLowerCase().startsWith(mode.getKeyName())) {
							SQLiteTileSource ts = new SQLiteTileSource(app, f, new ArrayList<>());
							rs.put(f.getName(), ts);
							fileModified.put(f.getName(), f.lastModified());
						}
					}
				}
				return rs;
			}
		};
		OsmAndTaskManager.executeTask(task);
	}

	private SQLiteTileSource createTileSource(@NonNull OsmandApplication app) {
		return new SQLiteTileSource(app, null, new ArrayList<>()) {
			
			@Override
			protected SQLiteConnection getDatabase() {
				throw new UnsupportedOperationException();
			}

			List<String> getTileSource(int x, int y, int zoom) {
				ArrayList<String> ls = new ArrayList<>();
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
			public byte[] getBytes(int x, int y, int zoom, String dirWithTiles, long[] timeHolder) throws IOException {
				List<String> ts = getTileSource(x, y, zoom);
				for (String t : ts) {
					SQLiteTileSource sqLiteTileSource = resources.get(t);
					if (sqLiteTileSource != null) {
						byte[] res = sqLiteTileSource.getBytes(x, y, zoom, null, timeHolder);
						if (res != null) {
							return res;
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
				return Algorithms.capitalizeFirstLetter(mode.getKeyName().toLowerCase());
			}
			
			@Override
			public String getTileFormat() {
				return "jpg";
			}
		};
	}
}
