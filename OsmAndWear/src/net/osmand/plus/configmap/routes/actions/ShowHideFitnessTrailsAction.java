package net.osmand.plus.configmap.routes.actions;

import static net.osmand.plus.quickaction.QuickActionIds.SHOW_HIDE_CYCLE_ROUTES_ACTION;
import static net.osmand.plus.quickaction.QuickActionIds.SHOW_HIDE_FITNESS_TRAILS_ROUTES_ACTION;

import androidx.annotation.NonNull;

import net.osmand.osm.OsmRouteType;
import net.osmand.plus.R;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;

public class ShowHideFitnessTrailsAction extends BaseRouteQuickAction {

	public static final QuickActionType TYPE = new QuickActionType(SHOW_HIDE_FITNESS_TRAILS_ROUTES_ACTION,
			"fitness_trails.routes.showhide", ShowHideFitnessTrailsAction.class)
			.nameActionRes(R.string.quick_action_verb_show_hide)
			.nameRes(R.string.rendering_attr_showFitnessTrails_name)
			.iconRes(R.drawable.mx_sport_athletics)
			.category(QuickActionType.CONFIGURE_MAP)
			.nonEditable();

	public ShowHideFitnessTrailsAction() {
		super(TYPE);
	}

	public ShowHideFitnessTrailsAction(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	public QuickActionType getActionType() {
		return TYPE;
	}

	@NonNull
	@Override
	protected OsmRouteType getOsmRouteType() {
		return OsmRouteType.FITNESS;
	}
}
