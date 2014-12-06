package net.osmand.plus.views.corenative;

import java.io.File;

import android.content.Context;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;
import net.osmand.IndexConstants;
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
				NativeCore.load(CoreResourcesFromAndroidAssetsCustom.loadFromCurrentApplication(app));
			if (NativeCore.isLoaded()) {
				
				File directory = app.getAppPath("");
				Logger.get().addLogSink(QIODeviceLogSink.createFileLogSink(
						directory.getAbsolutePath() + "osmandcore.log"));
				
				WindowManager mgr = (WindowManager)app.getSystemService(Context.WINDOW_SERVICE);
				DisplayMetrics dm = new DisplayMetrics();
				mgr.getDefaultDisplay().getMetrics(dm);
				
				ObfsCollection obfsCollection = new ObfsCollection();
				obfsCollection.addDirectory(directory.getAbsolutePath(), false);
				
				MapStylesCollection mapStylesCollection = setupMapStyleCollection(app);
				mapRendererContext = new MapRendererContext(app, dm.density);
				mapRendererContext.setupObfMap(mapStylesCollection, obfsCollection);
				init = true;
			}
		}
	}

	private static MapStylesCollection setupMapStyleCollection(
			OsmandApplication app) {
		MapStylesCollection mapStylesCollection = new MapStylesCollection();
		// Alexey TODO
//		internalRenderers.put("Touring-view_(more-contrast-and-details)", "Touring-view_(more-contrast-and-details)" +".render.xml");
//		internalRenderers.put("UniRS", "UniRS" + ".render.xml");
//		internalRenderers.put("LightRS", "LightRS" + ".render.xml");
//		internalRenderers.put("High-contrast-roads", "High-contrast-roads" + ".render.xml");
//		internalRenderers.put("Winter-and-ski", "Winter-and-ski" + ".render.xml");
		File renderers = app.getAppPath(IndexConstants.RENDERERS_DIR);
		File[] lf = renderers.listFiles();
		if(lf != null) {
			for(File f : lf) {
				if(f.getName().endsWith(IndexConstants.RENDERER_INDEX_EXT)) {
					mapStylesCollection.addStyleFromFile(f.getAbsolutePath());
				}
			}
		}
		return mapStylesCollection;
	}

	
	public static MapRendererContext getMapRendererContext() {
		return mapRendererContext;
	}
}
