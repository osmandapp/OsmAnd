package net.osmand.plus.quickaction.actions;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.widget.SwitchCompat;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.other.DestinationReachedMenu;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;
import net.osmand.plus.routing.RoutingHelper;

public class NavStartStopAction extends QuickAction {

	private static final String KEY_DIALOG = "dialog";
	public static final QuickActionType TYPE = new QuickActionType(25,
			"nav.startstop", NavStartStopAction .class).
			nameRes(R.string.quick_action_start_stop_navigation).iconRes(R.drawable.ic_action_start_navigation).nonEditable().
			category(QuickActionType.NAVIGATION);



	public NavStartStopAction() {
		super(TYPE);
	}

	public NavStartStopAction(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	public void execute(MapActivity activity) {
		RoutingHelper helper = activity.getRoutingHelper();
		if (helper.isPauseNavigation() || helper.isFollowingMode()) {
			if (Boolean.valueOf(getParams().get(KEY_DIALOG))) {
				DestinationReachedMenu.show(activity);
			} else {
				activity.getMapLayers().getMapControlsLayer().stopNavigation();
			}
		} else {
			activity.getMapLayers().getMapControlsLayer().doRoute(false);
		}
	}

	@Override
	public void drawUI(ViewGroup parent, MapActivity activity) {
		View view = LayoutInflater.from(parent.getContext())
				.inflate(R.layout.quick_action_start_stop_navigation, parent, false);

		final SwitchCompat showDialogSwitch = (SwitchCompat) view.findViewById(R.id.show_dialog_switch);

		if (!getParams().isEmpty()) {
			showDialogSwitch.setChecked(Boolean.valueOf(getParams().get(KEY_DIALOG)));
		}

		view.findViewById(R.id.show_dialog_row).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				showDialogSwitch.setChecked(!showDialogSwitch.isChecked());
			}
		});

		parent.addView(view);
	}

	@Override
	public boolean fillParams(View root, MapActivity activity) {
		getParams().put(KEY_DIALOG, Boolean
				.toString(((SwitchCompat) root.findViewById(R.id.show_dialog_switch)).isChecked()));
		return true;
	}

	@Override
	public String getActionText(OsmandApplication application) {
		RoutingHelper helper = application.getRoutingHelper();
		if (helper.isPauseNavigation() || helper.isFollowingMode()) {
			return application.getString(R.string.cancel_navigation);
		}
		return application.getString(R.string.follow);
	}

	@Override
	public int getIconRes(Context context) {
		if (context instanceof MapActivity) {
			RoutingHelper helper = ((MapActivity) context).getRoutingHelper();
			if (!helper.isRoutePlanningMode() && !helper.isFollowingMode()) {
				return ((MapActivity) context).getMapActions().getRouteMode(null).getIconRes();
			}
			return helper.getAppMode().getIconRes();
		}
		return super.getIconRes(context);
	}

	@Override
	public boolean isActionWithSlash(OsmandApplication application) {
		RoutingHelper helper = application.getRoutingHelper();
		return helper.isPauseNavigation() || helper.isFollowingMode();
	}
}