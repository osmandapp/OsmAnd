package net.osmand.plus.helpers;

import android.util.Pair;
import android.view.GestureDetector;
import android.view.MotionEvent;

import androidx.annotation.NonNull;

import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.actions.LockScreenAction;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.views.controls.maphudbuttons.QuickActionButton;
import net.osmand.plus.views.layers.MapQuickActionLayer;
import net.osmand.plus.views.mapwidgets.configure.buttons.QuickActionButtonState;

public class LockGestureDetector extends GestureDetector {
	private final MapActivity mapActivity;
	private Pair<QuickAction, QuickActionButton> pressedLockAction;

	public LockGestureDetector(@NonNull MapActivity mapActivity, @NonNull OnGestureListener listener) {
		super(mapActivity, listener);
		this.mapActivity = mapActivity;
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		if (ev.getAction() == MotionEvent.ACTION_DOWN) {
			pressedLockAction = getPressedLockAction(mapActivity, ev);
			if (pressedLockAction != null) {
				pressedLockAction.second.setPressed(true);
			}
		} else if (ev.getAction() == MotionEvent.ACTION_UP) {
			if (pressedLockAction != null) {
				pressedLockAction.second.setPressed(false);
			}
		}
		return super.onTouchEvent(ev);
	}

	private static Pair<QuickAction, QuickActionButton> getPressedLockAction(@NonNull MapActivity mapActivity, @NonNull MotionEvent ev) {
		float x = ev.getRawX();
		float y = ev.getRawY();
		QuickActionButton quickActionButton;
		QuickAction lockAction;
		MapQuickActionLayer quickActionLayer = mapActivity.getMapLayers().getMapQuickActionLayer();
		for (QuickActionButton actionButton : quickActionLayer.getActionButtons()) {
			QuickActionButtonState buttonState = actionButton.getButtonState();
			if (buttonState != null) {
				for (QuickAction action : buttonState.getQuickActions()) {
					if (action instanceof LockScreenAction && AndroidUtils.getViewBoundOnScreen(actionButton).contains((int) x, (int) y)) {
						quickActionButton = actionButton;
						lockAction = action;
						return new Pair<>(lockAction, quickActionButton);
					}
				}
			}
		}
		return null;
	}

	private static OnGestureListener getOnGestureListener(@NonNull MapActivity mapActivity) {
		return new GestureDetector.SimpleOnGestureListener() {
			@Override
			public boolean onSingleTapConfirmed(@NonNull MotionEvent e) {
				Pair<QuickAction, QuickActionButton> pressedLockAction = getPressedLockAction(mapActivity, e);

				if (pressedLockAction != null) {
					QuickAction lockAction = pressedLockAction.first;
					QuickActionButtonState buttonState = pressedLockAction.second.getButtonState();
					if (buttonState != null) {
						mapActivity.getMapLayers().getMapQuickActionLayer().onActionSelected(buttonState, lockAction);
						return true;
					}
				}
				return false;
			}
		};
	}

	public static LockGestureDetector getDetector(@NonNull MapActivity mapActivity) {
		return new LockGestureDetector(mapActivity, getOnGestureListener(mapActivity));
	}
}