package net.osmand.plus.views.controls.maphudbuttons;

import android.annotation.SuppressLint;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.preferences.FabMarginPreference;
import net.osmand.plus.utils.AndroidUtils;

public class MapButtonTouchListener implements OnTouchListener {

	private final MapActivity activity;
	private final FabMarginPreference preference;

	private int initialMarginX = 0;
	private int initialMarginY = 0;
	private float initialTouchX = 0;
	private float initialTouchY = 0;

	public MapButtonTouchListener(@NonNull MapActivity activity, @NonNull FabMarginPreference preference) {
		this.activity = activity;
		this.preference = preference;
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
				FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) view.getLayoutParams();
				if (AndroidUiHelper.isOrientationPortrait(activity))
					preference.setPortraitFabMargin(params.rightMargin, params.bottomMargin);
				else preference.setLandscapeFabMargin(params.rightMargin, params.bottomMargin);
				return true;
			}
			case MotionEvent.ACTION_MOVE -> {
				if (initialMarginX == 0 && initialMarginY == 0 && initialTouchX == 0 && initialTouchY == 0)
					setUpInitialValues(view, event);
				int padding = AndroidUtils.calculateTotalSizePx(activity, R.dimen.map_button_margin);
				FrameLayout parent = (FrameLayout) view.getParent();
				FrameLayout.LayoutParams param = (FrameLayout.LayoutParams) view.getLayoutParams();
				int deltaX = (int) (initialTouchX - event.getRawX());
				int deltaY = (int) (initialTouchY - event.getRawY());
				int newMarginX = interpolate(initialMarginX + deltaX, view.getWidth(), parent.getWidth() - padding * 2);
				int newMarginY = interpolate(initialMarginY + deltaY, view.getHeight(), parent.getHeight() - padding * 2);
				if (view.getHeight() + newMarginY <= parent.getHeight() - padding * 2 && newMarginY > 0)
					param.bottomMargin = newMarginY;
				if (view.getWidth() + newMarginX <= parent.getWidth() - padding * 2 && newMarginX > 0) {
					param.rightMargin = newMarginX;
				}
				view.setLayoutParams(param);
				return true;
			}
		}
		return false;
	}

	private int interpolate(int value, int divider, int boundsSize) {
		if (value <= divider && value > 0) return value * value / divider;
		else {
			int leftMargin = boundsSize - value - divider;
			if (leftMargin <= divider && value < boundsSize - divider)
				return leftMargin - (leftMargin * leftMargin / divider) + value;
			else return value;
		}
	}

	private void setUpInitialValues(@NonNull View view, @NonNull MotionEvent event) {
		MarginLayoutParams params = (MarginLayoutParams) view.getLayoutParams();

		initialMarginX = params.rightMargin;
		initialMarginY = params.bottomMargin;

		initialTouchX = event.getRawX();
		initialTouchY = event.getRawY();
	}
}