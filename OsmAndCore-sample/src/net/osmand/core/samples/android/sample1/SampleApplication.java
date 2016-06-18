package net.osmand.core.samples.android.sample1;

import android.app.Application;
import android.os.Environment;

import net.osmand.osm.AbstractPoiType;
import net.osmand.osm.MapPoiTypes;

import java.lang.reflect.Field;

public class SampleApplication extends Application
{
	private MapPoiTypes poiTypes;

	@Override
	public void onCreate()
	{
		super.onCreate();

		initPoiTypes();
	}

	public MapPoiTypes getPoiTypes() {
		return poiTypes;
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
}