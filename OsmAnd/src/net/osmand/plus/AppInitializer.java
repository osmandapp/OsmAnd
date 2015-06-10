package net.osmand.plus;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import net.osmand.IProgress;
import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.access.AccessibleAlertBuilder;
import net.osmand.map.OsmandRegions;
import net.osmand.osm.AbstractPoiType;
import net.osmand.osm.MapPoiTypes;
import net.osmand.plus.activities.DayNightHelper;
import net.osmand.plus.activities.HelpActivity;
import net.osmand.plus.activities.SavingTrackHelper;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.helpers.AvoidSpecificRoads;
import net.osmand.plus.helpers.WaypointHelper;
import net.osmand.plus.monitoring.LiveMonitoringHelper;
import net.osmand.plus.poi.PoiFiltersHelper;
import net.osmand.plus.render.MapRenderRepositories;
import net.osmand.plus.render.NativeOsmandLibrary;
import net.osmand.plus.render.RendererRegistry;
import net.osmand.plus.resources.ResourceManager;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.views.corenative.NativeCoreContext;
import net.osmand.plus.voice.CommandPlayerException;
import net.osmand.plus.voice.CommandPlayerFactory;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.router.RoutingConfiguration;
import net.osmand.util.Algorithms;

import org.xmlpull.v1.XmlPullParserException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import btools.routingapp.BRouterServiceConnection;

/**
 * Created by Denis
 * on 03.03.15.
 */
public class AppInitializer implements IProgress {

	public static final boolean TIPS_AND_TRICKS = false;
	private static final String FIRST_TIME_APP_RUN = "FIRST_TIME_APP_RUN"; //$NON-NLS-1$
	private static final String VECTOR_INDEXES_CHECK = "VECTOR_INDEXES_CHECK"; //$NON-NLS-1$
	private static final String VERSION_INSTALLED = "VERSION_INSTALLED"; //$NON-NLS-1$
	private static final String EXCEPTION_FILE_SIZE = "EXCEPTION_FS"; //$NON-NLS-1$

	public static final String LATEST_CHANGES_URL = "changes-2.0.html";
	public static final int APP_EXIT_CODE = 4;
	public static final String APP_EXIT_KEY = "APP_EXIT_KEY";
	private OsmandApplication app;
	private static final org.apache.commons.logging.Log LOG = PlatformUtil.getLog(AppInitializer.class);
	
	private boolean initSettings = false;
	private boolean firstTime;
	private boolean activityChangesShowed = false;
	private boolean appVersionChanged;
	private long startTime;
	private long startBgTime;
	private boolean appInitializing = true;
	private List<String> warnings = new ArrayList<String>();
	private String taskName;
	private List<AppInitializeListener> listeners = new ArrayList<AppInitializer.AppInitializeListener>();
	
	public enum InitEvents {
		FAVORITES_INITIALIZED, NATIVE_INITIALIZED,
		NATIVE_OPEN_GLINITIALIZED,
		TASK_CHANGED, MAPS_INITIALIZED, POI_TYPES_INITIALIZED, ASSETS_COPIED, INIT_RENDERERS,
		RESTORE_BACKUPS, INDEX_REGION_BOUNDARIES, SAVE_GPX_TRACKS, LOAD_GPX_TRACKS;
	}
	
	public interface AppInitializeListener {
		
		public void onProgress(AppInitializer init, InitEvents event);
		
		public void onFinish(AppInitializer init);
	}
	
	
	public AppInitializer(OsmandApplication app) {
		this.app = app;
	}
	
	
	public List<String> getWarnings() {
		return warnings;
	}
	
