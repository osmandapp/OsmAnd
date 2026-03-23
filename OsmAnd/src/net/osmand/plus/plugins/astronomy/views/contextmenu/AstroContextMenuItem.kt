package net.osmand.plus.plugins.astronomy.views.contextmenu

import android.net.Uri
import net.osmand.plus.R
import net.osmand.plus.mapcontextmenu.builders.cards.AbstractCard
import net.osmand.plus.plugins.astronomy.Catalog
import java.time.LocalDate
import java.time.ZoneId

enum class AstroContextCardKey(val stableId: Long) {
	KNOWLEDGE(1L),
	DESCRIPTION(2L),
	CATALOGS(3L),
	GALLERY(4L),
	VISIBILITY(5L),
	SCHEDULE(6L)
}

sealed interface AstroContextMenuItem {
	val key: AstroContextCardKey
}

enum class AstroKnowledgeCardState {
	UPSELL,
	DOWNLOAD
}

sealed class AstroGalleryCardState {
	data object Collapsed : AstroGalleryCardState()
	data object Loading : AstroGalleryCardState()
	data class Ready(val cards: List<AbstractCard?>) : AstroGalleryCardState()
}

data class AstroKnowledgeCardItem(
	val state: AstroKnowledgeCardState,
	val buttonTitle: String,
	val actionEnabled: Boolean
) : AstroContextMenuItem {
	override val key: AstroContextCardKey = AstroContextCardKey.KNOWLEDGE

	fun getTitleId(): Int {
		return when (state) {
			AstroKnowledgeCardState.UPSELL -> R.string.astro_expand_your_universe_title
			AstroKnowledgeCardState.DOWNLOAD -> R.string.astro_offline_knowledge_base_title
		}
	}

	fun getDescriptionId(): Int {
		return when (state) {
			AstroKnowledgeCardState.UPSELL -> R.string.astro_expand_your_universe_description
			AstroKnowledgeCardState.DOWNLOAD -> R.string.astro_offline_knowledge_base_description
		}
	}

	fun getIconResId(): Int {
		return when (state) {
			AstroKnowledgeCardState.UPSELL -> R.drawable.ic_action_telescope_colored
			AstroKnowledgeCardState.DOWNLOAD -> R.drawable.ic_action_sky_map_download
		}
	}
}

data class AstroDescriptionCardItem(
	val description: String,
	val wikiUri: Uri
) : AstroContextMenuItem {
	override val key: AstroContextCardKey = AstroContextCardKey.DESCRIPTION
}

data class AstroCatalogsCardItem(
	val catalogs: List<Catalog>,
	val expanded: Boolean
) : AstroContextMenuItem {
	override val key: AstroContextCardKey = AstroContextCardKey.CATALOGS
}

data class AstroGalleryCardItem(
	val wid: String,
	val showAllTitle: String?,
	val state: AstroGalleryCardState
) : AstroContextMenuItem {
	override val key: AstroContextCardKey = AstroContextCardKey.GALLERY
}

data class AstroVisibilityGraphSnapshot(
	val startMillis: Long,
	val endMillis: Long,
	val zoneId: ZoneId,
	val objectAltitudes: DoubleArray,
	val objectAzimuths: DoubleArray,
	val sunAltitudes: DoubleArray
) {
	val size: Int
		get() = objectAltitudes.size

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as AstroVisibilityGraphSnapshot

		if (startMillis != other.startMillis) return false
		if (endMillis != other.endMillis) return false
		if (zoneId != other.zoneId) return false
		if (!objectAltitudes.contentEquals(other.objectAltitudes)) return false
		if (!objectAzimuths.contentEquals(other.objectAzimuths)) return false
		if (!sunAltitudes.contentEquals(other.sunAltitudes)) return false
		if (size != other.size) return false

		return true
	}

	override fun hashCode(): Int {
		var result = startMillis.hashCode()
		result = 31 * result + endMillis.hashCode()
		result = 31 * result + zoneId.hashCode()
		result = 31 * result + objectAltitudes.contentHashCode()
		result = 31 * result + objectAzimuths.contentHashCode()
		result = 31 * result + sunAltitudes.contentHashCode()
		result = 31 * result + size
		return result
	}
}

data class AstroVisibilityCardItem(
	val graph: AstroVisibilityGraphSnapshot?,
	val cursorReferenceTimeMillis: Long,
	val riseTime: String?,
	val culminationTime: String?,
	val setTime: String?,
	val locationText: String,
	val culminationColor: Int,
	val titleText: String,
	val showResetButton: Boolean
) : AstroContextMenuItem {
	override val key: AstroContextCardKey = AstroContextCardKey.VISIBILITY
}

data class AstroScheduleDayGraphSnapshot(
	val sunAltitudes: DoubleArray,
	val objectAltitudes: DoubleArray
) {
	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as AstroScheduleDayGraphSnapshot

		if (!sunAltitudes.contentEquals(other.sunAltitudes)) return false
		if (!objectAltitudes.contentEquals(other.objectAltitudes)) return false

		return true
	}

	override fun hashCode(): Int {
		var result = sunAltitudes.contentHashCode()
		result = 31 * result + objectAltitudes.contentHashCode()
		return result
	}
}

data class AstroScheduleDayItem(
	val date: LocalDate,
	val dayLabel: String,
	val riseTime: String?,
	val setTime: String?,
	val setNextDay: Boolean,
	val graph: AstroScheduleDayGraphSnapshot
)

data class AstroScheduleCardItem(
	val periodStart: LocalDate,
	val rangeLabel: String,
	val days: List<AstroScheduleDayItem>,
	val showResetPeriodButton: Boolean
) : AstroContextMenuItem {
	override val key: AstroContextCardKey = AstroContextCardKey.SCHEDULE
}
