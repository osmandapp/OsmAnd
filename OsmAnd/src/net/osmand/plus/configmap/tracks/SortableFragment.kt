package net.osmand.plus.configmap.tracks

interface SortableFragment {
    fun showSortByDialog()
    fun getSelectedTab(): TrackTab?
    fun setTracksSortMode(sortMode: TracksSortMode)
}