package net.osmand.plus.views.corenative;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.IndexConstants;
import net.osmand.core.android.CoreResourcesFromAndroidAssets;
import net.osmand.core.android.MapRendererContext;
import net.osmand.core.android.NativeCore;
import net.osmand.core.jni.Logger;
import net.osmand.core.jni.MapStylesCollection;
import net.osmand.core.jni.ObfsCollection;
import net.osmand.core.jni.QIODeviceLogSink;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.Version;
import net.osmand.plus.inapp.InAppPurchaseHelper;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.openseamaps.NauticalMapsPlugin;
import net.osmand.plus.plugins.srtm.SRTMPlugin;

import java.io.File;

/**
 * Created by Denis on 01.10.2014.
 */
public class NativeCoreContext {

	private static boolean init;

	private static MapRendererContext mapRendererContext;

	public static boolean isInit() {
		return init;
	}

	public static void init(@NonNull OsmandApplication app) {
		if (!init && NativeCore.isAvailable() && !Version.isQnxOperatingSystem()) {
			if (!NativeCore.isLoaded())
				NativeCore.load(CoreResourcesFromAndroidAssets.loadFromCurrentApplication(app));
			if (NativeCore.isLoaded()) {
				File directory = app.getAppPath("");
				Logger.get().addLogSink(QIODeviceLogSink.createFileLogSink(
						directory.getAbsolutePath() + "/osmandcore.log"));

				WindowManager mgr = (WindowManager) app.getSystemService(Context.WINDOW_SERVICE);
				DisplayMetrics dm = new DisplayMetrics();
				mgr.getDefaultDisplay().getMetrics(dm);

				ObfsCollection obfsCollection = new ObfsCollection();
				obfsCollection.setIndexCacheFile(app.getCacheDir().getAbsolutePath() + "/ind_core.cache");
				obfsCollection.addDirectory(directory.getAbsolutePath(), false);
				obfsCollection.addDirectory(app.getAppPath(IndexConstants.ROADS_INDEX_DIR).getAbsolutePath(), false);
				obfsCollection.addDirectory(app.getAppPath(IndexConstants.LIVE_INDEX_DIR).getAbsolutePath(), false);

				if (PluginsHelper.isActive(NauticalMapsPlugin.class) ||	InAppPurchaseHelper.isDepthContoursPurchased(app)) {
					File nauticalIndexDir = app.getAppPath(IndexConstants.NAUTICAL_INDEX_DIR);
					if (!nauticalIndexDir.exists()) {
						nauticalIndexDir.mkdir();
					}			
					obfsCollection.addDirectory(nauticalIndexDir.getAbsolutePath(), false);
				}
				if (PluginsHelper.isActive(SRTMPlugin.class) ||	InAppPurchaseHelper.isContourLinesPurchased(app)) {
					File srtmIndexDir = app.getAppPath(IndexConstants.SRTM_INDEX_DIR);
					if (!srtmIndexDir.exists()) {
						srtmIndexDir.mkdir();
					}			
					obfsCollection.addDirectory(srtmIndexDir.getAbsolutePath(), false);
				}

				mapRendererContext = new MapRendererContext(app, dm.density);
				mapRendererContext.setupObfMap(new MapStylesCollection(), obfsCollection);
				init = true;
			}
		}
	}

	@Nullable
	public static MapRendererContext getMapRendererContext() {
		return mapRendererContext;
	}
}
