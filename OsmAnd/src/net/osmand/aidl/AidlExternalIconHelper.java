package net.osmand.aidl;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.util.Algorithms;

import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loads custom icons supplied by external apps as content:// URIs,
 * similar to how {@code AMapPoint.POINT_IMAGE_URI_PARAM} images are loaded
 * <p>
 * The external app must grant OsmAnd read access to the URI (e.g. a FileProvider
 * URI with {@code FLAG_GRANT_READ_URI_PERMISSION}). Results are cached by URI, so
 * the underlying stream is read only once per URI
 */
public class AidlExternalIconHelper {

	private static final Map<String, Drawable> ICON_CACHE = new ConcurrentHashMap<>();

	/**
	 * Returns a Drawable for the given content URI, or null if the URI is empty or
	 * cannot be read/decoded (caller should fall back to a resource icon)
	 */
	@Nullable
	public static Drawable getIconDrawable(@NonNull Context context, @Nullable String uriString) {
		if (Algorithms.isEmpty(uriString)) {
			return null;
		}
		Drawable cached = ICON_CACHE.get(uriString);
		if (cached != null) {
			return cached;
		}
		try {
			Uri uri = Uri.parse(uriString);
			InputStream is = context.getContentResolver().openInputStream(uri);
			if (is != null) {
				try {
					Bitmap bitmap = BitmapFactory.decodeStream(is);
					if (bitmap != null) {
						Drawable drawable = new BitmapDrawable(context.getResources(), bitmap);
						ICON_CACHE.put(uriString, drawable);
						return drawable;
					}
				} finally {
					is.close();
				}
			}
		} catch (Exception e) {
		}
		return null;
	}
}
