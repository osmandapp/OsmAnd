package net.osmand.plus.quickaction.actions;

import static net.osmand.plus.quickaction.QuickActionIds.CONTINUOUS_MAP_ZOOM_OUT_ACTION;

import net.osmand.plus.R;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;

public class ContinuousMapZoomOutAction extends BaseMapZoomAction {

	public static final QuickActionType TYPE = new QuickActionType(CONTINUOUS_MAP_ZOOM_OUT_ACTION,
			"map.zoom.continuous.out", ContinuousMapZoomOutAction.class)
			.nameRes(R.string.key_event_action_continuous_zoom_out)
			.iconRes(R.drawable.ic_action_magnifier_minus).nonEditable()
			.category(QuickActionType.MAP_INTERACTIONS);

	public ContinuousMapZoomOutAction() {
		super(TYPE);
	}

	public ContinuousMapZoomOutAction(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	protected boolean isContinuous() {
		return true;
	}

	@Override
	protected boolean shouldIncrement() {
		return false;
	}

	@Override
	public int getQuickActionDescription() {
		return R.string.key_event_action_continuous_zoom_out;
	}

}