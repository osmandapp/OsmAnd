package net.osmand.plus.helpers;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import android.content.Context;
import android.graphics.Typeface;

public class FontCache {
    private static Map<String, Typeface> fontMap = new ConcurrentHashMap<String, Typeface>();
    public static final String ROBOTO_MEDIUM = "fonts/Roboto-Medium.ttf";
    public static final String ROBOTO_REGULAR = "fonts/Roboto-Regular.ttf";
    
    public static Typeface getRobotoMedium(Context context) {
    	return getFont(context, ROBOTO_MEDIUM);
    }
 
	public static Typeface getFont(Context context, String fontName) {
		if (fontMap.containsKey(fontName)) {
			return fontMap.get(fontName);
		} else {
			Typeface tf = Typeface.createFromAsset(context.getAssets(), fontName);
			fontMap.put(fontName, tf);
			return tf;
		}
	}
}