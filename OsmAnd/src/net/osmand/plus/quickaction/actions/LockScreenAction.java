package net.osmand.plus.quickaction.actions;

import static net.osmand.plus.quickaction.MapButtonsHelper.KEY_EVENT_KEY;
import static net.osmand.plus.quickaction.QuickActionIds.LOCK_SCREEN_ACTION;

import android.content.Context;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.keyevent.KeySymbolMapper;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;
import net.osmand.plus.utils.UiUtilities;

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
	public void execute(@NonNull MapActivity mapActivity, @Nullable Bundle params) {
		KeyEvent event = null;
		if (params != null && params.containsKey(KEY_EVENT_KEY)) {
			event = params.getParcelable(KEY_EVENT_KEY);
		}
		toggleLockScreen(mapActivity, event);
	}

	private void toggleLockScreen(@NonNull MapActivity mapActivity, @Nullable KeyEvent event){
		mapActivity.getApp().getLockHelper().toggleLockScreen();
		mapActivity.getMapLayers().getMapQuickActionLayer().refreshLayer();
		showToast(mapActivity, event);
	}

	private void showToast(@NonNull MapActivity mapActivity, @Nullable KeyEvent event) {
		OsmandApplication app = mapActivity.getApp();
		String toastString;
		if (app.getLockHelper().isScreenLocked()) {
			if (event != null) {
				int keyCode = event.getKeyCode();
				String keyLabel = KeySymbolMapper.getKeySymbol(mapActivity, keyCode);
				toastString = app.getString(R.string.screen_is_locked_by_external_button, keyLabel);
			} else {
				toastString = app.getString(R.string.screen_is_locked_by_action_button);
			}
		} else {
			toastString = app.getString(R.string.screen_is_unlocked);
		}
		mapActivity.getApp().showToastMessage(toastString);
	}

	@Override
	public void drawUI(@NonNull ViewGroup parent, @NonNull MapActivity mapActivity, boolean nightMode) {
		View view = UiUtilities.inflate(parent.getContext(), nightMode, R.layout.quick_action_with_text, parent, false);
		((TextView) view.findViewById(R.id.text)).setText(mapActivity.getString(R.string.lock_screen_description));
		parent.addView(view);
	}

	@Override
	public int getIconRes(Context context) {
		OsmandApplication app = (OsmandApplication) context.getApplicationContext();
		return app.getLockHelper().isScreenLocked() ? R.drawable.ic_action_lock_open : R.drawable.ic_action_lock;
	}
}
