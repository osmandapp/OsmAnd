package net.osmand.plus.configmap.routes.actions;

import static net.osmand.plus.quickaction.QuickActionIds.SHOW_HIDE_SKI_SLOPES_ROUTES_ACTION;

import androidx.annotation.NonNull;

import net.osmand.osm.OsmRouteType;
import net.osmand.plus.R;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;

public class ShowHideSkiSlopesAction extends BaseRouteQuickAction {

	public static final QuickActionType TYPE = new QuickActionType(SHOW_HIDE_SKI_SLOPES_ROUTES_ACTION,
			"ski_slopes.routes.showhide", ShowHideSkiSlopesAction.class)
			.nameActionRes(R.string.quick_action_verb_show_hide)
			.nameRes(R.string.rendering_attr_pisteRoutes_name)
			.iconRes(R.drawable.ic_action_skiing)
			.category(QuickActionType.CONFIGURE_MAP)
			.nonEditable();

	public ShowHideSkiSlopesAction() {
		super(TYPE);
	}

	public ShowHideSkiSlopesAction(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	public QuickActionType getActionType() {
		return TYPE;
	}

	@NonNull
	@Override
	protected OsmRouteType getOsmRouteType() {
		return OsmRouteType.SKI;
	}
}
