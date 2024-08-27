package net.osmand.shared.filters

import kotlinx.serialization.Serializable
import net.osmand.plus.configmap.tracks.TrackItem
import net.osmand.shared.util.KAlgorithms
import net.osmand.shared.util.KStringMatcher
import net.osmand.shared.util.PlatformUtil

class TextTrackFilter(
	trackFilterType: TrackFilterType,
	filterChangedListener: FilterChangedListener?)
	: BaseTrackFilter(trackFilterType, filterChangedListener) {

	@Serializable
	var value = ""
		set(value) {
			if (!KAlgorithms.stringsEqual(field, value)) {
				field = value
				updateMatcher()
			}
			filterChangedListener?.onFilterChanged()
		}

	private var nameMatcher = createMatcher()

	private fun updateMatcher() {
		nameMatcher = createMatcher()
	}

	private fun createMatcher(): KStringMatcher {
		return PlatformUtil.getOsmAndContext().getNameStringMatcher(value.trim { it <= ' ' })
	}

	override fun isTrackAccepted(trackItem: TrackItem): Boolean {
		if (nameMatcher == null) {
			updateMatcher()
		}
		return nameMatcher.matches(trackItem.name)
	}

	override fun isEnabled(): Boolean {
		return !KAlgorithms.isEmpty(value)
	}

	override fun initWithValue(sourseFilter: BaseTrackFilter) {
		if (sourseFilter is TextTrackFilter) {
			this.value = sourseFilter.value
			updateMatcher()
			super.initWithValue(sourseFilter)
		}
	}

	override fun equals(other: Any?): Boolean {
		return super.equals(other) &&
				other is TextTrackFilter &&
				KAlgorithms.stringsEqual(other.value, value)
	}

	override fun hashCode(): Int {
		return value.hashCode()
	}
}