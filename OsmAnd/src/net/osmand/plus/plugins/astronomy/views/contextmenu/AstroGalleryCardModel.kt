package net.osmand.plus.plugins.astronomy.views.contextmenu

import net.osmand.plus.OsmandApplication
import net.osmand.plus.mapcontextmenu.builders.cards.AbstractCard
import net.osmand.plus.plugins.astronomy.views.contextmenu.AstroContextCard

class AstroGalleryCardModel(app: OsmandApplication, var wid: String) : AstroContextCard(app) {
	var state: GalleryState = GalleryState.Collapsed

	sealed class GalleryState {
		data object Collapsed : GalleryState()
		data object Loading : GalleryState()
		data class Ready(val cards: List<AbstractCard?>) : GalleryState()
		data class Error(val message: String) : GalleryState()
	}
}