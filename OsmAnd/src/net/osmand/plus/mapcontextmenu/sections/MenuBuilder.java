package net.osmand.plus.mapcontextmenu.sections;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;

import net.osmand.plus.IconsCache;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.render.RenderingIcons;

public abstract class MenuBuilder {

	public static final float SHADOW_HEIGHT_TOP_DP = 16f;
	public static final float SHADOW_HEIGHT_BOTTOM_DP = 6f;

	protected OsmandApplication app;

	public MenuBuilder(OsmandApplication app) {
		this.app = app;
	}

	public abstract void build(View view);

	public Drawable getRowIcon(int iconId) {
		IconsCache iconsCache = app.getIconsCache();
		boolean light = app.getSettings().isLightContent();
		return iconsCache.getIcon(iconId,
				light ? R.color.icon_color : R.color.icon_color_light);
	}

	public Drawable getRowIcon(Context ctx, String fileName) {
		Bitmap iconBitmap = RenderingIcons.getIcon(ctx, fileName, false);
		if (iconBitmap != null) {
			return new BitmapDrawable(ctx.getResources(), iconBitmap);
		} else {
			return null;
		}
	}
}
