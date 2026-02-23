package net.osmand.plus.card.color.palette.gradient.editor.section

import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.card.color.palette.gradient.editor.data.EditorUiState
import net.osmand.plus.card.color.palette.solid.ColorsPaletteCard
import net.osmand.plus.card.color.palette.solid.SolidPaletteController
import net.osmand.plus.palette.contract.IExternalPaletteListener

class ColorSection(
	rootView: View,
	activity: FragmentActivity,
	app: OsmandApplication,
	nightMode: Boolean,
	listener: IExternalPaletteListener,
	private val controller: SolidPaletteController
) : UiSection(app, nightMode)  {

	private val cardContainer: ViewGroup = rootView.findViewById(R.id.solid_colors_card_container)

	init {
		controller.setPaletteListener(listener)
		val colorsCard = ColorsPaletteCard(activity, controller)
		val cardView = colorsCard.build(activity)
		cardContainer.addView(cardView)
	}

	override fun update(oldUiState: EditorUiState?, newUiState: EditorUiState) {
		val oldState = oldUiState?.colorState
		val newState = newUiState.colorState
		val initialRender = oldState == null
		if (oldState?.colorInt != newState.colorInt) {
			val colorInt = newUiState.colorState.colorInt
			val item = controller.findPaletteItem(colorInt, addIfNotFound = true)
			controller.selectPaletteItemSilently(item)
			controller.scrollToPaletteItem(item, smoothScroll = !initialRender)
		}
	}
}