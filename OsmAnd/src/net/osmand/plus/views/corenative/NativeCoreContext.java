package net.osmand.plus.views.corenative;

import java.io.File;

import android.content.Context;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;
import net.osmand.core.android.CoreResourcesFromAndroidAssetsCustom;
import net.osmand.core.android.MapRendererContext;
import net.osmand.core.android.NativeCore;
import net.osmand.core.jni.Logger;
import net.osmand.core.jni.MapStylesCollection;
import net.osmand.core.jni.ObfsCollection;
import net.osmand.core.jni.QIODeviceLogSink;
import net.osmand.plus.OsmandApplication;

/**
 * Created by Denis on 01.10.2014.
 */
public class NativeCoreContext {
	private static final String TAG = "NativeCoreContext";
	
	private static boolean init;
	
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
				NativeCore.load(CoreResourcesFromAndroidAssetsCustom.loadFromCurrentApplication(app));
			if (NativeCore.isLoaded()) {
				
				File directory = app.getAppPath("");
				Logger.get().addLogSink(QIODeviceLogSink.createFileLogSink(
						directory.getAbsolutePath() + "osmandcore.log"));
				
				WindowManager mgr = (WindowManager)app.getSystemService(Context.WINDOW_SERVICE);
				DisplayMetrics dm = new DisplayMetrics();
				mgr.getDefaultDisplay().getMetrics(dm);

				// Get device display density factor
//				DisplayMetrics displayMetrics = new DisplayMetrics();
//				act.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
				DisplayMetrics displayMetrics = app.getResources().getDisplayMetrics();
				// TODO getSettings().getSettingsZoomScale() + Math.sqrt(Math.max(0, getDensity() - 1))
				float scaleCoefficient = displayMetrics.density;
				if (Math.min(dm.widthPixels / (dm.density * 160), dm.heightPixels / (dm.density * 160)) > 2.5f) {
					// large screen
					scaleCoefficient *= 1.5f;
				}
				float displayDensityFactor = scaleCoefficient;
				
				_obfsCollection = new ObfsCollection();
				_obfsCollection.addDirectory(directory.getAbsolutePath(), false);
				
				_mapStylesCollection = new MapStylesCollection();
				
				_mapRendererContext = new MapRendererContext();
				_mapRendererContext.setDisplayDensityFactor(displayDensityFactor);
				_mapRendererContext.setupObfMap(
						_mapStylesCollection.getResolvedStyleByName("default"),
						_obfsCollection);
				
				init = true;
			}
		}
	}
	
	private static MapStylesCollection _mapStylesCollection;
	
	private static ObfsCollection _obfsCollection;
	
	private static MapRendererContext _mapRendererContext;
	
	public static MapRendererContext getMapRendererContext() {
		return _mapRendererContext;
	}
}
