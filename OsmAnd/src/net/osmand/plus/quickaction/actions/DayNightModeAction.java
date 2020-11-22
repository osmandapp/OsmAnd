package net.osmand.plus.quickaction.actions;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.helpers.enums.DayNightMode;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;

public class DayNightModeAction extends QuickAction {

	public static final QuickActionType TYPE = new QuickActionType(27,
			"daynight.switch", DayNightModeAction.class).
			nameRes(R.string.quick_action_day_night_switch_mode).iconRes(R.drawable.ic_action_map_day).nonEditable().
			category(QuickActionType.NAVIGATION);

	public DayNightModeAction() {super(TYPE);}

	public DayNightModeAction(QuickAction quickAction) {super(quickAction);}

	@Override
	public void execute(MapActivity activity) {
		if (activity.getMyApplication().getDaynightHelper().isNightMode()) {
			activity.getMyApplication().getSettings().DAYNIGHT_MODE.set(DayNightMode.DAY);
		} else {
			activity.getMyApplication().getSettings().DAYNIGHT_MODE.set(DayNightMode.NIGHT);
		}
	}

	@Override
	public void drawUI(ViewGroup parent, MapActivity activity) {
		View view = LayoutInflater.from(parent.getContext())
				.inflate(R.layout.quick_action_with_text, parent, false);
		((TextView) view.findViewById(R.id.text))
				.setText(R.string.quick_action_switch_day_night_descr);
		parent.addView(view);
	}

	@Override
	public int getIconRes(Context context) {
		if (context instanceof MapActivity
			&& ((MapActivity) context).getMyApplication().getDaynightHelper().isNightMode()) {
			return R.drawable.ic_action_map_day;
		}
		return R.drawable.ic_action_map_night;
	}

	@Override
	public String getActionText(OsmandApplication application) {
		if (application.getDaynightHelper().isNightMode()) {
			return String.format(application.getString(R.string.quick_action_day_night_mode),
				DayNightMode.DAY.toHumanString(application));
		} else {
			return String.format(application.getString(R.string.quick_action_day_night_mode),
				DayNightMode.NIGHT.toHumanString(application));
		}
	}
}
