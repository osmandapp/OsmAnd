package net.osmand.plus.settings.backend.preferences;

import net.osmand.StateChangedListener;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public abstract class PreferenceWithListener<T> implements OsmandPreference<T> {
	private List<WeakReference<StateChangedListener<T>>> l;

	@Override
	public synchronized void addListener(StateChangedListener<T> listener) {
		if (l == null) {
			l = new LinkedList<WeakReference<StateChangedListener<T>>>();
		}
		if (!l.contains(new WeakReference<StateChangedListener<T>>(listener))) {
			l.add(new WeakReference<StateChangedListener<T>>(listener));
		}
	}

	public synchronized void fireEvent(T value) {
		if (l != null) {
			Iterator<WeakReference<StateChangedListener<T>>> it = l.iterator();
			while (it.hasNext()) {
				StateChangedListener<T> t = it.next().get();
				if (t == null) {
					it.remove();
				} else {
					t.stateChanged(value);
				}
			}
		}
	}

	@Override
	public synchronized void removeListener(StateChangedListener<T> listener) {
		if (l != null) {
			Iterator<WeakReference<StateChangedListener<T>>> it = l.iterator();
			while (it.hasNext()) {
				StateChangedListener<T> t = it.next().get();
				if (t == listener) {
					it.remove();
				}
			}
		}
	}
}