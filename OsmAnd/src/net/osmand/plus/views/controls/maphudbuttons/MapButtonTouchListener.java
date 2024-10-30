package net.osmand.plus.views.controls.maphudbuttons;

import static net.osmand.plus.views.controls.maphudbuttons.ButtonPositionSize.DEF_MARGIN_DP;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.ViewParent;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.views.controls.MapHudLayout;
import net.osmand.plus.views.mapwidgets.configure.buttons.MapButtonState;

public class MapButtonTouchListener implements OnTouchListener {

	private final MapButtonState buttonState;
	private final int padding;
	private int initialMarginX = 0;
	private int initialMarginY = 0;
	private float initialTouchX = 0;
	private float initialTouchY = 0;

	public MapButtonTouchListener(@NonNull MapButtonState buttonState, @NonNull Context context) {
		this.buttonState = buttonState;
		padding = AndroidUtils.dpToPx(context, DEF_MARGIN_DP);
	}

	@SuppressLint("ClickableViewAccessibility")
	@Override
	public boolean onTouch(View view, MotionEvent event) {
		switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN -> {
				setUpInitialValues(view, event);
				return true;
			}
			case MotionEvent.ACTION_UP -> {
				view.setOnTouchListener(null);
				view.setPressed(false);
				view.setScaleX(1);
				view.setScaleY(1);
				view.setAlpha(1f);

				ViewParent parent = view.getParent();
				if (parent instanceof MapHudLayout layout) {
					layout.updateButton((MapButton) view, true);
				}
				return true;
			}
			case MotionEvent.ACTION_MOVE -> {
				if (initialMarginX == 0 && initialMarginY == 0 && initialTouchX == 0 && initialTouchY == 0) {
					setUpInitialValues(view, event);
				}
				moveButton(view, event);
				return true;
			}
		}
		return false;
	}

	private void moveButton(@NonNull View view, @NonNull MotionEvent event) {
		ButtonPositionSize s = buttonState.getPositionSize();
		FrameLayout parent = (FrameLayout) view.getParent();
		FrameLayout.LayoutParams param = (FrameLayout.LayoutParams) view.getLayoutParams();
		int deltaX = (int) (initialTouchX - event.getRawX());
		int deltaY = (int) (initialTouchY - event.getRawY());
		if (s.isLeft()) {
			deltaX = -deltaX;
		}
		if (s.isTop()) {
			deltaY = -deltaY;
		}
		int newMarginX = interpolate(initialMarginX + deltaX, view.getWidth(), parent.getWidth() - padding);
		int newMarginY = interpolate(initialMarginY + deltaY, view.getHeight(), parent.getHeight() - padding);
		if (view.getHeight() + newMarginY <= parent.getHeight() - padding && newMarginY > 0) {
			if (s.isTop()) {
				param.topMargin = newMarginY;
			} else {
				param.bottomMargin = newMarginY;
			}
		}
		if (view.getWidth() + newMarginX <= parent.getWidth() - padding && newMarginX > 0) {
			if (s.isLeft()) {
				param.leftMargin = newMarginX;
			} else {
				param.rightMargin = newMarginX;
			}
		}
		view.setLayoutParams(param);
	}

	private int interpolate(int value, int divider, int boundsSize) {
		if (value <= divider && value > 0) {
			return value * value / divider;
		}
		int leftMargin = boundsSize - value - divider;
		if (leftMargin <= divider && value < boundsSize - divider) {
			return leftMargin - (leftMargin * leftMargin / divider) + value;
		}
		return value;
	}

	private void setUpInitialValues(@NonNull View view, @NonNull MotionEvent event) {
		MarginLayoutParams params = (MarginLayoutParams) view.getLayoutParams();
		ButtonPositionSize s = buttonState.getPositionSize();
		initialMarginX = s.isLeft() ? params.leftMargin : params.rightMargin;
		initialMarginY = s.isTop() ? params.topMargin : params.bottomMargin;

		initialTouchX = event.getRawX();
		initialTouchY = event.getRawY();
	}
}