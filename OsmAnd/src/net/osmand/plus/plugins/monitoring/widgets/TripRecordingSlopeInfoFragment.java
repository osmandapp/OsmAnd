package net.osmand.plus.plugins.monitoring.widgets;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.R;
import net.osmand.plus.plugins.monitoring.widgets.TripRecordingSlopeWidgetState.AverageSlopeMode;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.views.mapwidgets.configure.settings.BaseSimpleWidgetInfoFragment;
import net.osmand.plus.widgets.alert.AlertDialogData;
import net.osmand.plus.widgets.alert.CustomAlert;

public class TripRecordingSlopeInfoFragment extends BaseSimpleWidgetInfoFragment {

	private static final String SLOPE_MODE = "slope_mode";

	@Nullable
	private OsmandPreference<AverageSlopeMode> modeOsmandPreference;
	private ImageView iconMode;
	private TextView titleMode;
	private TextView descriptionMode;

	private int selectedMode;


	@Override
	protected void initParams(@NonNull Bundle bundle) {
		super.initParams(bundle);
		if (widgetInfo != null) {
			TripRecordingSlopeWidget widget = (TripRecordingSlopeWidget) widgetInfo.widget;
			modeOsmandPreference = widget.getAverageSlopeModeOsmandPreference();
		}
		selectedMode = bundle.getInt(SLOPE_MODE, modeOsmandPreference != null ?
				modeOsmandPreference.getModeValue(appMode).ordinal() : AverageSlopeMode.LAST_UPHILL.ordinal());
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
		CharSequence[] items = new CharSequence[AverageSlopeMode.values().length];
		for (int i = 0; i < AverageSlopeMode.values().length; i++) {
			items[i] = getString(AverageSlopeMode.values()[i].getTitleId());
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
		descriptionMode.setText(getString(AverageSlopeMode.values()[selectedMode].getTitleId()));
		iconMode.setImageDrawable(app.getUIUtilities().getIcon(AverageSlopeMode.values()[selectedMode].getIcon(nightMode)));
	}

	@Override
	protected void applySettings() {
		super.applySettings();
		if (modeOsmandPreference != null) {
			modeOsmandPreference.setModeValue(appMode, AverageSlopeMode.values()[selectedMode]);
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(SLOPE_MODE, selectedMode);
	}

	@Override
	public void onResume() {
		super.onResume();
		updateModeSetting();
	}
}