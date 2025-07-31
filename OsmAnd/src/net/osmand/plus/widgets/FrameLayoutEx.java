package net.osmand.plus.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.views.controls.ViewChangeProvider;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class FrameLayoutEx extends FrameLayout implements ViewChangeProvider {

	private final Set<ViewChangeListener> viewChangeListeners = new HashSet<>();

	public FrameLayoutEx(@NonNull Context context) {
		super(context);
	}

	public FrameLayoutEx(@NonNull Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
	}

	public FrameLayoutEx(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	public FrameLayoutEx(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
	}

	@NonNull
	@Override
	public Collection<ViewChangeListener> getViewChangeListeners() {
		return viewChangeListeners;
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		notifySizeChanged(this, w, h, oldw, oldh);
	}

	@Override
	protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
		super.onVisibilityChanged(changedView, visibility);
		notifyVisibilityChanged(changedView, visibility);
	}
}
