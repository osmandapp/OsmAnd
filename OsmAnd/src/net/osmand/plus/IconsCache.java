package net.osmand.plus;

import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;

import gnu.trove.map.hash.TLongObjectHashMap;

public class IconsCache {

	private TLongObjectHashMap<Drawable> drawable = new TLongObjectHashMap<Drawable>();
	private OsmandApplication app;
	
	public IconsCache(OsmandApplication app) {
		this.app = app;
	}
	
	
	private Drawable getDrawable(int resId, int clrId) {
		long hash = ((long)resId << 31l) + clrId;
		Drawable d = drawable.get(hash);
		if(d == null) {
			d = app.getResources().getDrawable(resId).mutate();
			d.clearColorFilter();
			if (clrId != 0) {
				d.setColorFilter(app.getResources().getColor(clrId), PorterDuff.Mode.MULTIPLY);
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
			d.setColorFilter(color, PorterDuff.Mode.MULTIPLY);
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


	public Drawable getContentIcon(int id) {
		return getDrawable(id, app.getSettings().isLightContent() ? R.color.icon_color_light : 0);
	}


	public Drawable getIcon(int id) {
		return getDrawable(id, 0);
	}
	
	public Drawable getIcon(int id, boolean light) {
		return getDrawable(id, light ? R.color.icon_color_light : 0);
	}

}
