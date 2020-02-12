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

	public NavVoiceAction() {
		super(TYPE);
	}

	public NavVoiceAction(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	public void execute(MapActivity activity) {
		boolean mute = activity.getMyApplication().getSettings().VOICE_MUTE.get();
		activity.getMyApplication().getSettings().VOICE_MUTE.set(!mute);
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
