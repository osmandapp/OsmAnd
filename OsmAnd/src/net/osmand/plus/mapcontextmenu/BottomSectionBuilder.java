package net.osmand.plus.mapcontextmenu;

import android.graphics.drawable.Drawable;
import android.view.View;

import net.osmand.plus.IconsCache;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;

public abstract class BottomSectionBuilder {

	protected OsmandApplication app;

	public BottomSectionBuilder(OsmandApplication app) {
		this.app = app;
	}

	public abstract void buildSection(View view);

	public Drawable getRowIcon(int iconId) {
		IconsCache iconsCache = app.getIconsCache();
		boolean light = app.getSettings().isLightContent();
		return iconsCache.getIcon(iconId,
				light ? R.color.icon_color : R.color.icon_color_light);
	}

}
