package net.osmand.plus.plugins.astronomy.views.contextmenu

import java.time.LocalDate

data class AstroContextUiState(
	val selectedObjectId: String? = null,
	val currentLocalDate: LocalDate? = null,
	val selectedVisibilityDateOverride: LocalDate? = null,
	val visibilityCursorReferenceTimeMillis: Long? = null,
	val schedulePeriodStart: LocalDate? = null,
	val catalogsExpanded: Boolean = false,
	val galleryState: AstroGalleryCardState = AstroGalleryCardState.Collapsed
)
