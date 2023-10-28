package net.osmand.plus.widgets.tools;

import com.github.ksoichiro.android.observablescrollview.ObservableScrollViewCallbacks;
import com.github.ksoichiro.android.observablescrollview.ScrollState;

/**
 * Interface for scenarios where user wants to
 * implement only part of ObservableScrollViewCallbacks' methods.
 */
public interface SimpleObservableScrollViewCallbacks extends ObservableScrollViewCallbacks {

	default void onScrollChanged(int scrollY, boolean firstScroll, boolean dragging) {}

	default void onDownMotionEvent() {}

	default void onUpOrCancelMotionEvent(ScrollState scrollState) {}

}
