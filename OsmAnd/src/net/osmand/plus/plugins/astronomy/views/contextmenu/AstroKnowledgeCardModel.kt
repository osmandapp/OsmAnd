package net.osmand.plus.plugins.astronomy.views.contextmenu

import net.osmand.plus.OsmandApplication
import net.osmand.plus.R

class AstroKnowledgeCardModel(
	app: OsmandApplication,
	var state: CardState,
	var buttonTitle: CharSequence,
	var actionEnabled: Boolean
) : AstroContextCard(app) {

	enum class CardState {
		UPSELL,
		DOWNLOAD
	}

	fun updateCard(state: CardState, buttonTitle: CharSequence, actionEnabled: Boolean) {
		this.state = state
		this.buttonTitle = buttonTitle
		this.actionEnabled = actionEnabled
	}

	fun getTitleId(): Int {
		return when (state) {
			CardState.UPSELL -> R.string.astro_expand_your_universe_title
			CardState.DOWNLOAD -> R.string.astro_offline_knowledge_base_title
		}
	}

	fun getDescriptionId(): Int {
		return when (state) {
			CardState.UPSELL -> R.string.astro_expand_your_universe_description
			CardState.DOWNLOAD -> R.string.astro_offline_knowledge_base_description
		}
	}

	fun getIconResId(nightMode: Boolean): Int {
		return when (state) {
			CardState.UPSELL -> R.drawable.ic_action_telescope_colored
			CardState.DOWNLOAD -> R.drawable.ic_action_sky_map_download
		}
	}
}
