package net.osmand.plus.quickaction.actions;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;

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
	public void execute(@NonNull MapActivity mapActivity) {
		mapActivity.getMyApplication().getOsmandMap().getMapView().changeZoomManually(shouldIncrement() ? 1 : -1);
	}

	@Override
	public void drawUI(@NonNull ViewGroup parent, @NonNull MapActivity mapActivity) {
		View view = LayoutInflater.from(parent.getContext())
				.inflate(R.layout.quick_action_with_text, parent, false);
		((TextView) view.findViewById(R.id.text)).setText(mapActivity.getString(getQuickActionDescription()));
		parent.addView(view);
	}
}
