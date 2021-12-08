package net.osmand.plus.quickaction.actions;

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
import net.osmand.plus.settings.backend.preferences.CommonPreference;

public class DisplayPositionAction extends QuickAction {

	public static final int ENABLE_ICON_ID = R.drawable.ic_action_display_position_center;
	public static final int DISABLE_ICON_ID = R.drawable.ic_action_display_position_bottom;

	public static final QuickActionType TYPE =
			new QuickActionType(36, "display.position.switch", DisplayPositionAction.class)
					.nameActionRes(R.string.shared_string_change)
					.nameRes(R.string.always_center_position_on_map)
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
	public void execute(@NonNull MapActivity mapActivity) {
		CommonPreference<Boolean> pref = getPreference(mapActivity);
		boolean currentState = pref.get();
		pref.set(!currentState);
		mapActivity.updateLayers();
	}

	private CommonPreference<Boolean> getPreference(@NonNull Context ctx) {
		OsmandApplication app = (OsmandApplication) ctx.getApplicationContext();
		return app.getSettings().CENTER_POSITION_ON_MAP;
	}

	@Override
	public void drawUI(@NonNull ViewGroup parent, @NonNull MapActivity mapActivity) {
		LayoutInflater inflater = LayoutInflater.from(parent.getContext());
		View view = inflater.inflate(R.layout.quick_action_with_text, parent, false);
		TextView tvDescription = (TextView) view.findViewById(R.id.text);
		tvDescription.setText(R.string.quick_action_toggle_preference);
		parent.addView(view);
	}

	@Override
	public int getIconRes(Context context) {
		if (getPreference(context).get()) {
			return DISABLE_ICON_ID;
		} else {
			return ENABLE_ICON_ID;
		}
	}

	@Override
	public String getActionText(OsmandApplication application) {
		if (getPreference(application).get()) {
			return application.getString(R.string.shared_string_disable);
		} else {
			return application.getString(R.string.shared_string_enable);
		}
	}

}
