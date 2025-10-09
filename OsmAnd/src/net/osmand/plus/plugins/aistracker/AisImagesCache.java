package net.osmand.plus.plugins.aistracker;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.views.layers.base.OsmandMapLayer;

import gnu.trove.map.hash.TLongObjectHashMap;

public class AisImagesCache {
	private final TLongObjectHashMap<Bitmap> bitmapCache = new TLongObjectHashMap<>();
	private final OsmandApplication app;

	public AisImagesCache(OsmandApplication app) {
		this.app = app;
	}

	@Nullable
	public Bitmap getBitmap(@DrawableRes int drawableId) {
		Bitmap bitmap = null;
		if (drawableId != 0) {
			float textScale = OsmandMapLayer.getTextScale(app);
			long key = ((long) drawableId << 32L) + (int)(textScale * 1000);
			bitmap = bitmapCache.get(key);
			if (bitmap == null) {
				Drawable icon = app.getUIUtilities().getIcon(drawableId);
				bitmap = AndroidUtils.drawableToBitmap(icon, textScale, true);
				bitmapCache.put(key, bitmap);
			}
		}
		return bitmap;
	}

	public synchronized void clearCache() {
		bitmapCache.clear();
	}
}
