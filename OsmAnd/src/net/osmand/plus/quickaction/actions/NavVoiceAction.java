package net.osmand.plus.quickaction.actions;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.quickaction.QuickAction;

public class NavVoiceAction extends QuickAction {
	public static final int TYPE = 11;

	private boolean voiceMute;

	public NavVoiceAction() {
		super(TYPE);
	}

	public NavVoiceAction(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	public void execute(MapActivity activity) {

		boolean voice = activity.getMyApplication().getSettings().VOICE_MUTE.get();

		activity.getMyApplication().getSettings().VOICE_MUTE.set(!voice);
		activity.getRoutingHelper().getVoiceRouter().setMute(!voice);
	}

	@Override
	public void drawUI(ViewGroup parent, MapActivity activity) {

		View view = LayoutInflater.from(parent.getContext())
				.inflate(R.layout.quick_action_with_text, parent, false);

		((TextView) view.findViewById(R.id.text)).setText(
				R.string.quick_action_navigation_voice_descr);

		parent.addView(view);
	}

	@Override
	public void checkState(OsmandApplication app) {
		voiceMute = app.getSettings().VOICE_MUTE.get();
	}

	@Override
	public String getActionText(OsmandApplication application) {

		return voiceMute
				? application.getString(R.string.quick_action_navigation_voice_off)
				: application.getString(R.string.quick_action_navigation_voice_on);
	}

	@Override
	public boolean isActionWithSlash() {

		return voiceMute;
	}
}
