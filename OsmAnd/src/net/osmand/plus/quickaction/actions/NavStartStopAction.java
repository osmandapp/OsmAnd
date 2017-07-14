package net.osmand.plus.quickaction.actions;

import android.content.Context;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.other.DestinationReachedMenu;
import net.osmand.plus.quickaction.QuickAction;

public class NavStartStopAction extends QuickAction {

    public static final int TYPE = 25;
    private static final String KEY_DIALOG = "dialog";

    public NavStartStopAction() {
        super(TYPE);
    }

    public NavStartStopAction(QuickAction quickAction) {
        super(quickAction);
    }

    @Override
    public void execute(MapActivity activity) {
        if (activity.getRoutingHelper().isFollowingMode()) {
            if (Boolean.valueOf(getParams().get(KEY_DIALOG))) {
                DestinationReachedMenu.show(activity);
            } else {
                activity.getMapLayers().getMapControlsLayer().stopNavigation();
            }
        } else {
            activity.getMapLayers().getMapControlsLayer().showNavigationDialog();
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
        return application.getRoutingHelper().isFollowingMode()
                ? application.getString(R.string.cancel_navigation)
                : application.getString(R.string.follow);
    }

    @Override
    public int getIconRes(Context context) {
        if (context instanceof MapActivity) {
            return ((MapActivity) context).getRoutingHelper().isFollowingMode()
                    ? R.drawable.ic_action_target
                    : R.drawable.ic_action_start_navigation;
        }
        return super.getIconRes(context);
    }
}
