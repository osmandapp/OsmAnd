package net.osmand.plus.plugins.monitoring.widgets;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.R;
import net.osmand.plus.plugins.monitoring.widgets.TripRecordingMaxSpeedWidgetState.MaxSpeedMode;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.views.mapwidgets.configure.settings.BaseSimpleWidgetInfoFragment;
import net.osmand.plus.widgets.alert.AlertDialogData;
import net.osmand.plus.widgets.alert.CustomAlert;

public class TripRecordingMaxSpeedWidgetInfoFragment extends BaseSimpleWidgetInfoFragment {

	private static final String MAX_SPEED_MODE = "max_speed_mode";

	@Nullable
	private OsmandPreference<MaxSpeedMode> modeOsmandPreference;
	private ImageView iconMode;
	private TextView titleMode;
	private TextView descriptionMode;

	private int selectedMode;

	@Override
	protected void initParams(@NonNull Bundle bundle) {
		super.initParams(bundle);
		if (widgetInfo != null) {
			TripRecordingMaxSpeedWidget widget = (TripRecordingMaxSpeedWidget) widgetInfo.widget;
			modeOsmandPreference = widget.getMaxSpeedModeOsmandPreference();
		}
		selectedMode = bundle.getInt(MAX_SPEED_MODE, modeOsmandPreference != null ?
				modeOsmandPreference.getModeValue(appMode).ordinal() : MaxSpeedMode.TOTAL.ordinal());
	}

	@Override
	protected void setupMainContent(@NonNull ViewGroup container) {
		if (modeOsmandPreference != null) {
			View modeButton = inflate(R.layout.bottom_sheet_item_with_descr_72dp, container);
			iconMode = container.findViewById(R.id.icon);
			titleMode = container.findViewById(R.id.title);
			titleMode.setText(R.string.shared_string_mode);
			descriptionMode = container.findViewById(R.id.description);
			iconMode.setImageDrawable(app.getUIUtilities().getIcon(modeOsmandPreference.get().getIcon(nightMode)));
			modeButton.setOnClickListener(v -> showModeDialog());
			modeButton.setBackground(getPressedStateDrawable());
		}
	}

	private void showModeDialog() {
		CharSequence[] items = new CharSequence[MaxSpeedMode.values().length];
		for (int i = 0; i < MaxSpeedMode.values().length; i++) {
			items[i] = getString(MaxSpeedMode.values()[i].getTitleId());
		}

		AlertDialogData dialogData = new AlertDialogData(titleMode.getContext(), nightMode)
				.setTitle(R.string.shared_string_mode)
				.setControlsColor(ColorUtilities.getActiveColor(app, nightMode));

		CustomAlert.showSingleSelection(dialogData, items, selectedMode, v -> {
			selectedMode = (int) v.getTag();
			updateModeSetting();
		});
	}

	private void updateModeSetting() {
		descriptionMode.setText(getString(MaxSpeedMode.values()[selectedMode].getTitleId()));
		iconMode.setImageDrawable(app.getUIUtilities().getIcon(MaxSpeedMode.values()[selectedMode].getIcon(nightMode)));
	}

	@Override
	protected void applySettings() {
		super.applySettings();
		if (modeOsmandPreference != null) {
			modeOsmandPreference.setModeValue(appMode, MaxSpeedMode.values()[selectedMode]);
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(MAX_SPEED_MODE, selectedMode);
	}

	@Override
	public void onResume() {
		super.onResume();
		updateModeSetting();
	}
}