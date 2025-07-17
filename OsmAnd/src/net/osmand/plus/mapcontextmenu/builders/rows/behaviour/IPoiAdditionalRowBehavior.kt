package net.osmand.plus.mapcontextmenu.builders.rows.behaviour

interface IPoiAdditionalRowBehavior {

	fun applyCustomRules(params: PoiRowParams)

	fun applyCommonRules(params: PoiRowParams)
}