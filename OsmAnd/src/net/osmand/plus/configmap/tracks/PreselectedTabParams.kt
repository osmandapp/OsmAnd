package net.osmand.plus.configmap.tracks

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PreselectedTabParams(
	val tabId: String,
	val subGroupId: String? = null,
	val specificPaths: List<String>? = null,
	val selectAll: Boolean = false
) : Parcelable {

	companion object {
		@JvmStatic
		fun openTab(tabId: String) = PreselectedTabParams(tabId)

		@JvmStatic
		fun selectAll(tabId: String) =
			PreselectedTabParams(tabId, selectAll = true)

		@JvmStatic
		fun selectGroup(tabId: String, subGroupId: String) =
			PreselectedTabParams(tabId, subGroupId = subGroupId)

		@JvmStatic
		fun selectPaths(tabId: String, paths: List<String>) =
			PreselectedTabParams(tabId, specificPaths = paths)
	}
}