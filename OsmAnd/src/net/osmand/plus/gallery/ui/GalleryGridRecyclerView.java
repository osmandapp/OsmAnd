package net.osmand.plus.gallery.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

public class GalleryGridRecyclerView extends RecyclerView {
	private ScaleGestureDetector scaleDetector;
	@Nullable
	private Runnable gestureFinishedListener;
	boolean isScaling;

	public GalleryGridRecyclerView(@NonNull Context context) {
		super(context);
	}

	public GalleryGridRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
	}

	public GalleryGridRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	public void setScaleDetector(ScaleGestureDetector scaleDetector) {
		this.scaleDetector = scaleDetector;
	}

	public void setGestureFinishedListener(@Nullable Runnable listener) {
		this.gestureFinishedListener = listener;
	}

	@Override
	public boolean onTouchEvent(MotionEvent e) {
		scaleDetector.onTouchEvent(e);
		if (e.getAction() == MotionEvent.ACTION_UP) {
			isScaling = false;
		} else if (e.getAction() == MotionEvent.ACTION_MOVE && isScaling) {
			return true;
		}
		return super.onTouchEvent(e);
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent e) {
		scaleDetector.onTouchEvent(e);
		if (e.getAction() == MotionEvent.ACTION_UP) {
			isScaling = false;
		}
		if (isScaling) {
			stopScroll();
			return true;
		} else {
			return super.onInterceptTouchEvent(e);
		}
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent e) {
		if (e.getPointerCount() > 1) {
			isScaling = true;
			stopScroll();
		}
		boolean handled = super.dispatchTouchEvent(e);
		int action = e.getActionMasked();
		if ((action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL)
				&& gestureFinishedListener != null) {
			gestureFinishedListener.run();
		}
		return handled;
	}
}
