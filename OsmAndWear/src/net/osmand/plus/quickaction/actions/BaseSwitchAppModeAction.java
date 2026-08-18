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
import net.osmand.plus.settings.backend.OsmandSettings;

public abstract class BaseSwitchAppModeAction extends QuickAction {

	public BaseSwitchAppModeAction(QuickActionType type) {
		super(type);
	}

	public BaseSwitchAppModeAction(QuickAction quickAction) {
		super(quickAction);
	}

	protected abstract boolean shouldChangeForward();

	@StringRes
	public abstract int getQuickActionDescription();

	@Override
	public void execute(@NonNull MapActivity mapActivity) {
		OsmandSettings settings = mapActivity.getMyApplication().getSettings();
		if (shouldChangeForward()) {
			settings.switchAppModeToNext();
		} else {
			settings.switchAppModeToPrevious();
		}
	}

	@Override
	public void drawUI(@NonNull ViewGroup parent, @NonNull MapActivity mapActivity) {
		View view = LayoutInflater.from(parent.getContext())
				.inflate(R.layout.quick_action_with_text, parent, false);
		((TextView) view.findViewById(R.id.text)).setText(mapActivity.getString(getQuickActionDescription()));
		parent.addView(view);
	}
}
