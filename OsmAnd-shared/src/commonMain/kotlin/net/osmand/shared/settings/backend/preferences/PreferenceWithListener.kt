package net.osmand.shared.settings.backend.preferences

import co.touchlab.stately.concurrency.Synchronizable
import co.touchlab.stately.concurrency.synchronize
import net.osmand.shared.api.KStateChangedListener

abstract class PreferenceWithListener<T> : OsmandPreference<T> {

	private var syncObj = Synchronizable()

	private val listeners = mutableListOf<KStateChangedListener<T>?>()

	override fun addListener(listener: KStateChangedListener<T>) {
		syncObj.synchronize {
			if (listeners.none { it === listener }) {
				listeners.add(listener)
			}
		}
	}

	fun fireEvent(value: T) {
		syncObj.synchronize {
			val iterator = listeners.iterator()
			while (iterator.hasNext()) {
				val listener = iterator.next()
				if (listener == null) {
					iterator.remove()
				} else {
					listener.stateChanged(value)
				}
			}
		}
	}

	override fun removeListener(listener: KStateChangedListener<T>) {
		syncObj.synchronize {
			val iterator = listeners.iterator()
			while (iterator.hasNext()) {
				val registeredListener = iterator.next()
				if (registeredListener === listener) {
					iterator.remove()
				}
			}
		}
	}
}