package net.osmand.plus.quickaction.actions;

import static net.osmand.plus.quickaction.QuickActionIds.NAV_VOICE_ACTION_ID;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;
import net.osmand.plus.utils.UiUtilities;

public class NavVoiceAction extends QuickAction {
	public static final QuickActionType TYPE = new QuickActionType(NAV_VOICE_ACTION_ID,
			"nav.voice", NavVoiceAction.class).
			nameRes(R.string.voices).iconRes(R.drawable.ic_action_volume_up).nonEditable().
			category(QuickActionType.NAVIGATION)
			.nameActionRes(R.string.quick_action_verb_turn_on_off);

	public NavVoiceAction() {
		super(TYPE);
	}

	public NavVoiceAction(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	public void execute(@NonNull MapActivity mapActivity, @Nullable Bundle params) {
		boolean oldMuteState = mapActivity.getSettings().VOICE_MUTE.get();
		mapActivity.getSettings().VOICE_MUTE.set(!oldMuteState);
	}

	@Override
	public void drawUI(@NonNull ViewGroup parent, @NonNull MapActivity mapActivity, boolean nightMode) {
		View view = UiUtilities.inflate(parent.getContext(), nightMode, R.layout.quick_action_with_text, parent, false);

		((TextView) view.findViewById(R.id.text)).setText(
				R.string.quick_action_navigation_voice_descr);

		parent.addView(view);
	}

	@Override
	public String getActionText(@NonNull OsmandApplication app) {
		return app.getSettings().VOICE_MUTE.get()
				? app.getString(R.string.quick_action_navigation_voice_off)
				: app.getString(R.string.quick_action_navigation_voice_on);
	}

	@Override
	public boolean isActionWithSlash(@NonNull OsmandApplication app) {
		return !app.getSettings().VOICE_MUTE.get();
	}
}
