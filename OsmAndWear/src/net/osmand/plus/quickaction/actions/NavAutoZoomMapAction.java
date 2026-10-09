package net.osmand.plus.quickaction.actions;

import static net.osmand.plus.quickaction.QuickActionIds.NAV_AUTO_ZOOM_MAP_ACTION_ID;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;

public class NavAutoZoomMapAction extends QuickAction {

	public static final QuickActionType TYPE = new QuickActionType(NAV_AUTO_ZOOM_MAP_ACTION_ID,
			"nav.autozoom", NavAutoZoomMapAction.class).
			nameRes(R.string.quick_action_auto_zoom).iconRes(R.drawable.ic_action_search_dark).nonEditable().
			category(QuickActionType.NAVIGATION)
			.nameActionRes(R.string.quick_action_verb_turn_on_off);


	public NavAutoZoomMapAction() {
		super(TYPE);
	}

	public NavAutoZoomMapAction(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	public void execute(@NonNull MapActivity mapActivity) {
		OsmandSettings settings = mapActivity.getMyApplication().getSettings();
		settings.AUTO_ZOOM_MAP.set(!settings.AUTO_ZOOM_MAP.get());
		Toast.makeText(mapActivity, mapActivity.getString(!settings.AUTO_ZOOM_MAP.get()
				? R.string.quick_action_auto_zoom_off : R.string.quick_action_auto_zoom_on), Toast.LENGTH_SHORT).show();
	}

	@Override
	public void drawUI(@NonNull ViewGroup parent, @NonNull MapActivity mapActivity) {

		View view = LayoutInflater.from(parent.getContext())
				.inflate(R.layout.quick_action_with_text, parent, false);

		((TextView) view.findViewById(R.id.text)).setText(
				R.string.quick_action_auto_zoom_desc);

		parent.addView(view);
	}

	@Override
	public String getActionText(@NonNull OsmandApplication app) {

		return app.getSettings().AUTO_ZOOM_MAP.get()
				? app.getString(R.string.quick_action_auto_zoom_off) : app.getString(R.string.quick_action_auto_zoom_on);
	}

	@Override
	public boolean isActionWithSlash(@NonNull OsmandApplication app) {

		return app.getSettings().AUTO_ZOOM_MAP.get();
	}
}
