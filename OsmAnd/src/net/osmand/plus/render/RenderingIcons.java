package net.osmand.plus.render;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.drawable.DrawableCompat;

import net.osmand.PlatformUtil;
import net.osmand.data.Amenity;
import net.osmand.osm.PoiType;
import net.osmand.plus.R;
import net.osmand.plus.R.drawable;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

public class RenderingIcons {

	private static final Log log = PlatformUtil.getLog(RenderingIcons.class);

	private static final Map<String, Integer> shaderIcons = new LinkedHashMap<>();
	private static final Map<String, Integer> smallIcons = new LinkedHashMap<>();
	private static final Map<String, Integer> bigIcons = new LinkedHashMap<>();
	private static final Map<String, Bitmap> iconsBmp = new LinkedHashMap<>();
	private static final Map<String, Drawable> iconsDrawable = new LinkedHashMap<>();

	private static Bitmap cacheBmp;
	private static final String defaultPoiIconName = "craft_default";

	public static boolean containsSmallIcon(String s) {
		return smallIcons.containsKey(s);
	}

	public static boolean containsBigIcon(String s) {
		return bigIcons.containsKey(s);
	}

	public static synchronized Bitmap getBitmapFromVectorDrawable(@NonNull Context context, int drawableId) {
		return getBitmapFromVectorDrawable(context, drawableId, 1f);
	}

	public static synchronized Bitmap getBitmapFromVectorDrawable(@NonNull Context context, int drawableId, float scale) {
		Drawable drawable = AppCompatResources.getDrawable(context, drawableId);
		if (drawable == null) {
			return null;
		}
		int width = (int) (drawable.getIntrinsicWidth() * scale);
		int height = (int) (drawable.getIntrinsicHeight() * scale);
		if (cacheBmp == null || cacheBmp.getWidth() != width || cacheBmp.getHeight() != height) {
			cacheBmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		}
		cacheBmp.eraseColor(Color.TRANSPARENT);
		Canvas canvas = new Canvas(cacheBmp);
		drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
		drawable.draw(canvas);
		return cacheBmp;
	}

	private static synchronized byte[] getPngFromVectorDrawable(@NonNull Context context, int drawableId) {
		Bitmap bmp = getBitmapFromVectorDrawable(context, drawableId);
		if (bmp == null) {
			return null;
		}
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		bmp.compress(Bitmap.CompressFormat.PNG, 100, baos);
		return baos.toByteArray();
	}

	public static byte[] getIconRawData(Context ctx, String s) {
		Integer resId = shaderIcons.get(s);
		if (resId == null) {
			resId = smallIcons.get(s);
		}
		if (resId == null) {
			return null;
		}
		try {
			InputStream inputStream = ctx.getResources().openRawResource(resId);
			ByteArrayOutputStream proxyOutputStream = new ByteArrayOutputStream(1024);
			byte[] ioBuffer = new byte[1024];
			int bytesRead;
			while ((bytesRead = inputStream.read(ioBuffer)) >= 0) {
				proxyOutputStream.write(ioBuffer, 0, bytesRead);
			}
			inputStream.close();
			byte[] bitmapData = proxyOutputStream.toByteArray();
			if (isVectorData(bitmapData)) {
				return getPngFromVectorDrawable(ctx, resId);
			}
			return bitmapData;
		} catch (Throwable e) {
			log.error("Failed to get byte stream from icon", e);
			return null;
		}
	}

	private static boolean isVectorData(byte[] bitmapData) {
		for (int i = 0; i < bitmapData.length - 8 && i < 32; i++) {
			if (bitmapData[i] == 'P' && bitmapData[i + 1] == 'N' && bitmapData[i + 2] == 'G') {
				return false;
			}
		}
		return bitmapData.length > 4 && bitmapData[0] == 3 && bitmapData[1] == 0
				&& bitmapData[2] == 8 && bitmapData[3] == 0;
	}

	@Nullable
	public static String getIconNameForAmenity(@NonNull Amenity amenity) {
		String subType = amenity.getSubType();
		String[] subtypes = subType.split(";");//multivalued
		PoiType poiType = amenity.getType().getPoiTypeByKeyName(subtypes[0]);
		return poiType != null ? getIconNameForPoiType(poiType) : null;
	}

