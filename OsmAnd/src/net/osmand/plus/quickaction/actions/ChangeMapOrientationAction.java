package net.osmand.plus.quickaction.actions;

import static net.osmand.plus.quickaction.QuickActionIds.CHANGE_MAP_ORIENTATION_ACTION;

import android.content.Context;
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
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.enums.CompassMode;
import net.osmand.plus.settings.enums.ThemeUsageContext;
import net.osmand.plus.utils.UiUtilities;

public class ChangeMapOrientationAction extends QuickAction {

	public static final QuickActionType TYPE = new QuickActionType(CHANGE_MAP_ORIENTATION_ACTION,
			"change.map.orientation", ChangeMapOrientationAction.class)
			.nameActionRes(R.string.shared_string_change)
			.nameRes(R.string.rotate_map_to)
			.iconRes(R.drawable.ic_action_compass_rotated)
			.category(QuickActionType.SETTINGS)
			.nonEditable();

	public ChangeMapOrientationAction() {
		super(TYPE);
	}

	public ChangeMapOrientationAction(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	public void execute(@NonNull MapActivity mapActivity) {
		mapActivity.getMapViewTrackingUtilities().requestSwitchCompassToNextMode();
	}

	@Override
	public int getIconRes(Context context) {
		OsmandApplication app = (OsmandApplication) context.getApplicationContext();
		OsmandSettings settings = app.getSettings();
		CompassMode compassMode = settings.getCompassMode();
		boolean nightMode = app.getDaynightHelper().isNightMode(ThemeUsageContext.APP);
		return compassMode.getIconId(nightMode);
	}

	@Override
	public void drawUI(@NonNull ViewGroup parent, @NonNull MapActivity mapActivity, boolean nightMode) {
		View view = UiUtilities.inflate(parent.getContext(), nightMode, R.layout.quick_action_with_text, parent, false);
		((TextView) view.findViewById(R.id.text))
				.setText(R.string.quick_action_switch_compass_desc);
		parent.addView(view);
	}
}
