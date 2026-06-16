package net.osmand.plus.views.mapwidgets.configure.settings;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.coordinates.CoordinateFormat;
import net.osmand.plus.settings.coordinates.CoordinateFormatFormatter;
import net.osmand.plus.settings.coordinates.CoordinateFormatSelectorBottomSheet;
import net.osmand.plus.settings.fragments.CoordinatesFormatFragment;
import net.osmand.plus.views.layers.MapInfoLayer;
import net.osmand.plus.views.mapwidgets.widgets.CoordinatesBaseWidget;

public class CoordinatesWidgetInfoFragment extends WidgetInfoBaseFragment {

	private static final String REQUEST_COORDINATES_WIDGET_FORMAT = "coordinates_widget_format";
	private static final String SELECTED_FORMAT_ID_KEY = "selected_format_id";

	private String selectedFormatId;
	private TextView summary;

	@Override
	protected void initParams(@NonNull Bundle bundle) {
		super.initParams(bundle);
		if (widgetInfo != null && widgetInfo.widget instanceof CoordinatesBaseWidget widget) {
			String storedFormatId = widget.getCoordinateFormatPref().getModeValue(appMode);
			selectedFormatId = bundle.getString(SELECTED_FORMAT_ID_KEY, storedFormatId);
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(SELECTED_FORMAT_ID_KEY, selectedFormatId);
	}

	@Override
	protected void setupMainContent(@NonNull ViewGroup container) {
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.main_container), true);
		View row = inflate(R.layout.coordinates_widget_format_setting, container, false);
		container.addView(row);
		row.setBackground(getPressedStateDrawable());
		summary = row.findViewById(R.id.summary);
		row.setOnClickListener(v -> showFormatSelector());
		updateSummary();

		CoordinateFormatSelectorBottomSheet.setupResultListener(getChildFragmentManager(), this,
				new CoordinateFormatSelectorBottomSheet.FormatSelectionListener() {
					@Override
					public void onFormatSelected(@NonNull String formatId) {
						selectedFormatId = formatId;
						applySettings();
						updateSummary();
						updateWidget();
					}

					@Override
					public void onSelectOtherFormat() {
						CoordinatesFormatFragment.showAddFormat(requireActivity(), appMode);
					}
				}, REQUEST_COORDINATES_WIDGET_FORMAT);
	}

	@Override
	public void onResume() {
		super.onResume();
		updateSummary();
	}

	private void showFormatSelector() {
		CoordinateFormat selectedFormat = getSelectedFormat();
		CoordinateFormatSelectorBottomSheet.showInstance(getChildFragmentManager(),
				REQUEST_COORDINATES_WIDGET_FORMAT, appMode, selectedFormat.getId(), true);
	}

	@NonNull
	private CoordinateFormat getSelectedFormat() {
		if (widgetInfo != null && widgetInfo.widget instanceof CoordinatesBaseWidget widget) {
			return selectedFormatId == null || selectedFormatId.isEmpty()
					? CoordinateFormatFormatter.getPrimaryFormat(app, appMode)
					: CoordinateFormatFormatter.resolve(app, selectedFormatId);
		}
		return CoordinateFormatFormatter.getPrimaryFormat(app, appMode);
	}

	private void updateSummary() {
		if (summary != null) {
			summary.setText(getSelectedFormat().getTitle());
		}
	}

	@Override
	protected void applySettings() {
		if (widgetInfo != null && widgetInfo.widget instanceof CoordinatesBaseWidget widget) {
			widget.getCoordinateFormatPref().setModeValue(appMode, selectedFormatId == null ? "" : selectedFormatId);
		}
	}

	private void updateWidget() {
		if (widgetInfo != null && widgetInfo.widget instanceof CoordinatesBaseWidget widget) {
			widget.updateInfo(null);
		}
		MapInfoLayer mapInfoLayer = app.getOsmandMap().getMapLayers().getMapInfoLayer();
		if (mapInfoLayer != null) {
			mapInfoLayer.recreateControls();
		}
	}
}
