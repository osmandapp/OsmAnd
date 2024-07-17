package net.osmand.plus.quickaction.actions;

import static net.osmand.plus.quickaction.QuickActionIds.MAP_SCROLL_RIGHT_ACTION;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.helpers.MapScrollHelper;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;

public class MapScrollRightAction extends BaseMapScrollAction{

	public static final QuickActionType TYPE = new QuickActionType(MAP_SCROLL_RIGHT_ACTION,
			"map.scroll.right", MapScrollRightAction.class)
			.nameRes(R.string.quick_action_move_map_right)
			.iconRes(R.drawable.ic_action_map_move_right).nonEditable()
			.category(QuickActionType.MAP_INTERACTIONS)
			.nameActionRes(R.string.shared_string_move);

	public MapScrollRightAction() {
		super(TYPE);
	}

	public MapScrollRightAction(QuickAction quickAction) {
		super(quickAction);
	}

	@NonNull
	@Override
	protected MapScrollHelper.ScrollDirection getScrollingDirection() {
		return MapScrollHelper.ScrollDirection.RIGHT;
	}

	@Override
	public int getQuickActionDescription() {
		return R.string.key_event_action_move_right;
	}
}
