package net.osmand.plus.views.corenative;

import java.io.File;

import net.osmand.IndexConstants;
import net.osmand.core.android.CoreResourcesFromAndroidAssets;
import net.osmand.core.android.MapRendererContext;
import net.osmand.core.android.NativeCore;
import net.osmand.core.jni.Logger;
import net.osmand.core.jni.MapStylesCollection;
import net.osmand.core.jni.ObfsCollection;
import net.osmand.core.jni.QIODeviceLogSink;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.inapp.InAppPurchaseHelper;
import net.osmand.plus.srtmplugin.SRTMPlugin;
import android.content.Context;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

/**
 * Created by Denis on 01.10.2014.
 */
public class NativeCoreContext {
	private static final String TAG = "NativeCoreContext";
	
	private static boolean init;
	
	private static MapRendererContext mapRendererContext;
	
	public static boolean isInit() {
		return init;
	}
	
	public static boolean tryCatchInit(OsmandApplication app) {
		try {
			init(app);
			return true;
		} catch(Throwable t) {
			t.printStackTrace();
			Log.e(TAG, "Failed to initialize", t);
			return false;
		}
		
	}
	
	public static void init(OsmandApplication app) {
		if (!init && NativeCore.isAvailable()) {
			if (!NativeCore.isLoaded())
				NativeCore.load(CoreResourcesFromAndroidAssets.loadFromCurrentApplication(app));
			if (NativeCore.isLoaded()) {
				
				File directory = app.getAppPath("");
				Logger.get().addLogSink(QIODeviceLogSink.createFileLogSink(
						directory.getAbsolutePath() + "/osmandcore.log"));
				
				WindowManager mgr = (WindowManager)app.getSystemService(Context.WINDOW_SERVICE);
				DisplayMetrics dm = new DisplayMetrics();
				mgr.getDefaultDisplay().getMetrics(dm);
				
				ObfsCollection obfsCollection = new ObfsCollection();
				obfsCollection.addDirectory(directory.getAbsolutePath(), false);
				if (OsmandPlugin.getEnabledPlugin(SRTMPlugin.class) != null || InAppPurchaseHelper.isContourLinesPurchased(app)) {
					obfsCollection.addDirectory(app.getAppPath(IndexConstants.SRTM_INDEX_DIR).getAbsolutePath(), false);
				}

                mapRendererContext = new MapRendererContext(app, dm.density);
				mapRendererContext.setupObfMap(new MapStylesCollection(), obfsCollection);
                app.getRendererRegistry().setRendererLoadedEventListener(mapRendererContext);
				init = true;
			}
		}
	}

	public static MapRendererContext getMapRendererContext() {
		return mapRendererContext;
	}
}
