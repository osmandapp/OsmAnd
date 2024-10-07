package net.osmand.plus.quickaction.actions;

import static net.osmand.plus.quickaction.QuickActionIds.LOCK_SCREEN_ACTION;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;

public class LockScreenAction extends QuickAction {

	public static final QuickActionType TYPE = new QuickActionType(LOCK_SCREEN_ACTION,
			"lock_screen_action", LockScreenAction.class)
			.nameRes(R.string.lock_screen)
			.iconRes(R.drawable.ic_action_touch_screen_lock)
			.category(QuickActionType.INTERFACE)
			.nameActionRes(R.string.quick_action_verb_turn_on_off);

	public LockScreenAction() {
		super(TYPE);
	}

	public LockScreenAction(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	public void execute(@NonNull MapActivity mapActivity) {
		mapActivity.getMyApplication().getLockHelper().toggleLockScreen();
		mapActivity.getMapLayers().getMapQuickActionLayer().refreshLayer();
	}

	@Override
	public void drawUI(@NonNull ViewGroup parent, @NonNull MapActivity mapActivity) {
		View view = LayoutInflater.from(parent.getContext())
				.inflate(R.layout.quick_action_with_text, parent, false);
		((TextView) view.findViewById(R.id.text)).setText(mapActivity.getString(R.string.lock_screen_description));
		parent.addView(view);
	}

	@Override
	public int getIconRes(Context context) {
		OsmandApplication app = (OsmandApplication) context.getApplicationContext();
		return app.getLockHelper().isScreenLocked() ? R.drawable.ic_action_touch_screen_unlock : R.drawable.ic_action_touch_screen_lock;
	}
}
