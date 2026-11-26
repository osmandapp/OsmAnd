package net.osmand.plus.views.mapwidgets.configure.settings;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import net.osmand.plus.R;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.widgets.GlideTargetWidget;
import net.osmand.plus.widgets.alert.AlertDialogData;
import net.osmand.plus.widgets.alert.SingleSelectionDialogFragment;

public class GlideTargetWidgetInfoFragment extends BaseSimpleWidgetInfoFragment {

	private static final String KEY_TARGET_ELEVATION_MODE = "target_elevation_mode";
	private static final String MODE_REQUEST_KEY = "mode_request_key";

	private GlideTargetWidget widget;

	private boolean targetElevationMode;

	private TextView modeDescription;

	@NonNull
	@Override
	public WidgetType getWidget() {
		return WidgetType.GLIDE_TARGET;
	}

	@Override
	protected void initParams(@NonNull Bundle bundle) {
		super.initParams(bundle);
		if (widgetInfo != null) {
			widget = ((GlideTargetWidget) widgetInfo.widget);
			boolean defaultMode = widget.getModePreference().getModeValue(appMode);
			targetElevationMode = bundle.getBoolean(KEY_TARGET_ELEVATION_MODE, defaultMode);
		} else {
			dismiss();
		}
	}

	@Override
	protected void setupMainContent(@NonNull ViewGroup container) {
		inflate(R.layout.fragment_widget_settings_glide_target, container);

		View modeButton = view.findViewById(R.id.mode_container);
		modeButton.setOnClickListener(v -> showModeDialog());
		modeButton.setBackground(getPressedStateDrawable());
		modeDescription = view.findViewById(R.id.mode_description);
		updateModeDescription();
		SingleSelectionDialogFragment.setResultListener(this, MODE_REQUEST_KEY, index -> {
			targetElevationMode = index == 1;
			updateModeDescription();
		});
	}

	private void showModeDialog() {
		CharSequence[] items = {getString(getModeTitleId(false)), getString(getModeTitleId(true))};
		AlertDialogData dialogData = new AlertDialogData(modeDescription.getContext(), nightMode)
				.setTitle(R.string.shared_string_mode)
				.setControlsColor(ColorUtilities.getActiveColor(app, nightMode));

		SingleSelectionDialogFragment.showInstance(this, MODE_REQUEST_KEY, dialogData, items, targetElevationMode ? 1 : 0);
	}

	private void updateModeDescription() {
		modeDescription.setText(getModeTitleId(targetElevationMode));
	}

	@StringRes
	private static int getModeTitleId(boolean targetElevationMode) {
		return targetElevationMode ? R.string.target_elevation : R.string.glide_ratio_to_target;
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(KEY_TARGET_ELEVATION_MODE, targetElevationMode);
	}

	@Override
	protected void applySettings() {
		super.applySettings();
		widget.getModePreference().setModeValue(appMode, targetElevationMode);
	}
}
