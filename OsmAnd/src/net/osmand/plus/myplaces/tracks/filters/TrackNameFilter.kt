package net.osmand.plus.myplaces.tracks.filters

import com.google.gson.annotations.Expose
import net.osmand.CollatorStringMatcher
import net.osmand.plus.R
import net.osmand.plus.configmap.tracks.TrackItem
import net.osmand.plus.myplaces.tracks.filters.FilterType.NAME
import net.osmand.search.core.SearchPhrase
import net.osmand.util.Algorithms

class TrackNameFilter(filterChangedListener: FilterChangedListener)
	: BaseTrackFilter(R.string.shared_string_name, NAME, filterChangedListener) {
	@Expose
	var value = ""
		set(value) {
			if (!Algorithms.stringsEqual(field, value)) {
				updateMatcher()
			}
			field = value
			filterChangedListener.onFilterChanged()
		}

	private fun updateMatcher() {
		nameMatcher = createMatcher()
	}

	private var nameMatcher = createMatcher()

	private fun createMatcher(): SearchPhrase.NameStringMatcher {
		return SearchPhrase.NameStringMatcher(
			value.trim { it <= ' ' },
			CollatorStringMatcher.StringMatcherMode.CHECK_CONTAINS)
	}

	override fun isTrackAccepted(trackItem: TrackItem): Boolean {
		return nameMatcher.matches(trackItem.name)
	}

	override fun isEnabled(): Boolean {
		return !Algorithms.isEmpty(value)
	}
}