package net.osmand.plus.quickaction.actions;

import static net.osmand.plus.quickaction.QuickActionIds.DAY_NIGHT_MODE_ACTION_ID;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.enums.DayNightMode;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;
import net.osmand.plus.settings.enums.ThemeUsageContext;
import net.osmand.plus.utils.UiUtilities;

public class DayNightModeAction extends QuickAction {

	public static final QuickActionType TYPE = new QuickActionType(DAY_NIGHT_MODE_ACTION_ID,
			"daynight.switch", DayNightModeAction.class).
			nameRes(R.string.map_mode).iconRes(R.drawable.ic_action_map_day).nonEditable().
			category(QuickActionType.CONFIGURE_MAP).nameActionRes(R.string.shared_string_change);

	public DayNightModeAction() {super(TYPE);}

	public DayNightModeAction(QuickAction quickAction) {super(quickAction);}

	@Override
	public void execute(@NonNull MapActivity mapActivity, @Nullable Bundle params) {
		if (mapActivity.getApp().getDaynightHelper().isNightMode(ThemeUsageContext.MAP)) {
			mapActivity.getSettings().DAYNIGHT_MODE.set(DayNightMode.DAY);
		} else {
			mapActivity.getSettings().DAYNIGHT_MODE.set(DayNightMode.NIGHT);
		}
	}

	@Override
	public void drawUI(@NonNull ViewGroup parent, @NonNull MapActivity mapActivity, boolean nightMode) {
		View view = UiUtilities.inflate(parent.getContext(), nightMode, R.layout.quick_action_with_text, parent, false);
		((TextView) view.findViewById(R.id.text))
				.setText(R.string.quick_action_switch_day_night_descr);
		parent.addView(view);
	}

	@Override
	public int getIconRes(Context context) {
		OsmandApplication app = (OsmandApplication) context.getApplicationContext();
		boolean nightMode = app.getDaynightHelper().isNightMode(ThemeUsageContext.MAP);
		return nightMode ? R.drawable.ic_action_map_day : R.drawable.ic_action_map_night;
	}

	@Override
	public String getActionText(@NonNull OsmandApplication app) {
		if (app.getDaynightHelper().isNightMode(ThemeUsageContext.MAP)) {
			return String.format(app.getString(R.string.quick_action_day_night_mode),
				DayNightMode.DAY.toHumanString(app));
		} else {
			return String.format(app.getString(R.string.quick_action_day_night_mode),
				DayNightMode.NIGHT.toHumanString(app));
		}
	}
}
