package net.osmand.plus.configmap.tracks

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import net.osmand.CollatorStringMatcher
import net.osmand.data.LatLon
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.configmap.tracks.viewholders.EmptySearchResultViewHolder
import net.osmand.plus.configmap.tracks.viewholders.SortTracksViewHolder
import net.osmand.plus.configmap.tracks.viewholders.SortTracksViewHolder.SortTracksListener
import net.osmand.plus.settings.enums.TracksSortMode
import net.osmand.plus.utils.UiUtilities
import net.osmand.util.Algorithms
import java.util.*

class SearchableTrackAdapter(
    app: OsmandApplication,
    trackTab: TrackTab,
    fragment: TracksFragment,
    nightMode: Boolean,
    sortTracksListener: SortTracksListener
) : TracksAdapter(app, trackTab, fragment, nightMode) {

    private var filterTracksQuery: String? = null
    private val TYPE_NO_FOUND_TRACKS = 5
    private val allItems = ArrayList<Any>()
    private var sortMode: TracksSortMode
    private var app: OsmandApplication
    private var sortTracksListener: SortTracksListener
    private var filteredTrackItems = ArrayList<TrackItem>()

    init {
        this.app = app
        this.sortTracksListener = sortTracksListener
        sortMode = trackTab.sortMode
        updateAllItems()
        getFilteredItems()
    }

    private fun updateAllItems() {
        allItems.clear()
        allItems.addAll(trackTab.items)
        val latLon: LatLon = app.mapViewTrackingUtilities.defaultLocation
        Collections.sort(allItems, TracksComparator(sortMode, latLon))
    }

    fun updateContent() {
        updateAllItems()
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        val itemObject = items[position]
        if (itemObject is Int) {
            if (TYPE_NO_FOUND_TRACKS == itemObject) {
                return TYPE_NO_FOUND_TRACKS
            }
        }
        return super.getItemViewType(position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (viewType == TYPE_NO_FOUND_TRACKS) {
            val inflater = UiUtilities.getInflater(parent.context, nightMode)
            val view = inflater.inflate(R.layout.empty_search_results, parent, false)
            return EmptySearchResultViewHolder(view)
        }
        return super.onCreateViewHolder(parent, viewType)
    }

    override fun getItems(): MutableList<Any> {
        return getFilteredItems()
    }

    private fun getFilteredItems(): MutableList<Any> {
        val filteredItems: MutableList<Any> = ArrayList()
        filteredTrackItems.clear()
        var hasTrackItems = false
        var hasFoundTrackItems = false
        var collator: CollatorStringMatcher? = null
        if (filterTracksQuery != null) {
            collator = CollatorStringMatcher(
                filterTracksQuery,
                CollatorStringMatcher.StringMatcherMode.CHECK_CONTAINS)
        }
        for (itemObject in allItems) {
            if (itemObject is TrackItem) {
                hasTrackItems = true
                if (Algorithms.isEmpty(filterTracksQuery)
                    || collator != null && collator.matches(itemObject.name.lowercase())) {
                    filteredItems.add(itemObject)
                    filteredTrackItems.add(itemObject)
                    hasFoundTrackItems = true
                }
            } else {
                filteredItems.add(itemObject)
            }
        }
        if (hasTrackItems && !hasFoundTrackItems) {
            filteredItems.add(TYPE_NO_FOUND_TRACKS)
        }
        return filteredItems
    }

    fun getCurrentTrackItems(): List<TrackItem> {
        return filteredTrackItems
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is SortTracksViewHolder) {
            holder.bindView(true)
        } else {
            super.onBindViewHolder(holder, position)
        }
    }

    fun setFilterTracksQuery(query: String?) {
        filterTracksQuery = query
        notifyDataSetChanged()
    }

    fun setTracksSortMode(sortMode: TracksSortMode) {
        this.sortMode = sortMode
        updateContent()
    }

    override fun createSortTracksViewHolder(
        parent: ViewGroup,
        inflater: LayoutInflater): SortTracksViewHolder {
        val view = inflater.inflate(R.layout.sort_type_view, parent, false)
        return SortTracksViewHolder(view, sortTracksListener, nightMode)
    }
}