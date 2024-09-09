package net.osmand.plus.mapcontextmenu.gallery;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

public class GalleryGridRecyclerView extends RecyclerView {
	private ScaleGestureDetector scaleDetector;
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

	@Override
	public boolean onInterceptTouchEvent(MotionEvent e) {
		return super.onInterceptTouchEvent(e);
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {
		if (scaleDetector != null) {
			scaleDetector.onTouchEvent(ev);
		}
		if (ev.getPointerCount() > 1) {
			isScaling = true;
			stopScroll();
		}
		if (ev.getAction() == MotionEvent.ACTION_UP) {
			isScaling = false;
		}
		if (isScaling) {
			return true;
		} else {
			return super.dispatchTouchEvent(ev);
		}
	}
}
