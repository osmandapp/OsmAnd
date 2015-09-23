package net.osmand.plus;

import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

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

	private Drawable getDrawable(int resId, int clrId) {
		return getDrawable(resId, clrId, 0);
	}

	private Drawable getDrawable(int resId, int clrId, float scale) {
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

	private Drawable getPaintedDrawable(int resId, int color){
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

	public Drawable getPaintedContentIcon(int id, int color){
		return getPaintedDrawable(id, color);
	}

	public Drawable getIcon(int id, int colorId) {
		return getDrawable(id, colorId);
	}

	public Drawable getIcon(int id, int colorId, float scale) {
		return getDrawable(id, colorId, scale);
	}

	public Drawable getContentIcon(int id) {
		return getDrawable(id, app.getSettings().isLightContent() ? R.color.icon_color : 0);
	}

	public Drawable getIcon(int id) {
		return getDrawable(id, 0);
	}
	
	public Drawable getIcon(int id, boolean light) {
		return getDrawable(id, light ? R.color.icon_color : 0);
	}

}
