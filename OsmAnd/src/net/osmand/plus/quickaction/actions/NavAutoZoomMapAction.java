package net.osmand.plus.quickaction.actions;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.OsmandSettings.AutoZoomMap;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.quickaction.QuickAction;

public class NavAutoZoomMapAction extends QuickAction {

	public static final int TYPE = 23;

	public NavAutoZoomMapAction() {
		super(TYPE);
	}

	public NavAutoZoomMapAction(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	public void execute(MapActivity activity) {

		OsmandSettings settings = activity.getMyApplication().getSettings();
		if (settings.AUTO_ZOOM_MAP.get() == AutoZoomMap.NONE) {
			settings.AUTO_ZOOM_MAP.set(settings.AUTO_ZOOM_MAP_PREV.get());
		} else {
			settings.AUTO_ZOOM_MAP_PREV.set(settings.AUTO_ZOOM_MAP.get());
			settings.AUTO_ZOOM_MAP.set(AutoZoomMap.NONE);
		}
	}

	@Override
	public void drawUI(ViewGroup parent, MapActivity activity) {

		View view = LayoutInflater.from(parent.getContext())
				.inflate(R.layout.quick_action_with_text, parent, false);

		((TextView) view.findViewById(R.id.text)).setText(
				R.string.quick_action_auto_zoom_desc);

		parent.addView(view);
	}

	@Override
	public String getActionText(OsmandApplication application) {

		return application.getSettings().AUTO_ZOOM_MAP.get() != AutoZoomMap.NONE
				? application.getString(R.string.quick_action_auto_zoom_off)
				: application.getString(R.string.quick_action_auto_zoom_on);
	}

	@Override
	public boolean isActionWithSlash(OsmandApplication application) {

		return application.getSettings().AUTO_ZOOM_MAP.get() != AutoZoomMap.NONE;
	}
}
