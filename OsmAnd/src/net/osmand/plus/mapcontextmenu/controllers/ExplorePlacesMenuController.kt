package net.osmand.plus.mapcontextmenu.controllers

import android.graphics.drawable.Drawable
import net.osmand.data.ExploreTopPlacePoint
import net.osmand.data.PointDescription
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.mapcontextmenu.MenuController
import net.osmand.plus.mapcontextmenu.builders.ExplorePlacesMenuBuilder
import net.osmand.plus.render.RenderingIcons

class ExplorePlacesMenuController(
	mapActivity: MapActivity,
	pointDescription: PointDescription,
	private val pointObject: ExploreTopPlacePoint) :
	MenuController(
		ExplorePlacesMenuBuilder(mapActivity, pointObject),
		pointDescription,
		mapActivity) {
	private val app: OsmandApplication = mapActivity.myApplication
	private var coloredIcon: Drawable? = null

	init {
		initData()
	}

	private fun initData() {
		val poiTypes = app.poiTypes
		val subType = poiTypes.getPoiTypeByKey(pointObject.poisubtype)
		val poiIcon = RenderingIcons.getBigIcon(app, subType.keyName)
		val uiUtilities = app.uiUtilities
		val nightMode = app.daynightHelper.isNightMode
		coloredIcon = if (poiIcon != null) {
			uiUtilities.getRenderingIcon(
				app,
				subType.keyName,
				nightMode
			)
		} else {
			uiUtilities.getIcon(R.drawable.ic_action_info_dark, nightMode)
		}

	}

	override fun setObject(`object`: Any) {
		initData()
	}

	override fun getObject(): Any? {
		return null
	}

	override fun displayStreetNameInTitle(): Boolean {
		return false
	}

	override fun displayDistanceDirection(): Boolean {
		return true
	}

	override fun getRightIcon(): Drawable? {
		return coloredIcon
	}

	override fun getSecondLineTypeIcon(): Drawable? {
		return null
	}

	override fun getTypeStr(): String {
		val poiTypes = app.poiTypes
		val subType = poiTypes.getPoiTypeByKey(pointObject.poisubtype)
		return subType.translation
	}

	override fun getCommonTypeStr(): String {
		val poiTypes = app.poiTypes
		val subType = poiTypes.getPoiTypeByKey(pointObject.poisubtype)
		return subType.translation
	}

	override fun needStreetName(): Boolean {
		return false
	}
}
