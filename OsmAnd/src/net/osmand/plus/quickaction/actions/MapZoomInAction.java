package net.osmand.plus.quickaction.actions;

import static net.osmand.plus.quickaction.QuickActionIds.MAP_ZOOM_IN_ACTION;

import net.osmand.plus.R;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;

public class MapZoomInAction extends BaseMapZoomAction {

	public static final QuickActionType TYPE = new QuickActionType(MAP_ZOOM_IN_ACTION,
			"map.zoom.in", MapZoomInAction.class)
			.nameRes(R.string.key_event_action_zoom_in)
			.iconRes(R.drawable.ic_action_magnifier_plus).nonEditable()
			.category(QuickActionType.MAP_INTERACTIONS)
			.nameActionRes(R.string.shared_string_map);

	public MapZoomInAction() {
		super(TYPE);
	}

	public MapZoomInAction(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	protected boolean shouldIncrement() {
		return true;
	}

	@Override
	public int getQuickActionDescription() {
		return R.string.key_event_action_zoom_in;
	}
}
