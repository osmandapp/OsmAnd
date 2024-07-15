package net.osmand.plus.mapcontextmenu.editors.icon.data;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

import net.osmand.osm.PoiType;
import net.osmand.plus.render.RenderingIcons;

public class IconSearchResult {

	private final String iconKey;
	private final String iconName;
	@DrawableRes
	private final int iconId;
	private final String categoryName;

	public IconSearchResult(@NonNull String iconKey, @NonNull PoiType poiType) {
		this.iconKey = iconKey;
		this.iconName = poiType.getTranslation();
		this.iconId = RenderingIcons.getBigIconResourceId(iconKey);
		this.categoryName = poiType.getCategory().getTranslation();
	}

	@NonNull
	public String getIconKey() {
		return iconKey;
	}

	@NonNull
	public String getIconName() {
		return iconName;
	}

	@DrawableRes
	public int getIconId() {
		return iconId;
	}

	@NonNull
	public String getCategoryName() {
		return categoryName;
	}
}