	public boolean isAppInitializing() {
		return appInitializing;
	}
	
	
	private void initUiVars(Activity activity) {
		if(initSettings) {
			return;
		}
		SharedPreferences pref = activity.getPreferences(Context.MODE_WORLD_WRITEABLE);
		if (!pref.contains(FIRST_TIME_APP_RUN)) {
			firstTime = true;
			pref.edit().putBoolean(FIRST_TIME_APP_RUN, true).commit();
			pref.edit().putString(VERSION_INSTALLED, Version.getFullVersion(app)).commit();
		} else if (!Version.getFullVersion(app).equals(pref.getString(VERSION_INSTALLED, ""))) {
			pref.edit().putString(VERSION_INSTALLED, Version.getFullVersion(app)).commit();
			appVersionChanged = true;
		}
		initSettings = true;
	}
	
	public boolean isFirstTime(Activity activity) {
		initUiVars(activity);
		return firstTime;
	}
	
	public void setFirstTime(boolean firstTime) {
		this.firstTime = firstTime;
	}
	
	public boolean checkAppVersionChanged(Activity activity) {
		initUiVars(activity);
		boolean showRecentChangesDialog = !firstTime && appVersionChanged;
//		showRecentChangesDialog = true;
		if (showRecentChangesDialog && !activityChangesShowed) {
			final Intent helpIntent = new Intent(activity, HelpActivity.class);
			helpIntent.putExtra(HelpActivity.TITLE, Version.getAppVersion(app));
			helpIntent.putExtra(HelpActivity.URL, LATEST_CHANGES_URL);
			activity.startActivity(helpIntent);
			activityChangesShowed = true;
			return true;
		}
		return false;
	}

	public boolean checkPreviousRunsForExceptions(Activity activity) {
		initUiVars(activity);
		long size = activity.getPreferences(Context.MODE_WORLD_READABLE).getLong(EXCEPTION_FILE_SIZE, 0);
		final File file = app.getAppPath(OsmandApplication.EXCEPTION_PATH);
		if (file.exists() && file.length() > 0) {
			if (size != file.length() && !firstTime) {
				activity.getPreferences(Context.MODE_WORLD_WRITEABLE).edit().putLong(EXCEPTION_FILE_SIZE, file.length()).commit();
				return true;
			}
		} else {
			if (size > 0) {
				activity.getPreferences(Context.MODE_WORLD_WRITEABLE).edit().putLong(EXCEPTION_FILE_SIZE, 0).commit();
			}
		}
		return false;
	}

