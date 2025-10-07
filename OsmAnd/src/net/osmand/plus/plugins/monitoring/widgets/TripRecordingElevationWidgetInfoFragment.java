package net.osmand.plus.plugins.monitoring.widgets;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.R;
import net.osmand.plus.plugins.monitoring.widgets.TripRecordingElevationWidgetState.TripRecordingElevationMode;
import net.osmand.plus.plugins.monitoring.widgets.TripRecordingSlopeWidgetState.AverageSlopeMode;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.views.mapwidgets.configure.settings.BaseSimpleWidgetInfoFragment;
import net.osmand.plus.widgets.alert.AlertDialogData;
import net.osmand.plus.widgets.alert.CustomAlert;

public class TripRecordingElevationWidgetInfoFragment extends BaseSimpleWidgetInfoFragment {

	private static final String ELEVATION_MODE = "elevation_mode";

	@Nullable
	private OsmandPreference<TripRecordingElevationMode> elevationModePreference;
	private TripRecordingElevationWidget widget;
	private TextView titleMode;
	private TextView descriptionMode;
	private ImageView iconMode;
	private int selectedMode;


	@Override
	protected void initParams(@NonNull Bundle bundle) {
		super.initParams(bundle);
		if (widgetInfo != null) {
			widget = (TripRecordingElevationWidget) widgetInfo.widget;
			elevationModePreference = widget.elevationModePreference();
		}
		selectedMode = bundle.getInt(ELEVATION_MODE, elevationModePreference != null ?
				elevationModePreference.getModeValue(appMode).ordinal() : TripRecordingElevationMode.TOTAL.ordinal());
	}

	@Override
	protected void setupMainContent(@NonNull ViewGroup container) {
		if(elevationModePreference != null){
			View modeButton = inflate(R.layout.bottom_sheet_item_with_descr_72dp, container);
			iconMode = container.findViewById(R.id.icon);
			titleMode = container.findViewById(R.id.title);
			titleMode.setText(R.string.shared_string_mode);
			descriptionMode = container.findViewById(R.id.description);
			iconMode.setImageDrawable(app.getUIUtilities().getIcon(elevationModePreference.get().getIcon(widget.isUphillType(), nightMode)));
			modeButton.setOnClickListener(v -> showElevationModeDialog());
			modeButton.setBackground(getPressedStateDrawable());
		}
	}

	private void showElevationModeDialog() {
		CharSequence[] items = new CharSequence[TripRecordingElevationMode.values().length];
		for (int i = 0; i < TripRecordingElevationMode.values().length; i++) {
			items[i] = getString(TripRecordingElevationMode.values()[i].getTitleId(true));
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
		descriptionMode.setText(getString(TripRecordingElevationMode.values()[selectedMode].getTitleId(widget.isUphillType())));
		iconMode.setImageDrawable(app.getUIUtilities().getIcon(TripRecordingElevationMode.values()[selectedMode].getIcon(widget.isUphillType(), nightMode)));
	}

	@Override
	protected void applySettings() {
		super.applySettings();
		if (elevationModePreference != null) {
			elevationModePreference.setModeValue(appMode, TripRecordingElevationMode.values()[selectedMode]);
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(ELEVATION_MODE, selectedMode);
	}

	@Override
	public void onResume() {
		super.onResume();
		updateModeSetting();
	}
}