package net.osmand.plus.configmap.routes.actions;

import static net.osmand.plus.quickaction.QuickActionIds.SHOW_HIDE_RUNNING_ROUTES_ACTION;

import androidx.annotation.NonNull;

import net.osmand.osm.OsmRouteType;
import net.osmand.plus.R;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;

public class ShowHideRunningRoutesAction extends BaseRouteQuickAction {

	public static final QuickActionType TYPE = new QuickActionType(SHOW_HIDE_RUNNING_ROUTES_ACTION,
			"running.routes.showhide", ShowHideRunningRoutesAction.class)
			.nameActionRes(R.string.quick_action_verb_show_hide)
			.nameRes(R.string.rendering_attr_showRunningRoutes_name)
			.iconRes(R.drawable.mx_running)
			.category(QuickActionType.CONFIGURE_MAP)
			.nonEditable();

	public ShowHideRunningRoutesAction() {
		super(TYPE);
	}

	public ShowHideRunningRoutesAction(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	public QuickActionType getActionType() {
		return TYPE;
	}

	@NonNull
	@Override
	protected OsmRouteType getOsmRouteType() {
		return OsmRouteType.RUNNING;
	}
}
