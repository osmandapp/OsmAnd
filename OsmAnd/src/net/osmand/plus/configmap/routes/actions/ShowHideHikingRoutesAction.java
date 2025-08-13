package net.osmand.plus.configmap.routes.actions;

import static net.osmand.plus.quickaction.QuickActionIds.SHOW_HIDE_HIKING_ROUTES_ACTION;

import androidx.annotation.NonNull;

import net.osmand.osm.OsmRouteType;
import net.osmand.plus.R;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;

public class ShowHideHikingRoutesAction extends BaseRouteQuickAction {

	public static final QuickActionType TYPE = new QuickActionType(SHOW_HIDE_HIKING_ROUTES_ACTION,
			"hiking.routes.showhide", ShowHideHikingRoutesAction.class)
			.nameActionRes(R.string.quick_action_verb_show_hide)
			.nameRes(R.string.rendering_attr_hikingRoutesOSMC_name)
			.iconRes(R.drawable.ic_action_trekking_dark)
			.category(QuickActionType.CONFIGURE_MAP)
			.nonEditable();

	public ShowHideHikingRoutesAction() {
		super(TYPE);
	}

	public ShowHideHikingRoutesAction(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	public QuickActionType getActionType() {
		return TYPE;
	}

	@NonNull
	@Override
	protected OsmRouteType getOsmRouteType() {
		return OsmRouteType.HIKING;
	}
}
