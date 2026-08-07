package net.osmand.plus.mapcontextmenu.builders.rows.behaviour

import android.content.Context
import net.osmand.data.AmenityRowData
import net.osmand.osm.PoiType
import net.osmand.plus.OsmandApplication
import net.osmand.plus.mapcontextmenu.MenuBuilder
import net.osmand.plus.mapcontextmenu.builders.rows.PoiAdditionalUiRule

data class PoiRowParams(
	val app: OsmandApplication,
	val context: Context,
	val builder: AmenityRowData.Builder,
	val menuBuilder: MenuBuilder,
	val poiType: PoiType?,
	val rule: PoiAdditionalUiRule,
	val key: String,
	val value: String,
	val subtype: String?
)