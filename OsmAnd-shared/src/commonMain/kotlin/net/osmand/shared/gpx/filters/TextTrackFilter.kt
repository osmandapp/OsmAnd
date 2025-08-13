package net.osmand.shared.gpx.filters

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.osmand.shared.api.KStringMatcherMode
import net.osmand.shared.gpx.TrackItem
import net.osmand.shared.util.KAlgorithms
import net.osmand.shared.util.KStringMatcher
import net.osmand.shared.util.PlatformUtil

@Serializable
class TextTrackFilter
	: BaseTrackFilter {

	constructor(
		trackFilterType: TrackFilterType,
		filterChangedListener: FilterChangedListener?) : super(
		trackFilterType,
		filterChangedListener)

	@Serializable
	var value = ""
		set(value) {
			if (!KAlgorithms.stringsEqual(field, value)) {
				field = value
				updateMatcher()
			}
			filterChangedListener?.onFilterChanged()
		}

	@Transient
	private var nameMatcher = createMatcher()

	private fun updateMatcher() {
		nameMatcher = createMatcher()
	}

	private fun createMatcher(): KStringMatcher {
		return PlatformUtil.getOsmAndContext().getNameStringMatcher(
			value.trim { it <= ' ' },
			KStringMatcherMode.CHECK_CONTAINS)
	}

	override fun isTrackAccepted(trackItem: TrackItem): Boolean =
		nameMatcher.matches(trackItem.name)

	override fun isEnabled(): Boolean = !KAlgorithms.isEmpty(value)

	override fun initWithValue(value: BaseTrackFilter) {
		if (value is TextTrackFilter) {
			this.value = value.value
			updateMatcher()
			super.initWithValue(value)
		}
	}

	override fun equals(other: Any?): Boolean {
		return super.equals(other) &&
				other is TextTrackFilter &&
				KAlgorithms.stringsEqual(other.value, value)
	}

	override fun hashCode(): Int = value.hashCode()
}