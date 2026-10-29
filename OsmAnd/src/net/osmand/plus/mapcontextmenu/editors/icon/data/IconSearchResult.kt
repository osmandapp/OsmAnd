package net.osmand.plus.mapcontextmenu.editors.icon.data

import androidx.annotation.DrawableRes
import net.osmand.osm.PoiType
import net.osmand.plus.render.RenderingIcons

class IconSearchResult(
	val iconKey: String,
	val iconName: String,
	@DrawableRes val iconId: Int,
	val categoryKey: String,
	val categoryName: String
) {
	constructor(iconKey: String, poiType: PoiType) : this(
		iconKey = iconKey,
		iconName = poiType.translation,
		iconId = RenderingIcons.getBigIconResourceId(iconKey),
		categoryKey = poiType.category.keyName,
		categoryName = poiType.category.translation
	)
}
