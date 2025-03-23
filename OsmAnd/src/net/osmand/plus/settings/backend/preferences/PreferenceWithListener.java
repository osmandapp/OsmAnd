package net.osmand.plus.settings.backend.preferences;

import androidx.annotation.NonNull;

import net.osmand.StateChangedListener;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public abstract class PreferenceWithListener<T> implements OsmandPreference<T> {
	private List<WeakReference<StateChangedListener<T>>> listeners;

	@Override
	public synchronized void addListener(@NonNull StateChangedListener<T> listener) {
		if (listeners == null) {
			listeners = new LinkedList<>();
		}
		if (!listeners.contains(new WeakReference<>(listener))) {
			listeners.add(new WeakReference<>(listener));
		}
	}

	public synchronized void fireEvent(T value) {
		if (listeners != null) {
			Iterator<WeakReference<StateChangedListener<T>>> it = listeners.iterator();
			while (it.hasNext()) {
				StateChangedListener<T> listener = it.next().get();
				if (listener == null) {
					it.remove();
				} else {
					listener.stateChanged(value);
				}
			}
		}
	}

	@Override
	public synchronized void removeListener(@NonNull StateChangedListener<T> listener) {
		if (listeners != null) {
			listeners.removeIf(ref -> ref.get() == listener);
		}
	}
}