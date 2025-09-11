package net.osmand.plus.quickaction.actions;

import static net.osmand.plus.OsmAndConstants.UI_HANDLER_MAP_CONTROLS;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.UiUtilities;

public abstract class BaseSwitchAppModeAction extends QuickAction {
	private static final long CHANGE_PROFILE_DELAY = 3500;

	public BaseSwitchAppModeAction(QuickActionType type) {
		super(type);
	}

	public BaseSwitchAppModeAction(QuickAction quickAction) {
		super(quickAction);
	}

	protected abstract boolean shouldChangeForward();

	@StringRes
	public abstract int getQuickActionDescription();

	private static ApplicationMode delayedSwitchProfile;
	private static Toast delayedSwitchProfileToast;

	public void delayedSwitchAppMode(@NonNull MapActivity mapActivity) {
		OsmandSettings settings = mapActivity.getSettings();
		ApplicationMode appMode = settings.getApplicationMode();
		boolean next = shouldChangeForward();

		if (delayedSwitchProfile == null) {
			delayedSwitchProfile = appMode;
		}
		delayedSwitchProfile = settings.getSwitchedAppMode(delayedSwitchProfile, next);
		cancelDelayedToast();

		String patternDelayedSwitch = mapActivity.getString(R.string.selected_delayed_profile);
		String messageDelayedSwitch = String.format(patternDelayedSwitch, delayedSwitchProfile.toHumanString());
		delayedSwitchProfileToast = Toast.makeText(mapActivity, messageDelayedSwitch, Toast.LENGTH_SHORT);
		delayedSwitchProfileToast.show();

		mapActivity.getApp().runInUIThreadAndCancelPrevious(UI_HANDLER_MAP_CONTROLS + 1, () -> {
			if (delayedSwitchProfile != null && appMode != delayedSwitchProfile && settings.setApplicationMode(delayedSwitchProfile)) {
				cancelDelayedToast();
				mapActivity.getApp().showShortToastMessage(R.string.application_profile_changed, delayedSwitchProfile.toHumanString());
			}
			delayedSwitchProfileToast = null;
			delayedSwitchProfile = null;
		}, CHANGE_PROFILE_DELAY);
	}

	private void cancelDelayedToast(){
		if (delayedSwitchProfileToast != null) {
			delayedSwitchProfileToast.cancel();
		}
	}

	@Override
	public void execute(@NonNull MapActivity mapActivity, @Nullable Bundle params) {
		delayedSwitchAppMode(mapActivity);
	}

	@Override
	public void drawUI(@NonNull ViewGroup parent, @NonNull MapActivity mapActivity, boolean nightMode) {
		View view = UiUtilities.inflate(parent.getContext(), nightMode, R.layout.quick_action_with_text, parent, false);
		((TextView) view.findViewById(R.id.text)).setText(mapActivity.getString(getQuickActionDescription()));
		parent.addView(view);
	}
}
