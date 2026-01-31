package net.osmand.plus.card.color.palette.main

import androidx.fragment.app.FragmentActivity
import net.osmand.plus.R
import net.osmand.plus.palette.controller.BasePaletteController
import net.osmand.plus.palette.view.PaletteCard
import net.osmand.plus.palette.view.binder.PaletteItemViewBinder
import net.osmand.plus.palette.view.binder.SolidViewBinder
import net.osmand.plus.settings.backend.ApplicationMode

open class ColorsPaletteCard(
	activity: FragmentActivity, controller: BasePaletteController,
	appMode: ApplicationMode? = null, usedOnMap: Boolean
) : PaletteCard(activity, controller, appMode, usedOnMap) {

	constructor(activity: FragmentActivity, controller: BasePaletteController)
			: this(activity, controller, appMode = null, usedOnMap = true)

	override fun createViewBinder(): PaletteItemViewBinder = SolidViewBinder(activity, nightMode)

	override fun getShowAllButtonTitle() = getString(R.string.shared_string_all_colors)
}