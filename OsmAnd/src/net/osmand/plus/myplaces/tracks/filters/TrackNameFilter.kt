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
			field = value
			enabled = !Algorithms.isEmpty(value)
			filterChangedListener.onFilterChanged()
		}

	override fun isTrackOutOfFilterBounds(trackItem: TrackItem): Boolean {
		val namePart: String = value
		val matcher = SearchPhrase.NameStringMatcher(
			namePart.trim { it <= ' ' },
			CollatorStringMatcher.StringMatcherMode.CHECK_CONTAINS)
		return !matcher.matches(trackItem.name)
	}
}