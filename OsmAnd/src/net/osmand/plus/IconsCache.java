package net.osmand.plus;

import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;

import gnu.trove.map.hash.TLongObjectHashMap;

public class IconsCache {

	private TLongObjectHashMap<Drawable> drawable = new TLongObjectHashMap<Drawable>();
	private OsmandApplication app;
	
	public IconsCache(OsmandApplication app) {
		this.app = app;
	}

	public Drawable scaleImage(Drawable image, float scaleFactor) {
		if ((image == null) || !(image instanceof BitmapDrawable)) {
			return image;
		}
		Bitmap b = ((BitmapDrawable)image).getBitmap();

		int sizeX = Math.round(image.getIntrinsicWidth() * scaleFactor);
		int sizeY = Math.round(image.getIntrinsicHeight() * scaleFactor);

		Bitmap bitmapResized = Bitmap.createScaledBitmap(b, sizeX, sizeY, false);
		return new BitmapDrawable(app.getResources(), bitmapResized);
	}

	private Drawable getDrawable(@DrawableRes int resId, @ColorRes int clrId) {
		return getDrawable(resId, clrId, 0);
	}

	private Drawable getDrawable(@DrawableRes int resId, @ColorRes int clrId, float scale) {
		long hash = ((long)resId << 31l) + clrId + (int)(scale * 10000f);
		Drawable d = drawable.get(hash);
		if(d == null) {
			if (scale > 0) {
				d = scaleImage(app.getResources().getDrawable(resId).mutate(), scale);
			} else {
				d = app.getResources().getDrawable(resId).mutate();
			}
			d.clearColorFilter();
			if (clrId != 0) {
				d.setColorFilter(app.getResources().getColor(clrId), PorterDuff.Mode.SRC_IN);
			}
			drawable.put(hash, d);
		}
		return d;
	}

	private Drawable getPaintedDrawable(@DrawableRes int resId, @ColorInt int color){
		long hash = ((long)resId << 31l) + color;
		Drawable d = drawable.get(hash);
		if(d == null) {
			d = app.getResources().getDrawable(resId).mutate();
			d.clearColorFilter();
			d.setColorFilter(color, PorterDuff.Mode.SRC_IN);
			drawable.put(hash, d);
		}
		return d;
	}

	public Drawable getPaintedContentIcon(@DrawableRes int id, @ColorInt int color){
		return getPaintedDrawable(id, color);
	}

	public Drawable getIcon(@DrawableRes int id, @ColorRes int colorId) {
		return getDrawable(id, colorId);
	}

	public Drawable getIcon(@DrawableRes int id, @ColorRes int colorId, float scale) {
		return getDrawable(id, colorId, scale);
	}

	public Drawable getIcon(@DrawableRes int backgroundId, @DrawableRes int id, @ColorRes int colorId) {
		Drawable b = getDrawable(backgroundId, 0);
		Drawable f = getDrawable(id, colorId);
		Drawable[] layers = new Drawable[2];
		layers[0] = b;
		layers[1] = f;
		return new LayerDrawable(layers);
	}

	public Drawable getContentIcon(@DrawableRes int id) {
		return getDrawable(id, app.getSettings().isLightContent() ? R.color.icon_color : 0);
	}

	public Drawable getContentIcon(@DrawableRes int id, boolean isLightContent) {
		return getDrawable(id, isLightContent ? R.color.icon_color : 0);
	}

	public Drawable getIcon(@DrawableRes int id) {
		return getDrawable(id, 0);
	}
	
	public Drawable getIcon(@DrawableRes int id, boolean light) {
		return getDrawable(id, light ? R.color.icon_color : 0);
	}

}
