package net.osmand.plus.views.corenative;

import android.util.DisplayMetrics;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.IndexConstants;
import net.osmand.core.android.CoreResourcesFromAndroidAssets;
import net.osmand.core.android.MapRendererContext;
import net.osmand.core.android.MapRendererContext.ProviderType;
import net.osmand.core.android.NativeCore;
import net.osmand.core.jni.Logger;
import net.osmand.core.jni.MapStylesCollection;
import net.osmand.core.jni.ObfsCollection;
import net.osmand.core.jni.QIODeviceLogSink;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.Version;
import net.osmand.plus.inapp.InAppPurchaseUtils;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.openseamaps.NauticalMapsPlugin;
import net.osmand.plus.plugins.srtm.SRTMPlugin;
import net.osmand.plus.utils.AndroidUtils;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Denis on 01.10.2014.
 */
public class NativeCoreContext {

	private static final String LOG_FILE_NAME = "osmandcore.log";
	private static final String CACHE_FILE_NAME = "ind_core.cache";

	private static boolean init;

	private static Map<ProviderType, ObfsCollection> obfsCollectionsByProviderType;
	private static MapRendererContext mapRendererContext;

	public static boolean isInit() {
		return init;
	}

	public static void init(@NonNull OsmandApplication app) {
		if (!init && Version.isOpenGlAvailable(app)) {
			if (!NativeCore.isLoaded()) {
				CoreResourcesFromAndroidAssets assets = CoreResourcesFromAndroidAssets.loadFromCurrentApplication(app);
				File fontDir = app.getAppPath(IndexConstants.FONT_INDEX_DIR);
				if (fontDir.exists())
					NativeCore.load(assets, fontDir.getAbsolutePath());
				else
					NativeCore.load(assets, "");
			}
			if (NativeCore.isLoaded()) {
				File directory = app.getAppPath("");
				Logger.get().addLogSink(QIODeviceLogSink.createFileLogSink(new File(directory, LOG_FILE_NAME).getAbsolutePath()));

				DisplayMetrics dm = new DisplayMetrics();
				AndroidUtils.getDisplay(app).getMetrics(dm);

				String cacheFilePath = new File(app.getCacheDir(), CACHE_FILE_NAME).getAbsolutePath();

				obfsCollectionsByProviderType = new HashMap<>();

				ObfsCollection obfsCollection = new ObfsCollection();
				obfsCollection.setIndexCacheFile(cacheFilePath);
				obfsCollection.addDirectory(directory.getAbsolutePath(), false);
				obfsCollection.addDirectory(app.getAppInternalPath(IndexConstants.HIDDEN_DIR).getAbsolutePath(), false);
				obfsCollection.addDirectory(app.getAppPath(IndexConstants.ROADS_INDEX_DIR).getAbsolutePath(), false);
				obfsCollection.addDirectory(app.getAppPath(IndexConstants.LIVE_INDEX_DIR).getAbsolutePath(), false);
				if (app.getSettings().SHOW_TRAVEL.get()) {
					obfsCollection.addDirectory(app.getAppPath(IndexConstants.WIKIVOYAGE_INDEX_DIR).getAbsolutePath(), false);
				}
				obfsCollectionsByProviderType.put(ProviderType.MAIN, obfsCollection);

				ObfsCollection contourLinesObfsCollection = null;

				if (PluginsHelper.isActive(NauticalMapsPlugin.class) || InAppPurchaseUtils.isDepthContoursAvailable(app)) {
					File nauticalIndexDir = app.getAppPath(IndexConstants.NAUTICAL_INDEX_DIR);
					if (!nauticalIndexDir.exists()) {
						nauticalIndexDir.mkdir();
					}
					obfsCollection.addDirectory(nauticalIndexDir.getAbsolutePath(), false);
				}
				if (PluginsHelper.isActive(SRTMPlugin.class) || InAppPurchaseUtils.isContourLinesAvailable(app)) {
					File srtmIndexDir = app.getAppPath(IndexConstants.SRTM_INDEX_DIR);
					if (!srtmIndexDir.exists()) {
						srtmIndexDir.mkdir();
					}
					obfsCollection.addDirectory(srtmIndexDir.getAbsolutePath(), false);

					contourLinesObfsCollection = new ObfsCollection();
					contourLinesObfsCollection.setIndexCacheFile(cacheFilePath);
					contourLinesObfsCollection.addDirectory(srtmIndexDir.getAbsolutePath(), false);
					obfsCollectionsByProviderType.put(ProviderType.CONTOUR_LINES, contourLinesObfsCollection);
				}

				setMapRendererContext(app, dm.density);
				init = true;
			}
		}
	}

	@Nullable
	public static MapRendererContext getMapRendererContext() {
		return mapRendererContext;
	}

	public static void setMapRendererContext(@NonNull OsmandApplication app, float density) {
		if (mapRendererContext != null) {
			if (mapRendererContext.getDensity() == density)
				return;
			mapRendererContext.setMapRendererView(null);
		}
		mapRendererContext = new MapRendererContext(app, density);
		mapRendererContext.setupObfMap(new MapStylesCollection(), obfsCollectionsByProviderType);
	}
}
