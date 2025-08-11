package net.osmand.plus.quickaction.actions;

import static net.osmand.plus.quickaction.QuickActionIds.DISPLAY_POSITION_ACTION_ID;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.utils.UiUtilities;

public class DisplayPositionAction extends QuickAction {

	public static final int ENABLE_ICON_ID = R.drawable.ic_action_display_position_center;
	public static final int DISABLE_ICON_ID = R.drawable.ic_action_display_position_bottom;

	public static final QuickActionType TYPE =
			new QuickActionType(DISPLAY_POSITION_ACTION_ID, "display.position.switch", DisplayPositionAction.class)
					.nameActionRes(R.string.shared_string_change)
					.nameRes(R.string.quick_action_display_position_in_center)
					.iconRes(ENABLE_ICON_ID)
					.nonEditable()
					.category(QuickActionType.SETTINGS);

	public DisplayPositionAction() {
		super(TYPE);
	}

	public DisplayPositionAction(QuickAction qa) {
		super(qa);
	}

	@Override
	public void execute(@NonNull MapActivity mapActivity, @Nullable Bundle params) {
		CommonPreference<Integer> pref = getPreference(mapActivity);
		int currentState = pref.get();
		pref.set((currentState == 2) ? 0 : currentState + 1);
		mapActivity.updateLayers();
	}

	private CommonPreference<Integer> getPreference(@NonNull Context ctx) {
		OsmandApplication app = (OsmandApplication) ctx.getApplicationContext();
		return app.getSettings().POSITION_PLACEMENT_ON_MAP;
	}

	@Override
	public void drawUI(@NonNull ViewGroup parent, @NonNull MapActivity mapActivity, boolean nightMode) {
		View view = UiUtilities.inflate(parent.getContext(), nightMode, R.layout.quick_action_with_text, parent, false);
		TextView tvDescription = view.findViewById(R.id.text);
		tvDescription.setText(R.string.quick_action_toggle_preference);
		parent.addView(view);
	}

	@Override
	public int getIconRes(Context context) {
		if (getPreference(context).get() == 1) {
			return DISABLE_ICON_ID;
		} else {
			return ENABLE_ICON_ID;
		}
	}

	@Override
	public String getActionText(@NonNull OsmandApplication app) {
		String nameRes = app.getString(getNameRes());
		String actionName;
		if (getPreference(app).get() == 1) {
			actionName = app.getString(R.string.shared_string_disable);
		} else {
			actionName = app.getString(R.string.shared_string_enable);
		}
		return app.getString(R.string.ltr_or_rtl_combine_via_dash, actionName, nameRes);
	}
}
