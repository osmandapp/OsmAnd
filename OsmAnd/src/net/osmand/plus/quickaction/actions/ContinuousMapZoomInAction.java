package net.osmand.plus.quickaction.actions;

import static net.osmand.plus.quickaction.QuickActionIds.CONTINUOUS_MAP_ZOOM_IN_ACTION;

import net.osmand.plus.R;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;

public class ContinuousMapZoomInAction extends BaseMapZoomAction {

	public static final QuickActionType TYPE = new QuickActionType(CONTINUOUS_MAP_ZOOM_IN_ACTION,
			"map.zoom.continuous.in", ContinuousMapZoomInAction.class)
			.nameRes(R.string.key_event_action_continuous_zoom_in)
			.iconRes(R.drawable.ic_action_magnifier_plus).nonEditable()
			.category(QuickActionType.MAP_INTERACTIONS);

	public ContinuousMapZoomInAction() {
		super(TYPE);
	}

	public ContinuousMapZoomInAction(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	protected boolean isContinuous() {
		return true;
	}

	@Override
	protected boolean shouldIncrement() {
		return true;
	}

	@Override
	public int getQuickActionDescription() {
		return R.string.key_event_action_continuous_zoom_in;
	}

}
