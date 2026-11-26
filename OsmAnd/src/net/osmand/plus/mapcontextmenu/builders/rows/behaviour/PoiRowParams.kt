package net.osmand.plus.mapcontextmenu.builders.rows.behaviour

import android.content.Context
import net.osmand.plus.OsmandApplication
import net.osmand.plus.mapcontextmenu.MenuBuilder
import net.osmand.plus.mapcontextmenu.builders.rows.PoiAdditionalUiRule
import net.osmand.shared.data.AdditionalInfoBundle
import net.osmand.shared.data.AmenityTagEntry
import net.osmand.shared.osm.PoiType

data class PoiRowParams(
	val app: OsmandApplication,
	val context: Context,
	val builder: AmenityTagEntry.Builder,
	val menuBuilder: MenuBuilder,
	val additionalInfo: AdditionalInfoBundle,
	val poiType: PoiType?,
	val rule: PoiAdditionalUiRule,
	val key: String,
	val value: String,
	val subtype: String?
)