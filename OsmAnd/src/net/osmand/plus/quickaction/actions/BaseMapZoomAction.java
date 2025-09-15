package net.osmand.plus.quickaction.actions;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;
import net.osmand.plus.utils.UiUtilities;

public abstract class BaseMapZoomAction extends QuickAction {

	public BaseMapZoomAction(QuickActionType type) {
		super(type);
	}

	public BaseMapZoomAction(QuickAction quickAction) {
		super(quickAction);
	}

	protected abstract boolean shouldIncrement();

	@StringRes
	public abstract int getQuickActionDescription();

	@Override
	public boolean onKeyDown(@NonNull MapActivity mapActivity, int keyCode, KeyEvent event) {
		if (isContinuous()) {
			onActionSelected(mapActivity, event);
		}
		return true;
	}

	@Override
	public boolean onKeyUp(@NonNull MapActivity mapActivity, int keyCode, KeyEvent event) {
		if (!isContinuous()) {
			return super.onKeyUp(mapActivity, keyCode, event);
		}
		return true;
	}

	@Override
	public void execute(@NonNull MapActivity mapActivity, @Nullable Bundle params) {
		changeZoom(mapActivity.getApp(), shouldIncrement() ? 1 : -1);
	}

	private void changeZoom(@NonNull OsmandApplication app, int zoomStep) {
		app.getOsmandMap().getMapView().changeZoomManually(zoomStep);
	}

	protected boolean isContinuous() {
		return false;
	}

	@Override
	public void drawUI(@NonNull ViewGroup parent, @NonNull MapActivity mapActivity, boolean nightMode) {
		View view = UiUtilities.inflate(parent.getContext(), nightMode, R.layout.quick_action_with_text, parent, false);
		((TextView) view.findViewById(R.id.text)).setText(mapActivity.getString(getQuickActionDescription()));
		parent.addView(view);
	}
}
