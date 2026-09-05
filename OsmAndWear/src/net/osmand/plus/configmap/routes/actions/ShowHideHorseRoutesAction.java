package net.osmand.plus.configmap.routes.actions;

import static net.osmand.plus.quickaction.QuickActionIds.SHOW_HIDE_HORSE_ROUTES_ACTION;

import androidx.annotation.NonNull;

import net.osmand.osm.OsmRouteType;
import net.osmand.plus.R;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;

public class ShowHideHorseRoutesAction extends BaseRouteQuickAction {

	public static final QuickActionType TYPE = new QuickActionType(SHOW_HIDE_HORSE_ROUTES_ACTION,
			"horse.routes.showhide", ShowHideHorseRoutesAction.class)
			.nameActionRes(R.string.quick_action_verb_show_hide)
			.nameRes(R.string.rendering_attr_horseRoutes_name)
			.iconRes(R.drawable.ic_action_horse)
			.category(QuickActionType.CONFIGURE_MAP)
			.nonEditable();

	public ShowHideHorseRoutesAction() {
		super(TYPE);
	}

	public ShowHideHorseRoutesAction(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	public QuickActionType getActionType() {
		return TYPE;
	}

	@NonNull
	@Override
	protected OsmRouteType getOsmRouteType() {
		return OsmRouteType.HORSE;
	}
}
