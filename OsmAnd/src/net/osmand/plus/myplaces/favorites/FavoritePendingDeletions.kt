package net.osmand.plus.myplaces.favorites

class FavoritePendingDeletions {

	val pointKeys: MutableSet<String> = HashSet()
	val groupNames: MutableSet<String> = HashSet()

	val isEmpty: Boolean
		get() = pointKeys.isEmpty() && groupNames.isEmpty()

	fun addPoint(pointKey: String) {
		if (pointKey.isNotEmpty()) {
			pointKeys.add(pointKey)
		}
	}

	fun addGroup(groupName: String) {
		groupNames.add(groupName)
	}
}