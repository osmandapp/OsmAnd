package net.osmand.plus.myplaces.favorites

import net.osmand.util.Algorithms
import java.util.LinkedHashSet
import java.util.concurrent.CountDownLatch

class SaveBatch(
	groups: List<FavoriteGroup>,
	var saveAllGroups: Boolean,
	listener: FavoritesListener?,
	waiter: CountDownLatch?
) {

	var groups: MutableList<FavoriteGroup> = ArrayList(groups)
		private set

	val listeners: MutableSet<FavoritesListener> = LinkedHashSet()
	val waiters: MutableList<CountDownLatch> = ArrayList()

	init {
		addRequest(listener, waiter)
	}

	fun merge(
		groups: List<FavoriteGroup>,
		saveAllGroups: Boolean,
		listener: FavoritesListener?,
		waiter: CountDownLatch?
	) {
		if (saveAllGroups) {
			this.groups = ArrayList(groups)
			this.saveAllGroups = true
		} else {
			mergeGroups(this.groups, groups)
		}
		addRequest(listener, waiter)
	}

	private fun addRequest(listener: FavoritesListener?, waiter: CountDownLatch?) {
		if (listener != null) {
			listeners.add(listener)
		}
		if (waiter != null) {
			waiters.add(waiter)
		}
	}

	private fun mergeGroups(destination: MutableList<FavoriteGroup>, source: List<FavoriteGroup>) {
		for (sourceGroup in source) {
			val index = destination.indexOfFirst { Algorithms.stringsEqual(it.name, sourceGroup.name) }
			if (index == -1) {
				destination.add(sourceGroup)
			} else {
				destination[index] = sourceGroup
			}
		}
	}
}
