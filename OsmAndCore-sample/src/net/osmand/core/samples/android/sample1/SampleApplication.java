package net.osmand.core.samples.android.sample1;

import android.app.Application;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;

import net.osmand.core.android.CoreResourcesFromAndroidAssets;
import net.osmand.core.android.NativeCore;
import net.osmand.core.jni.LogSeverityLevel;
import net.osmand.core.jni.Logger;
import net.osmand.osm.AbstractPoiType;
import net.osmand.osm.MapPoiTypes;

import java.io.File;
import java.lang.reflect.Field;

public class SampleApplication extends Application
{
	private CoreResourcesFromAndroidAssets assetsCustom;
	private MapPoiTypes poiTypes;
	private IconsCache iconsCache;
	Handler uiHandler;

	@Override
	public void onCreate()
	{
		super.onCreate();
		uiHandler = new Handler();

		initPoiTypes();

		// Initialize native core
		if (NativeCore.isAvailable() && !NativeCore.isLoaded()) {
			assetsCustom = CoreResourcesFromAndroidAssets.loadFromCurrentApplication(this);
			NativeCore.load(assetsCustom);
		}
		Logger.get().setSeverityLevelThreshold(LogSeverityLevel.Debug);

		iconsCache = new IconsCache(assetsCustom);
	}

	public MapPoiTypes getPoiTypes() {
		return poiTypes;
	}

	public IconsCache getIconsCache() {
		return iconsCache;
	}

	private void initPoiTypes() {
		poiTypes = MapPoiTypes.getDefaultNoInit();
		poiTypes.init(Environment.getExternalStorageDirectory() + "/osmand/poi_types.xml");
		poiTypes.setPoiTranslator(new MapPoiTypes.PoiTranslator() {

			@Override
			public String getTranslation(AbstractPoiType type) {
				if(type.getBaseLangType() != null) {
					return getTranslation(type.getBaseLangType()) +  " (" + getLangTranslation(type.getLang()).toLowerCase() +")";
				}
				try {
					Field f = R.string.class.getField("poi_" + type.getIconKeyName());
					if (f != null) {
						Integer in = (Integer) f.get(null);
						return getString(in);
					}
				} catch (Exception e) {
					System.err.println("No translation for "+ type.getIconKeyName() + " " + e.getMessage());
				}
				return null;
			}
		});
	}

	public String getAbsoluteAppPath() {
		return Environment.getExternalStorageDirectory() + "/osmand";
	}

	public File getAppPath(String path) {
		if (path == null) {
			path = "";
		}
		return new File(getAbsoluteAppPath(), path);
	}

	public String getLangTranslation(String l) {
		try {
			java.lang.reflect.Field f = R.string.class.getField("lang_"+l);
			if (f != null) {
				Integer in = (Integer) f.get(null);
				return getString(in);
			}
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
		return l;
	}

	public void runInUIThread(Runnable run) {
		uiHandler.post(run);
	}

	public void runInUIThread(Runnable run, long delay) {
		uiHandler.postDelayed(run, delay);
	}
}