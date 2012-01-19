package net.osmand.plus.render;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import net.osmand.plus.R;
import net.osmand.plus.R.drawable;

public class RenderingIcons {
	
	private static Map<String, Integer> icons = new LinkedHashMap<String, Integer>();
	private static Map<String, Bitmap> iconsBmp = new LinkedHashMap<String, Bitmap>();
	private static DisplayMetrics dm;
	
	public static boolean containsIcon(String s){
		return icons.containsKey(s);
	}
	
	public static Bitmap getIcon(Context ctx, String s){
		if(!iconsBmp.containsKey(s)){
			Integer resId = icons.get(s);
			if(resId != null){
				if(dm == null) {
					dm = new DisplayMetrics();
					WindowManager wmgr = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
					wmgr.getDefaultDisplay().getMetrics(dm);
				}
				Bitmap bmp = UnscaledBitmapLoader.loadFromResource(ctx.getResources(), resId.intValue(), null, dm);
				iconsBmp.put(s, bmp);
			} else {
				iconsBmp.put(s, null);
			}
		}
		return iconsBmp.get(s);
	}
	
	
	static {
		initIcons();
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
