package net.osmand.plus.quickaction.actions;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.quickaction.QuickAction;

public class DayNightModeAction extends QuickAction {
	public static final int TYPE = 27;

	public DayNightModeAction() {super(TYPE);}

	public DayNightModeAction(QuickAction quickAction) {super(quickAction);}

	@Override
	public void execute(MapActivity activity) {
		switch (activity.getMyApplication().getSettings().DAYNIGHT_MODE.get()){
			case DAY: {
				activity.getMyApplication().getSettings().DAYNIGHT_MODE.set(OsmandSettings.DayNightMode.NIGHT);
				break;
			}
			case NIGHT: {
				activity.getMyApplication().getSettings().DAYNIGHT_MODE.set(OsmandSettings.DayNightMode.DAY);
				break;
			}
			case AUTO: {
				activity.getMyApplication().getSettings().DAYNIGHT_MODE.set(OsmandSettings.DayNightMode.DAY);
				break;
			}
			case SENSOR: {
				activity.getMyApplication().getSettings().DAYNIGHT_MODE.set(OsmandSettings.DayNightMode.DAY);
				break;
			}
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
		if(context instanceof MapActivity) {
			switch (((MapActivity) context).getMyApplication().getSettings().DAYNIGHT_MODE.get()) {
				case NIGHT: {
					return R.drawable.ic_action_map_night;
				}
				case AUTO: {
					return R.drawable.ic_action_map_sunst;
				}
				case SENSOR: {
					return R.drawable.ic_action_map_light_sensor;
				}
			}
		}
		return R.drawable.ic_action_map_day;
	}

	@Override
	public String getActionText(OsmandApplication application) {
		return application.getSettings().DAYNIGHT_MODE.get().toHumanString(application) + " Mode";
	}
}
