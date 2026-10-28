package net.osmand.plus.quickaction.actions;

import static net.osmand.plus.quickaction.QuickActionIds.MAP_ZOOM_OUT_ACTION;

import net.osmand.plus.R;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;

public class MapZoomOutAction extends BaseMapZoomAction {

	public static final QuickActionType TYPE = new QuickActionType(MAP_ZOOM_OUT_ACTION,
			"map.zoom.out", MapZoomOutAction.class)
			.nameRes(R.string.key_event_action_zoom_out)
			.iconRes(R.drawable.ic_action_magnifier_minus).nonEditable()
			.category(QuickActionType.MAP_INTERACTIONS)
			.nameActionRes(R.string.shared_string_map);

	public MapZoomOutAction() {
		super(TYPE);
	}

	public MapZoomOutAction(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	protected boolean shouldIncrement() {
		return false;
	}

	@Override
	public int getQuickActionDescription() {
		return R.string.key_event_action_zoom_out;
	}
}
