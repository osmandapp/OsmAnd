package net.osmand.plus.quickaction.actions;

import static net.osmand.plus.quickaction.QuickActionIds.NEXT_PROFILE_PROFILE_ACTION;

import net.osmand.plus.R;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;

public class NextAppProfileAction extends BaseSwitchAppModeAction {

	public static final QuickActionType TYPE = new QuickActionType(NEXT_PROFILE_PROFILE_ACTION,
			"change.profile.next", NextAppProfileAction.class)
			.nameRes(R.string.quick_action_next_app_profile)
			.iconRes(R.drawable.ic_action_profile_next).nonEditable()
			.category(QuickActionType.SETTINGS)
			.nameActionRes(R.string.shared_string_change);

	public NextAppProfileAction() {
		super(TYPE);
	}

	public NextAppProfileAction(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	protected boolean shouldChangeForward() {
		return true;
	}

	@Override
	public int getQuickActionDescription() {
		return R.string.key_event_action_next_app_profile;
	}
}
