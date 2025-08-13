package net.osmand.plus.views.controls;

import android.view.View;

import androidx.annotation.NonNull;

import java.util.Collection;

public interface ViewChangeProvider {

	default void addViewChangeListener(@NonNull ViewChangeListener listener) {
		Collection<ViewChangeListener> listeners = getViewChangeListeners();
		if (!listeners.contains(listener)) {
			listeners.add(listener);
		}
	}

	default void removeViewChangeListener(@NonNull ViewChangeListener listener) {
		getViewChangeListeners().remove(listener);
	}

	default void notifySizeChanged(@NonNull View view, int w, int h, int oldWidth, int oldHeight) {
		for (ViewChangeListener listener : getViewChangeListeners()) {
			listener.onSizeChanged(view, w, h, oldWidth, oldHeight);
		}
	}

	default void notifyVisibilityChanged(@NonNull View view, int visibility) {
		for (ViewChangeListener listener : getViewChangeListeners()) {
			listener.onVisibilityChanged(view, visibility);
		}
	}

	@NonNull
	Collection<ViewChangeListener> getViewChangeListeners();

	interface ViewChangeListener {
		void onSizeChanged(@NonNull View view, int w, int h, int oldWidth, int oldHeight);

		void onVisibilityChanged(@NonNull View view, int visibility);
	}
}
