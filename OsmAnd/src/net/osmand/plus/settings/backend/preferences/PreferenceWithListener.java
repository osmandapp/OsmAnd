package net.osmand.plus.settings.backend.preferences;

import net.osmand.IStateChangeListener;
import net.osmand.SimpleStateChangeListener;
import net.osmand.StateChangedListener;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public abstract class PreferenceWithListener<T> implements OsmandPreference<T> {
	private List<WeakReference<IStateChangeListener>> listeners;

	@Override
	public synchronized void addListener(StateChangedListener<T> listener) {
		addListenerInternal(listener);
	}

	@Override
	public synchronized void addListener(SimpleStateChangeListener listener) {
		addListenerInternal(listener);
	}

	private synchronized void addListenerInternal(IStateChangeListener listener) {
		if (listeners == null) {
			listeners = new LinkedList<>();
		}
		listeners.add(new WeakReference<>(listener));
	}

	public synchronized void fireEvent(T value) {
		if (listeners != null) {
			Iterator<WeakReference<IStateChangeListener>> it = listeners.iterator();
			while (it.hasNext()) {
				IStateChangeListener listener = it.next().get();
				if (listener == null) {
					it.remove();
				} else {
					if (listener instanceof StateChangedListener l) {
						l.stateChanged(value);
					} else if (listener instanceof SimpleStateChangeListener l) {
						l.onStateChanged();
					}
				}
			}
		}
	}

	@Override
	public synchronized void removeListener(IStateChangeListener listener) {
		if (listeners != null) {
			listeners.removeIf(ref -> ref.get() == listener);
		}
	}
}