	@Nullable
	public static String getIconNameForPoiType(@NonNull PoiType poiType) {
		if (containsSmallIcon(poiType.getIconKeyName())) {
			return poiType.getIconKeyName();
		} else if (containsSmallIcon(poiType.getOsmTag() + "_" + poiType.getOsmValue())) {
			return poiType.getOsmTag() + "_" + poiType.getOsmValue();
		}

		String iconName = null;
		if (poiType.getParentType() != null) {
			iconName = poiType.getParentType().getIconKeyName();
		} else if (poiType.getFilter() != null) {
			iconName = poiType.getFilter().getIconKeyName();
		} else if (poiType.getCategory() != null) {
			iconName = poiType.getCategory().getIconKeyName();
		}
		if (containsSmallIcon(iconName)) {
			return iconName;
		}
		return defaultPoiIconName;
	}

	@Nullable
	public static String getBigIconNameForAmenity(@NonNull Amenity amenity) {
		PoiType poiType = amenity.getType().getPoiTypeByKeyName(amenity.getSubType());
		if (poiType == null) {
			return null;
		} else if (containsBigIcon(poiType.getIconKeyName())) {
			return poiType.getIconKeyName();
		} else if (containsBigIcon(poiType.getOsmTag() + "_" + poiType.getOsmValue())) {
			return poiType.getOsmTag() + "_" + poiType.getOsmValue();
		}
		return null;
	}

	public static int getBigIconResourceId(String s) {
		Integer i = bigIcons.get(s);
		return i == null ? 0 : i;
	}

	public static Drawable getBigIcon(Context ctx, String s) {
		Integer resId = bigIcons.get(s);
		if (resId != null) {
			return AppCompatResources.getDrawable(ctx, resId);
		}
		return null;
	}

	/**
	 * Return vector icon as bitmap. Used for java rendering only.
	 *
	 * @deprecated Use getDrawableIcon instead.
	 */
	@Deprecated
	public static Bitmap getIcon(Context ctx, String s, boolean includeShader) {
		if (s == null) {
			return null;
		}
		if (includeShader && shaderIcons.containsKey(s)) {
			s = "h_" + s;
		}
		if (!iconsBmp.containsKey(s)) {
			Integer resId = s.startsWith("h_") ? shaderIcons.get(s.substring(2)) : smallIcons.get(s);
			if (resId != null) {
				Bitmap bmp = getBitmapFromVectorDrawable(ctx, resId);
				iconsBmp.put(s, bmp);
			} else {
				iconsBmp.put(s, null);
			}
		}
		return iconsBmp.get(s);
	}

	public static Drawable getDrawableIcon(Context ctx, String s, boolean includeShader) {
		if (s == null) {
			return null;
		}
		if (includeShader && shaderIcons.containsKey(s)) {
			s = "h_" + s;
		}
		Drawable d = iconsDrawable.get(s);
		if (d == null) {
			Integer drawableId = s.startsWith("h_") ? shaderIcons.get(s.substring(2)) : smallIcons.get(s);
			if (drawableId != null) {
				d = AppCompatResources.getDrawable(ctx, drawableId);
				if (d != null) {
					d = DrawableCompat.wrap(d);
					d.mutate();
					iconsDrawable.put(s, d);
				}
			}
		}
		return d;
	}

	@Nullable
	public static String getBigIconName(@NonNull Integer iconId) {
		for (String key : bigIcons.keySet()) {
			if (iconId.equals(bigIcons.get(key))) {
				return key;
			}
		}
		return null;
	}

	public static Integer getResId(@NonNull String id) {
		if (id.startsWith("mx_")) {
			return bigIcons.get(id.substring(3));
		} else if (id.startsWith("h_")) {
			return shaderIcons.get(id.substring(2));
		} else {
			return smallIcons.get(id);
		}
	}

	static {
		initIcons();
	}

	public static void initIcons() {
		Class<? extends drawable> cl = R.drawable.class;
		for (Field f : cl.getDeclaredFields()) {
			try {
				if (f.getName().startsWith("h_")) {
					shaderIcons.put(f.getName().substring(2), f.getInt(null));
				} else if (f.getName().startsWith("mm_")) {
					smallIcons.put(f.getName().substring(3), f.getInt(null));
				} else if (f.getName().startsWith("mx_")) {
					bigIcons.put(f.getName().substring(3), f.getInt(null));
				}
			} catch (IllegalArgumentException | IllegalAccessException e) {
				log.error(e);
			}
		}
	}

	public static int getPreselectedIconId(@NonNull Amenity amenity) {
		String gpxIconId = amenity.getGpxIcon();
		String preselectedIconName = Algorithms.isEmpty(gpxIconId) ? getIconNameForAmenity(amenity) : gpxIconId;
		return Algorithms.isEmpty(preselectedIconName) ? 0 : getBigIconResourceId(preselectedIconName);
	}
}