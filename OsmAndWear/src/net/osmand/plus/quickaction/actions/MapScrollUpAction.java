package net.osmand.plus.quickaction.actions;

import static net.osmand.plus.quickaction.QuickActionIds.MAP_SCROLL_UP_ACTION;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.helpers.MapScrollHelper;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;

public class MapScrollUpAction extends BaseMapScrollAction {

	public static final QuickActionType TYPE = new QuickActionType(MAP_SCROLL_UP_ACTION,
			"map.scroll.up", MapScrollUpAction.class)
			.nameRes(R.string.quick_action_move_map_up)
			.iconRes(R.drawable.ic_action_map_move_up).nonEditable()
			.category(QuickActionType.MAP_INTERACTIONS)
			.nameActionRes(R.string.shared_string_move);

	public MapScrollUpAction() {
		super(TYPE);
	}

	public MapScrollUpAction(QuickAction quickAction) {
		super(quickAction);
	}

	@NonNull
	@Override
	protected MapScrollHelper.ScrollDirection getScrollingDirection() {
		return MapScrollHelper.ScrollDirection.UP;
	}

	@Override
	public int getQuickActionDescription() {
		return R.string.key_event_action_move_up;
	}
}