	// TODO
	public void checkVectorIndexesDownloaded(final Activity ctx) {
		OsmandApplication app = (OsmandApplication)ctx.getApplication();
		MapRenderRepositories maps = app.getResourceManager().getRenderer();
		SharedPreferences pref = ctx.getPreferences(Context.MODE_WORLD_WRITEABLE);
		boolean check = pref.getBoolean(VECTOR_INDEXES_CHECK, true);
		// do not show each time
		if (check && new Random().nextInt() % 5 == 1) {
			AlertDialog.Builder builder = new AccessibleAlertBuilder(ctx);
			if (maps.isEmpty()) {
				builder.setMessage(R.string.vector_data_missing);
			} else if (!maps.basemapExists()) {
				builder.setMessage(R.string.basemap_missing);
			} else {
				return;
			}
			builder.setPositiveButton(R.string.shared_string_download, new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					ctx.startActivity(new Intent(ctx, DownloadActivity.class));
				}

			});
			builder.setNeutralButton(R.string.vector_map_not_needed, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					ctx.getPreferences(Context.MODE_WORLD_WRITEABLE).edit().putBoolean(VECTOR_INDEXES_CHECK, false).commit();
				}
			});
			builder.setNegativeButton(R.string.first_time_continue, null);
			builder.show();
		}

	}

	private void indexRegionsBoundaries(List<String> warnings) {
		try {
			File file = app.getAppPath("regions.ocbf");
			if (file != null) {
				if (!file.exists()) {
					Algorithms.streamCopy(OsmandRegions.class.getResourceAsStream("regions.ocbf"),
							new FileOutputStream(file));
				}
				app.regions.prepareFile(file.getAbsolutePath());
			}
		} catch (Exception e) {
			warnings.add(e.getMessage());
			LOG.error(e.getMessage(), e);
		}
	}

	
	private void initPoiTypes() {
		if(app.getAppPath("poi_types.xml").exists()) {
			app.poiTypes.init(app.getAppPath("poi_types.xml").getAbsolutePath());
		} else {
			app.poiTypes.init();
		}
		app.poiTypes.setPoiTranslator(new MapPoiTypes.PoiTranslator() {
			
			@Override
			public String getTranslation(AbstractPoiType type) {
				try {
					Field f = R.string.class.getField("poi_" + type.getIconKeyName());
					if (f != null) {
						Integer in = (Integer) f.get(null);
						return app.getString(in);
					}
				} catch (Exception e) {
					System.err.println(e.getMessage());
				}
				return null;
			}
		});
	}



	public void onCreateApplication() {
		// always update application mode to default
		OsmandSettings osmandSettings = app.getSettings();
		if (!osmandSettings.FOLLOW_THE_ROUTE.get()) {
			osmandSettings.APPLICATION_MODE.set(osmandSettings.DEFAULT_APPLICATION_MODE.get());
		}
		startTime = System.currentTimeMillis();
		try {
			app.bRouterServiceConnection = startupInit(BRouterServiceConnection.connect(app), BRouterServiceConnection.class);
		} catch (Exception e) {
			e.printStackTrace();
		}
		app.applyTheme(app);
		app.poiTypes = startupInit(MapPoiTypes.getDefaultNoInit(), MapPoiTypes.class); 
		app.routingHelper = startupInit(new RoutingHelper(app), RoutingHelper.class);
		app.resourceManager = startupInit(new ResourceManager(app), ResourceManager.class);
		app.daynightHelper = startupInit(new DayNightHelper(app), DayNightHelper.class);
		app.avoidSpecificRoads = startupInit(new AvoidSpecificRoads(app), AvoidSpecificRoads.class);
		app.locationProvider = startupInit(new OsmAndLocationProvider(app), OsmAndLocationProvider.class);
		app.savingTrackHelper = startupInit(new SavingTrackHelper(app), SavingTrackHelper.class);
		app.liveMonitoringHelper = startupInit(new LiveMonitoringHelper(app), LiveMonitoringHelper.class);
		app.selectedGpxHelper = startupInit(new GpxSelectionHelper(app, app.savingTrackHelper), GpxSelectionHelper.class);
		app.favorites = startupInit(new FavouritesDbHelper(app), FavouritesDbHelper.class);
		app.waypointHelper = startupInit(new WaypointHelper(app), WaypointHelper.class);
		app.regions = startupInit(new OsmandRegions(), OsmandRegions.class);
		String lang = osmandSettings.PREFERRED_LOCALE.get();
		String clang = "".equals(lang) ? new Locale(lang).getLanguage() : lang;
		app.regions.setLocale(clang);
		app.poiFilters = startupInit(new PoiFiltersHelper(app), PoiFiltersHelper.class);
		app.rendererRegistry = startupInit(new RendererRegistry(app), RendererRegistry.class);
		app.targetPointsHelper = startupInit(new TargetPointsHelper(app), TargetPointsHelper.class);
	}



	private <T> T startupInit(T object, Class<T> class1) {
		long t = System.currentTimeMillis();
		if(t - startTime > 7) {
			System.err.println("Startup service " + class1.getName() + " took too long " + (t - startTime)  + " ms");
		}
		startTime = t;
		return object;
	}



	public net.osmand.router.RoutingConfiguration.Builder getLazyDefaultRoutingConfig() {
		long tm = System.currentTimeMillis();
		try {
			File routingXml = app.getAppPath(IndexConstants.ROUTING_XML_FILE);
			if (routingXml.exists() && routingXml.canRead()) {
				try {
					return RoutingConfiguration.parseFromInputStream(new FileInputStream(routingXml));
				} catch (XmlPullParserException e) {
					throw new IllegalStateException(e);
				} catch (IOException e) {
					throw new IllegalStateException(e);
				}
			} else {
				return RoutingConfiguration.getDefault();
			}
		} finally {
			long te = System.currentTimeMillis();
			if(te - tm > 30) {
				System.err.println("Defalt routing config init took " + (te - tm) + " ms");
			}
		}
	}
	



	public void initVoiceDataInDifferentThread(final Activity uiContext, final String voiceProvider, final Runnable run, boolean showDialog) {
		final ProgressDialog dlg = showDialog ? ProgressDialog.show(uiContext, app.getString(R.string.loading_data),
				app.getString(R.string.voice_data_initializing)) : null;
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					if (app.player != null) {
						app.player.clear();
					}
					app.player = CommandPlayerFactory.createCommandPlayer(voiceProvider, app, uiContext);
					app.getRoutingHelper().getVoiceRouter().setPlayer(app.player);
					if(dlg != null) {
						dlg.dismiss();
					}
					if (run != null && uiContext != null) {
						uiContext.runOnUiThread(run);
					}
				} catch (CommandPlayerException e) {
					if(dlg != null) {
						dlg.dismiss();
					}
					app.showToastMessage(e.getError());
				}
			}
		}).start();
	}
	
	private void startApplicationBackground() {
		try {
			startBgTime = System.currentTimeMillis();
			app.favorites.loadFavorites();
			notifyEvent(InitEvents.FAVORITES_INITIALIZED);
			// init poi types before indexes and before POI
			initPoiTypes();
			notifyEvent(InitEvents.POI_TYPES_INITIALIZED);
			app.resourceManager.reloadIndexesOnStart(this, warnings);
			
			app.getRendererRegistry().initRenderers(this);
			notifyEvent(InitEvents.INIT_RENDERERS);
			// native depends on renderers
			initNativeCore();
			notifyEvent(InitEvents.NATIVE_INITIALIZED);

			app.poiFilters.reloadAllPoiFilters();
			notifyEvent(InitEvents.POI_TYPES_INITIALIZED);			
			indexRegionsBoundaries(warnings);
			notifyEvent(InitEvents.INDEX_REGION_BOUNDARIES);
			app.selectedGpxHelper.loadGPXTracks(this);
			notifyEvent(InitEvents.LOAD_GPX_TRACKS);
			saveGPXTracks();
			notifyEvent(InitEvents.SAVE_GPX_TRACKS);
			// restore backuped favorites to normal file
			restoreBackupForFavoritesFiles();
			notifyEvent(InitEvents.RESTORE_BACKUPS);
		} catch (RuntimeException e) {
			e.printStackTrace();
			warnings.add(e.getMessage());
		} finally {
			appInitializing = false;
			notifyFinish();
			if (warnings != null && !warnings.isEmpty()) {
				app.showToastMessage(formatWarnings(warnings).toString());
			}
		}
	}


	private void restoreBackupForFavoritesFiles() {
		final File appDir = app.getAppPath(null);
		File save = new File(appDir, FavouritesDbHelper.FILE_TO_SAVE);
		File bak = new File(appDir, FavouritesDbHelper.FILE_TO_BACKUP);
		if (bak.exists() && (!save.exists() || bak.lastModified() > save.lastModified())) {
			if (save.exists()) {
				save.delete();
			}
			bak.renameTo(save);
		}
	}


	private void saveGPXTracks() {
		if (app.savingTrackHelper.hasDataToSave()) {
			long timeUpdated = app.savingTrackHelper.getLastTrackPointTime();
			if (System.currentTimeMillis() - timeUpdated >= 1000 * 60 * 30) {
				startTask(app.getString(R.string.saving_gpx_tracks), -1);
				try {
					warnings.addAll(app.savingTrackHelper.saveDataToGpx(app.getAppCustomization().getTracksDir()));
				} catch (RuntimeException e) {
					warnings.add(e.getMessage());
				}
			} else {
				app.savingTrackHelper.loadGpxFromDatabase();
			}
		}
		if(app.getSettings().SAVE_GLOBAL_TRACK_TO_GPX.get()){
			app.startNavigationService(NavigationService.USED_BY_GPX);
		}
	}


	private void initNativeCore() {
		if (!"qnx".equals(System.getProperty("os.name"))) {
			OsmandSettings osmandSettings = app.getSettings();
			if (osmandSettings.USE_OPENGL_RENDER.get()) {
				boolean success = false;
				if (!osmandSettings.OPENGL_RENDER_FAILED.get()) {
					osmandSettings.OPENGL_RENDER_FAILED.set(true);
					success = NativeCoreContext.tryCatchInit(app);
					if (success) {
						osmandSettings.OPENGL_RENDER_FAILED.set(false);
					}
				}
				if (!success) {
					// try next time once again ?
					osmandSettings.OPENGL_RENDER_FAILED.set(false);
					warnings.add("Native OpenGL library is not supported. Please try again after exit");
				}
			}
			if (osmandSettings.NATIVE_RENDERING_FAILED.get()) {
				osmandSettings.SAFE_MODE.set(true);
				osmandSettings.NATIVE_RENDERING_FAILED.set(false);
				warnings.add(app.getString(R.string.native_library_not_supported));
			} else {
				osmandSettings.SAFE_MODE.set(false);
				osmandSettings.NATIVE_RENDERING_FAILED.set(true);
				startTask(app.getString(R.string.init_native_library), -1);
				RenderingRulesStorage storage = app.getRendererRegistry().getCurrentSelectedRenderer();
				boolean initialized = NativeOsmandLibrary.getLibrary(storage, app) != null;
				osmandSettings.NATIVE_RENDERING_FAILED.set(false);
				if (!initialized) {
					LOG.info("Native library could not be loaded!");
				}
			}
			app.getResourceManager().initMapBoundariesCacheNative();
		}
	}
	

	private StringBuilder formatWarnings(List<String> warnings) {
		final StringBuilder b = new StringBuilder();
		boolean f = true;
		for (String w : warnings) {
			if (f) {
				f = false;
			} else {
				b.append('\n');
			}
			b.append(w);
		}
		return b;
	}


	public void notifyFinish() {
		app.uiHandler.post(new Runnable() {
			
			@Override
			public void run() {
				for(AppInitializeListener l : listeners) {
					l.onFinish(AppInitializer.this);
				}
			}
		});
	}
	public void notifyEvent(final InitEvents event) {
		if (event != InitEvents.TASK_CHANGED) {
			long time = System.currentTimeMillis();
			System.out.println("Initialized " + event + " in " + (time - startBgTime) + " ms");
			startBgTime = time;
		}
		app.uiHandler.post(new Runnable() {
			
			@Override
			public void run() {
				for(AppInitializeListener l : listeners) {
					l.onProgress(AppInitializer.this, event);			
				}
			}
		});

	}


	@Override
	public void startTask(String taskName, int work) {
		this.taskName = taskName;
		notifyEvent(InitEvents.TASK_CHANGED);
	}


	@Override
	public void startWork(int work) {
	}


	@Override
	public void progress(int deltaWork) {
	}


	@Override
	public void remaining(int remainingWork) {
	}


	@Override
	public void finishTask() {
		taskName = null;
		notifyEvent(InitEvents.TASK_CHANGED);
	}
	
	public String getCurrentInitTaskName() {
		return taskName;
	}


	@Override
	public boolean isIndeterminate() {
		return true;
	}


	@Override
	public boolean isInterrupted() {
		return false;
	}


	private boolean applicationBgInitializing = false;
	

	public synchronized void startApplication() {
		if (applicationBgInitializing) {
			return;
		}
		applicationBgInitializing = true;
		new Thread(new Runnable() { //$NON-NLS-1$
					@Override
					public void run() {
						try {
							startApplicationBackground();
						} finally {
							applicationBgInitializing = false;
						}
					}
				}, "Initializing app").start();
		;

	}


	public void addListener(AppInitializeListener listener) {
		this.listeners.add(listener);
		if(!appInitializing) {
			listener.onFinish(this);
		}
	}


	public void removeListener(AppInitializeListener listener) {
		this.listeners.remove(listener);
	}
}
