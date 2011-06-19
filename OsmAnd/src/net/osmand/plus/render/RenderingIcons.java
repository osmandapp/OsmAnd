package net.osmand.plus.render;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

import net.osmand.plus.R;
import net.osmand.plus.R.drawable;

public class RenderingIcons {
	
	private static Map<String, Integer> icons = new LinkedHashMap<String, Integer>();
	
	public static Map<String, Integer> getIcons(){
		if(icons.isEmpty()){
			initIcons();
		}
		return icons;
	}

	public static void initIcons() {

		Class<? extends drawable> cl = R.drawable.class;
		for (Field f : cl.getDeclaredFields()) {
			if (f.getName().startsWith("h_") || f.getName().startsWith("g_")) {
				try {
					icons.put(f.getName().substring(2), f.getInt(null));
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
