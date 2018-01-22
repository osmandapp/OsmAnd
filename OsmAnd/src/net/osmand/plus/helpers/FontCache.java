package net.osmand.plus.helpers;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import android.content.Context;
import android.graphics.Typeface;
import android.util.Log;

public class FontCache {
	private static final String TAG = "FontCache";
    private static Map<String, Typeface> fontMap = new ConcurrentHashMap<String, Typeface>();
    public static final String ROBOTO_MEDIUM = "fonts/Roboto-Medium.ttf";
    public static final String ROBOTO_REGULAR = "fonts/Roboto-Regular.ttf";
    
    public static Typeface getRobotoMedium(Context context) {
    	return getFont(context, ROBOTO_MEDIUM);
    }

    public static Typeface getRobotoRegular(Context context) {
    	return getFont(context, ROBOTO_REGULAR);
	}
 
	public static Typeface getFont(Context context, String fontName) {
		Typeface typeface = fontMap.get(fontName);
		if (typeface != null)
			return typeface;

		try {
			typeface = Typeface.createFromAsset(context.getAssets(), fontName);
		} catch(Exception e) {
			Log.e(TAG, "Failed to create typeface from asset '" + fontName + "'", e);
			return null;
		}
		if (typeface == null)
			return null;
		fontMap.put(fontName, typeface);
		return typeface;
	}
}
