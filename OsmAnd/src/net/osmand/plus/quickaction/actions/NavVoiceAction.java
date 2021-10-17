package net.osmand.plus.quickaction.actions;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;

public class NavVoiceAction extends QuickAction {
	public static final QuickActionType TYPE = new QuickActionType(11,
			"nav.voice", NavVoiceAction.class).
			nameRes(R.string.quick_action_navigation_voice).iconRes(R.drawable.ic_action_volume_up).nonEditable().
			category(QuickActionType.NAVIGATION);

	public NavVoiceAction() {
		super(TYPE);
	}

	public NavVoiceAction(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	public void execute(@NonNull MapActivity mapActivity) {
		boolean mute = mapActivity.getMyApplication().getSettings().VOICE_MUTE.get();
		mapActivity.getMyApplication().getSettings().VOICE_MUTE.set(!mute);
	}

	@Override
	public void drawUI(@NonNull ViewGroup parent, @NonNull MapActivity mapActivity) {
		View view = LayoutInflater.from(parent.getContext())
				.inflate(R.layout.quick_action_with_text, parent, false);

		((TextView) view.findViewById(R.id.text)).setText(
				R.string.quick_action_navigation_voice_descr);

		parent.addView(view);
	}

	@Override
	public String getActionText(OsmandApplication application) {
		return application.getSettings().VOICE_MUTE.get()
				? application.getString(R.string.quick_action_navigation_voice_off)
				: application.getString(R.string.quick_action_navigation_voice_on);
	}

	@Override
	public boolean isActionWithSlash(OsmandApplication application) {
		return !application.getSettings().VOICE_MUTE.get();
	}
}
