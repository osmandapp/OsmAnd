package net.osmand.plus.mapcontextmenu.builders.rows

import android.content.Context
import net.osmand.osm.PoiType
import net.osmand.plus.OsmandApplication
import net.osmand.plus.mapcontextmenu.MenuBuilder
import net.osmand.plus.mapcontextmenu.builders.rows.AmenityInfoRow.Builder
import net.osmand.plus.mapcontextmenu.builders.rows.behaviour.DefaultPoiAdditionalRowBehaviour
import net.osmand.plus.mapcontextmenu.builders.rows.behaviour.IPoiAdditionalRowBehavior
import net.osmand.plus.mapcontextmenu.builders.rows.behaviour.PoiRowParams

class PoiAdditionalUiRule(
    val key: String,
    val customIconId: Int? = null,
    val customTextPrefixId: Int? = null,
    val isUrl: Boolean = false,
    val isWikipedia: Boolean = false,
    val isNeedLinks: Boolean = true,
    val isPhoneNumber: Boolean = false,
    val checkBaseKey: Boolean = true,
    val checkKeyOnContains: Boolean = false,
    var behavior: IPoiAdditionalRowBehavior = DefaultPoiAdditionalRowBehaviour()
) {

    fun fillRow(
        app: OsmandApplication,
        context: Context,
        rowBuilder: Builder,
        menuBuilder: MenuBuilder,
        poiType: PoiType?,
        key: String,
        value: String,
        subtype: String?
    ) {
        fillRow(PoiRowParams(
            app = app,
            context = context,
            builder = rowBuilder,
            menuBuilder = menuBuilder,
            poiType = poiType,
            rule = this,
            key = key,
            value = value,
            subtype = subtype
        ))
    }

    private fun fillRow(params: PoiRowParams) {
        behavior.applyCustomRules(params)
        behavior.applyCommonRules(params)
    }
}