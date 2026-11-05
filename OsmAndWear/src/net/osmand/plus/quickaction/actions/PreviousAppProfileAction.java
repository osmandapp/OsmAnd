package net.osmand.plus.quickaction.actions;

import static net.osmand.plus.quickaction.QuickActionIds.PREVIOUS_PROFILE_PROFILE_ACTION;

import net.osmand.plus.R;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;

public class PreviousAppProfileAction extends BaseSwitchAppModeAction {

	public static final QuickActionType TYPE = new QuickActionType(PREVIOUS_PROFILE_PROFILE_ACTION,
			"change.profile.previous", PreviousAppProfileAction.class)
			.nameRes(R.string.quick_action_previous_app_profile)
			.iconRes(R.drawable.ic_action_profile_previous).nonEditable()
			.category(QuickActionType.SETTINGS)
			.nameActionRes(R.string.shared_string_change);

	public PreviousAppProfileAction() {
		super(TYPE);
	}

	public PreviousAppProfileAction(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	protected boolean shouldChangeForward() {
		return false;
	}

	@Override
	public int getQuickActionDescription() {
		return R.string.key_event_action_previous_app_profile;
	}
}
